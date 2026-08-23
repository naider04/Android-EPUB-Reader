package com.example.data.epub

import android.content.Context
import android.net.Uri
import com.example.data.model.EpubBook
import com.example.data.model.EpubChapter
import com.example.data.model.PageBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class EpubParser(private val context: Context) {

    suspend fun parseFromUri(uri: Uri): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))
            val bytes = inputStream.use { it.readBytes() }
            parseFromBytes(bytes, uri.lastPathSegment ?: "Imported Book", uri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseFromBytes(bytes: ByteArray, defaultTitle: String, uriString: String? = null): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            val entriesMap = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entriesMap[normalizePath(entry.name)] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }

            if (entriesMap.isEmpty()) {
                return@withContext Result.failure(Exception("Invalid EPUB zip file: empty contents"))
            }

            // 1. Locate container.xml
            val containerBytes = entriesMap["META-INF/container.xml"]
                ?: entriesMap.entries.firstOrNull { it.key.endsWith("container.xml") }?.value
                ?: return@withContext Result.failure(Exception("Invalid EPUB: META-INF/container.xml not found"))

            val opfPath = extractOpfPath(containerBytes)
                ?: return@withContext Result.failure(Exception("Cannot find OPF package path in container.xml"))

            val opfBytes = entriesMap[normalizePath(opfPath)]
                ?: entriesMap.entries.firstOrNull { it.key.endsWith(File(opfPath).name) }?.value
                ?: return@withContext Result.failure(Exception("OPF file '$opfPath' not found in EPUB"))

            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

            // 2. Parse OPF metadata, manifest, and spine
            val opfContent = String(opfBytes, Charsets.UTF_8)
            val metadata = parseOpfMetadata(opfContent, defaultTitle)
            val manifest = parseOpfManifest(opfContent)
            val spine = parseOpfSpine(opfContent)

            // Extract cover if available
            var coverFilePath: String? = null
            val coverItemHref = manifest[metadata.coverId] ?: manifest["cover-image"] ?: manifest["cover"]
            if (coverItemHref != null) {
                val fullCoverPath = normalizePath(opfDir + coverItemHref)
                val coverBytes = entriesMap[fullCoverPath]
                    ?: entriesMap.entries.firstOrNull { it.key.endsWith(File(coverItemHref).name) }?.value
                if (coverBytes != null) {
                    try {
                        val coverFile = File(context.cacheDir, "cover_${UUID.randomUUID()}.jpg")
                        FileOutputStream(coverFile).use { it.write(coverBytes) }
                        coverFilePath = coverFile.absolutePath
                    } catch (_: Exception) {}
                }
            }

            // 3. Parse Chapters from Spine
            val chapters = mutableListOf<EpubChapter>()
            var chapterIndex = 1

            for (idref in spine) {
                val href = manifest[idref] ?: continue
                val fullChapterPath = normalizePath(opfDir + href)
                val chapterBytes = entriesMap[fullChapterPath]
                    ?: entriesMap.entries.firstOrNull { it.key.endsWith(File(href).name) }?.value
                    ?: continue

                val chapterHtml = String(chapterBytes, Charsets.UTF_8)
                val (chapterTitle, blocks, plainText) = parseHtmlToBlocks(chapterHtml, "Chapter $chapterIndex")

                if (blocks.isNotEmpty() || plainText.isNotBlank()) {
                    chapters.add(
                        EpubChapter(
                            id = idref,
                            title = chapterTitle,
                            rawHtml = chapterHtml,
                            blocks = blocks,
                            fullPlainText = plainText,
                            href = href
                        )
                    )
                    chapterIndex++
                }
            }

            if (chapters.isEmpty()) {
                return@withContext Result.failure(Exception("No readable chapter content found in EPUB."))
            }

            val book = EpubBook(
                id = UUID.randomUUID().toString(),
                title = metadata.title.ifBlank { defaultTitle },
                author = metadata.author.ifBlank { "Unknown Author" },
                description = metadata.description,
                coverFilePath = coverFilePath,
                chapters = chapters,
                fileUriString = uriString
            )

            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizePath(path: String): String {
        return path.replace('\\', '/').trimStart('/')
    }

    private fun extractOpfPath(containerBytes: ByteArray): String? {
        val xml = String(containerBytes, Charsets.UTF_8)
        val regex = Regex("""<rootfile[^>]+full-path=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = regex.find(xml)
        return match?.groupValues?.get(1)
    }

    private data class OpfMetadata(val title: String, val author: String, val description: String, val coverId: String?)

    private fun parseOpfMetadata(opfXml: String, fallbackTitle: String): OpfMetadata {
        val titleRegex = Regex("""<dc:title[^>]*>([^<]+)</dc:title>""", RegexOption.IGNORE_CASE)
        val creatorRegex = Regex("""<dc:creator[^>]*>([^<]+)</dc:creator>""", RegexOption.IGNORE_CASE)
        val descRegex = Regex("""<dc:description[^>]*>([^<]+)</dc:description>""", RegexOption.IGNORE_CASE)
        val coverMetaRegex = Regex("""<meta[^>]+name=["']cover["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        val title = titleRegex.find(opfXml)?.groupValues?.get(1)?.trim() ?: fallbackTitle
        val author = creatorRegex.find(opfXml)?.groupValues?.get(1)?.trim() ?: "Classic Author"
        val desc = descRegex.find(opfXml)?.groupValues?.get(1)?.trim() ?: "A literary classic in EPUB format."
        val coverId = coverMetaRegex.find(opfXml)?.groupValues?.get(1)?.trim()

        return OpfMetadata(decodeHtmlEntities(title), decodeHtmlEntities(author), decodeHtmlEntities(desc), coverId)
    }

    private fun parseOpfManifest(opfXml: String): Map<String, String> {
        val manifestMap = mutableMapOf<String, String>()
        val itemRegex = Regex("""<item[^>]+id=["']([^"']+)["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val itemRegexAlt = Regex("""<item[^>]+href=["']([^"']+)["'][^>]+id=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        itemRegex.findAll(opfXml).forEach {
            manifestMap[it.groupValues[1]] = it.groupValues[2]
        }
        itemRegexAlt.findAll(opfXml).forEach {
            manifestMap[it.groupValues[2]] = it.groupValues[1]
        }
        return manifestMap
    }

    private fun parseOpfSpine(opfXml: String): List<String> {
        val spineList = mutableListOf<String>()
        val itemrefRegex = Regex("""<itemref[^>]+idref=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        itemrefRegex.findAll(opfXml).forEach {
            spineList.add(it.groupValues[1])
        }
        return spineList
    }

    fun parseHtmlToBlocks(html: String, fallbackTitle: String): Triple<String, List<PageBlock>, String> {
        val blocks = mutableListOf<PageBlock>()
        val plainTextBuilder = StringBuilder()
        var extractedTitle = fallbackTitle

        // Remove script, style, head blocks
        var cleaned = html.replace(Regex("""<head[^>]*>.*?</head>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")

        // Check if there is an <h1>, <h2>, or <title> for the chapter title
        val titleMatch = Regex("""<h[1-2][^>]*>(.*?)</h[1-2]>""", RegexOption.IGNORE_CASE).find(cleaned)
            ?: Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE).find(html)

        if (titleMatch != null) {
            val candidate = stripTags(titleMatch.groupValues[1]).trim()
            if (candidate.isNotBlank() && candidate.length < 100) {
                extractedTitle = decodeHtmlEntities(candidate)
            }
        }

        // Match structured tags: headings, paragraphs, blockquotes, hr
        val tagPattern = Regex("""<(h[1-6]|p|blockquote|hr|div|li)[^>]*>(.*?)</\1>|<hr\s*/?>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val matches = tagPattern.findAll(cleaned).toList()

        if (matches.isNotEmpty()) {
            for (match in matches) {
                val fullTag = match.value
                val tagName = match.groupValues.getOrNull(1)?.lowercase() ?: "hr"
                val innerContent = match.groupValues.getOrNull(2) ?: ""

                if (fullTag.startsWith("<hr", ignoreCase = true)) {
                    blocks.add(PageBlock.Divider())
                    continue
                }

                val text = decodeHtmlEntities(stripTags(innerContent)).trim()
                if (text.isBlank()) continue

                when {
                    tagName.startsWith("h") -> {
                        val level = tagName.removePrefix("h").toIntOrNull() ?: 1
                        blocks.add(PageBlock.Title(text, level))
                        plainTextBuilder.appendLine(text).appendLine()
                    }
                    tagName == "blockquote" -> {
                        blocks.add(PageBlock.Blockquote(text))
                        plainTextBuilder.appendLine("\"$text\"").appendLine()
                    }
                    else -> {
                        val isItalic = innerContent.contains("<em>", ignoreCase = true) || innerContent.contains("<i>", ignoreCase = true)
                        val isBold = innerContent.contains("<strong>", ignoreCase = true) || innerContent.contains("<b>", ignoreCase = true)
                        blocks.add(PageBlock.Paragraph(text, isItalic, isBold))
                        plainTextBuilder.appendLine(text).appendLine()
                    }
                }
            }
        } else {
            // Fallback: split by line breaks or paragraph breaks
            val textOnly = decodeHtmlEntities(stripTags(cleaned))
            val paragraphs = textOnly.split(Regex("""\n\s*\n|\r\n\s*\r\n"""))
            for (p in paragraphs) {
                val t = p.trim()
                if (t.isNotBlank()) {
                    blocks.add(PageBlock.Paragraph(t))
                    plainTextBuilder.appendLine(t).appendLine()
                }
            }
        }

        return Triple(extractedTitle, blocks, plainTextBuilder.toString().trim())
    }

    private fun stripTags(html: String): String {
        return html.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s+"""), " ")
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&rsquo;", "’")
            .replace("&lsquo;", "‘")
            .replace("&rdquo;", "”")
            .replace("&ldquo;", "“")
    }
}

package com.example.data.epub

import android.content.Context
import android.net.Uri
import com.example.data.model.BlockAlignment
import com.example.data.model.EpubBook
import com.example.data.model.EpubChapter
import com.example.data.model.InlineSpan
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

    suspend fun parseFromFile(file: File, bookId: String? = null): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(Exception("File does not exist or is empty"))
            }
            val bytes = file.readBytes()
            parseFromBytes(bytes, file.nameWithoutExtension, file.absolutePath, bookId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseFromUri(uri: Uri, bookId: String? = null): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))
            val bytes = inputStream.use { it.readBytes() }
            parseFromBytes(bytes, uri.lastPathSegment ?: "Imported Book", uri.toString(), bookId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseFromBytes(
        bytes: ByteArray,
        defaultTitle: String,
        uriString: String? = null,
        bookId: String? = null
    ): Result<EpubBook> = withContext(Dispatchers.IO) {
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

            val resolvedBookId = bookId ?: "epub_${UUID.randomUUID().toString().take(12)}"

            // Extract cover if available
            var coverFilePath: String? = null
            val coverItemHref = manifest[metadata.coverId] ?: manifest["cover-image"] ?: manifest["cover"]
            if (coverItemHref != null) {
                val fullCoverPath = normalizePath(opfDir + coverItemHref)
                val coverBytes = entriesMap[fullCoverPath]
                    ?: entriesMap.entries.firstOrNull { it.key.endsWith(File(coverItemHref).name) }?.value
                if (coverBytes != null) {
                    try {
                        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                        val coverFile = File(coversDir, "cover_${resolvedBookId}.jpg")
                        FileOutputStream(coverFile).use { it.write(coverBytes) }
                        coverFilePath = coverFile.absolutePath
                    } catch (_: Exception) {}
                }
            }

            // 3. Parse Chapters from Spine
            val chapters = mutableListOf<EpubChapter>()
            var chapterIndex = 1

            // Internal link resolution: map normalized file paths -> chapter href + fragment
            val chapterHrefsByPath = mutableMapOf<String, String>()
            spine.forEach { idref ->
                val href = manifest[idref] ?: return@forEach
                val full = normalizePath(opfDir + href)
                chapterHrefsByPath[full] = href
                chapterHrefsByPath[File(full).name] = href
            }

            val imagesDir = File(context.filesDir, "book_images").apply { mkdirs() }

            for (idref in spine) {
                val href = manifest[idref] ?: continue
                val fullChapterPath = normalizePath(opfDir + href)
                val chapterBytes = entriesMap[fullChapterPath]
                    ?: entriesMap.entries.firstOrNull { it.key.endsWith(File(href).name) }?.value
                    ?: continue

                val chapterHtml = String(chapterBytes, Charsets.UTF_8)
                val chapterDir = if (fullChapterPath.contains("/")) fullChapterPath.substringBeforeLast("/") + "/" else ""
                val chapterFileName = File(href).name

                fun resolveChapterImage(rawSrc: String): String? {
                    if (rawSrc.isBlank()) return null
                    val cleanSrc = rawSrc.substringBefore("#")
                    var srcPath = if (cleanSrc.startsWith("/")) cleanSrc.trimStart('/') else normalizePath(chapterDir + cleanSrc)
                    srcPath = srcPath.removePrefix("./")
                    var found = entriesMap[srcPath]
                    if (found == null) {
                        val filename = File(srcPath).name
                        val entry = entriesMap.entries.firstOrNull { it.key.endsWith(filename) }
                        if (entry != null) {
                            found = entry.value
                            srcPath = entry.key
                        }
                    }
                    if (found == null) return null
                    return try {
                        val ext = File(srcPath).extension.ifBlank { "jpg" }
                        val out = File(imagesDir, "img_${idref}_${srcPath.hashCode().toUInt().toString(16)}.$ext")
                        if (!out.exists()) FileOutputStream(out).use { it.write(found) }
                        out.absolutePath
                    } catch (_: Exception) { null }
                }

                val (chapterTitle, blocks, plainText) = parseHtmlToBlocks(
                    chapterHtml = chapterHtml,
                    fallbackTitle = "Chapter $chapterIndex",
                    resolveImage = ::resolveChapterImage,
                    chapterHref = chapterFileName
                )

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
                id = resolvedBookId,
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

    fun parseHtmlToBlocks(
        chapterHtml: String,
        fallbackTitle: String,
        resolveImage: (String) -> String? = { null },
        chapterHref: String = ""
    ): Triple<String, List<PageBlock>, String> {
        val blocks = mutableListOf<PageBlock>()
        val plainTextBuilder = StringBuilder()
        var extractedTitle = fallbackTitle

        // Remove script, style, head blocks
        val cleaned = chapterHtml.replace(Regex("""<head[^>]*>.*?</head>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")

        // Check if there is an <h1>, <h2>, or <title> for the chapter title
        val titleMatch = Regex("""<h[1-2][^>]*>(.*?)</h[1-2]>""", RegexOption.IGNORE_CASE).find(cleaned)
            ?: Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE).find(chapterHtml)

        if (titleMatch != null) {
            val candidate = plainTextOf(titleMatch.groupValues[1]).trim()
            if (candidate.isNotBlank() && candidate.length < 100) {
                extractedTitle = decodeHtmlEntities(candidate)
            }
        }

        scanBlockElements(cleaned, blocks, plainTextBuilder, resolveImage, normalizeHref(chapterHref))

        if (blocks.isEmpty() && plainTextBuilder.isBlank()) {
            // Fallback: split by line breaks or paragraph breaks
            val textOnly = decodeHtmlEntities(plainTextOf(cleaned))
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

    private val blockTags = setOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "ul", "ol", "li", "hr", "img", "section", "figure")

    /**
     * Iterates over top-level block elements using an index scanner, so nested tags
     * (especially list items inside <ul>/<ol>) are handled correctly.
     */
    private fun scanBlockElements(
        html: String,
        blocks: MutableList<PageBlock>,
        plainTextBuilder: StringBuilder,
        resolveImage: (String) -> String?,
        chapterHref: String
    ) {
        var i = 0
        val n = html.length
        val startRegex = Regex("""<([a-zA-Z0-9]+)([^>]*)>""", RegexOption.IGNORE_CASE)

        while (i < n) {
            val lt = html.indexOf('<', i)
            if (lt == -1) break
            // skip plain text between blocks
            val gt = html.indexOf('>', lt)
            if (gt == -1) break
            val openMatch = startRegex.find(html, lt)
            if (openMatch == null) { i = lt + 1; continue }
            val tagName = openMatch.groupValues[1].lowercase()
            val attrs = openMatch.groupValues[2]
            val tagStart = openMatch.range.first

            if (tagName !in blockTags) {
                i = openMatch.range.last + 1
                continue
            }

            // handle void elements
            if (tagName == "hr") {
                blocks.add(PageBlock.Divider())
                i = gt + 1
                continue
            }
            if (tagName == "img") {
                val img = buildImageBlock(attrs, resolveImage)
                if (img != null) {
                    blocks.add(img)
                    plainTextBuilder.appendLine("[Image: ${img.alt}]")
                }
                i = gt + 1
                continue
            }

            // find matching closing tag index (accounting for nesting)
            val closeIdx = findClosingTag(html, openMatch.range.last + 1, tagName)
            if (closeIdx == -1) { i = gt + 1; continue }
            val innerContent = html.substring(openMatch.range.last + 1, closeIdx)

            when {
                tagName.startsWith("h") -> {
                    val level = tagName.removePrefix("h").toIntOrNull() ?: 1
                    val text = plainTextOf(innerContent).trim()
                    if (text.isNotBlank()) {
                        blocks.add(PageBlock.Title(text, level))
                        plainTextBuilder.appendLine(text).appendLine()
                    }
                }
                tagName == "blockquote" -> {
                    val spans = parseInlineSpans(innerContent, chapterHref)
                    val text = spans.joinToString("") { it.text }.trim()
                    if (text.isNotBlank()) {
                        blocks.add(PageBlock.Blockquote(spans))
                        plainTextBuilder.appendLine("\"$text\"").appendLine()
                    }
                }
                tagName == "ul" || tagName == "ol" -> {
                    scanListItems(html, openMatch.range.last + 1, closeIdx, tagName, blocks, plainTextBuilder, resolveImage, chapterHref)
                }
                tagName == "li" -> {
                    val spans = parseInlineSpans(innerContent, chapterHref)
                    val text = spans.joinToString("") { it.text }.trim()
                    if (text.isNotBlank()) {
                        blocks.add(PageBlock.ListItem(spans, marker = "•", indentLevel = 1))
                        plainTextBuilder.appendLine("• $text").appendLine()
                    }
                }
                tagName == "figure" -> {
                    // figure: try to extract any <img> inside and fall back to paragraph text
                    val innerImg = Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE).find(innerContent)
                    if (innerImg != null) {
                        val attrs2 = Regex("""<img([^>]*)>""", RegexOption.IGNORE_CASE).find(innerImg.value)?.groupValues?.get(1) ?: ""
                        val img = buildImageBlock(attrs2, resolveImage)
                        if (img != null) {
                            blocks.add(img)
                            plainTextBuilder.appendLine("[Image: ${img.alt}]")
                        }
                    } else {
                        val spans = parseInlineSpans(innerContent, chapterHref)
                        val text = spans.joinToString("") { it.text }.trim()
                        if (text.isNotBlank()) {
                            blocks.add(PageBlock.Paragraph(spans, detectAlignment(attrs)))
                            plainTextBuilder.appendLine(text).appendLine()
                        }
                    }
                }
                else -> { // p, div, section
                    val spans = parseInlineSpans(innerContent, chapterHref)
                    val text = spans.joinToString("") { it.text }.trim()
                    if (text.isBlank()) { i = closeIdx + 1; continue }
                    blocks.add(PageBlock.Paragraph(spans, detectAlignment(attrs)))
                    plainTextBuilder.appendLine(text).appendLine()
                }
            }

            i = closeIdx + 1
        }
    }

    private fun scanListItems(
        html: String,
        start: Int,
        end: Int,
        listTag: String,
        blocks: MutableList<PageBlock>,
        plainTextBuilder: StringBuilder,
        resolveImage: (String) -> String?,
        chapterHref: String
    ) {
        val ordered = listTag == "ol"
        var counter = 0
        var j = start
        while (j < end) {
            val open = Regex("""<li([^>]*)>""", RegexOption.IGNORE_CASE).find(html, j)
            if (open == null || open.range.first >= end) break
            val closeIdx = findClosingTag(html, open.range.last + 1, "li") ?: run {
                j = open.range.last + 1
                continue
            }
            if (closeIdx > end) break
            val innerContent = html.substring(open.range.last + 1, closeIdx)
            // handle nested img
            val innerImg = Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE).find(innerContent)
            if (innerImg != null && innerContent.trim().length - innerImg.value.length < 20) {
                val attrs2 = Regex("""<img([^>]*)>""", RegexOption.IGNORE_CASE).find(innerImg.value)?.groupValues?.get(1) ?: ""
                val img = buildImageBlock(attrs2, resolveImage)
                if (img != null) {
                    blocks.add(img)
                    plainTextBuilder.appendLine("[Image: ${img.alt}]")
                }
            } else {
                counter++
                val marker = if (ordered) "$counter." else "•"
                val spans = parseInlineSpans(innerContent, chapterHref)
                val text = spans.joinToString("") { it.text }.trim()
                if (text.isNotBlank()) {
                    blocks.add(PageBlock.ListItem(spans, marker = marker, indentLevel = 1))
                    plainTextBuilder.appendLine("$marker $text").appendLine()
                }
            }
            j = closeIdx + 1
        }
    }

    /**
     * Finds the index of the closing tag for [tagName] starting from [from].
     * Returns the index of the '<' of the closing tag, or -1 if not found.
     */
    private fun findClosingTag(html: String, from: Int, tagName: String): Int {
        val closeRegex = Regex("""</$tagName\s*>""", RegexOption.IGNORE_CASE)
        val openRegex = Regex("""<$tagName[\s>]""", RegexOption.IGNORE_CASE)
        var depth = 0
        var pos = from
        while (true) {
            val closeIdx = closeRegex.find(html, pos)?.range
            val openIdx = openRegex.find(html, pos)?.range
            when {
                openIdx != null && (closeIdx == null || openIdx.first < closeIdx.first) -> {
                    depth++
                    pos = openIdx.last + 1
                }
                closeIdx != null -> {
                    if (depth == 0) return closeIdx.first
                    depth--
                    pos = closeIdx.last + 1
                }
                else -> return -1
            }
        }
    }

    private fun buildImageBlock(attrs: String, resolveImage: (String) -> String?): PageBlock.Image? {
        val srcMatch = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
        val altMatch = Regex("""alt=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)
        val src = srcMatch?.groupValues?.get(1) ?: return null
        val alt = altMatch?.groupValues?.get(1) ?: "Illustration"
        val path = resolveImage(src)
        return PageBlock.Image(alt = alt.ifBlank { "Illustration" }, path = path)
    }

    private fun detectAlignment(attrs: String): BlockAlignment {
        val alignMatch = Regex("""align=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
        alignMatch?.let {
            return when (it.groupValues[1].lowercase()) {
                "center" -> BlockAlignment.CENTER
                "right" -> BlockAlignment.RIGHT
                "left" -> BlockAlignment.LEFT
                "justify" -> BlockAlignment.JUSTIFY
                else -> BlockAlignment.LEFT
            }
        }
        val centerMatch = Regex("""text-align\s*:\s*(center|right|left|justify)""", RegexOption.IGNORE_CASE).find(attrs)
        centerMatch?.let {
            return when (it.groupValues[1].lowercase()) {
                "center" -> BlockAlignment.CENTER
                "right" -> BlockAlignment.RIGHT
                "left" -> BlockAlignment.LEFT
                "justify" -> BlockAlignment.JUSTIFY
                else -> BlockAlignment.LEFT
            }
        }
        return BlockAlignment.JUSTIFY
    }

    private fun normalizeHref(chapterHref: String): String {
        val base = if (chapterHref.contains("/")) chapterHref.substringAfterLast("/") else chapterHref
        return base
    }

    /**
     * Lightweight inline tokenizer that produces formatted [InlineSpan]s from the inner HTML.
     * Handles <b>/<strong>, <i>/<em>, <u>, <sup>/<sub> (rendered inline), and <a href>.
     */
    private fun parseInlineSpans(innerHtml: String, chapterHref: String): List<InlineSpan> {
        val spans = mutableListOf<InlineSpan>()
        val stack = ArrayDeque<InlineStyle>()
        val text = StringBuilder()
        var linkTarget: String? = null

        fun flush() {
            if (text.isBlank()) return
            val hasBold = stack.any { it.bold }
            val hasItalic = stack.any { it.italic }
            val hasUnderline = stack.any { it.underline }
            spans.add(
                InlineSpan(
                    text = decodeHtmlEntities(text.toString()),
                    bold = hasBold,
                    italic = hasItalic,
                    underline = hasUnderline,
                    linkTarget = linkTarget
                )
            )
            text.clear()
        }

        var i = 0
        while (i < innerHtml.length) {
            val c = innerHtml[i]
            if (c == '<') {
                val closeIdx = innerHtml.indexOf('>', i)
                if (closeIdx == -1) { text.append(innerHtml.substring(i)); break }
                val tagContent = innerHtml.substring(i + 1, closeIdx)
                i = closeIdx + 1

                if (tagContent.startsWith("/")) {
                    val name = tagContent.substring(1).trim().lowercase()
                    if (name == "a") linkTarget = null
                    // pop matching style
                    val idx = stack.indexOfLast { it.tag == name }
                    if (idx >= 0) {
                        // re-evaluate link before popping? flush handles current state
                        flush()
                        stack.removeAt(idx)
                    }
                    continue
                }

                val tagMatch = Regex("""([a-zA-Z0-9]+)([^>]*)""").find(tagContent)
                val name = tagMatch?.groupValues?.get(1)?.lowercase() ?: ""
                val attrs = tagMatch?.groupValues?.get(2) ?: ""

                if (name == "br") { flush(); text.append('\n'); continue }
                if (name == "img") {
                    // Inline images are not representable inside a text span; the block-level
                    // scanner extracts standalone figures, so we skip these gracefully.
                    continue
                }
                if (name == "sup" || name == "sub") {
                    // keep the content flowing inline (superscript markers are lost in plain text pagination,
                    // but preserved via linkTarget if it is a footnote link)
                    continue
                }

                val style = InlineStyle(
                    tag = name,
                    bold = name == "b" || name == "strong",
                    italic = name == "i" || name == "em",
                    underline = name == "u" || name == "ins"
                )
                if (name == "a") {
                    val href = Regex("""href=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)
                    if (href != null) linkTarget = href.trim()
                }
                if (name == "b" || name == "strong" || name == "i" || name == "em" || name == "u" || name == "ins" || name == "a") {
                    stack.addLast(style)
                }
            } else {
                text.append(c)
                i++
            }
        }
        flush()

        val merged = mutableListOf<InlineSpan>()
        for (s in spans) {
            if (merged.isNotEmpty()) {
                val last = merged.last()
                if (last.bold == s.bold && last.italic == s.italic && last.underline == s.underline && last.linkTarget == s.linkTarget) {
                    merged[merged.size - 1] = last.copy(text = last.text + s.text)
                    continue
                }
            }
            merged.add(s)
        }
        return merged.filter { it.text.isNotBlank() }
    }

    private data class InlineStyle(
        val tag: String,
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean
    )

    private fun plainTextOf(html: String): String {
        var out = html
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
        return out.replace(Regex("""\s+"""), " ")
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

package com.example.data.epub

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubMetadata(
    val title: String,
    val author: String,
    val description: String,
    val coverFilePath: String?
)

/**
 * Lightweight EPUB metadata extraction (title / author / description / cover).
 * This replaces the old full content parser: the actual page rendering and
 * chapter content is handled by the embedded foliate-js engine in a WebView,
 * so Kotlin only needs enough to populate the library.
 */
class EpubMetadataExtractor(private val context: Context) {

    fun extract(bytes: ByteArray, fallbackTitle: String, bookId: String): EpubMetadata {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[normalize(entry.name)] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }

        val containerBytes = entries["META-INF/container.xml"]
            ?: entries.entries.firstOrNull { it.key.endsWith("container.xml") }?.value
            ?: return EpubMetadata(fallbackTitle, "Unknown Author", "", null)

        val opfPath = extractOpfPath(String(containerBytes, Charsets.UTF_8))
            ?: return EpubMetadata(fallbackTitle, "Unknown Author", "", null)

        val opfBytes = entries[normalize(opfPath)]
            ?: entries.entries.firstOrNull { it.key.endsWith(File(opfPath).name) }?.value
            ?: return EpubMetadata(fallbackTitle, "Unknown Author", "", null)

        val opf = String(opfBytes, Charsets.UTF_8)
        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

        val title = decode(findGroup(opf, """<dc:title[^>]*>([^<]+)</dc:title>""").ifBlank { fallbackTitle })
        val author = decode(findGroup(opf, """<dc:creator[^>]*>([^<]+)</dc:creator>""").ifBlank { "Unknown Author" })
        val description = decode(findGroup(opf, """<dc:description[^>]*>([^<]+)</dc:description>"""))

        val coverId = findGroup(opf, """<meta[^>]+name=["']cover["'][^>]+content=["']([^"']+)["']""")
        val manifest = parseManifest(opf)
        val coverHref = manifest[coverId] ?: manifest["cover-image"] ?: manifest["cover"]

        var coverFilePath: String? = null
        if (coverHref != null) {
            val fullPath = normalize(opfDir + coverHref)
            val coverBytes = entries[fullPath]
                ?: entries.entries.firstOrNull { it.key.endsWith(File(coverHref).name) }?.value
            if (coverBytes != null) {
                try {
                    val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                    val coverFile = File(coversDir, "cover_$bookId.jpg")
                    FileOutputStream(coverFile).use { it.write(coverBytes) }
                    coverFilePath = coverFile.absolutePath
                } catch (_: Exception) {
                }
            }
        }

        return EpubMetadata(title, author, description, coverFilePath)
    }

    private fun parseManifest(opf: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regexA = Regex("""<item[^>]+id=["']([^"']+)["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val regexB = Regex("""<item[^>]+href=["']([^"']+)["'][^>]+id=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        regexA.findAll(opf).forEach { map[it.groupValues[1]] = it.groupValues[2] }
        regexB.findAll(opf).forEach { map[it.groupValues[2]] = it.groupValues[1] }
        return map
    }

    private fun extractOpfPath(containerXml: String): String? =
        Regex("""<rootfile[^>]+full-path=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(containerXml)?.groupValues?.get(1)

    private fun findGroup(xml: String, regex: String): String =
        Regex(regex, RegexOption.IGNORE_CASE).find(xml)?.groupValues?.get(1)?.trim() ?: ""

    private fun normalize(path: String) = path.replace('\\', '/').trimStart('/')

    private fun decode(text: String): String = text
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

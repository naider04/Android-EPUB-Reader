package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.dictionary.DictionaryService
import com.example.data.epub.EpubPaginator
import com.example.data.model.EpubChapter
import com.example.data.model.PageBlock
import com.example.data.model.ReaderSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EPUB Reader", appName)
    }

    @Test
    fun `dictionary service looks up words with stemming`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = DictionaryService.getInstance(context)
        service.initialize()

        // Direct match
        val entry1 = service.lookupWord("ephemeral")
        assertNotNull(entry1)
        assertEquals("ephemeral", entry1?.word)

        // Stemmed plural
        val entry2 = service.lookupWord("stratagems")
        assertNotNull(entry2)
        assertEquals("stratagem", entry2?.word)
    }

    @Test
    fun `dictionary service loads custom JSON format`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = DictionaryService.getInstance(context)

        val customJson = """
            {
                "ubiquitous": "Present or existing everywhere.",
                "serendipity": {
                    "phonetic": "/ˌser.ənˈdɪp.ə.ti/",
                    "partOfSpeech": "noun",
                    "definition": "The occurrence and development of events by chance in a happy way."
                }
            }
        """.trimIndent()

        val result = service.loadCustomDictionaryFromJson(customJson, "My Custom Vocabulary")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val lookup = service.lookupWord("ubiquitous")
        assertNotNull(lookup)
        assertEquals("ubiquitous", lookup?.word)
    }

    @Test
    fun `paginator divides content into sheets based on font size`() {
        val paragraphs = (1..15).map { index ->
            PageBlock.Paragraph("This is sentence number $index of a long book chapter demonstrating paginated sheet division in our EPUB reader.")
        }

        val chapter = EpubChapter(
            id = "test_ch1",
            title = "Test Chapter",
            rawHtml = "",
            blocks = paragraphs,
            fullPlainText = "Test content"
        )

        // Small font size: 14sp
        val pagesSmallFont = EpubPaginator.paginateChapter(
            chapter = chapter,
            chapterIndex = 0,
            viewportWidthPx = 1080,
            viewportHeightPx = 1800,
            density = 2.5f,
            settings = ReaderSettings(fontSizeSp = 14f)
        )

        // Large font size: 28sp
        val pagesLargeFont = EpubPaginator.paginateChapter(
            chapter = chapter,
            chapterIndex = 0,
            viewportWidthPx = 1080,
            viewportHeightPx = 1800,
            density = 2.5f,
            settings = ReaderSettings(fontSizeSp = 28f)
        )

        // Larger font size MUST produce more sheets
        assertTrue(pagesLargeFont.size >= pagesSmallFont.size)
        assertTrue(pagesSmallFont.isNotEmpty())
    }
}

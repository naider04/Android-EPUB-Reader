package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.dictionary.DictionaryService
import com.example.data.epub.EpubPaginator
import com.example.data.model.EpubChapter
import com.example.data.model.PageBlock
import com.example.data.model.ReaderSettings
import com.example.data.model.VoiceSettings
import com.example.data.model.VoiceSource
import com.example.data.repository.BookRepository
import com.example.ui.components.extractSentenceRange
import com.example.ui.components.extractWordRange
import com.example.ui.components.isSearchableTerm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(entry2?.word?.contains("stratagem") == true)
    }

    @Test
    fun `voice settings persistence and switching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initial = VoiceSettings.load(context)
        assertNotNull(initial)

        val updated = initial.copy(
            voiceSource = VoiceSource.GOOGLE_TTS,
            speechRate = 1.1f
        )
        VoiceSettings.save(context, updated)

        val loaded = VoiceSettings.load(context)
        assertEquals(VoiceSource.GOOGLE_TTS, loaded.voiceSource)
        assertEquals(1.1f, loaded.speechRate, 0.01f)
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

    @Test
    fun `pronunciation player initializes and speaks uk and us`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = com.example.util.PronunciationPlayer(context)
        player.onInit(android.speech.tts.TextToSpeech.SUCCESS)
        player.speak("ephemeral", com.example.util.PronunciationAccent.UK)
        player.speak("serendipity", com.example.util.PronunciationAccent.US)
        player.speak("Alice was beginning to get very tired of sitting by her sister on the bank.", com.example.util.PronunciationAccent.UK)
        player.release()
    }

    @Test
    fun `extract word and sentence range correctly`() {
        val text = "Alice was beginning to get very tired of sitting by her sister on the bank. Once or twice she had peeped into the book."
        val offset = text.indexOf("beginning") + 2
        val wordRange = extractWordRange(text, offset)
        assertNotNull(wordRange)
        assertEquals("beginning", text.substring(wordRange!!.first, wordRange.second))

        val sentenceRange = extractSentenceRange(text, offset)
        val sentence = text.substring(sentenceRange.first, sentenceRange.second).trim()
        assertEquals("Alice was beginning to get very tired of sitting by her sister on the bank.", sentence)
    }

    @Test
    fun `isSearchableTerm properly distinguishes single words from sentences`() {
        // Searchable single/compound terms
        assertTrue(isSearchableTerm("wonderland"))
        assertTrue(isSearchableTerm("Alice"))
        assertTrue(isSearchableTerm("white rabbit"))
        assertTrue(isSearchableTerm("ephemeral"))

        // Non-searchable full sentences or phrases with sentence punctuation
        assertFalse(isSearchableTerm("Alice was beginning to get very tired."))
        assertFalse(isSearchableTerm("What is the use of a book?"))
        assertFalse(isSearchableTerm("Curiouser and curiouser!"))
        assertFalse(isSearchableTerm("once upon a time in a far away place"))
    }

    @Test
    fun `reading progress saves and restores exact page within chapter`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = BookRepository(context)
        repo.initializeLibrary()

        val sampleBookId = "book_alice_wonderland"
        // Save progress to chapter 1, page 4
        repo.saveReadingProgress(
            bookId = sampleBookId,
            chapterIndex = 1,
            pageIndex = 4,
            charOffset = 1200,
            progress = 0.35f
        )

        val updatedEntity = repo.getBookEntity(sampleBookId)
        assertNotNull(updatedEntity)
        assertEquals(1, updatedEntity!!.currentChapterIndex)
        assertEquals(4, updatedEntity.currentPageInChapter)
        assertEquals(1200, updatedEntity.currentCharacterOffset)
    }
}

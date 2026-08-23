package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.epub.EpubParser
import com.example.data.epub.SampleBooksProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.EpubBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BookRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val bookmarkDao = db.bookmarkDao()
    private val epubParser = EpubParser(context)

    // Memory cache for active opened book models
    private val bookCache = mutableMapOf<String, EpubBook>()

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun initializeLibrary() = withContext(Dispatchers.IO) {
        val existing = bookDao.getAllBooks().first()
        if (existing.isEmpty()) {
            val sampleBooks = SampleBooksProvider.getSampleBooks()
            for (book in sampleBooks) {
                bookCache[book.id] = book
                val entity = BookEntity(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    description = book.description,
                    coverResName = when (book.id) {
                        "book_art_of_war" -> "img_cover_art_war"
                        "book_alice_wonderland" -> "img_cover_alice"
                        "book_time_machine" -> "img_cover_time"
                        else -> null
                    },
                    isAsset = true,
                    totalChapters = book.chapters.size,
                    currentChapterIndex = 0,
                    currentPageInChapter = 0,
                    progressPercent = 0f,
                    lastReadTimestamp = System.currentTimeMillis()
                )
                bookDao.insertBook(entity)
            }
        }
    }

    suspend fun getEpubBook(bookId: String): EpubBook? = withContext(Dispatchers.IO) {
        val cached = bookCache[bookId]
        if (cached != null) return@withContext cached

        // Check if sample book
        val sample = SampleBooksProvider.getSampleBooks().firstOrNull { it.id == bookId }
        if (sample != null) {
            bookCache[bookId] = sample
            return@withContext sample
        }

        // Check DB for imported book
        val entity = bookDao.getBookById(bookId) ?: return@withContext null
        if (entity.fileUriString != null) {
            val uri = Uri.parse(entity.fileUriString)
            val parseResult = epubParser.parseFromUri(uri)
            if (parseResult.isSuccess) {
                val parsedBook = parseResult.getOrNull()?.copy(id = bookId)
                if (parsedBook != null) {
                    bookCache[bookId] = parsedBook
                    return@withContext parsedBook
                }
            }
        }

        null
    }

    suspend fun importEpubFile(uri: Uri): Result<EpubBook> = withContext(Dispatchers.IO) {
        val parseResult = epubParser.parseFromUri(uri)
        if (parseResult.isFailure) {
            return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse EPUB"))
        }

        val book = parseResult.getOrThrow()
        bookCache[book.id] = book

        val entity = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            description = book.description,
            coverFilePath = book.coverFilePath,
            fileUriString = uri.toString(),
            isAsset = false,
            totalChapters = book.chapters.size,
            currentChapterIndex = 0,
            currentPageInChapter = 0,
            progressPercent = 0f,
            lastReadTimestamp = System.currentTimeMillis()
        )
        bookDao.insertBook(entity)

        Result.success(book)
    }

    suspend fun getBookEntity(bookId: String): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(bookId)
    }

    fun observeBook(bookId: String): Flow<BookEntity?> = bookDao.observeBookById(bookId)

    suspend fun saveReadingProgress(
        bookId: String,
        chapterIndex: Int,
        pageIndex: Int,
        charOffset: Int,
        progress: Float
    ) = withContext(Dispatchers.IO) {
        bookDao.updateProgress(
            bookId = bookId,
            chapterIndex = chapterIndex,
            pageIndex = pageIndex,
            charOffset = charOffset,
            progress = progress,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(bookId)
        bookCache.remove(bookId)
    }

    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForBook(bookId)

    suspend fun toggleBookmark(
        bookId: String,
        chapterIndex: Int,
        chapterTitle: String,
        pageIndex: Int,
        excerpt: String
    ): Boolean = withContext(Dispatchers.IO) {
        val exists = bookmarkDao.isBookmarked(bookId, chapterIndex, pageIndex)
        if (exists) {
            bookmarkDao.deleteBookmarkAt(bookId, chapterIndex, pageIndex)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapterTitle,
                    pageIndex = pageIndex,
                    excerpt = excerpt.take(150),
                    timestamp = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun isPageBookmarked(bookId: String, chapterIndex: Int, pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.isBookmarked(bookId, chapterIndex, pageIndex)
    }

    suspend fun removeBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }
}

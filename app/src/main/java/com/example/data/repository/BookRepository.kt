package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.epub.EpubMetadataExtractor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class BookRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val bookmarkDao = db.bookmarkDao()
    private val metadataExtractor = EpubMetadataExtractor(context)

    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun initializeLibrary() = withContext(Dispatchers.IO) {
        // The library is populated by importing EPUB files; nothing to seed here.
    }

    /** Returns the persisted .epub file bytes source for a book, if available. */
    suspend fun getEpubFile(bookId: String): File? = withContext(Dispatchers.IO) {
        val file = File(booksDir, "$bookId.epub")
        if (file.exists() && file.length() > 0L) return@withContext file
        val entity = bookDao.getBookById(bookId)
        val uriString = entity?.fileUriString
        if (uriString != null) {
            val direct = File(uriString)
            if (direct.exists() && direct.length() > 0L) return@withContext direct
        }
        null
    }

    suspend fun getBookEntity(bookId: String): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(bookId)
    }

    fun observeBook(bookId: String): Flow<BookEntity?> = bookDao.observeBookById(bookId)

    suspend fun importEpubFile(uri: Uri): Result<BookEntity> = withContext(Dispatchers.IO) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream for selected EPUB"))
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.isEmpty()) {
                return@withContext Result.failure(Exception("Selected file is empty"))
            }

            val bookId = "epub_" + java.util.UUID.randomUUID().toString().take(12)
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('?') ?: "Imported Book"

            val metadata = metadataExtractor.extract(bytes, fileName, bookId)

            // Reuse an existing import of the same title/author to preserve progress.
            val existing = bookDao.getAllBooks().first().find {
                it.title.equals(metadata.title, ignoreCase = true) &&
                    (it.author.equals(metadata.author, ignoreCase = true) || metadata.author.isBlank())
            }
            if (existing != null) {
                return@withContext Result.success(existing)
            }

            val persistentFile = File(booksDir, "$bookId.epub")
            persistentFile.writeBytes(bytes)

            val entity = BookEntity(
                id = bookId,
                title = metadata.title,
                author = metadata.author,
                description = metadata.description,
                coverFilePath = metadata.coverFilePath,
                fileUriString = persistentFile.absolutePath,
                isAsset = false,
                totalChapters = 0,
                currentChapterIndex = 0,
                currentPageInChapter = 0,
                progressPercent = 0f,
                lastReadTimestamp = System.currentTimeMillis()
            )
            bookDao.insertBook(entity)

            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveReadingProgress(
        bookId: String,
        fraction: Float,
        cfi: String
    ) = withContext(Dispatchers.IO) {
        bookDao.updateProgress(
            bookId = bookId,
            chapterIndex = 0,
            pageIndex = 0,
            charOffset = 0,
            cfi = cfi,
            progress = fraction.coerceIn(0f, 1f),
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(bookId)
        try {
            File(booksDir, "$bookId.epub").delete()
            File(context.filesDir, "covers/cover_$bookId.jpg").delete()
        } catch (_: Exception) {
        }
    }

    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForBook(bookId)

    suspend fun toggleBookmark(
        bookId: String,
        chapterTitle: String,
        cfi: String,
        excerpt: String
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarksForBook(bookId).first()
            .firstOrNull { it.cfi.isNotBlank() && it.cfi == cfi }
        if (existing != null) {
            bookmarkDao.deleteBookmark(existing.id)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    chapterIndex = 0,
                    chapterTitle = chapterTitle,
                    pageIndex = 0,
                    excerpt = excerpt.take(150),
                    cfi = cfi,
                    timestamp = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun isBookmarked(bookId: String, cfi: String): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.getBookmarksForBook(bookId).first()
            .any { it.cfi.isNotBlank() && it.cfi == cfi }
    }

    suspend fun removeBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }

    suspend fun saveQuote(
        bookId: String,
        chapterTitle: String,
        quote: String,
        cfi: String
    ): Long = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                bookId = bookId,
                chapterIndex = 0,
                chapterTitle = chapterTitle,
                pageIndex = 0,
                excerpt = quote.trim(),
                note = "Quote",
                cfi = cfi,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

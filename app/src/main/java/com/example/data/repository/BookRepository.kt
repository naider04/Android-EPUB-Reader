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
        val sampleBooks = SampleBooksProvider.getSampleBooks()
        if (existing.isEmpty()) {
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
        } else {
            for (book in sampleBooks) {
                bookCache[book.id] = book
            }
        }
    }

    suspend fun getEpubBook(bookId: String): EpubBook? = withContext(Dispatchers.IO) {
        val cached = bookCache[bookId]
        if (cached != null) return@withContext cached

        // 1. Check if sample book by ID
        val sample = SampleBooksProvider.getSampleBooks().firstOrNull {
            it.id == bookId || it.id.equals(bookId, ignoreCase = true)
        }
        if (sample != null) {
            bookCache[bookId] = sample
            return@withContext sample
        }

        // 2. Check DB for entity
        val entity = bookDao.getBookById(bookId)

        // If entity is marked as asset or title matches sample book, return sample
        if (entity != null && entity.isAsset) {
            val sampleBook = SampleBooksProvider.getSampleBooks().firstOrNull {
                it.id == entity.id || it.title.equals(entity.title, ignoreCase = true)
            }
            if (sampleBook != null) {
                bookCache[bookId] = sampleBook
                return@withContext sampleBook
            }
        }

        // 3. Try to load from internal persistent storage
        val localPersistentFile = java.io.File(context.filesDir, "books/$bookId.epub")
        if (localPersistentFile.exists() && localPersistentFile.length() > 0L) {
            val parseResult = epubParser.parseFromFile(localPersistentFile, bookId)
            if (parseResult.isSuccess) {
                val parsedBook = parseResult.getOrNull()
                if (parsedBook != null) {
                    bookCache[bookId] = parsedBook
                    return@withContext parsedBook
                }
            }
        }

        // 4. Try from entity.fileUriString (either direct file path or URI)
        if (entity?.fileUriString != null) {
            val pathOrUri = entity.fileUriString
            val directFile = java.io.File(pathOrUri)
            if (directFile.exists() && directFile.length() > 0L) {
                val parseResult = epubParser.parseFromFile(directFile, bookId)
                if (parseResult.isSuccess) {
                    val parsedBook = parseResult.getOrNull()
                    if (parsedBook != null) {
                        bookCache[bookId] = parsedBook
                        return@withContext parsedBook
                    }
                }
            }

            // Try URI parsing
            try {
                val uri = Uri.parse(pathOrUri)
                val parseResult = epubParser.parseFromUri(uri, bookId)
                if (parseResult.isSuccess) {
                    val parsedBook = parseResult.getOrNull()
                    if (parsedBook != null) {
                        // Persist to internal storage so future launches never fail
                        try {
                            val booksDir = java.io.File(context.filesDir, "books").apply { mkdirs() }
                            val persistFile = java.io.File(booksDir, "$bookId.epub")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                java.io.FileOutputStream(persistFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            bookDao.insertBook(entity.copy(fileUriString = persistFile.absolutePath))
                        } catch (_: Exception) {}

                        bookCache[bookId] = parsedBook
                        return@withContext parsedBook
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. Fallback: match by title across sample books
        if (entity != null) {
            val fallbackSample = SampleBooksProvider.getSampleBooks().firstOrNull {
                it.title.equals(entity.title, ignoreCase = true)
            }
            if (fallbackSample != null) {
                bookCache[bookId] = fallbackSample
                return@withContext fallbackSample
            }
        }

        null
    }

    suspend fun importEpubFile(uri: Uri): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            // Try to take persistable URI permission if available
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream for selected EPUB"))
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.isEmpty()) {
                return@withContext Result.failure(Exception("Selected file is empty"))
            }

            val bookId = "epub_" + java.util.UUID.randomUUID().toString().take(12)

            // Save permanently into internal app storage
            val booksDir = java.io.File(context.filesDir, "books").apply { mkdirs() }
            val persistentFile = java.io.File(booksDir, "$bookId.epub")
            persistentFile.writeBytes(bytes)

            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('?') ?: "Imported Book"
            val parseResult = epubParser.parseFromBytes(
                bytes = bytes,
                defaultTitle = fileName,
                uriString = persistentFile.absolutePath,
                bookId = bookId
            )

            if (parseResult.isFailure) {
                persistentFile.delete()
                return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse EPUB"))
            }

            val book = parseResult.getOrThrow()

            // If this book was already imported, reuse the existing one to preserve progress and bookmarks
            val existingBooks = bookDao.getAllBooks().first()
            val existing = existingBooks.find {
                it.title.equals(book.title, ignoreCase = true) &&
                (it.author.equals(book.author, ignoreCase = true) || book.author.isBlank())
            }
            if (existing != null) {
                persistentFile.delete()
                val existingBook = getEpubBook(existing.id)
                if (existingBook != null) {
                    return@withContext Result.success(existingBook)
                }
            }

            bookCache[book.id] = book

            val entity = BookEntity(
                id = book.id,
                title = book.title,
                author = book.author,
                description = book.description,
                coverFilePath = book.coverFilePath,
                fileUriString = persistentFile.absolutePath,
                isAsset = false,
                totalChapters = book.chapters.size,
                currentChapterIndex = 0,
                currentPageInChapter = 0,
                progressPercent = 0f,
                lastReadTimestamp = System.currentTimeMillis()
            )
            bookDao.insertBook(entity)

            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        try {
            java.io.File(context.filesDir, "books/$bookId.epub").delete()
            java.io.File(context.filesDir, "covers/cover_$bookId.jpg").delete()
        } catch (_: Exception) {}
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

    suspend fun saveQuote(
        bookId: String,
        chapterIndex: Int,
        chapterTitle: String,
        pageIndex: Int,
        quote: String
    ): Long = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                chapterTitle = chapterTitle,
                pageIndex = pageIndex,
                excerpt = quote.trim(),
                note = "Quote",
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

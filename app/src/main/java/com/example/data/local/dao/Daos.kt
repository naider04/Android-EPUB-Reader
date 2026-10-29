package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.CustomDictionaryEntity
import com.example.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun observeBookById(id: String): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET currentChapterIndex = :chapterIndex, currentPageInChapter = :pageIndex, currentCharacterOffset = :charOffset, currentCfi = :cfi, progressPercent = :progress, lastReadTimestamp = :timestamp WHERE id = :bookId")
    suspend fun updateProgress(bookId: String, chapterIndex: Int, pageIndex: Int, charOffset: Int, cfi: String, progress: Float, timestamp: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND chapterIndex = :chapterIndex AND pageIndex = :pageIndex")
    suspend fun deleteBookmarkAt(bookId: String, chapterIndex: Int, pageIndex: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE bookId = :bookId AND chapterIndex = :chapterIndex AND pageIndex = :pageIndex)")
    suspend fun isBookmarked(bookId: String, chapterIndex: Int, pageIndex: Int): Boolean
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY timestamp DESC")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE word = :word COLLATE NOCASE LIMIT 1")
    suspend fun getVocabularyByWord(word: String): VocabularyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(item: VocabularyEntity)

    @Query("UPDATE vocabulary SET isLearned = :learned WHERE id = :id")
    suspend fun setLearnedStatus(id: Long, learned: Boolean)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteVocabulary(id: Long)

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getVocabularyCount(): Flow<Int>
}

@Dao
interface CustomDictionaryDao {
    @Query("SELECT * FROM custom_dictionaries ORDER BY addedTimestamp DESC")
    fun getAllDictionaries(): Flow<List<CustomDictionaryEntity>>

    @Query("SELECT * FROM custom_dictionaries WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveDictionary(): CustomDictionaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionary(dict: CustomDictionaryEntity)

    @Query("UPDATE custom_dictionaries SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE custom_dictionaries SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("DELETE FROM custom_dictionaries WHERE id = :id")
    suspend fun deleteDictionary(id: String)
}

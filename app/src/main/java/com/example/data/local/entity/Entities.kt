package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverResName: String? = null,
    val coverFilePath: String? = null,
    val fileUriString: String? = null,
    val isAsset: Boolean = false,
    val assetPath: String? = null,
    val totalChapters: Int = 0,
    val currentChapterIndex: Int = 0,
    val currentPageInChapter: Int = 0,
    val currentCharacterOffset: Int = 0,
    val progressPercent: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val pageIndex: Int,
    val excerpt: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val phonetic: String? = null,
    val partOfSpeech: String = "",
    val definition: String,
    val exampleSentence: String = "",
    val bookTitle: String = "",
    val isLearned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_dictionaries")
data class CustomDictionaryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rawJson: String,
    val wordCount: Int,
    val isActive: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)

package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.VocabularyEntity
import com.example.data.model.DictionaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VocabularyRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val vocabularyDao = db.vocabularyDao()

    val allVocabulary: Flow<List<VocabularyEntity>> = vocabularyDao.getAllVocabulary()
    val vocabularyCount: Flow<Int> = vocabularyDao.getVocabularyCount()

    suspend fun saveWordFromDictionary(
        entry: DictionaryEntry,
        bookTitle: String = "",
        contextSentence: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val firstMeaning = entry.meanings.firstOrNull()
        val definition = firstMeaning?.definitions?.firstOrNull() ?: "Literary term"
        val partOfSpeech = firstMeaning?.partOfSpeech ?: "noun"
        val example = if (contextSentence.isNotBlank()) {
            contextSentence
        } else {
            firstMeaning?.examples?.firstOrNull() ?: ""
        }

        val existing = vocabularyDao.getVocabularyByWord(entry.word)
        if (existing != null) {
            existing.id
        } else {
            val entity = VocabularyEntity(
                word = entry.word,
                phonetic = entry.phonetic,
                partOfSpeech = partOfSpeech,
                definition = definition,
                exampleSentence = example,
                bookTitle = bookTitle,
                isLearned = false,
                timestamp = System.currentTimeMillis()
            )
            vocabularyDao.insertVocabulary(entity)
            entity.id
        }
    }

    suspend fun toggleLearned(id: Long, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        vocabularyDao.setLearnedStatus(id, !currentStatus)
    }

    suspend fun deleteWord(id: Long) = withContext(Dispatchers.IO) {
        vocabularyDao.deleteVocabulary(id)
    }

    suspend fun isWordSaved(word: String): Boolean = withContext(Dispatchers.IO) {
        vocabularyDao.getVocabularyByWord(word) != null
    }
}

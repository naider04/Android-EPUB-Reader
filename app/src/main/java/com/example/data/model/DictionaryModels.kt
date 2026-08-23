package com.example.data.model

data class DictionaryMeaning(
    val partOfSpeech: String,
    val definitions: List<String>,
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList()
)

data class DictionaryEntry(
    val word: String,
    val phonetic: String? = null,
    val meanings: List<DictionaryMeaning>,
    val origin: String? = null,
    val source: String = "Built-in Dictionary"
)

data class ActiveDictionaryInfo(
    val id: String = "default",
    val name: String = "Default Literary English Dictionary",
    val wordCount: Int = 0,
    val isCustom: Boolean = false,
    val dateLoaded: Long = System.currentTimeMillis()
)

package com.example.data.dictionary

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomDictionaryEntity
import com.example.data.model.ActiveDictionaryInfo
import com.example.data.model.DictionaryEntry
import com.example.data.model.DictionaryMeaning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

class DictionaryService private constructor(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val customDictionaryDao = db.customDictionaryDao()

    private val _activeDictionaryInfo = MutableStateFlow(ActiveDictionaryInfo(wordCount = DefaultDictionaryData.entries.size))
    val activeDictionaryInfo: StateFlow<ActiveDictionaryInfo> = _activeDictionaryInfo.asStateFlow()

    private var customEntries: MutableMap<String, DictionaryEntry> = mutableMapOf()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val activeCustom = customDictionaryDao.getActiveDictionary()
        if (activeCustom != null) {
            val parsed = parseJsonDictionary(activeCustom.rawJson, activeCustom.name)
            if (parsed.isNotEmpty()) {
                customEntries = parsed.toMutableMap()
                _activeDictionaryInfo.value = ActiveDictionaryInfo(
                    id = activeCustom.id,
                    name = activeCustom.name,
                    wordCount = customEntries.size,
                    isCustom = true,
                    dateLoaded = activeCustom.addedTimestamp
                )
                return@withContext
            }
        }
        _activeDictionaryInfo.value = ActiveDictionaryInfo(
            id = "default",
            name = "Default Literary English Dictionary",
            wordCount = DefaultDictionaryData.entries.size,
            isCustom = false
        )
    }

    suspend fun lookupWord(rawWord: String): DictionaryEntry? = withContext(Dispatchers.Default) {
        val clean = cleanWord(rawWord)
        if (clean.isBlank()) return@withContext null

        // 1. Direct search in custom or default
        var entry = findEntry(clean)
        if (entry != null) return@withContext entry

        // 2. Lowercase search
        val lower = clean.lowercase()
        entry = findEntry(lower)
        if (entry != null) return@withContext entry

        // 3. Morphological lemmatization & stem search (e.g. plurals, past tense, adverbs, gerunds)
        val candidates = getWordStems(lower)
        for (stem in candidates) {
            entry = findEntry(stem)
            if (entry != null) return@withContext entry.copy(word = "$clean (root: ${entry.word})")
        }

        // 4. If still not found, build a helpful generated dictionary card with definition template
        return@withContext buildFallbackDefinition(clean)
    }

    private fun findEntry(word: String): DictionaryEntry? {
        val custom = customEntries[word]
        if (custom != null) return custom
        return DefaultDictionaryData.entries[word]
    }

    private fun cleanWord(word: String): String {
        return word.trim()
            .trimStart { !it.isLetterOrDigit() }
            .trimEnd { !it.isLetterOrDigit() }
    }

    private fun getWordStems(word: String): List<String> {
        val stems = mutableListOf<String>()
        if (word.endsWith("ies") && word.length > 4) stems.add(word.dropLast(3) + "y")
        if (word.endsWith("ing") && word.length > 4) {
            val base = word.dropLast(3)
            stems.add(base)
            stems.add(base + "e")
            if (base.length > 2 && base.last() == base[base.length - 2]) {
                stems.add(base.dropLast(1))
            }
        }
        if (word.endsWith("ed") && word.length > 3) {
            val base = word.dropLast(2)
            stems.add(base)
            stems.add(word.dropLast(1)) // e.g. loved -> love
            if (base.length > 2 && base.last() == base[base.length - 2]) {
                stems.add(base.dropLast(1))
            }
        }
        if (word.endsWith("es") && word.length > 3) stems.add(word.dropLast(2))
        if (word.endsWith("s") && word.length > 2) stems.add(word.dropLast(1))
        if (word.endsWith("ly") && word.length > 3) {
            stems.add(word.dropLast(2))
            stems.add(word.dropLast(2) + "e")
            if (word.endsWith("ily")) stems.add(word.dropLast(3) + "y")
        }
        if (word.endsWith("er") && word.length > 3) stems.add(word.dropLast(2))
        if (word.endsWith("est") && word.length > 4) stems.add(word.dropLast(3))
        return stems.distinct()
    }

    private fun buildFallbackDefinition(word: String): DictionaryEntry {
        val capital = word.replaceFirstChar { it.uppercase() }
        return DictionaryEntry(
            word = word,
            phonetic = "/$word/",
            meanings = listOf(
                DictionaryMeaning(
                    partOfSpeech = "vocabulary",
                    definitions = listOf("Word recognized in literature context: '$word'."),
                    examples = listOf("Tap 'Save' to collect '$word' into your vocabulary study list or import a custom JSON dictionary in Settings.")
                )
            ),
            source = "Reader Vocabulary Engine"
        )
    }

    suspend fun loadCustomDictionaryFromStream(inputStream: InputStream, name: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val rawJson = inputStream.bufferedReader().use { it.readText() }
            loadCustomDictionaryFromJson(rawJson, name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadCustomDictionaryFromJson(jsonString: String, name: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parsed = parseJsonDictionary(jsonString, name)
            if (parsed.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No valid word entries found in the JSON file. Ensure JSON contains key-value pairs or dictionary objects."))
            }

            customEntries = parsed.toMutableMap()
            val id = UUID.randomUUID().toString()

            customDictionaryDao.deactivateAll()
            val entity = CustomDictionaryEntity(
                id = id,
                name = name.ifBlank { "Custom Dictionary" },
                rawJson = jsonString,
                wordCount = parsed.size,
                isActive = true
            )
            customDictionaryDao.insertDictionary(entity)

            _activeDictionaryInfo.value = ActiveDictionaryInfo(
                id = id,
                name = entity.name,
                wordCount = parsed.size,
                isCustom = true
            )

            Result.success(parsed.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetToDefaultDictionary() = withContext(Dispatchers.IO) {
        customDictionaryDao.deactivateAll()
        customEntries.clear()
        _activeDictionaryInfo.value = ActiveDictionaryInfo(
            id = "default",
            name = "Default Literary English Dictionary",
            wordCount = DefaultDictionaryData.entries.size,
            isCustom = false
        )
    }

    /**
     * Supports multiple flexible JSON schemas:
     * 1. Simple Key-Value: {"word": "definition string", ...}
     * 2. Object map: {"word": {"phonetic": "/.../", "definition": "...", "partOfSpeech": "...", "examples": [...]}}
     * 3. Array of objects: [{"word": "...", "definition": "..."}, ...]
     * 4. FreeDictionary API format: [{"word": "...", "phonetic": "...", "meanings": [{"partOfSpeech": "...", "definitions": [{"definition": "..."}]}]}]
     */
    private fun parseJsonDictionary(jsonString: String, sourceName: String): Map<String, DictionaryEntry> {
        val resultMap = mutableMapOf<String, DictionaryEntry>()
        val trimmed = jsonString.trim()

        try {
            if (trimmed.startsWith("[")) {
                // Array format
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val word = obj.optString("word", "").trim()
                    if (word.isBlank()) continue

                    val phonetic = obj.optString("phonetic").takeIf { it.isNotBlank() }
                        ?: obj.optJSONArray("phonetics")?.let { phArr ->
                            (0 until phArr.length()).mapNotNull { idx ->
                                phArr.optJSONObject(idx)?.optString("text")
                            }.firstOrNull { it.isNotBlank() }
                        }

                    val meaningsList = mutableListOf<DictionaryMeaning>()

                    val meaningsArr = obj.optJSONArray("meanings")
                    if (meaningsArr != null) {
                        for (mIdx in 0 until meaningsArr.length()) {
                            val mObj = meaningsArr.optJSONObject(mIdx) ?: continue
                            val pos = mObj.optString("partOfSpeech", "general")
                            val defs = mutableListOf<String>()
                            val exList = mutableListOf<String>()
                            val synList = mutableListOf<String>()

                            val defsArr = mObj.optJSONArray("definitions")
                            if (defsArr != null) {
                                for (dIdx in 0 until defsArr.length()) {
                                    val dItem = defsArr.get(dIdx)
                                    if (dItem is JSONObject) {
                                        val defStr = dItem.optString("definition", "")
                                        if (defStr.isNotBlank()) defs.add(defStr)
                                        val exStr = dItem.optString("example", "")
                                        if (exStr.isNotBlank()) exList.add(exStr)
                                    } else if (dItem is String) {
                                        defs.add(dItem)
                                    }
                                }
                            }
                            if (defs.isEmpty()) {
                                val singleDef = mObj.optString("definition", "")
                                if (singleDef.isNotBlank()) defs.add(singleDef)
                            }
                            if (defs.isNotEmpty()) {
                                meaningsList.add(DictionaryMeaning(pos, defs, exList, synList))
                            }
                        }
                    } else {
                        val singleDef = obj.optString("definition", "")
                        if (singleDef.isNotBlank()) {
                            val pos = obj.optString("partOfSpeech", "general")
                            meaningsList.add(DictionaryMeaning(pos, listOf(singleDef)))
                        }
                    }

                    if (meaningsList.isNotEmpty()) {
                        resultMap[word.lowercase()] = DictionaryEntry(
                            word = word,
                            phonetic = phonetic,
                            meanings = meaningsList,
                            source = sourceName
                        )
                    }
                }
            } else if (trimmed.startsWith("{")) {
                // Object map format
                val root = JSONObject(trimmed)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = root.get(key)

                    if (value is String) {
                        resultMap[key.lowercase()] = DictionaryEntry(
                            word = key,
                            phonetic = null,
                            meanings = listOf(
                                DictionaryMeaning(
                                    partOfSpeech = "general",
                                    definitions = listOf(value)
                                )
                            ),
                            source = sourceName
                        )
                    } else if (value is JSONObject) {
                        val phonetic = value.optString("phonetic").takeIf { it.isNotBlank() }
                        val partOfSpeech = value.optString("partOfSpeech", value.optString("pos", "general"))
                        val defs = mutableListOf<String>()

                        val defVal = value.opt("definition") ?: value.opt("definitions")
                        if (defVal is JSONArray) {
                            for (d in 0 until defVal.length()) {
                                defs.add(defVal.getString(d))
                            }
                        } else if (defVal is String) {
                            defs.add(defVal)
                        }

                        val examples = mutableListOf<String>()
                        val exVal = value.opt("examples") ?: value.opt("example")
                        if (exVal is JSONArray) {
                            for (e in 0 until exVal.length()) examples.add(exVal.getString(e))
                        } else if (exVal is String) {
                            examples.add(exVal)
                        }

                        if (defs.isNotEmpty()) {
                            resultMap[key.lowercase()] = DictionaryEntry(
                                word = key,
                                phonetic = phonetic,
                                meanings = listOf(
                                    DictionaryMeaning(
                                        partOfSpeech = partOfSpeech,
                                        definitions = defs,
                                        examples = examples
                                    )
                                ),
                                source = sourceName
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultMap
    }

    companion object {
        @Volatile
        private var INSTANCE: DictionaryService? = null

        fun getInstance(context: Context): DictionaryService {
            return INSTANCE ?: synchronized(this) {
                val instance = DictionaryService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

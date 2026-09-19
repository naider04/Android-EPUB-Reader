package com.example.data.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomDictionaryEntity
import com.example.data.model.ActiveDictionaryInfo
import com.example.data.model.DictionaryEntry
import com.example.data.model.DictionaryMeaning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

class DictionaryService private constructor(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val customDictionaryDao = db.customDictionaryDao()

    companion object {
        const val DICTIONARY_ID = "oald10"
        const val DICTIONARY_NAME = "OALD 10th Edition"
        const val SOURCE_LABEL = "Oxford Advanced Learner's Dictionary (10th Ed.)"

        // The .gz asset is transparently decompressed by AGP when packaged into the APK,
        // so at runtime it is available as a plain SQLite database file.
        const val ASSET_DB_PATH = "dictionaries/oald10.db"
        private const val MAX_EXAMPLES_PER_MEANING = 8

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

    private val _activeDictionaryInfo = MutableStateFlow(ActiveDictionaryInfo(id = DICTIONARY_ID, name = DICTIONARY_NAME))
    val activeDictionaryInfo: StateFlow<ActiveDictionaryInfo> = _activeDictionaryInfo.asStateFlow()

    private var customEntries: MutableMap<String, DictionaryEntry> = mutableMapOf()

    @Volatile
    private var oaldDb: SQLiteDatabase? = null
    private val oaldMutex = Mutex()

    private val installDir: File get() = File(context.filesDir, "dictionaries")
    private val installedFile: File get() = File(installDir, "oald10.db")

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
        activateOaldDictionary()
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun lookupWord(rawWord: String): DictionaryEntry? = withContext(Dispatchers.Default) {
        val clean = cleanWord(rawWord)
        if (clean.isBlank()) return@withContext null

        // 1. Custom imported dictionary takes priority when active
        var entry = findCustom(clean) ?: findCustom(clean.lowercase())
        if (entry != null) return@withContext entry

        // 2. Offline Dictionary (OALD 10th Ed.) is used ONLY when there is no internet connection
        val hasInternet = isNetworkAvailable()
        if (hasInternet) {
            // When internet is available, return immediately so the dictionary sheet
            // displays instantly without any network timeout or blocking delay.
            return@withContext null
        }

        // 3. Offline Dictionary (OALD 10th Ed.) lookup from local SQLite database (takes <2ms)
        entry = lookupOald(normalizeTerm(clean), maxDepth = 1)
        if (entry != null) return@withContext entry

        // Morphological lemmatization & stem search in offline dictionary
        for (stem in getWordStems(clean.lowercase())) {
            entry = lookupOald(normalizeTerm(stem), maxDepth = 1)
            if (entry != null) {
                return@withContext entry.copy(word = "$clean (root: ${entry.word})")
            }
        }

        // If still not found offline, build fallback entry
        return@withContext buildFallbackDefinition(clean)
    }

    private fun findCustom(word: String): DictionaryEntry? {
        return customEntries[word]
    }

    /**
     * Looks up a normalized term in the bundled OALD SQLite database.
     * [maxDepth] limits how many cross-references are followed automatically
     * (e.g. "took" -> "PAST TENSE OF take").
     */
    private suspend fun lookupOald(term: String, maxDepth: Int = 0): DictionaryEntry? {
        if (term.isBlank() || term.length > 64) return null
        val json = queryOaldJson(term) ?: return null
        return parseOaldJson(json, maxDepth)
    }

    private suspend fun queryOaldJson(term: String): String? {
        val database = ensureOaldReady() ?: return null
        return withContext(Dispatchers.IO) {
            database.rawQuery(
                """
                SELECT e.json FROM entries e
                JOIN words w ON w.headword = e.headword
                WHERE w.term = ? LIMIT 1
                """.trimIndent(),
                arrayOf(term)
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }

    /** Copies the gzipped DB from assets into internal storage on first use and opens it read-only. */
    private suspend fun ensureOaldReady(): SQLiteDatabase? = oaldMutex.withLock {
        oaldDb?.let { return it }
        withContext(Dispatchers.IO) {
            try {
                if (!installedFile.exists() || installedFile.length() == 0L) {
                    installDir.mkdirs()
                    val tmp = File(installDir, "oald10.db.tmp")
                    context.assets.open(ASSET_DB_PATH).use { input ->
                        tmp.outputStream().use { out -> input.copyTo(out, DEFAULT_BUFFER_SIZE * 8) }
                    }
                    if (installedFile.exists()) installedFile.delete()
                    if (!tmp.renameTo(installedFile)) {
                        tmp.copyTo(installedFile, overwrite = true)
                        tmp.delete()
                    }
                }
                oaldDb?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runCatching {
                oaldDb = SQLiteDatabase.openDatabase(
                    installedFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
                )
            }.onFailure { it.printStackTrace() }
            oaldDb
        }
    }

    /**
     * Parses the pre-processed entry JSON produced at build time:
     * {"headword","pronunciation","groups":[{"partOfSpeech","definitions","examples"}],"cross_ref":{"label","base"}}
     */
    private suspend fun parseOaldJson(json: String, crossRefDepth: Int): DictionaryEntry? {
        return try {
            val obj = JSONObject(json)
            val headword = obj.optString("headword").ifBlank { return null }
            val groups = obj.optJSONArray("groups")
            val meanings = mutableListOf<DictionaryMeaning>()

            if (groups != null) {
                for (i in 0 until groups.length()) {
                    val g = groups.optJSONObject(i) ?: continue
                    val defsArr = g.optJSONArray("definitions") ?: continue
                    val defs = mutableListOf<String>()
                    for (d in 0 until defsArr.length()) {
                        val def = defsArr.optString(d)
                        if (def.isNotBlank()) defs.add(def)
                    }
                    if (defs.isEmpty()) continue

                    val examples = mutableListOf<String>()
                    g.optJSONArray("examples")?.let { exArr ->
                        var e = 0
                        while (e < exArr.length() && examples.size < MAX_EXAMPLES_PER_MEANING) {
                            val ex = exArr.optString(e)
                            if (ex.isNotBlank()) examples.add(ex)
                            e++
                        }
                    }

                    meanings.add(
                        DictionaryMeaning(
                            partOfSpeech = g.optString("part_of_speech", "").ifBlank { "general" },
                            definitions = defs,
                            examples = examples
                        )
                    )
                }
            }

            // Entries that only exist as inflected forms point at their base word.
            if (meanings.isEmpty()) {
                val crossRef = obj.optJSONObject("cross_ref")
                if (crossRef != null && crossRefDepth > 0) {
                    val label = crossRef.optString("label", "")
                    val baseTerm = normalizeTerm(crossRef.optString("base", ""))
                    if (baseTerm.isNotBlank() && baseTerm != normalizeTerm(headword)) {
                        lookupOald(baseTerm, maxDepth = crossRefDepth - 1)?.let { base ->
                            return base.copy(
                                word = "${base.word} ($label)",
                                source = base.source
                            )
                        }
                    }
                    return DictionaryEntry(
                        word = headword,
                        phonetic = obj.optString("pronunciation").takeIf { it.isNotBlank() },
                        meanings = listOf(
                            DictionaryMeaning(
                                partOfSpeech = "verb form",
                                definitions = listOf("$label of \u201C${crossRef.optString("base")}\u201D.")
                            )
                        ),
                        source = SOURCE_LABEL
                    )
                }
                return null
            }

            DictionaryEntry(
                word = headword,
                phonetic = obj.optString("pronunciation").takeIf { it.isNotBlank() },
                meanings = meanings,
                origin = obj.optString("origin").takeIf { it.isNotBlank() },
                source = SOURCE_LABEL
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Mirrors the web reader's strip(): lowercase, drop interpuncts, punctuation -> space. */
    private fun normalizeTerm(word: String): String {
        return word.lowercase()
            .replace("·", "")
            .replace(Regex("""[“”"'’‘.,;:!?\(\)\[\]{}«»—–\-]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
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
        return DictionaryEntry(
            word = word,
            phonetic = "/$word/",
            meanings = listOf(
                DictionaryMeaning(
                    partOfSpeech = "vocabulary",
                    definitions = listOf("No definition found for '$word' in the dictionary."),
                    examples = listOf("Tap 'Save' to collect '$word' into your vocabulary study list.")
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
        activateOaldDictionary()
    }

    private suspend fun activateOaldDictionary() {
        ensureOaldReady()
        val count = oaldDb?.let { database ->
            withContext(Dispatchers.IO) {
                database.rawQuery("SELECT COUNT(*) FROM entries", emptyArray()).use { c ->
                    if (c.moveToFirst()) c.getInt(0) else 0
                }
            }
        } ?: 0
        _activeDictionaryInfo.value = ActiveDictionaryInfo(
            id = DICTIONARY_ID,
            name = DICTIONARY_NAME,
            wordCount = count,
            isCustom = false,
            dateLoaded = System.currentTimeMillis()
        )
    }

    /**
     * Supports multiple flexible JSON schemas for user-imported dictionaries:
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
}

package com.tbtechsdev.lexiread.data.dictionary

import android.util.Log
import android.util.LruCache
import com.tbtechsdev.lexiread.data.db.dao.CachedDefinitionDao
import com.tbtechsdev.lexiread.data.db.entities.CachedDefinition
import com.tbtechsdev.lexiread.domain.WordNormalizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DictionaryRepository"
private const val LRU_CACHE_CAPACITY = 200

@Singleton
class DictionaryRepository @Inject constructor(
    private val dao: CachedDefinitionDao
) : IDictionaryRepository {

    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val memoryCache = LruCache<String, CachedDefinition>(LRU_CACHE_CAPACITY)

    constructor(
        dao: CachedDefinitionDao,
        ioDispatcher: CoroutineDispatcher
    ) : this(dao) {
        this.ioDispatcher = ioDispatcher
    }

    override suspend fun getDefinition(word: String): CachedDefinition? = withContext(ioDispatcher) {
        val trimmed = word.trim().lowercase()
        if (trimmed.isEmpty()) return@withContext null

        // Step 0 — Check in-memory LruCache (capacity 200) first
        synchronized(memoryCache) {
            val memoryHit = memoryCache.get(trimmed)
            if (memoryHit != null) {
                Log.d(TAG, "LruCache hit for '$word'")
                return@withContext memoryHit
            }
        }

        try {
            // Step 1 — Check Room cache for existing entry with rich details:
            val cached = dao.getByWord(trimmed) ?: dao.getByWord(WordNormalizer(trimmed))
            if (cached != null && (cached.phonetic.isNotBlank() || cached.example.isNotBlank() || cached.synonyms.isNotBlank())) {
                Log.d(TAG, "Room cache hit with rich details for '$word'")
                synchronized(memoryCache) {
                    memoryCache.put(trimmed, cached)
                }
                return@withContext cached
            }

            // Step 2 — Query Free Dictionary API for full live details (phonetics, examples, synonyms, antonyms):
            val result = fetchFromApi(trimmed) ?: run {
                val normalized = WordNormalizer(trimmed)
                if (normalized.isNotBlank() && normalized != trimmed) {
                    fetchFromApi(normalized)
                } else null
            }

            if (result != null) {
                dao.upsert(result)
                synchronized(memoryCache) {
                    memoryCache.put(trimmed, result)
                }
                Log.d(TAG, "Cached rich definition for '${result.word}' in Room and memory")
                return@withContext result
            }

            // Step 3 — If API unavailable (offline, 404), return existing Room cache if present
            if (cached != null) {
                Log.d(TAG, "Falling back to basic Room cache for '$word'")
                synchronized(memoryCache) {
                    memoryCache.put(trimmed, cached)
                }
                return@withContext cached
            }

            // Step 4 — Fallback to built-in Offline Dictionary (zero network latency, works offline):
            val offlineResult = OfflineDictionaryProvider.lookup(trimmed)
                ?: OfflineDictionaryProvider.lookup(WordNormalizer(trimmed))
            if (offlineResult != null) {
                dao.upsert(offlineResult)
                synchronized(memoryCache) {
                    memoryCache.put(trimmed, offlineResult)
                }
                Log.d(TAG, "Built-in offline dictionary fallback for '$word'")
                return@withContext offlineResult
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get definition for '$word': ${e.message}")
            dao.getByWord(trimmed) ?: OfflineDictionaryProvider.lookup(trimmed)
        }
    }

    private fun fetchFromApi(lookupWord: String): CachedDefinition? {
        var connection: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(lookupWord, "UTF-8")
            val apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$encoded"
            Log.i(TAG, "API call to dictionaryapi.dev for '$lookupWord' -> $apiUrl")

            val url = URL(apiUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "FolioReader/1.0")
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "dictionaryapi.dev response code for '$lookupWord': $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val firstEntry = jsonArray.getJSONObject(0)

                    // 1. Phonetic IPA transcription
                    var phonetic = firstEntry.optString("phonetic", "").trim()
                    if (phonetic.isBlank()) {
                        val phoneticsArr = firstEntry.optJSONArray("phonetics")
                        if (phoneticsArr != null) {
                            for (i in 0 until phoneticsArr.length()) {
                                val pText = phoneticsArr.getJSONObject(i).optString("text", "").trim()
                                if (pText.isNotBlank()) {
                                    phonetic = pText
                                    break
                                }
                            }
                        }
                    }

                    // 2. Meanings, definitions, example sentence, synonyms & antonyms
                    var partOfSpeech = ""
                    var englishDefinition = ""
                    var example = ""
                    val synonymsSet = linkedSetOf<String>()
                    val antonymsSet = linkedSetOf<String>()

                    val meanings = firstEntry.optJSONArray("meanings")
                    if (meanings != null) {
                        for (i in 0 until meanings.length()) {
                            val meaning = meanings.getJSONObject(i)
                            if (partOfSpeech.isBlank()) {
                                partOfSpeech = meaning.optString("partOfSpeech", "")
                            }

                            // Collect meaning-level synonyms & antonyms
                            val meaningSynonyms = meaning.optJSONArray("synonyms")
                            if (meaningSynonyms != null) {
                                for (s in 0 until meaningSynonyms.length()) {
                                    val syn = meaningSynonyms.optString(s, "").trim()
                                    if (syn.isNotBlank() && !syn.equals(lookupWord, ignoreCase = true)) {
                                        synonymsSet.add(syn)
                                    }
                                }
                            }
                            val meaningAntonyms = meaning.optJSONArray("antonyms")
                            if (meaningAntonyms != null) {
                                for (a in 0 until meaningAntonyms.length()) {
                                    val ant = meaningAntonyms.optString(a, "").trim()
                                    if (ant.isNotBlank() && !ant.equals(lookupWord, ignoreCase = true)) {
                                        antonymsSet.add(ant)
                                    }
                                }
                            }

                            val definitions = meaning.optJSONArray("definitions")
                            if (definitions != null) {
                                for (d in 0 until definitions.length()) {
                                    val defObj = definitions.getJSONObject(d)
                                    val defText = defObj.optString("definition", "").trim()
                                    if (englishDefinition.isBlank() && defText.isNotBlank()) {
                                        englishDefinition = defText
                                    }
                                    val exText = defObj.optString("example", "").trim()
                                    if (example.isBlank() && exText.isNotBlank()) {
                                        example = exText
                                    }

                                    val defSynonyms = defObj.optJSONArray("synonyms")
                                    if (defSynonyms != null) {
                                        for (s in 0 until defSynonyms.length()) {
                                            val syn = defSynonyms.optString(s, "").trim()
                                            if (syn.isNotBlank() && !syn.equals(lookupWord, ignoreCase = true)) {
                                                synonymsSet.add(syn)
                                            }
                                        }
                                    }
                                    val defAntonyms = defObj.optJSONArray("antonyms")
                                    if (defAntonyms != null) {
                                        for (a in 0 until defAntonyms.length()) {
                                            val ant = defAntonyms.optString(a, "").trim()
                                            if (ant.isNotBlank() && !ant.equals(lookupWord, ignoreCase = true)) {
                                                antonymsSet.add(ant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (englishDefinition.isNotBlank()) {
                        return CachedDefinition(
                            word = lookupWord,
                            partOfSpeech = partOfSpeech,
                            englishDefinition = englishDefinition,
                            hindiMeaning = "",
                            cachedAtMs = System.currentTimeMillis(),
                            phonetic = phonetic,
                            example = example,
                            synonyms = synonymsSet.take(8).joinToString(", "),
                            antonyms = antonymsSet.take(8).joinToString(", ")
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "API call error for '$lookupWord': ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    override fun isCommonWord(word: String): Boolean {
        val cleaned = word.lowercase().trim()
        return CommonWordsProvider.commonWords.contains(cleaned)
    }

    override fun getFrequencyRank(word: String): Int {
        val cleaned = word.lowercase().trim()
        return WordFrequencyProvider.frequencyMap[cleaned] ?: 99999
    }

    override suspend fun getAllCachedDefinitions(): List<CachedDefinition> = withContext(ioDispatcher) {
        dao.getAll()
    }
}

/**
 * Typealias for backward-compatibility with prior code references.
 */
typealias DictionaryRepositoryImpl = DictionaryRepository

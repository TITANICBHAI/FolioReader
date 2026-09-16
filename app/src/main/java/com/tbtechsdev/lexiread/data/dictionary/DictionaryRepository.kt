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
            // Step 1 — Check Room cache:
            val cached = dao.getByWord(trimmed) ?: dao.getByWord(WordNormalizer(trimmed))
            if (cached != null) {
                Log.d(TAG, "Room cache hit for '$word'")
                synchronized(memoryCache) {
                    memoryCache.put(trimmed, cached)
                }
                return@withContext cached
            }

            // Step 1b — Check built-in Offline Dictionary (zero network latency, works offline):
            val offlineResult = OfflineDictionaryProvider.lookup(trimmed)
                ?: OfflineDictionaryProvider.lookup(WordNormalizer(trimmed))
            if (offlineResult != null) {
                dao.upsert(offlineResult)
                synchronized(memoryCache) {
                    memoryCache.put(trimmed, offlineResult)
                }
                Log.d(TAG, "Built-in offline dictionary hit for '$word'")
                return@withContext offlineResult
            }

            // Step 2 — Call Free Dictionary API (no key needed):
            // GET https://api.dictionaryapi.dev/api/v2/entries/en/{word}
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
                Log.d(TAG, "Cached definition for '${result.word}' in Room and memory")
                return@withContext result
            }

            // Step 3 — On any failure (no internet, HTTP 404, timeout): return null
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get definition for '$word': ${e.message}")
            null
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
                setRequestProperty("User-Agent", "LexiRead/1.0")
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "dictionaryapi.dev response code for '$lookupWord': $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val firstEntry = jsonArray.getJSONObject(0)
                    val meanings = firstEntry.optJSONArray("meanings")
                    if (meanings != null && meanings.length() > 0) {
                        val firstMeaning = meanings.getJSONObject(0)
                        val partOfSpeech = firstMeaning.optString("partOfSpeech", "")
                        val definitions = firstMeaning.optJSONArray("definitions")
                        val englishDefinition = if (definitions != null && definitions.length() > 0) {
                            definitions.getJSONObject(0).optString("definition", "")
                        } else ""

                        if (englishDefinition.isNotBlank()) {
                            return CachedDefinition(
                                word = lookupWord,
                                partOfSpeech = partOfSpeech,
                                englishDefinition = englishDefinition,
                                hindiMeaning = "",
                                cachedAtMs = System.currentTimeMillis()
                            )
                        }
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
}

/**
 * Typealias for backward-compatibility with prior code references.
 */
typealias DictionaryRepositoryImpl = DictionaryRepository

package com.ncb.drugtestcompanion.localization

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe local cache for dynamic text translations persisted in app-private storage (`translation_cache.json`).
 */
@Singleton
class TranslationCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val cacheMap = ConcurrentHashMap<String, String>()
    private val cacheFile = File(context.filesDir, "translation_cache.json")

    init {
        loadCacheFromDisk()
    }

    private fun makeKey(sourceLang: String, targetLang: String, text: String): String {
        return "$sourceLang|$targetLang|${text.trim()}"
    }

    fun get(sourceLang: String, targetLang: String, text: String): String? {
        val key = makeKey(sourceLang, targetLang, text)
        return cacheMap[key]
    }

    fun put(sourceLang: String, targetLang: String, text: String, translatedText: String) {
        if (text.isBlank() || translatedText.isBlank()) return
        val key = makeKey(sourceLang, targetLang, text)
        cacheMap[key] = translatedText
        saveCacheToDisk()
    }

    @Synchronized
    private fun loadCacheFromDisk() {
        if (!cacheFile.exists()) return
        try {
            val jsonStr = cacheFile.readText(Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                cacheMap[key] = json.getString(key)
            }
        } catch (_: Throwable) {
            // Ignore corrupted disk cache safely
        }
    }

    @Synchronized
    private fun saveCacheToDisk() {
        try {
            val json = JSONObject()
            for ((key, value) in cacheMap) {
                json.put(key, value)
            }
            cacheFile.writeText(json.toString(), Charsets.UTF_8)
        } catch (_: Throwable) {
            // Ignore write errors safely
        }
    }
}

package com.ncb.drugtestcompanion.localization

import android.util.Log
import com.ncb.drugtestcompanion.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Cloud Translation v2 API fallback implementation.
 * Securely checks for configured API keys without hardcoding or committing keys into repository source.
 */
@Singleton
class GoogleTranslationService @Inject constructor() : TranslationService {

    override suspend fun translateDynamicText(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        if (text.isBlank() || sourceLang == targetLang) {
            return Result.success(text)
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "UNCONFIGURED_KEY") {
            logDiagnostic("Google Translation API key is unconfigured. Falling back gracefully.")
            return Result.failure(IllegalStateException("Google Translation API key unconfigured"))
        }

        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val urlString = "https://translation.googleapis.com/language/translate/v2?key=$apiKey&q=$encodedText&source=$sourceLang&target=$targetLang"
            val url = URL(urlString)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val responseStr = reader.use { it.readText() }

                val json = JSONObject(responseStr)
                val translatedText = json.getJSONObject("data")
                    .getJSONArray("translations")
                    .getJSONObject(0)
                    .getString("translatedText")

                Result.success(translatedText)
            } else {
                logDiagnostic("Google Translation API returned HTTP $responseCode")
                Result.failure(IllegalStateException("HTTP error $responseCode"))
            }
        } catch (e: Throwable) {
            logDiagnostic("Google Translation API request failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun getApiKey(): String {
        return try {
            val envKey = System.getenv("GOOGLE_TRANSLATION_API_KEY")
            if (!envKey.isNullOrBlank()) return envKey

            // Check if BuildConfig defined optional field
            val buildConfigClass = BuildConfig::class.java
            val field = buildConfigClass.getField("GOOGLE_TRANSLATION_API_KEY")
            field.get(null) as? String ?: "UNCONFIGURED_KEY"
        } catch (_: Throwable) {
            "UNCONFIGURED_KEY"
        }
    }

    private fun logDiagnostic(msg: String) {
        try {
            Log.d("GoogleTranslationService", msg)
        } catch (_: Throwable) {
            println("[GoogleTranslationService] $msg")
        }
    }
}

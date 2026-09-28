package com.ncb.drugtestcompanion.localization

/**
 * Interface abstraction for dynamic translation services (e.g. Google Cloud Translation API).
 */
interface TranslationService {
    suspend fun translateDynamicText(
        text: String,
        sourceLang: String = "en",
        targetLang: String
    ): Result<String>
}

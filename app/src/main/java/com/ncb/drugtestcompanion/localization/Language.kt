package com.ncb.drugtestcompanion.localization

/**
 * Supported application languages with ISO 639-1 language codes, native names, and English display names.
 */
enum class AppLanguage(val code: String, val nativeName: String, val englishName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "हिन्दी", "Hindi"),
    KANNADA("kn", "ಕನ್ನಡ", "Kannada"),
    TAMIL("ta", "தமிழ்", "Tamil"),
    TELUGU("te", "తెలుగు", "Telugu");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code == null) return ENGLISH
            return entries.find { it.code.lowercase() == code.lowercase() } ?: ENGLISH
        }
    }
}

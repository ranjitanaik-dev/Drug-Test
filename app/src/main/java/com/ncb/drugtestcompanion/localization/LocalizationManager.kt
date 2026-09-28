package com.ncb.drugtestcompanion.localization

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.domain.model.QualityFailureReason
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates application-wide hybrid localization and translation fallback chain:
 * Local Resource -> Local Translation Cache -> Google Translation API -> English Fallback.
 */
@Singleton
class LocalizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languageRepository: LanguageRepository,
    private val translationService: TranslationService,
    private val translationCache: TranslationCache
) {

    val currentLanguage: Flow<AppLanguage> = languageRepository.selectedLanguage

    suspend fun changeLanguage(language: AppLanguage) {
        languageRepository.setLanguage(language)
        applyLocaleToContext(context, language.code)
    }

    suspend fun translateDynamic(text: String, targetLangCode: String): String {
        if (text.isBlank() || targetLangCode == "en") return text

        // 1. Check local translation cache
        val cached = translationCache.get("en", targetLangCode, text)
        if (cached != null) return cached

        // 2. Google Translation API Fallback
        val apiResult = translationService.translateDynamicText(
            text = text,
            sourceLang = "en",
            targetLang = targetLangCode
        )

        return apiResult.fold(
            onSuccess = { translatedText ->
                translationCache.put("en", targetLangCode, text, translatedText)
                translatedText
            },
            onFailure = {
                logDiagnostic("Dynamic translation failed/unconfigured. Falling back to English.")
                text // Fallback to English
            }
        )
    }

    fun applyLocaleToContext(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getLocalizedResultCategory(resultCategoryName: String, localizedContext: Context): String {
        return when (resultCategoryName.uppercase(Locale.US)) {
            "POSITIVE" -> localizedContext.getString(R.string.result_positive)
            "NEGATIVE" -> localizedContext.getString(R.string.result_negative)
            "INCONCLUSIVE" -> localizedContext.getString(R.string.result_inconclusive)
            else -> resultCategoryName
        }
    }

    fun getLocalizedFailureReason(reason: QualityFailureReason, localizedContext: Context): String {
        return when (reason) {
            QualityFailureReason.BLUR -> localizedContext.getString(R.string.disclaimer_text)
            else -> reason.name
        }
    }

    private fun logDiagnostic(msg: String) {
        try {
            Log.d("LocalizationManager", msg)
        } catch (_: Throwable) {
            println("[LocalizationManager] $msg")
        }
    }
}

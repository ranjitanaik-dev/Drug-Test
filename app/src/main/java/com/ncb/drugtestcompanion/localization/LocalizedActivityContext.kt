package com.ncb.drugtestcompanion.localization

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * ContextWrapper preserving ComponentActivity instance for Hilt and Navigation
 * while providing localized Resources for Jetpack Compose stringResource resolution.
 */
class LocalizedActivityContext(
    baseActivity: Context,
    languageCode: String
) : ContextWrapper(baseActivity) {

    private val localizedResources: Resources by lazy {
        try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(baseActivity.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            baseActivity.createConfigurationContext(config).resources
        } catch (_: Throwable) {
            baseActivity.resources
        }
    }

    override fun getResources(): Resources {
        return localizedResources
    }
}

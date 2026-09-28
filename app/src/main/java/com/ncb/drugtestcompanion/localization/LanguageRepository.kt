package com.ncb.drugtestcompanion.localization

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "language_preferences")

/**
 * DataStore-backed repository for persisting language selection (`en`, `hi`, `kn`, `ta`, `te`).
 */
@Singleton
class LanguageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val keySelectedLanguage = stringPreferencesKey("selected_language_code")

    val selectedLanguage: Flow<AppLanguage> = context.applicationContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val code = prefs[keySelectedLanguage] ?: AppLanguage.ENGLISH.code
            AppLanguage.fromCode(code)
        }

    suspend fun setLanguage(language: AppLanguage) {
        try {
            context.applicationContext.dataStore.edit { prefs ->
                prefs[keySelectedLanguage] = language.code
            }
        } catch (_: Throwable) {
            // Prevent crash if DataStore write fails
        }
    }
}

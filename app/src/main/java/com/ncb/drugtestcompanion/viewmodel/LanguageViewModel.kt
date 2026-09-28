package com.ncb.drugtestcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncb.drugtestcompanion.localization.AppLanguage
import com.ncb.drugtestcompanion.localization.LocalizationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application-wide ViewModel for managing language preferences and locale changes.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val localizationManager: LocalizationManager
) : ViewModel() {

    val currentLanguage: StateFlow<AppLanguage> = localizationManager.currentLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.ENGLISH
        )

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            localizationManager.changeLanguage(language)
        }
    }
}

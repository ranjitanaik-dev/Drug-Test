package com.ncb.drugtestcompanion.localization

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalizationManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var languageRepository: LanguageRepository
    private lateinit var translationService: TranslationService
    private lateinit var translationCache: TranslationCache
    private lateinit var localizationManager: LocalizationManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempFolder.root
        languageRepository = mockk(relaxed = true)
        translationService = mockk()
        translationCache = TranslationCache(context)
        localizationManager = LocalizationManager(
            context,
            languageRepository,
            translationService,
            translationCache
        )
    }

    @Test
    fun `translateDynamic returns text directly when target is English`() = runTest {
        val result = localizationManager.translateDynamic("Hello", "en")
        assertEquals("Hello", result)
    }

    @Test
    fun `translateDynamic falls back to English when API fails`() = runTest {
        coEvery { translationService.translateDynamicText("Dynamic Notice", "en", "kn") } returns Result.failure(RuntimeException("Network error"))

        val result = localizationManager.translateDynamic("Dynamic Notice", "kn")
        assertEquals("Dynamic Notice", result)
    }

    @Test
    fun `translateDynamic returns cached translation before calling API`() = runTest {
        translationCache.put("en", "kn", "Cached Text", "ಕ್ಯಾಶ್ ಮಾಡಲಾದ ಪಠ್ಯ")

        val result = localizationManager.translateDynamic("Cached Text", "kn")
        assertEquals("ಕ್ಯಾಶ್ ಮಾಡಲಾದ ಪಠ್ಯ", result)
    }
}

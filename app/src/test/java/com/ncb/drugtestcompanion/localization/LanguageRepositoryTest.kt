package com.ncb.drugtestcompanion.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageRepositoryTest {

    @Test
    fun `AppLanguage fromCode maps correct codes and falls back to ENGLISH`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
        assertEquals(AppLanguage.KANNADA, AppLanguage.fromCode("kn"))
        assertEquals(AppLanguage.TAMIL, AppLanguage.fromCode("ta"))
        assertEquals(AppLanguage.TELUGU, AppLanguage.fromCode("te"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("invalid_code"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
    }

    @Test
    fun `AppLanguage native names match specified language titles`() {
        assertEquals("English", AppLanguage.ENGLISH.nativeName)
        assertEquals("हिन्दी", AppLanguage.HINDI.nativeName)
        assertEquals("ಕನ್ನಡ", AppLanguage.KANNADA.nativeName)
        assertEquals("தமிழ்", AppLanguage.TAMIL.nativeName)
        assertEquals("తెలుగు", AppLanguage.TELUGU.nativeName)
    }
}

package com.ncb.drugtestcompanion.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleConfigurationTest {

    @Test
    fun `AppLanguage codes map to expected locale codes`() {
        assertEquals("en", AppLanguage.ENGLISH.code)
        assertEquals("hi", AppLanguage.HINDI.code)
        assertEquals("kn", AppLanguage.KANNADA.code)
        assertEquals("ta", AppLanguage.TAMIL.code)
        assertEquals("te", AppLanguage.TELUGU.code)
    }
}

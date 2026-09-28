package com.ncb.drugtestcompanion.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DebugImageTamperHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var tamperHelper: DebugImageTamperHelper

    @Before
    fun setUp() {
        tamperHelper = DebugImageTamperHelper()
    }

    @Test
    fun `tamperImageBytes alters image bytes and restoreImageBytes restores original bytes`() {
        val testFile = tempFolder.newFile("sample_image.jpg").apply {
            writeBytes(byteArrayOf(0x10, 0x20, 0x30, 0x40))
        }

        // 1. Tamper image bytes
        val tampered = tamperHelper.tamperImageBytes(testFile.absolutePath)
        assertTrue(tampered)
        assertTrue(tamperHelper.isTampered(testFile.absolutePath))

        val tamperedBytes = testFile.readBytes()
        assertEquals(4, tamperedBytes.size)
        // First byte 0x10 xor 0xFF = 0xEF
        assertEquals(0xEF.toByte(), tamperedBytes[0])

        // 2. Restore original bytes
        val restored = tamperHelper.restoreImageBytes(testFile.absolutePath)
        assertTrue(restored)
        assertFalse(tamperHelper.isTampered(testFile.absolutePath))

        val restoredBytes = testFile.readBytes()
        assertEquals(0x10.toByte(), restoredBytes[0])
        assertEquals(0x20.toByte(), restoredBytes[1])
    }
}

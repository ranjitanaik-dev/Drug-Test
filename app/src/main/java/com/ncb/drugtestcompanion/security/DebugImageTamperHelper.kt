package com.ncb.drugtestcompanion.security

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Development/Debug-only helper for simulating image byte tampering and restoring original image bytes.
 */
@Singleton
class DebugImageTamperHelper @Inject constructor() {

    private val backupMap = mutableMapOf<String, ByteArray>()

    fun tamperImageBytes(imagePath: String): Boolean {
        if (imagePath.isBlank()) return false
        val file = File(imagePath)
        if (!file.exists()) return false

        return try {
            val originalBytes = file.readBytes()
            if (!backupMap.containsKey(imagePath)) {
                backupMap[imagePath] = originalBytes
            }
            if (originalBytes.isEmpty()) return false

            val modifiedBytes = originalBytes.copyOf()
            // Modify exactly one byte (flip byte 0)
            modifiedBytes[0] = (modifiedBytes[0].toInt() xor 0xFF).toByte()

            file.writeBytes(modifiedBytes)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun restoreImageBytes(imagePath: String): Boolean {
        val backupBytes = backupMap[imagePath] ?: return false
        val file = File(imagePath)

        return try {
            file.writeBytes(backupBytes)
            backupMap.remove(imagePath)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun isTampered(imagePath: String): Boolean {
        return backupMap.containsKey(imagePath)
    }
}

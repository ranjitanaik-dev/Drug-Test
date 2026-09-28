package com.ncb.drugtestcompanion.security

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security helper for generating cryptographic SHA-256 digests of captured image files or byte arrays.
 */
@Singleton
class EvidenceHasher @Inject constructor() {

    fun calculateSha256(file: File): String {
        if (!file.exists()) return "FILE_NOT_FOUND"
        return try {
            val bytes = file.readBytes()
            calculateSha256(bytes)
        } catch (_: Throwable) {
            "HASH_CALCULATION_FAILED"
        }
    }

    fun calculateSha256(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            "HASH_CALCULATION_FAILED"
        }
    }
}

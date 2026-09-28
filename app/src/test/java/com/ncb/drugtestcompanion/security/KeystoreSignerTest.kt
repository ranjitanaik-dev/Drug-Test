package com.ncb.drugtestcompanion.security

import com.ncb.drugtestcompanion.domain.model.TestRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class KeystoreSignerTest {

    private lateinit var keystoreSigner: KeystoreSigner

    @Before
    fun setUp() {
        keystoreSigner = KeystoreSigner()
    }

    @Test
    fun `TEST 4 - Canonical record string generation is deterministic`() {
        val record = TestRecord(
            testId = "DT-20260919-123456-ABCDEF12",
            timestamp = 1789818302000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            operatorId = "OP-LOCAL",
            latitude = 12.9716,
            longitude = 77.5946,
            locationStatus = "AVAILABLE"
        )

        val canonical1 = keystoreSigner.generateCanonicalString(record)
        val canonical2 = keystoreSigner.generateCanonicalString(record)

        assertEquals(canonical1, canonical2)
        assertTrue(canonical1.contains("testId=DT-20260919-123456-ABCDEF12"))
        assertTrue(canonical1.contains("kitId=KIT_A"))
        assertTrue(canonical1.contains("latitude=12.9716"))
    }

    @Test
    fun `TEST 5, 6, 7 & 8 - RSA key signing and verification detects tampered record fields`() {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048)
        val keyPair = keyPairGen.generateKeyPair()

        val record = TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = "hash123",
            operatorId = "OP-LOCAL",
            locationStatus = "UNAVAILABLE"
        )

        val canonicalData = keystoreSigner.generateCanonicalString(record).toByteArray(Charsets.UTF_8)

        // Sign canonical data using test RSA key
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(keyPair.private)
            update(canonicalData)
        }
        val sigBytes = signature.sign()
        val base64Sig = Base64.getEncoder().encodeToString(sigBytes)

        // Verify original record with public key
        val isValid = keystoreSigner.verifySignature(record, base64Sig, keyPair.public)
        assertTrue(isValid)

        // Tamper record result from NEGATIVE to POSITIVE
        val tamperedRecord = record.copy(result = "POSITIVE")
        val isTamperedValid = keystoreSigner.verifySignature(tamperedRecord, base64Sig, keyPair.public)
        assertFalse(isTamperedValid)
    }
}

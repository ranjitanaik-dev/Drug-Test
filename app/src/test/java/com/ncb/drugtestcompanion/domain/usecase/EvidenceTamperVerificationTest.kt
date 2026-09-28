package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.security.EvidenceHasher
import com.ncb.drugtestcompanion.security.KeystoreSigner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Temporary development-only tamper verification test for Phase 6.
 * Verifies that modifying image bytes of a copied capture file causes [VerifyEvidenceIntegrityUseCase]
 * to return [IntegrityStatus.IMAGE_HASH_MISMATCH] while keeping stored imageSha256 and digital signature intact.
 */
class EvidenceTamperVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var evidenceHasher: EvidenceHasher
    private lateinit var keystoreSigner: KeystoreSigner
    private lateinit var verifyUseCase: VerifyEvidenceIntegrityUseCase

    @Before
    fun setUp() {
        evidenceHasher = EvidenceHasher()
        keystoreSigner = mockk()
        verifyUseCase = VerifyEvidenceIntegrityUseCase(evidenceHasher, keystoreSigner)
    }

    @Test
    fun `DEVELOPMENT TAMPER TEST - Modifying copied image bytes causes IMAGE_HASH_MISMATCH while signature remains valid`() {
        // 1. Create original full-frame test image
        val originalImageFile = tempFolder.newFile("original_test_capture.jpg").apply {
            writeBytes("AuthenticFullFrameJpegImageBytes_2026".toByteArray(Charsets.UTF_8))
        }
        val originalSha256 = evidenceHasher.calculateSha256(originalImageFile)

        // 2. Build valid TestRecord with original imageSha256
        val signedRecord = TestRecord(
            testId = "DT-20260919-123456-TAMPER01",
            timestamp = 1789818302000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.80f,
            distance = 16.86,
            imageSha256 = originalSha256,
            operatorId = "OP-LOCAL",
            locationStatus = "UNAVAILABLE",
            signature = "validDigitalSignatureBase64String123"
        )

        // Mock digital signature verification to return true (signature remains valid for canonical data)
        every { keystoreSigner.verifySignature(any(), "validDigitalSignatureBase64String123", null) } returns true

        // 3. Create temporary copy of the test image and modify its bytes only
        val copiedImageFile = tempFolder.newFile("copied_test_capture.jpg")
        originalImageFile.copyTo(copiedImageFile, overwrite = true)

        // Modify copied image bytes (simulating image tampering)
        copiedImageFile.writeBytes("TAMPERED_FullFrameJpegImageBytes_2026".toByteArray(Charsets.UTF_8))

        // 4. Verify image SHA-256 of modified copy differs from original
        val modifiedSha256 = evidenceHasher.calculateSha256(copiedImageFile)
        assertNotEquals("Modified image SHA-256 must differ from original SHA-256", originalSha256, modifiedSha256)

        // 5. Run existing VerifyEvidenceIntegrityUseCase against the modified image file copy
        val verificationResult = verifyUseCase(signedRecord, copiedImageFile)

        // 6. Assertions: Signature is valid, but image hash fails -> IMAGE_HASH_MISMATCH
        assertEquals(IntegrityStatus.IMAGE_HASH_MISMATCH, verificationResult.status)
        assertTrue("Signature validity must remain true for record data", verificationResult.isSignatureValid)
        assertFalse("Image hash validity must be false due to byte alteration", verificationResult.isImageHashValid)
        assertEquals("Captured image hash does not match stored record digest", verificationResult.detailMessage)
    }
}

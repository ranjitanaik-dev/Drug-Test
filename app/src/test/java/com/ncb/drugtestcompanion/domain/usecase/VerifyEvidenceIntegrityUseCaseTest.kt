package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.security.EvidenceHasher
import com.ncb.drugtestcompanion.security.KeystoreSigner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VerifyEvidenceIntegrityUseCaseTest {

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
    fun `CASE 1 - Original image and valid signature return VERIFIED`() {
        val imageFile = tempFolder.newFile("original_capture.jpg").apply {
            writeBytes("FullFrameImageBytes".toByteArray())
        }
        val expectedSha256 = evidenceHasher.calculateSha256(imageFile)

        val record = TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = expectedSha256,
            signature = "validSignature123"
        )

        every { keystoreSigner.verifySignature(any(), "validSignature123", null) } returns true

        val result = verifyUseCase(record, imageFile)

        assertEquals(IntegrityStatus.VERIFIED, result.status)
        assertTrue(result.isSignatureValid)
        assertTrue(result.isImageHashValid)
    }

    @Test
    fun `CASE 2 - Modified image bytes return IMAGE_HASH_MISMATCH`() {
        val imageFile = tempFolder.newFile("tampered_capture.jpg").apply {
            writeBytes("ModifiedImageBytes".toByteArray())
        }

        val record = TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = "originalHash123",
            signature = "validSignature123"
        )

        every { keystoreSigner.verifySignature(any(), "validSignature123", null) } returns true

        val result = verifyUseCase(record, imageFile)

        assertEquals(IntegrityStatus.IMAGE_HASH_MISMATCH, result.status)
        assertTrue(result.isSignatureValid)
        assertFalse(result.isImageHashValid)
    }

    @Test
    fun `CASE 3 & 5 - Invalid digital signature returns SIGNATURE_INVALID`() {
        val imageFile = tempFolder.newFile("capture.jpg").apply {
            writeBytes("ImageBytes".toByteArray())
        }

        val record = TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = "hash123",
            signature = "tamperedSignature"
        )

        every { keystoreSigner.verifySignature(any(), "tamperedSignature", null) } returns false

        val result = verifyUseCase(record, imageFile)

        assertEquals(IntegrityStatus.SIGNATURE_INVALID, result.status)
        assertFalse(result.isSignatureValid)
    }

    @Test
    fun `CASE 4 - Missing image file returns INTEGRITY_CHECK_UNAVAILABLE`() {
        val missingFile = File(tempFolder.root, "non_existent.jpg")

        val record = TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.85f,
            distance = 3.25,
            imageSha256 = "hash123",
            signature = "validSignature123"
        )

        every { keystoreSigner.verifySignature(any(), "validSignature123", null) } returns true

        val result = verifyUseCase(record, missingFile)

        assertEquals(IntegrityStatus.INTEGRITY_CHECK_UNAVAILABLE, result.status)
        assertTrue(result.isSignatureValid)
        assertFalse(result.isImageHashValid)
    }
}

package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.security.EvidenceHasher
import com.ncb.drugtestcompanion.security.KeystoreSigner
import java.io.File
import javax.inject.Inject

enum class IntegrityStatus {
    VERIFIED,
    IMAGE_HASH_MISMATCH,
    SIGNATURE_INVALID,
    INTEGRITY_CHECK_UNAVAILABLE
}

data class IntegrityVerificationResult(
    val status: IntegrityStatus,
    val isSignatureValid: Boolean,
    val isImageHashValid: Boolean,
    val detailMessage: String
)

/**
 * UseCase for verifying evidence integrity by recalculating image SHA-256 hash and verifying
 * Keystore digital signature against stored [TestRecord].
 */
class VerifyEvidenceIntegrityUseCase @Inject constructor(
    private val evidenceHasher: EvidenceHasher,
    private val keystoreSigner: KeystoreSigner
) {

    operator fun invoke(record: TestRecord, imageFileOverride: File? = null): IntegrityVerificationResult {
        // 1. Verify Digital Signature
        val unsignedRecord = record.copy(signature = "")
        val isSignatureValid = keystoreSigner.verifySignature(unsignedRecord, record.signature)

        if (!isSignatureValid) {
            return IntegrityVerificationResult(
                status = IntegrityStatus.SIGNATURE_INVALID,
                isSignatureValid = false,
                isImageHashValid = false,
                detailMessage = "Digital signature verification failed - record data altered"
            )
        }

        // 2. Resolve persistent JPEG image file on disk
        val imageFile = imageFileOverride ?: if (record.imagePath.isNotBlank()) File(record.imagePath) else null

        if (imageFile == null || !imageFile.exists()) {
            return IntegrityVerificationResult(
                status = IntegrityStatus.INTEGRITY_CHECK_UNAVAILABLE,
                isSignatureValid = true,
                isImageHashValid = false,
                detailMessage = "Original image file not found on disk"
            )
        }

        // 3. Verify Full-Frame Image SHA-256 Digest
        val recalculatedHash = evidenceHasher.calculateSha256(imageFile)
        val isHashValid = recalculatedHash.equals(record.imageSha256, ignoreCase = true)

        if (!isHashValid) {
            return IntegrityVerificationResult(
                status = IntegrityStatus.IMAGE_HASH_MISMATCH,
                isSignatureValid = true,
                isImageHashValid = false,
                detailMessage = "Captured image hash does not match stored record digest"
            )
        }

        return IntegrityVerificationResult(
            status = IntegrityStatus.VERIFIED,
            isSignatureValid = true,
            isImageHashValid = true,
            detailMessage = "Digital signature and full-frame image integrity verified"
        )
    }
}

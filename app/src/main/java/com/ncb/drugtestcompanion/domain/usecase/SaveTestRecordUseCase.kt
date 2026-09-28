package com.ncb.drugtestcompanion.domain.usecase

import android.content.Context
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestKitProfile
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.repository.TestRecordRepository
import com.ncb.drugtestcompanion.location.LocationProvider
import com.ncb.drugtestcompanion.security.EvidenceHasher
import com.ncb.drugtestcompanion.security.KeystoreSigner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * SaveTestRecordUseCase creates, digitally signs, and persists an evidence [TestRecord] ONLY after classification successfully produces a [ClassificationResult].
 * Persists the actual full-frame captured JPEG into app-private storage BEFORE calculating SHA-256 and signing.
 */
class SaveTestRecordUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val testRecordRepository: TestRecordRepository,
    private val evidenceHasher: EvidenceHasher,
    private val locationProvider: LocationProvider,
    private val keystoreSigner: KeystoreSigner
) {

    suspend operator fun invoke(
        imagePath: String,
        kitProfile: TestKitProfile,
        classificationResult: ClassificationResult
    ): TestRecord {
        val timestamp = System.currentTimeMillis()
        val testId = generateUniqueTestId(timestamp)

        // 1. Persist captured JPEG into app-private persistent internal storage
        val persistentImageFile = persistCapturedImage(imagePath, testId)
        val sha256 = evidenceHasher.calculateSha256(persistentImageFile)
        val location = locationProvider.getCurrentLocation()

        // 2. Build unsigned TestRecord referencing persistent imagePath, address & sha256
        val unsignedRecord = TestRecord(
            testId = testId,
            timestamp = timestamp,
            kitId = kitProfile.kitId,
            referenceCardProfileId = kitProfile.referenceCardProfileId,
            result = classificationResult.result.name,
            confidence = classificationResult.confidence,
            distance = classificationResult.distance,
            imageSha256 = sha256,
            imagePath = persistentImageFile.absolutePath,
            operatorId = "OP-LOCAL",
            latitude = location.latitude,
            longitude = location.longitude,
            address = location.address,
            locationStatus = location.status,
            signature = "",
            signatureAlgorithm = "SHA256withRSA"
        )

        // 3. Digitally sign canonical record representation using Android Keystore
        val digitalSignature = keystoreSigner.sign(unsignedRecord)
        val signedRecord = unsignedRecord.copy(signature = digitalSignature)

        testRecordRepository.saveTestRecord(signedRecord)
        return signedRecord
    }

    private fun persistCapturedImage(sourcePath: String, testId: String): File {
        val sourceFile = File(sourcePath)
        val evidenceDir = File(context.filesDir, "persistent_evidence").apply {
            if (!exists()) mkdirs()
        }
        val destinationFile = File(evidenceDir, "evidence_${testId}.jpg")

        if (sourceFile.exists() && sourceFile.absolutePath != destinationFile.absolutePath) {
            try {
                sourceFile.copyTo(destinationFile, overwrite = true)
                return destinationFile
            } catch (_: Throwable) {
                return sourceFile
            }
        }
        return if (sourceFile.exists()) sourceFile else destinationFile
    }

    fun generateUniqueTestId(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val dateStr = dateFormat.format(Date(timestamp))
        val uuidShort = UUID.randomUUID().toString().take(8).uppercase(Locale.US)
        return "DT-$dateStr-$uuidShort"
    }
}

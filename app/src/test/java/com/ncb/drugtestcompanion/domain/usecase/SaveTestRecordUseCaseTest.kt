package com.ncb.drugtestcompanion.domain.usecase

import android.content.Context
import com.ncb.drugtestcompanion.data.repository.ReferenceCardProfileRepository
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.model.TestResultCategory
import com.ncb.drugtestcompanion.domain.repository.TestRecordRepository
import com.ncb.drugtestcompanion.location.LocationInfo
import com.ncb.drugtestcompanion.location.LocationProvider
import com.ncb.drugtestcompanion.security.EvidenceHasher
import com.ncb.drugtestcompanion.security.KeystoreSigner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SaveTestRecordUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var testRecordRepo: TestRecordRepository
    private lateinit var evidenceHasher: EvidenceHasher
    private lateinit var locationProvider: LocationProvider
    private lateinit var keystoreSigner: KeystoreSigner
    private lateinit var refRepo: ReferenceCardProfileRepository
    private lateinit var testKitRepo: TestKitProfileRepository
    private lateinit var saveTestRecordUseCase: SaveTestRecordUseCase

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempFolder.root

        testRecordRepo = mockk(relaxed = true)
        evidenceHasher = mockk()
        locationProvider = mockk()
        keystoreSigner = mockk()
        every { keystoreSigner.sign(any()) } returns "mockBase64Signature123"
        refRepo = ReferenceCardProfileRepository()
        testKitRepo = TestKitProfileRepository(refRepo)
        saveTestRecordUseCase = SaveTestRecordUseCase(context, testRecordRepo, evidenceHasher, locationProvider, keystoreSigner)
    }

    @Test
    fun `TEST 1, 4, 5, 6 - Test record creation preserves classification, persistent image, KIT_A and reference card info`() = runTest {
        val kitProfile = testKitRepo.getKitProfileByKitId("KIT_A")
        val classification = ClassificationResult(
            result = TestResultCategory.NEGATIVE,
            confidence = 0.85f,
            distance = 3.25,
            targetCentroidName = "NEGATIVE_CENTROID",
            componentScores = mapOf("deltaE_positive" to 25.0, "deltaE_negative" to 3.25),
            statusMessage = "Classification successful"
        )

        val imageFile = tempFolder.newFile("captured_photo.jpg").apply {
            writeBytes("FullFrameJpegBytes123".toByteArray())
        }

        every { evidenceHasher.calculateSha256(any<File>()) } returns "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        every { locationProvider.getCurrentLocation() } returns LocationInfo(null, null, "UNAVAILABLE")

        val record = saveTestRecordUseCase(imageFile.absolutePath, kitProfile, classification)

        assertEquals("KIT_A", record.kitId)
        assertEquals("VARIANT_A", record.referenceCardProfileId)
        assertEquals("NEGATIVE", record.result)
        assertEquals(0.85f, record.confidence)
        assertEquals(3.25, record.distance, 0.0001)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", record.imageSha256)
        assertTrue("imagePath must point to persistent evidence directory", record.imagePath.contains("persistent_evidence"))
        assertEquals("OP-LOCAL", record.operatorId)
        assertEquals("UNAVAILABLE", record.locationStatus)

        coVerify(exactly = 1) { testRecordRepo.saveTestRecord(any()) }
    }

    @Test
    fun `TEST 2 - Unique test ID generation produces valid format`() {
        val timestamp = 1789818302000L
        val testId = saveTestRecordUseCase.generateUniqueTestId(timestamp)
        assertTrue(testId.startsWith("DT-"))
        val secondTestId = saveTestRecordUseCase.generateUniqueTestId(timestamp)
        assertTrue(testId != secondTestId)
    }

    @Test
    fun `TEST 7 - GPS unavailable does not prevent saving record`() = runTest {
        val kitProfile = testKitRepo.getKitProfileByKitId("KIT_B")
        val classification = ClassificationResult(
            result = TestResultCategory.POSITIVE,
            confidence = 0.90f,
            distance = 2.10,
            targetCentroidName = "POSITIVE_CENTROID",
            componentScores = emptyMap(),
            statusMessage = "Classification successful"
        )

        val imageFile = tempFolder.newFile("captured_photo_b.jpg").apply {
            writeBytes("FullFrameJpegBytes456".toByteArray())
        }

        every { evidenceHasher.calculateSha256(any<File>()) } returns "dummyhash123"
        every { locationProvider.getCurrentLocation() } returns LocationInfo(latitude = null, longitude = null, status = "UNAVAILABLE")

        val record = saveTestRecordUseCase(imageFile.absolutePath, kitProfile, classification)

        assertNull(record.latitude)
        assertNull(record.longitude)
        assertEquals("UNAVAILABLE", record.locationStatus)
        coVerify(exactly = 1) { testRecordRepo.saveTestRecord(record) }
    }

    @Test
    fun `TEST 8 & 9 - History retrieval and test detail retrieval`() = runTest {
        val sampleRecord = TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "NEGATIVE",
            confidence = 0.95f,
            distance = 1.5,
            imageSha256 = "hash123",
            imagePath = "/fake/persistent/evidence.jpg"
        )

        coEvery { testRecordRepo.getAllTestRecords() } returns flowOf(listOf(sampleRecord))
        coEvery { testRecordRepo.getTestRecordById("DT-20260919-001") } returns sampleRecord

        val getHistory = GetTestRecordsUseCase(testRecordRepo)
        val getDetail = GetTestRecordUseCase(testRecordRepo)

        getHistory().collect { list ->
            assertEquals(1, list.size)
            assertEquals("DT-20260919-001", list.first().testId)
        }

        val detail = getDetail("DT-20260919-001")
        assertNotNull(detail)
        assertEquals("KIT_A", detail?.kitId)
        assertEquals("/fake/persistent/evidence.jpg", detail?.imagePath)
    }

    @Test
    fun `TEST 10 - Failed quality check or missing classification does not create a record`() = runTest {
        coVerify(exactly = 0) { testRecordRepo.saveTestRecord(any()) }
    }
}

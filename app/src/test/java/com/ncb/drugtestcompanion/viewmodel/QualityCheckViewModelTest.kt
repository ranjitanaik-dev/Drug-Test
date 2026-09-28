package com.ncb.drugtestcompanion.viewmodel

import android.content.Context
import app.cash.turbine.test
import com.ncb.drugtestcompanion.cv.ColorCalibrator
import com.ncb.drugtestcompanion.cv.LabColor
import com.ncb.drugtestcompanion.cv.QualityGate
import com.ncb.drugtestcompanion.cv.QualityResult
import com.ncb.drugtestcompanion.cv.RoiExtractionResult
import com.ncb.drugtestcompanion.cv.RoiFeatureVector
import com.ncb.drugtestcompanion.data.repository.ReferenceCardProfileRepository
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.model.TestResultCategory
import com.ncb.drugtestcompanion.domain.usecase.ClassifyResultUseCase
import com.ncb.drugtestcompanion.domain.usecase.GeneratePdfReportUseCase
import com.ncb.drugtestcompanion.domain.usecase.SaveTestRecordUseCase
import com.ncb.drugtestcompanion.ml.DrugTestClassifier
import com.ncb.drugtestcompanion.ui.qualitycheck.QualityCheckUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class QualityCheckViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var qualityGate: QualityGate
    private lateinit var colorCalibrator: ColorCalibrator
    private lateinit var classifyResultUseCase: ClassifyResultUseCase
    private lateinit var drugTestClassifier: DrugTestClassifier
    private lateinit var refRepo: ReferenceCardProfileRepository
    private lateinit var testKitRepo: TestKitProfileRepository
    private lateinit var saveTestRecordUseCase: SaveTestRecordUseCase
    private lateinit var generatePdfReportUseCase: GeneratePdfReportUseCase
    private lateinit var viewModel: QualityCheckViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempFolder.root
        qualityGate = mockk()
        colorCalibrator = ColorCalibrator()
        classifyResultUseCase = ClassifyResultUseCase(colorCalibrator)
        drugTestClassifier = mockk(relaxed = true)
        refRepo = ReferenceCardProfileRepository()
        testKitRepo = TestKitProfileRepository(refRepo)
        saveTestRecordUseCase = mockk()
        generatePdfReportUseCase = mockk()
        viewModel = QualityCheckViewModel(
            context, qualityGate, classifyResultUseCase, drugTestClassifier, testKitRepo, saveTestRecordUseCase, generatePdfReportUseCase
        ).apply {
            defaultDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Checking`() {
        assertEquals(QualityCheckUiState.Checking, viewModel.uiState.value)
    }

    @Test
    fun `TEST 1, 2, 3, 5 - ResultScreen and saved TestRecord contain identical result, confidence, and distance`() = runTest {
        val testPath = tempFolder.newFile("valid_negative.jpg").absolutePath
        val profileA = refRepo.getProfileByVariantId("VARIANT_A")
        val negativeFeatureVector = RoiFeatureVector(
            averageLab = LabColor(l = 89.0, a = 1.0, b = 1.0),
            averageHsv = floatArrayOf(0f, 0.05f, 0.9f)
        )
        val roiSuccess = mockk<RoiExtractionResult.Success>()
        coEvery { roiSuccess.featureVector } returns negativeFeatureVector

        val expectedClassification = ClassificationResult(
            result = TestResultCategory.NEGATIVE,
            confidence = 0.95f,
            distance = 1.25,
            targetCentroidName = "TFLITE_MOBILENETV2",
            statusMessage = "TFLite ML Classification Completed"
        )
        coEvery { drugTestClassifier.classify(any()) } returns expectedClassification

        coEvery { qualityGate.analyzeImage(testPath, profileA) } returns QualityResult.ImageValid(
            roiExtractionResult = roiSuccess,
            mlRoiBitmap = mockk(relaxed = true)
        )

        // Mock saveTestRecordUseCase to return a TestRecord matching the ClassificationResult EXACTLY
        coEvery { saveTestRecordUseCase.invoke(testPath, any(), any()) } answers {
            val classResult = arg<ClassificationResult>(2)
            TestRecord(
                testId = "DT-20260919-001",
                timestamp = 1000L,
                kitId = "KIT_A",
                referenceCardProfileId = "VARIANT_A",
                result = classResult.result.name,
                confidence = classResult.confidence,
                distance = classResult.distance,
                imageSha256 = "dummyhash123"
            )
        }

        viewModel.uiState.test {
            assertEquals(QualityCheckUiState.Checking, awaitItem())

            viewModel.runQualityCheck(testPath, profileA)

            val validItem = awaitItem()
            assertTrue(validItem is QualityCheckUiState.Valid)
            val state = validItem as QualityCheckUiState.Valid

            val classification = state.classificationResult
            val savedRecord = state.savedRecord

            assertNotNull(classification)
            assertNotNull(savedRecord)

            // EXACT MATCH VERIFICATION
            assertEquals(classification!!.result.name, savedRecord!!.result)
            assertEquals(classification.confidence, savedRecord.confidence, 0.0001f)
            assertEquals(classification.distance, savedRecord.distance, 0.0001)
        }
    }

    @Test
    fun `TEST 6 & 7 - Repeated runQualityCheck calls for same image path do NOT trigger duplicate classification or saves`() = runTest {
        val testPath = tempFolder.newFile("single_pass.jpg").absolutePath
        val profileA = refRepo.getProfileByVariantId("VARIANT_A")
        val featureVector = RoiFeatureVector(
            averageLab = LabColor(l = 44.0, a = 54.0, b = 27.0),
            averageHsv = floatArrayOf(0f, 0.8f, 0.7f)
        )
        val roiSuccess = mockk<RoiExtractionResult.Success>()
        coEvery { roiSuccess.featureVector } returns featureVector

        val expectedClassification = ClassificationResult(
            result = TestResultCategory.POSITIVE,
            confidence = 0.98f,
            distance = 0.50,
            targetCentroidName = "TFLITE_MOBILENETV2",
            statusMessage = "TFLite ML Classification Completed"
        )
        coEvery { drugTestClassifier.classify(any()) } returns expectedClassification

        coEvery { qualityGate.analyzeImage(testPath, profileA) } returns QualityResult.ImageValid(
            roiExtractionResult = roiSuccess,
            mlRoiBitmap = mockk(relaxed = true)
        )

        coEvery { saveTestRecordUseCase.invoke(testPath, any(), any()) } returns TestRecord(
            testId = "DT-20260919-001",
            timestamp = 1000L,
            kitId = "KIT_A",
            referenceCardProfileId = "VARIANT_A",
            result = "POSITIVE",
            confidence = 0.98f,
            distance = 0.50,
            imageSha256 = "hash123"
        )

        viewModel.runQualityCheck(testPath, profileA)
        testScheduler.advanceUntilIdle()

        // Call runQualityCheck a second and third time for the same path (simulating recomposition)
        viewModel.runQualityCheck(testPath, profileA)
        viewModel.runQualityCheck(testPath, profileA)
        testScheduler.advanceUntilIdle()

        // Verify saveTestRecordUseCase was invoked EXACTLY ONCE
        coVerify(exactly = 1) { saveTestRecordUseCase.invoke(testPath, any(), any()) }
    }
}

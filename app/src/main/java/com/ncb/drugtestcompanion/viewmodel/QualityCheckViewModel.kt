package com.ncb.drugtestcompanion.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncb.drugtestcompanion.cv.QualityGate
import com.ncb.drugtestcompanion.cv.QualityResult
import com.ncb.drugtestcompanion.data.repository.TestKitProfileRepository
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.usecase.ClassifyResultUseCase
import com.ncb.drugtestcompanion.domain.usecase.GeneratePdfReportUseCase
import com.ncb.drugtestcompanion.domain.usecase.SaveTestRecordUseCase
import com.ncb.drugtestcompanion.ml.DrugTestClassifier
import com.ncb.drugtestcompanion.pdf.ReportState
import com.ncb.drugtestcompanion.ui.qualitycheck.QualityCheckUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * ViewModel managing QualityCheck screen UI state.
 *
 * Executes:
 * 1. Image quality checking & reference-card detection
 * 2. Reference card 3x3 RGB color calibration transform
 * 3. Test-device detection & 386x133 ML ROI extraction
 * 4. Apply 3x3 RGB calibration matrix to ML ROI
 * 5. TFLite classification on Calibrated ML ROI (Single Source of Truth!)
 * 6. Evidence record persistence & PDF report generation
 */
@HiltViewModel
class QualityCheckViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val qualityGate: QualityGate,
    private val classifyResultUseCase: ClassifyResultUseCase,
    private val drugTestClassifier: DrugTestClassifier,
    private val testKitProfileRepository: TestKitProfileRepository,
    private val saveTestRecordUseCase: SaveTestRecordUseCase,
    private val generatePdfReportUseCase: GeneratePdfReportUseCase
) : ViewModel() {

    var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val _uiState = MutableStateFlow<QualityCheckUiState>(QualityCheckUiState.Checking)
    val uiState: StateFlow<QualityCheckUiState> = _uiState.asStateFlow()

    private var processedImagePath: String? = null

    fun runQualityCheck(
        imagePath: String,
        profile: ReferenceCardProfile? = null
    ) {
        if (processedImagePath == imagePath) {
            return
        }
        processedImagePath = imagePath

        viewModelScope.launch {
            _uiState.value = QualityCheckUiState.Checking

            val result = withContext(defaultDispatcher) {
                qualityGate.analyzeImage(imagePath, profile)
            }

            _uiState.value = when (result) {
                is QualityResult.ImageValid -> {
                    val kitProfile = testKitProfileRepository.getKitProfileByKitId(profile?.variantId ?: "KIT_A")
                    val rawMlRoiBitmap = result.rawMlRoiBitmap
                    val calibratedMlRoiBitmap = result.calibratedMlRoiBitmap ?: rawMlRoiBitmap

                    val finalClassification: ClassificationResult? = if (calibratedMlRoiBitmap != null) {
                        val sourceName = File(imagePath).nameWithoutExtension

                        // Save both Debug Images (Requirements 12)
                        if (rawMlRoiBitmap != null) {
                            saveDebugMlInputImage(rawMlRoiBitmap, "debug_raw_roi", sourceName)
                        }
                        saveDebugMlInputImage(calibratedMlRoiBitmap, "debug_calibrated_roi", sourceName)

                        // Evaluate RAW ROI for logging comparison (Requirement 14)
                        if (rawMlRoiBitmap != null) {
                            val rawClassification = drugTestClassifier.classify(rawMlRoiBitmap)
                            val negP = rawClassification.componentScores["negative_prob"] ?: 0.0
                            val posP = rawClassification.componentScores["positive_prob"] ?: 0.0
                            val incP = rawClassification.componentScores["inconclusive_prob"] ?: 0.0
                            logClassificationValidation("RAW: NEGATIVE=$negP, POSITIVE=$posP, INCONCLUSIVE=$incP")
                        }

                        // Evaluate CALIBRATED ROI as FINAL SOURCE OF TRUTH (Requirement 13 & 15)
                        val calibClassification = drugTestClassifier.classify(calibratedMlRoiBitmap)
                        val cNegP = calibClassification.componentScores["negative_prob"] ?: 0.0
                        val cPosP = calibClassification.componentScores["positive_prob"] ?: 0.0
                        val cIncP = calibClassification.componentScores["inconclusive_prob"] ?: 0.0

                        logClassificationValidation("CALIBRATED: NEGATIVE=$cNegP, POSITIVE=$cPosP, INCONCLUSIVE=$cIncP")
                        logClassificationValidation("ML_PIPELINE: TFLite classification completed = YES")
                        logClassificationValidation("ML_PIPELINE: FINAL RESULT = ${calibClassification.result.name}")
                        logClassificationValidation("ML_PIPELINE: FINAL CONFIDENCE = ${calibClassification.confidence}")

                        calibClassification
                    } else {
                        null
                    }

                    var savedRecord: TestRecord? = null
                    if (finalClassification != null) {
                        savedRecord = saveTestRecordUseCase(imagePath, kitProfile, finalClassification)
                    }

                    QualityCheckUiState.Valid(
                        classificationResult = finalClassification,
                        savedRecord = savedRecord
                    )
                }

                is QualityResult.ImageInvalid -> {
                    QualityCheckUiState.Invalid(result.reason)
                }
            }
        }
    }

    private fun saveDebugMlInputImage(bitmap: Bitmap, prefix: String, sourceName: String): String {
        return try {
            val debugDir = File(context.filesDir, "debug_ml_inputs").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            val outputFile = File(debugDir, "${prefix}_${timestamp}_${sourceName}.png")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outputFile.absolutePath
        } catch (e: Throwable) {
            "SAVE_FAILED: ${e.message}"
        }
    }

    // ---------------------------------------------------------------------
    // PDF REPORT
    // ---------------------------------------------------------------------

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    fun generatePdfReport(record: TestRecord) {
        viewModelScope.launch {
            _reportState.value = ReportState.Generating
            val result = withContext(Dispatchers.IO) {
                generatePdfReportUseCase(record)
            }
            result.fold(
                onSuccess = { file -> _reportState.value = ReportState.Ready(file) },
                onFailure = { error -> _reportState.value = ReportState.Error(error.message ?: "Report generation failed") }
            )
        }
    }

    fun resetReportState() {
        _reportState.value = ReportState.Idle
    }

    fun resetCheck() {
        processedImagePath = null
        _uiState.value = QualityCheckUiState.Checking
        _reportState.value = ReportState.Idle
    }

    private fun logClassificationValidation(msg: String) {
        try {
            Log.d("ClassificationValidation", msg)
        } catch (_: Throwable) {
            println("[ClassificationValidation] $msg")
        }
    }

    private fun logTfliteDebug(msg: String) {
        try {
            Log.d("TFLITE_DEBUG", msg)
        } catch (_: Throwable) {
            println("[TFLITE_DEBUG] $msg")
        }
    }
}

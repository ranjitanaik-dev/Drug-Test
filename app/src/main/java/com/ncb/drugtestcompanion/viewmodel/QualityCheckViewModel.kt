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
 * 2. Test-device detection & 386x133 ML ROI extraction
 * 3. TensorFlow Lite MobileNetV2 classification (Single Source of Truth!)
 * 4. Evidence record persistence
 * 5. PDF report generation
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
                    val roiResult = result.roiExtractionResult
                    val mlRoiBitmap = result.mlRoiBitmap ?: roiResult?.roiBitmap

                    // -----------------------------------------------------
                    // TFLITE CLASSIFICATION (FINAL SOURCE OF TRUTH)
                    // -----------------------------------------------------
                    val tfliteResult: ClassificationResult? = if (mlRoiBitmap != null) {
                        logTfliteDebug("inputBitmapWidth=${mlRoiBitmap.width}")
                        logTfliteDebug("inputBitmapHeight=${mlRoiBitmap.height}")
                        logTfliteDebug("inputBitmapSource=reaction_result_window_roi")

                        val savedPath = saveDebugMlInputImage(mlRoiBitmap, imagePath)
                        logTfliteDebug("savedInputPath=$savedPath")
                        logTfliteDebug("inputSize=${mlRoiBitmap.width}x${mlRoiBitmap.height}")

                        val classification = drugTestClassifier.classify(mlRoiBitmap)

                        logClassificationValidation("ML_PIPELINE: TFLite classification completed = YES")
                        logClassificationValidation("ML_PIPELINE: FINAL RESULT = ${classification.result.name}")
                        logClassificationValidation("ML_PIPELINE: FINAL CONFIDENCE = ${classification.confidence}")

                        classification
                    } else if (roiResult?.featureVector != null) {
                        // Fallback classical classifier if ML ROI bitmap is completely missing
                        val fallback = classifyResultUseCase(roiResult.featureVector, kitProfile.centroids)
                        logClassificationValidation("ML_PIPELINE: Fallback classical result used = ${fallback.result.name}")
                        fallback
                    } else {
                        null
                    }

                    var savedRecord: TestRecord? = null
                    if (tfliteResult != null) {
                        savedRecord = saveTestRecordUseCase(imagePath, kitProfile, tfliteResult)
                    }

                    QualityCheckUiState.Valid(
                        classificationResult = tfliteResult,
                        savedRecord = savedRecord
                    )
                }

                is QualityResult.ImageInvalid -> {
                    QualityCheckUiState.Invalid(result.reason)
                }
            }
        }
    }

    private fun saveDebugMlInputImage(bitmap: Bitmap, sourcePath: String): String {
        return try {
            val debugDir = File(context.filesDir, "debug_ml_inputs").apply {
                if (!exists()) mkdirs()
            }
            val originalName = File(sourcePath).nameWithoutExtension
            val timestamp = System.currentTimeMillis()
            val outputFile = File(debugDir, "ml_input_${timestamp}_${originalName}.png")

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

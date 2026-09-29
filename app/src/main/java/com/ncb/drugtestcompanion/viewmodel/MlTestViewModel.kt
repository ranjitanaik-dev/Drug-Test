package com.ncb.drugtestcompanion.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncb.drugtestcompanion.data.ml.LiteRtImageClassifier
import com.ncb.drugtestcompanion.data.ml.MlClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.usecase.GeneratePdfReportUseCase
import com.ncb.drugtestcompanion.domain.usecase.IntegrityVerificationResult
import com.ncb.drugtestcompanion.domain.usecase.VerifyEvidenceIntegrityUseCase
import com.ncb.drugtestcompanion.location.LocationProvider
import com.ncb.drugtestcompanion.pdf.ReportState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DemoTestResult(
    val classification: MlClassificationResult,
    val testRecord: TestRecord
)

/**
 * ViewModel for managing ML Test screen state and evaluating [LiteRtImageClassifier] with complete Digital Record demonstration.
 */
@HiltViewModel
class MlTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationProvider: LocationProvider,
    private val generatePdfReportUseCase: GeneratePdfReportUseCase,
    private val verifyEvidenceIntegrityUseCase: VerifyEvidenceIntegrityUseCase
) : ViewModel() {

    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _demoResult = MutableStateFlow<DemoTestResult?>(null)
    val demoResult: StateFlow<DemoTestResult?> = _demoResult.asStateFlow()

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun runMlTest() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _reportState.value = ReportState.Idle
            try {
                val demo = withContext(ioDispatcher) {
                    val assetManager = context.assets
                    val roiPath = "test_rois/positive_test_roi.jpg"
                    val inputStream = assetManager.open(roiPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap == null) {
                        throw IllegalStateException("Failed to decode synthetic ROI bitmap from asset: $roiPath")
                    }

                    val classifier = LiteRtImageClassifier(context)
                    val classification = classifier.classify(bitmap)
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }

                    // Get actual current device location
                    val locationInfo = locationProvider.getCurrentLocation()
                    val timestamp = System.currentTimeMillis()

                    val record = TestRecord(
                        testId = "NRK-DEMO-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(timestamp))}",
                        timestamp = timestamp,
                        kitId = "RapidFor™ K2 Synthetic Cannabis Kit",
                        referenceCardProfileId = "CAL-CARD-V2",
                        result = classification.label,
                        confidence = classification.confidence,
                        distance = 1.24,
                        imageSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                        imagePath = "",
                        operatorId = "OP-1047",
                        latitude = locationInfo.latitude,
                        longitude = locationInfo.longitude,
                        address = locationInfo.address,
                        locationStatus = locationInfo.status,
                        signature = "RSA2048_DIGITAL_SIGNATURE_VERIFIED",
                        signatureAlgorithm = "SHA256withRSA"
                    )

                    DemoTestResult(classification, record)
                }
                _demoResult.value = demo
            } catch (e: Throwable) {
                _error.value = e.message ?: "An unexpected error occurred during ML evaluation"
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun verifyIntegrity(record: TestRecord): IntegrityVerificationResult {
        return verifyEvidenceIntegrityUseCase(record)
    }

    fun resetReportState() {
        _reportState.value = ReportState.Idle
    }
}

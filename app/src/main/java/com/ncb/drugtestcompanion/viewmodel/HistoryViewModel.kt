package com.ncb.drugtestcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncb.drugtestcompanion.BuildConfig
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.usecase.GeneratePdfReportUseCase
import com.ncb.drugtestcompanion.domain.usecase.GetTestRecordsUseCase
import com.ncb.drugtestcompanion.domain.usecase.IntegrityVerificationResult
import com.ncb.drugtestcompanion.domain.usecase.VerifyEvidenceIntegrityUseCase
import com.ncb.drugtestcompanion.pdf.ReportState
import com.ncb.drugtestcompanion.security.DebugImageTamperHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getTestRecordsUseCase: GetTestRecordsUseCase,
    private val verifyEvidenceIntegrityUseCase: VerifyEvidenceIntegrityUseCase,
    private val generatePdfReportUseCase: GeneratePdfReportUseCase,
    private val debugImageTamperHelper: DebugImageTamperHelper
) : ViewModel() {

    val testRecords: StateFlow<List<TestRecord>> = getTestRecordsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun verifyRecordIntegrity(record: TestRecord, imageFile: File? = null): IntegrityVerificationResult {
        return verifyEvidenceIntegrityUseCase(record, imageFile)
    }

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

    fun tamperImageForDebug(record: TestRecord): IntegrityVerificationResult? {
        if (!BuildConfig.DEBUG) return null
        val success = debugImageTamperHelper.tamperImageBytes(record.imagePath)
        return if (success) {
            verifyRecordIntegrity(record)
        } else null
    }

    fun restoreImageForDebug(record: TestRecord): IntegrityVerificationResult? {
        if (!BuildConfig.DEBUG) return null
        val success = debugImageTamperHelper.restoreImageBytes(record.imagePath)
        return if (success) {
            verifyRecordIntegrity(record)
        } else null
    }
}

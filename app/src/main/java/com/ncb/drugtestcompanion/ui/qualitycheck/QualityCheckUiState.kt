package com.ncb.drugtestcompanion.ui.qualitycheck

import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.QualityFailureReason
import com.ncb.drugtestcompanion.domain.model.TestRecord

sealed class QualityCheckUiState {
    data object Checking : QualityCheckUiState()
    data class Valid(
        val classificationResult: ClassificationResult? = null,
        val savedRecord: TestRecord? = null
    ) : QualityCheckUiState()
    data class Invalid(val reason: QualityFailureReason) : QualityCheckUiState()
}

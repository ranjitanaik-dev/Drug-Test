package com.ncb.drugtestcompanion.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ncb.drugtestcompanion.R

/**
 * Helper function for displaying localized test result categories (POSITIVE, NEGATIVE, INCONCLUSIVE)
 * while preserving machine-readable database and log values.
 */
@Composable
fun getLocalizedResultCategory(resultCategoryName: String): String {
    return when (resultCategoryName.uppercase()) {
        "POSITIVE" -> stringResource(R.string.result_positive)
        "NEGATIVE" -> stringResource(R.string.result_negative)
        "INCONCLUSIVE" -> stringResource(R.string.result_inconclusive)
        else -> resultCategoryName
    }
}

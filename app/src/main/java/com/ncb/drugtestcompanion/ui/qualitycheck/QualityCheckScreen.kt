package com.ncb.drugtestcompanion.ui.qualitycheck

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncb.drugtestcompanion.domain.model.QualityFailureReason
import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.pdf.ReportIntentHelper
import com.ncb.drugtestcompanion.ui.result.ResultScreenContent
import com.ncb.drugtestcompanion.viewmodel.QualityCheckViewModel

@Composable
fun QualityCheckScreen(
    imagePath: String,
    selectedProfile: ReferenceCardProfile? = null,
    onRetakePhoto: () -> Unit,
    onProceed: () -> Unit = {},
    viewModel: QualityCheckViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(imagePath, selectedProfile) {
        viewModel.runQualityCheck(imagePath, selectedProfile)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is QualityCheckUiState.Checking -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking image quality & classifying result...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                is QualityCheckUiState.Valid -> {
                    val classification = state.classificationResult
                    if (classification != null) {
                        val reportState by viewModel.reportState.collectAsState()
                        
                        ResultScreenContent(
                            classification = classification,
                            testRecord = state.savedRecord,
                            reportState = reportState,
                            onRetakePhoto = onRetakePhoto,
                            onExportPdf = {
                                state.savedRecord?.let { record ->
                                    viewModel.generatePdfReport(record)
                                }
                            },
                            onSharePdf = {
                                state.savedRecord?.let { record ->
                                    viewModel.generatePdfReport(record)
                                    // Wait, if we share, we just want to export and then share.
                                    // Let's just handle it via the ResultScreenContent taking ReportState.
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "PASS",
                            color = Color(0xFF2E7D32),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The captured image meets all quality standards.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onProceed) {
                            Text("Continue")
                        }
                    }
                }

                is QualityCheckUiState.Invalid -> {
                    Text(
                        text = "QUALITY CHECK FAILED",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = getHumanReadableReason(state.reason),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onRetakePhoto) {
                        Text("Retake Photo")
                    }
                }
            }
        }
    }
}

private fun getHumanReadableReason(reason: QualityFailureReason): String {
    return when (reason) {
        QualityFailureReason.BLUR -> "Image is blurry. Please hold the device steady and try again."
        QualityFailureReason.UNDEREXPOSED -> "Image is too dark (underexposed). Please ensure adequate lighting."
        QualityFailureReason.OVEREXPOSED -> "Image is too bright (overexposed). Please avoid direct harsh glare."
        QualityFailureReason.CARD_NOT_VISIBLE -> "Reference card is not visible in frame."
        QualityFailureReason.FRAMING_ERROR -> "Image framing is incorrect."
        QualityFailureReason.LOW_RESOLUTION -> "Image resolution is too low."
    }
}

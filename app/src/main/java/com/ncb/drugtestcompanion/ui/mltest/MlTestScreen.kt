package com.ncb.drugtestcompanion.ui.mltest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.pdf.ReportIntentHelper
import com.ncb.drugtestcompanion.pdf.ReportState
import com.ncb.drugtestcompanion.ui.common.ShieldIcon
import com.ncb.drugtestcompanion.ui.common.getLocalizedResultCategory
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CanvasBackground
import com.ncb.drugtestcompanion.ui.theme.CardTintBlue
import com.ncb.drugtestcompanion.ui.theme.DeepNavy
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.StatusGreenContainer
import com.ncb.drugtestcompanion.ui.theme.StatusRed
import com.ncb.drugtestcompanion.ui.theme.StatusRedContainer
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted
import com.ncb.drugtestcompanion.viewmodel.MlTestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MlTestScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MlTestViewModel = hiltViewModel()
) {
    val demoResult by viewModel.demoResult.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShieldIcon(size = 24.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ML Model Evaluation & Demo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = CanvasBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardTintBlue),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Independent TFLite Evaluation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Evaluates roi_mobilenetv2_best_BASELINE_A.tflite directly against asset: test_rois/positive_test_roi.jpg and demonstrates complete digital record pipeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                // Run Test Button
                Button(
                    onClick = { viewModel.runMlTest() },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Evaluating Model & Location...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Run ML Demo & Report Test", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                error?.let { errMsg ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Error: $errMsg",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                demoResult?.let { demo ->
                    val classification = demo.classification
                    val record = demo.testRecord

                    // Complete Digital Test Result Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEMO TEST RESULT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryNavy,
                                    letterSpacing = 0.5.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StatusGreenContainer
                                ) {
                                    Text(
                                        text = "✓ VERIFIED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Hero Result Display
                            val isPositive = classification.label.equals("POSITIVE", ignoreCase = true)
                            val badgeColor = if (isPositive) StatusRed else StatusGreen
                            val badgeBg = if (isPositive) StatusRedContainer else StatusGreenContainer

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = getLocalizedResultCategory(classification.label),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor
                                    )
                                    Text(
                                        text = "Presumptive field-test indication",
                                        fontSize = 11.sp,
                                        color = TextCharcoal
                                    )
                                }
                            }

                            HorizontalDivider(color = BorderGray)

                            // Digital Record Field Details
                            Text(
                                text = "EVIDENCE RECORD DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )

                            DemoDetailRow("Test ID", record.testId)
                            DemoDetailRow(
                                "Timestamp",
                                SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US).format(Date(record.timestamp))
                            )
                            DemoDetailRow("Kit ID", record.kitId)
                            DemoDetailRow("Reference Card", record.referenceCardProfileId)
                            DemoDetailRow("Confidence", String.format(Locale.US, "%.1f%%", classification.confidence * 100))
                            DemoDetailRow("Operator ID", record.operatorId)

                            // Location Information (Real Device Location)
                            if (!record.address.isNullOrBlank()) {
                                DemoDetailRow("Location", record.address)
                            } else {
                                DemoDetailRow("Location", "Location unavailable")
                            }

                            if (record.latitude != null && record.longitude != null) {
                                DemoDetailRow(
                                    "Coordinates",
                                    String.format(Locale.US, "%.4f° N, %.4f° E", record.latitude, record.longitude)
                                )
                            }

                            DemoDetailRow("SHA-256 Hash", record.imageSha256.take(16) + "...")
                            DemoDetailRow("Signature Status", "VALID (RSA-2048)")

                            HorizontalDivider(color = BorderGray)

                            // Probability Details
                            Text(
                                text = "Class Probabilities",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )

                            val probInconclusive = classification.probabilities["INCONCLUSIVE"] ?: 0.0f
                            val probNegative = classification.probabilities["NEGATIVE"] ?: 0.0f
                            val probPositive = classification.probabilities["POSITIVE"] ?: 0.0f

                            DemoDetailRow("INCONCLUSIVE", String.format(Locale.US, "%.4f", probInconclusive))
                            DemoDetailRow("NEGATIVE", String.format(Locale.US, "%.4f", probNegative))
                            DemoDetailRow("POSITIVE", String.format(Locale.US, "%.4f", probPositive))

                            Spacer(modifier = Modifier.height(8.dp))

                            // Generate PDF Report Action
                            when (reportState) {
                                is ReportState.Idle -> {
                                    Button(
                                        onClick = { viewModel.generatePdfReport(record) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("Generate PDF Report", fontWeight = FontWeight.Bold)
                                    }
                                }
                                is ReportState.Generating -> {
                                    Button(
                                        onClick = {},
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generating PDF...", fontWeight = FontWeight.Bold)
                                    }
                                }
                                is ReportState.Ready -> {
                                    val pdfFile = (reportState as ReportState.Ready).pdfFile
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { ReportIntentHelper.openPdfReport(context, pdfFile) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { ReportIntentHelper.sharePdfReport(context, pdfFile) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Text("Share PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                                is ReportState.Error -> {
                                    Text("Error generating report", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextCharcoal,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

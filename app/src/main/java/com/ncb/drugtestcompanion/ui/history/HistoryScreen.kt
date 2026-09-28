package com.ncb.drugtestcompanion.ui.history

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncb.drugtestcompanion.BuildConfig
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.usecase.IntegrityStatus
import com.ncb.drugtestcompanion.domain.usecase.IntegrityVerificationResult
import com.ncb.drugtestcompanion.pdf.ReportIntentHelper
import com.ncb.drugtestcompanion.pdf.ReportState
import com.ncb.drugtestcompanion.ui.common.getLocalizedResultCategory
import com.ncb.drugtestcompanion.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val records by viewModel.testRecords.collectAsState()
    var selectedRecord by remember { mutableStateOf<TestRecord?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Test History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (records.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No Saved Test Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed test records will appear here after image capture and classification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.testId }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onClick = { selectedRecord = record }
                        )
                    }
                }
            }
        }

        selectedRecord?.let { record ->
            ModalBottomSheet(
                onDismissRequest = { 
                    selectedRecord = null
                    viewModel.resetReportState() 
                }
            ) {
                val reportState by viewModel.reportState.collectAsState()
                RecordDetailSheet(
                    record = record,
                    reportState = reportState,
                    onVerifyIntegrity = { viewModel.verifyRecordIntegrity(record) },
                    onGenerateReport = { viewModel.generatePdfReport(record) },
                    onTamperImage = { viewModel.tamperImageForDebug(record) },
                    onRestoreImage = { viewModel.restoreImageForDebug(record) }
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: TestRecord,
    onClick: () -> Unit
) {
    val resultColor = when (record.result) {
        "POSITIVE" -> Color(0xFFC62828)
        "NEGATIVE" -> Color(0xFF2E7D32)
        else -> Color(0xFFE65100)
    }

    val isSigned = record.signature.isNotBlank() && record.signature != "SIGNATURE_GENERATION_FAILED"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.testId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSigned) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color(0xFFC62828).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isSigned) "VERIFIED" else "NOT VERIFIED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSigned) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Kit: ${record.kitId} • ${SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US).format(Date(record.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = resultColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = getLocalizedResultCategory(record.result),
                    color = resultColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordDetailSheet(
    record: TestRecord,
    reportState: ReportState,
    onVerifyIntegrity: () -> IntegrityVerificationResult,
    onGenerateReport: () -> Unit,
    onTamperImage: () -> IntegrityVerificationResult?,
    onRestoreImage: () -> IntegrityVerificationResult?
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var verificationResult by remember { mutableStateOf<IntegrityVerificationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Evidence Record Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Test ID", record.testId)
                DetailRow(
                    "Timestamp",
                    SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US).format(Date(record.timestamp))
                )
                DetailRow("Kit ID", record.kitId)
                DetailRow("Reference Card", record.referenceCardProfileId)
                DetailRow("Classification Result", getLocalizedResultCategory(record.result))
                DetailRow("Confidence", String.format(Locale.US, "%.1f%%", record.confidence * 100))
                DetailRow("Distance", String.format(Locale.US, "%.2f", record.distance))
                DetailRow("Image SHA-256", record.imageSha256)
                DetailRow("Operator ID", record.operatorId)
                if (!record.address.isNullOrBlank()) {
                    DetailRow("Location Address", record.address)
                }
                if (record.latitude != null && record.longitude != null) {
                    DetailRow("Coordinates", "${record.latitude}, ${record.longitude}")
                }
                DetailRow("Location Status", record.locationStatus)
                DetailRow("Digital Signature", if (record.signature.length > 16) "${record.signature.take(16)}..." else record.signature)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        verificationResult?.let { result ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (result.status == IntegrityStatus.VERIFIED) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color(0xFFC62828).copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (result.status == IntegrityStatus.IMAGE_HASH_MISMATCH) "DEBUG TAMPER TEST" else "Integrity Status: ${result.status.name}",
                        fontWeight = FontWeight.Bold,
                        color = if (result.status == IntegrityStatus.VERIFIED) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Signature: ${if (result.isSignatureValid) "VALID" else "INVALID"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (result.isSignatureValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        text = "Image Hash: ${if (result.isImageHashValid) "MATCH" else "MISMATCH"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (result.isImageHashValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        text = "Overall: ${result.status.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.status == IntegrityStatus.VERIFIED) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.detailMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = {
                verificationResult = onVerifyIntegrity()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify Integrity")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        when (reportState) {
            is ReportState.Idle -> {
                Button(
                    onClick = onGenerateReport,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Generate PDF Report", fontWeight = FontWeight.Bold)
                }
            }
            is ReportState.Generating -> {
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Report...", fontWeight = FontWeight.Bold)
                }
            }
            is ReportState.Ready -> {
                val pdfFile = (reportState as ReportState.Ready).pdfFile
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { ReportIntentHelper.openPdfReport(context, pdfFile) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Open Report", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { ReportIntentHelper.sharePdfReport(context, pdfFile) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Share Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
            is ReportState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${(reportState as ReportState.Error).message}", color = Color.Red, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onGenerateReport,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Retry Generate PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    onTamperImage()?.let { verificationResult = it }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DEBUG: Simulate Image Tampering", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onRestoreImage()?.let { verificationResult = it }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DEBUG: Restore Original Image")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Prototype classification only. This result is based on project-defined reference centroids and is not a validated forensic or laboratory result.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

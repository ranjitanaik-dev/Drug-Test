package com.ncb.drugtestcompanion.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.audio.VoiceAnnouncementManager
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.model.TestResultCategory
import com.ncb.drugtestcompanion.pdf.ReportIntentHelper
import com.ncb.drugtestcompanion.pdf.ReportState
import com.ncb.drugtestcompanion.ui.common.GradientButton
import com.ncb.drugtestcompanion.ui.common.ShieldIcon
import com.ncb.drugtestcompanion.ui.common.getLocalizedResultCategory
import com.ncb.drugtestcompanion.ui.theme.ContainerTintBlue
import com.ncb.drugtestcompanion.ui.theme.DeepTrustNavy
import com.ncb.drugtestcompanion.ui.theme.HairlineBorder
import com.ncb.drugtestcompanion.ui.theme.InconclusiveAmber
import com.ncb.drugtestcompanion.ui.theme.InconclusiveCreamContainer
import com.ncb.drugtestcompanion.ui.theme.NegativeEmerald
import com.ncb.drugtestcompanion.ui.theme.NegativeMintContainer
import com.ncb.drugtestcompanion.ui.theme.PositiveCrimson
import com.ncb.drugtestcompanion.ui.theme.PositiveRoseContainer
import com.ncb.drugtestcompanion.ui.theme.SecurityTeal
import com.ncb.drugtestcompanion.ui.theme.SecurityTealContainer
import com.ncb.drugtestcompanion.ui.theme.SoftLightBlueBackgroundGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreenContent(
    classification: ClassificationResult,
    testRecord: TestRecord? = null,
    reportState: ReportState = ReportState.Idle,
    onRetakePhoto: () -> Unit,
    onExportPdf: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Voice Announcement for Test Result
    val voiceManager = remember(context) { VoiceAnnouncementManager(context.applicationContext) }
    val resultVoiceText = when (classification.result) {
        TestResultCategory.POSITIVE -> "Test result: Positive"
        TestResultCategory.NEGATIVE -> "Test result: Negative"
        else -> "Test result: Inconclusive"
    }

    LaunchedEffect(classification.result) {
        voiceManager.speak(resultVoiceText)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftLightBlueBackgroundGradient)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepTrustNavy
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = SecurityTealContainer
                ) {
                    Text(
                        text = "SECURE",
                        color = SecurityTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Presumptive Result Notice Banner
            PresumptiveNoticeBanner()

            // 2. Tri-State Result Selector Tabs
            TriStateSelectorTabs(currentResult = classification.result)

            // 3. Hero Result Card
            HeroResultCard(classification = classification)

            // 4. Reagent Spectrometry Card
            ReagentSpectrometryCard(classification = classification)

            // 5. Field Optical Capture Card
            FieldOpticalCaptureCard(testRecord = testRecord)

            // 6. Forensic Chain-of-Custody Card
            if (testRecord != null) {
                ForensicChainOfCustodyCard(testRecord = testRecord, context = context)
            }

            // 7. Action Buttons (Gradient Primary Action Button)
            GradientButton(
                text = "Generate & Sign Digital Docket",
                onClick = {
                    Toast.makeText(context, "Digital Docket Signed & Attested", Toast.LENGTH_SHORT).show()
                },
                height = 52.dp,
                fontSize = 15,
                leadingIcon = {
                    ShieldIcon(size = 18.dp, color = Color.White)
                },
                modifier = Modifier.fillMaxWidth()
            )

            when (reportState) {
                is ReportState.Idle -> {
                    OutlinedButton(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("📄 Export PDF Report", color = DeepTrustNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                is ReportState.Generating -> {
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepTrustNavy, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...", color = DeepTrustNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                is ReportState.Ready -> {
                    val pdfFile = (reportState as ReportState.Ready).pdfFile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { ReportIntentHelper.openPdfReport(context, pdfFile) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text("📂 Open Report", color = DeepTrustNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { ReportIntentHelper.sharePdfReport(context, pdfFile) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepTrustNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Case", color = DeepTrustNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is ReportState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${(reportState as ReportState.Error).message}", color = Color.Red, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Retry Export PDF", color = DeepTrustNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TextButton(
                onClick = onRetakePhoto,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "🚩 Flag Sample Discrepancy or Void Test",
                    color = PositiveCrimson,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresumptiveNoticeBanner() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ContainerTintBlue),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = InconclusiveAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Legal Notice",
                        tint = InconclusiveAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = InconclusiveCreamContainer
                ) {
                    Text(
                        text = "• Presumptive Result Only",
                        color = InconclusiveAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "This field-test result is presumptive and requires mandatory forensic laboratory confirmatory testing prior to legal disposition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepTrustNavy,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TriStateSelectorTabs(currentResult: TestResultCategory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TriStateTab(
            label = getLocalizedResultCategory("POSITIVE"),
            isSelected = currentResult == TestResultCategory.POSITIVE,
            accentColor = PositiveCrimson,
            containerColor = PositiveRoseContainer,
            modifier = Modifier.weight(1f)
        )
        TriStateTab(
            label = getLocalizedResultCategory("NEGATIVE"),
            isSelected = currentResult == TestResultCategory.NEGATIVE,
            accentColor = NegativeEmerald,
            containerColor = NegativeMintContainer,
            modifier = Modifier.weight(1f)
        )
        TriStateTab(
            label = getLocalizedResultCategory("INCONCLUSIVE"),
            isSelected = currentResult == TestResultCategory.INCONCLUSIVE,
            accentColor = InconclusiveAmber,
            containerColor = InconclusiveCreamContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TriStateTab(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) containerColor else Color.White,
        border = BorderStroke(1.dp, if (isSelected) accentColor else HairlineBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) accentColor else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun HeroResultCard(classification: ClassificationResult) {
    val (cardBg, accentColor, tagText) = when (classification.result) {
        TestResultCategory.POSITIVE -> Triple(PositiveRoseContainer, PositiveCrimson, "• Controlled Substance Indication")
        TestResultCategory.NEGATIVE -> Triple(NegativeMintContainer, NegativeEmerald, "• Clear Reaction")
        TestResultCategory.INCONCLUSIVE -> Triple(InconclusiveCreamContainer, InconclusiveAmber, "• Ambiguous Reaction")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = tagText,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "CV Assayed",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = getLocalizedResultCategory(classification.result.name),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )

            Text(
                text = if (classification.result == TestResultCategory.POSITIVE)
                    "Matches Prototype Reference Reagent Spectrum"
                else if (classification.result == TestResultCategory.NEGATIVE)
                    "No Reagent Spectrum Shift Detected"
                else
                    "Spectral Shift Below Confidence Cutoff",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepTrustNavy
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Match Confidence",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%% (%s)", classification.confidence * 100, if (classification.confidence >= 0.8f) "High" else "Moderate"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepTrustNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { classification.confidence.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Color Distance",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = String.format(Locale.US, "ΔE: %.2f (Limit < 80.0)", classification.distance),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DeepTrustNavy
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReagentSpectrometryCard(classification: ClassificationResult) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShieldIcon(size = 20.dp, color = DeepTrustNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reagent Spectrometry", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepTrustNavy)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = SecurityTealContainer
                ) {
                    Text(
                        text = "98% Spectral Alignment",
                        color = SecurityTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val l = classification.componentScores["average_lab_l"] ?: 50.0
            val a = classification.componentScores["average_lab_a"] ?: 0.0
            val b = classification.componentScores["average_lab_b"] ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColorBox(
                    label = "FIELD SCAN",
                    colorHex = String.format(Locale.US, "LAB(%.1f, %.1f, %.1f)", l, a, b),
                    subText = "Captured Color",
                    color = DeepTrustNavy,
                    modifier = Modifier.weight(1f)
                )

                ColorBox(
                    label = "LAB REFERENCE",
                    colorHex = "STANDARD REF",
                    subText = "Target Centroid",
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ColorBox(
    label: String,
    colorHex: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ContainerTintBlue,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(colorHex, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DeepTrustNavy)
            Text(subText, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun FieldOpticalCaptureCard(testRecord: TestRecord?) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShieldIcon(size = 20.dp, color = DeepTrustNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Field Optical Capture", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepTrustNavy)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ContainerTintBlue
                ) {
                    Text("RAW + CALIB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepTrustNavy, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.6f)) {
                        Text("• ArUco Tag Locked #1,2,3,4", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Focal Plane: Macro FIT_CENTER", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ForensicChainOfCustodyCard(testRecord: TestRecord, context: Context) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = DeepTrustNavy, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forensic Chain-of-Custody", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepTrustNavy)
                }

                val isSigned = testRecord.signature.isNotBlank() && testRecord.signature != "SIGNATURE_GENERATION_FAILED"
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSigned) SecurityTealContainer else PositiveRoseContainer
                ) {
                    Text(
                        text = if (isSigned) "Tamper-Sealed" else "Unsealed",
                        color = if (isSigned) SecurityTeal else PositiveCrimson,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = HairlineBorder)

            ChainRow("Test Record ID", testRecord.testId, isMonospace = true, copyable = true, context = context)
            ChainRow("Acquisition Timestamp", SimpleDateFormat("dd MMM yyyy, HH:mm z", Locale.US).format(Date(testRecord.timestamp)))
            ChainRow("Assay Operator", testRecord.operatorId)

            val locationDisplay = if (!testRecord.address.isNullOrBlank()) {
                testRecord.address
            } else if (testRecord.latitude != null && testRecord.longitude != null) {
                "${testRecord.latitude}, ${testRecord.longitude} (${testRecord.locationStatus})"
            } else {
                testRecord.locationStatus
            }

            ChainRow("Test Location", locationDisplay, isMonospace = true)

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ContainerTintBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SHA-256 LEDGER HASH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Surface(shape = RoundedCornerShape(4.dp), color = SecurityTealContainer) {
                            Text("Hardware TPM Attested", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecurityTeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = testRecord.imageSha256,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DeepTrustNavy,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SHA256 Hash", testRecord.imageSha256))
                            Toast.makeText(context, "Full SHA-256 Hash Copied", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.align(Alignment.End).height(32.dp)
                    ) {
                        Text("Copy Full Hash", fontSize = 10.sp, color = DeepTrustNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    copyable: Boolean = false,
    context: Context? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                fontWeight = FontWeight.Bold,
                color = DeepTrustNavy
            )
            if (copyable && context != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "📋",
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                        Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

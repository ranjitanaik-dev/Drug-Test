package com.ncb.drugtestcompanion.ui.newtest

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.domain.model.TestKitProfile
import com.ncb.drugtestcompanion.ui.common.ShieldIcon
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CanvasBackground
import com.ncb.drugtestcompanion.ui.theme.CardTintGray
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted
import com.ncb.drugtestcompanion.viewmodel.NewTestViewModel

@Composable
fun NewTestScreen(
    onNavigateToCapture: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {},
    viewModel: NewTestViewModel = hiltViewModel()
) {
    val selectedKitId by viewModel.selectedKitId.collectAsState()
    val availableKits = viewModel.availableKitProfiles

    var subjectId by remember { mutableStateOf("SUB-3140") }
    var caseRefId by remember { mutableStateOf("NDPS-CASE-763") }
    var lotNumber by remember { mutableStateOf("LOT-7823") }
    var expiryDate by remember { mutableStateOf("Dec 2027") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val currentKitProfile = availableKits.find { it.kitId == selectedKitId } ?: availableKits.firstOrNull()

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(com.ncb.drugtestcompanion.ui.theme.SoftLightBlueBackgroundGradient)
        ) {
            // Top Bar with Back Navigation
            Surface(
                color = CanvasBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextCharcoal
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Start New Test",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        Text(
                            text = "Session: NRK-20260929-5792",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Horizontal Stepper Header (1 Details -> 2 Capture -> 3 Review -> 4 Analyze -> 5 Submit)
                StepperHeader(currentStep = 1)

                // 2. Section Header
                Column {
                    Text(
                        text = "Enter Test Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ensure kit lot number matches package barcode",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                // 3. Subject / Individual ID Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Subject / Individual ID",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    )
                    OutlinedTextField(
                        value = subjectId,
                        onValueChange = { subjectId = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PrimaryNavy,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Case / Incident Reference ID Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Case / Incident Reference ID",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    )
                    OutlinedTextField(
                        value = caseRefId,
                        onValueChange = { caseRefId = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PrimaryNavy,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. Test Kit & Reagent Dropdown Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Test Kit & Reagent",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    )

                    Box {
                        OutlinedTextField(
                            value = currentKitProfile?.displayName ?: "Scott Reagent (Cocaine HCl / Base)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Kit",
                                    tint = TextCharcoal,
                                    modifier = Modifier.clickable { isDropdownExpanded = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = PrimaryNavy,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            availableKits.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.displayName, fontWeight = FontWeight.Bold)
                                            Text("${profile.kitId} • ${profile.drugCategory}", fontSize = 11.sp, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectKit(profile.kitId)
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 6. Row of 2 fields: Lot Number & Expiry Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lot Number",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        OutlinedTextField(
                            value = lotNumber,
                            onValueChange = { lotNumber = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = PrimaryNavy,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Expiry Date",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCharcoal
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = PrimaryNavy,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 7. Telemetry Lock Box (Matching Image 3)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardTintGray),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShieldIcon(size = 18.dp, color = PrimaryNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUTOMATIC TELEMETRY LOCK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("• Location: Field Site 07 - Sector 4 (28.6139° N, 77.209° E)", fontSize = 11.sp, color = TextMuted)
                        Text("• Officer: OP-1047 (Verified Hardware Session)", fontSize = 11.sp, color = TextMuted)
                        Text("• Timestamp: Auto-synchronized with NIST time server", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 8. Primary Button: Next
                com.ncb.drugtestcompanion.ui.common.GradientButton(
                    text = "Proceed to AR Capture & Guidance  →",
                    onClick = {
                        val kitIdToUse = selectedKitId ?: availableKits.firstOrNull()?.kitId ?: "KIT_A"
                        onNavigateToCapture(kitIdToUse)
                    },
                    height = 52.dp,
                    fontSize = 15,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StepperHeader(currentStep: Int) {
    val steps = listOf("Details", "Capture", "Review", "Analyze", "Submit")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isActive = stepNumber == currentStep
            val isPassed = stepNumber < currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isActive || isPassed) PrimaryNavy else Color(0xFFF1F5F9),
                    border = if (!isActive && !isPassed) BorderStroke(1.dp, BorderGray) else null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPassed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = stepNumber.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) PrimaryNavy else TextMuted
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(if (stepNumber < currentStep) PrimaryNavy else BorderGray)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

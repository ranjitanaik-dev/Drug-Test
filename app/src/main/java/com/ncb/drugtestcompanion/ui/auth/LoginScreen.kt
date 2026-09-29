package com.ncb.drugtestcompanion.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.localization.AppLanguage
import com.ncb.drugtestcompanion.ui.common.GradientButton
import com.ncb.drugtestcompanion.ui.common.NirikshBrandHeader
import com.ncb.drugtestcompanion.ui.common.ShieldIcon
import com.ncb.drugtestcompanion.ui.theme.BorderBlueLight
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CardTintBlue
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.SoftLightBlueBackgroundGradient
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted

@Composable
fun LoginScreen(
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onLoginSuccess: (String) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var officerId by remember { mutableStateOf("OP-1047") }
    var password by remember { mutableStateOf("••••••••") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val emptyIdErrorMsg = stringResource(R.string.login_error_empty)
    val unconfiguredMsg = "Biometric authentication is not configured for $officerId. Please sign in with Officer ID."

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Niriksh Logo Watermark Background
            NirikshLoginWatermarkBackground()

            // 2. Main Login Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Brand Header with Niriksh Logo
                    NirikshBrandHeader(iconSize = 56.dp)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title & Subtitle
                    Text(
                        text = stringResource(R.string.nav_login),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextCharcoal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Biometric / Fingerprint Visual Graphic
                    BiometricVisualGraphic()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Input Fields Card Container
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, BorderBlueLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Operator / Officer ID Field
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.dashboard_officer_id),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )

                                OutlinedTextField(
                                    value = officerId,
                                    onValueChange = {
                                        officerId = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text(stringResource(R.string.login_enter_officer_id)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = stringResource(R.string.dashboard_officer_id),
                                            tint = TextMuted
                                        )
                                    },
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

                            // Security Password Field
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.login_label_password),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.login_label_password),
                                            tint = TextMuted
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Toggle password visibility",
                                                tint = if (isPasswordVisible) PrimaryNavy else TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                            errorMessage?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Primary Action Button: SIGN IN WITH OFFICER ID
                            GradientButton(
                                text = stringResource(R.string.login_btn_login) + "  →",
                                onClick = {
                                    if (officerId.isNotBlank()) {
                                        onLoginSuccess(officerId.trim())
                                    } else {
                                        errorMessage = emptyIdErrorMsg
                                    }
                                },
                                height = 50.dp,
                                fontSize = 15,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Secondary Action Button: LOGIN WITH BIOMETRICS
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, unconfiguredMsg, Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderGray),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.size(18.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        val centerX = w / 2f
                                        val centerY = h / 2f
                                        val strokeW = 1.8.dp.toPx()

                                        drawArc(
                                            color = PrimaryNavy,
                                            startAngle = 180f,
                                            sweepAngle = 180f,
                                            useCenter = false,
                                            topLeft = Offset(centerX - w * 0.40f, centerY - h * 0.40f),
                                            size = Size(w * 0.80f, h * 0.80f),
                                            style = Stroke(width = strokeW)
                                        )

                                        drawArc(
                                            color = PrimaryNavy,
                                            startAngle = 200f,
                                            sweepAngle = 140f,
                                            useCenter = false,
                                            topLeft = Offset(centerX - w * 0.28f, centerY - h * 0.28f),
                                            size = Size(w * 0.56f, h * 0.56f),
                                            style = Stroke(width = strokeW)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.login_btn_biometric),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                            }
                        }
                    }
                }

                // Bottom Footer Security Notice
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ShieldIcon(size = 14.dp, color = TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.login_footer_notice),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Niriksh Shield & Science Ribbon Logo Watermark Background
 */
@Composable
private fun NirikshLoginWatermarkBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftLightBlueBackgroundGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h * 0.38f
            val shieldScale = w * 0.72f
            val strokeW = 7.dp.toPx()
            val primaryCol = PrimaryNavy.copy(alpha = 0.08f)
            val tealCol = Color(0xFF00C9A7).copy(alpha = 0.12f)

            // Outer Security Shield Path
            val shieldPath = Path().apply {
                moveTo(centerX, centerY - shieldScale * 0.50f)
                cubicTo(
                    centerX + shieldScale * 0.35f, centerY - shieldScale * 0.50f,
                    centerX + shieldScale * 0.48f, centerY - shieldScale * 0.35f,
                    centerX + shieldScale * 0.50f, centerY - shieldScale * 0.10f
                )
                cubicTo(
                    centerX + shieldScale * 0.50f, centerY + shieldScale * 0.30f,
                    centerX + shieldScale * 0.25f, centerY + shieldScale * 0.55f,
                    centerX, centerY + shieldScale * 0.70f
                )
                cubicTo(
                    centerX - shieldScale * 0.25f, centerY + shieldScale * 0.55f,
                    centerX - shieldScale * 0.50f, centerY + shieldScale * 0.30f,
                    centerX - shieldScale * 0.50f, centerY - shieldScale * 0.10f
                )
                cubicTo(
                    centerX - shieldScale * 0.48f, centerY - shieldScale * 0.35f,
                    centerX - shieldScale * 0.35f, centerY - shieldScale * 0.50f,
                    centerX, centerY - shieldScale * 0.50f
                )
                close()
            }

            drawPath(path = shieldPath, color = tealCol, style = Stroke(width = strokeW))

            // Checkmark in Center of Shield Watermark
            val checkPath = Path().apply {
                moveTo(centerX - shieldScale * 0.18f, centerY)
                lineTo(centerX - shieldScale * 0.04f, centerY + shieldScale * 0.14f)
                lineTo(centerX + shieldScale * 0.22f, centerY - shieldScale * 0.14f)
            }

            drawPath(path = checkPath, color = primaryCol, style = Stroke(width = strokeW * 1.2f))

            // Science Data Line Curve
            val dataPath = Path().apply {
                moveTo(centerX - shieldScale * 0.25f, centerY + shieldScale * 0.25f)
                cubicTo(
                    centerX - shieldScale * 0.10f, centerY + shieldScale * 0.15f,
                    centerX + shieldScale * 0.05f, centerY + shieldScale * 0.10f,
                    centerX + shieldScale * 0.28f, centerY - shieldScale * 0.08f
                )
            }
            drawPath(path = dataPath, color = tealCol, style = Stroke(width = strokeW * 0.8f))

            // Node Circles
            drawCircle(color = tealCol, radius = 6.dp.toPx(), center = Offset(centerX - shieldScale * 0.25f, centerY + shieldScale * 0.25f))
            drawCircle(color = tealCol, radius = 6.dp.toPx(), center = Offset(centerX + shieldScale * 0.02f, centerY + shieldScale * 0.11f))
            drawCircle(color = tealCol, radius = 6.dp.toPx(), center = Offset(centerX + shieldScale * 0.28f, centerY - shieldScale * 0.08f))
        }
    }
}

/**
 * Biometric / Fingerprint Scanner Visual Graphic (UI Design Element)
 */
@Composable
private fun BiometricVisualGraphic() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = CardTintBlue,
                border = BorderStroke(1.5.dp, PrimaryNavy.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxSize()
            ) {}

            Canvas(modifier = Modifier.size(44.dp)) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val centerY = h / 2f
                val strokeW = 2.5.dp.toPx()

                // Drawing Concentric Circular Fingerprint Scanner Ridges
                drawArc(
                    color = PrimaryNavy,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.40f, centerY - h * 0.40f),
                    size = Size(w * 0.80f, h * 0.80f),
                    style = Stroke(width = strokeW)
                )

                drawArc(
                    color = PrimaryNavy,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.28f, centerY - h * 0.28f),
                    size = Size(w * 0.56f, h * 0.56f),
                    style = Stroke(width = strokeW)
                )

                drawArc(
                    color = PrimaryNavy,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.18f, centerY - h * 0.18f),
                    size = Size(w * 0.36f, h * 0.36f),
                    style = Stroke(width = strokeW)
                )

                drawCircle(
                    color = PrimaryNavy,
                    radius = 3.5.dp.toPx(),
                    center = Offset(centerX, centerY + h * 0.12f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Text(
                text = "SECURE ACCESS AREA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                letterSpacing = 0.8.sp
            )
        }
    }
}

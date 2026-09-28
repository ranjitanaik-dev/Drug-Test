package com.ncb.drugtestcompanion.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture as CameraXImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.ui.qualitycheck.QualityCheckScreen
import com.ncb.drugtestcompanion.viewmodel.CaptureViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CameraEntryPoint {
    fun imageCapture(): CameraXImageCapture
}

@Composable
fun CaptureScreen(
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel(),
    imageCapture: CameraXImageCapture? = null
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val singletonImageCapture = remember(imageCapture) {
        imageCapture ?: EntryPointAccessors.fromApplication(
            context.applicationContext,
            CameraEntryPoint::class.java
        ).imageCapture()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (hasCameraPermission) {
            CameraCaptureContent(
                viewModel = viewModel,
                imageCapture = singletonImageCapture
            )
        } else {
            PermissionDeniedContent(
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun CameraCaptureContent(
    viewModel: CaptureViewModel,
    imageCapture: CameraXImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    DisposableEffect(lifecycleOwner, previewView) {
        val currentPreviewView = previewView
        if (currentPreviewView != null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = currentPreviewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    viewModel.onCameraReady(true)
                } catch (e: Exception) {
                    Log.e("CaptureScreen", "Camera binding failed", e)
                    viewModel.onCameraReady(false)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            viewModel.onCameraReady(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is CaptureState.Captured -> {
                QualityCheckScreen(
                    imagePath = state.imagePath,
                    selectedProfile = state.selectedProfile,
                    onRetakePhoto = { viewModel.resetCapture() }
                )
            }

            else -> {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                        }.also { pv ->
                            previewView = pv
                            pv.post {
                                val displayRotation = pv.display?.rotation
                                logDiagnostic("10. CameraX PreviewView config: scaleType=${pv.scaleType}, viewSize=${pv.width}x${pv.height}, displayRotation=$displayRotation")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2:3 Aspect-Ratio Physical Reference-Card Guide Overlay
                ReferenceCardGuideOverlay(
                    modifier = Modifier.fillMaxSize()
                )

                // Top Controls: Kit Selection Header
                KitSelectionHeader(
                    selectedProfile = state.selectedProfile,
                    availableProfiles = viewModel.getAvailableProfiles(),
                    onSelectProfile = { viewModel.selectKitVariant(it.variantId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                )

                // Bottom Controls / Shutter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        is CaptureState.Idle -> {
                            ShutterButton(
                                onClick = { viewModel.capturePhoto() },
                                enabled = state.isCameraReady
                            )
                        }

                        is CaptureState.Capturing -> {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Saving image...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        is CaptureState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Error: ${state.message}",
                                    color = Color.Red,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.resetCapture() }) {
                                    Text("Retry")
                                }
                            }
                        }

                        is CaptureState.Captured -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun KitSelectionHeader(
    selectedProfile: ReferenceCardProfile,
    availableProfiles: List<ReferenceCardProfile>,
    onSelectProfile: (ReferenceCardProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "KIT: ${selectedProfile.variantId}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedProfile.kitFormat,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableProfiles.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "${profile.variantId}: ${profile.kitFormat}",
                                    fontWeight = if (profile.variantId == selectedProfile.variantId) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onSelectProfile(profile)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceCardGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val overlayColor = Color.Black.copy(alpha = 0.45f)
        val guideColor = Color.White

        // Correct Physical Reference Card Aspect Ratio: 100mm x 150mm = 2:3 (0.6667)
        val cardAspectRatio = 2f / 3f
        val cardHeight = size.height * 0.65f
        val cardWidth = cardHeight * cardAspectRatio

        val left = (size.width - cardWidth) / 2f
        val top = (size.height - cardHeight) / 2f

        val guideAspectRatio = if (cardHeight > 0) cardWidth / cardHeight else 0f
        val screenAspectRatio = if (size.height > 0) size.width / size.height else 0f
        logDiagnostic("10. Camera Guide Overlay config: screenSize=${size.width}x${size.height} (aspectRatio=$screenAspectRatio), cardGuide=${cardWidth}x${cardHeight} at (left=$left, top=$top) (aspectRatio=$guideAspectRatio)")

        // Draw translucent background overlay except card hole area
        drawRect(
            color = overlayColor
        )

        // Clear out center area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // Draw bounding box guide outline
        drawRoundRect(
            color = guideColor,
            topLeft = Offset(left, top),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .border(4.dp, if (enabled) Color.White else Color.Gray, CircleShape)
            .padding(6.dp)
            .background(if (enabled) Color.White else Color.Gray.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) {}
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera & Location Permission Required",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "DrugTestCompanion requires camera access to capture test card images and location access for evidence recording.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text(text = "Grant Permissions")
        }
    }
}

private fun logDiagnostic(msg: String) {
    try {
        Log.d("CaptureDiagnostic", msg)
    } catch (_: Throwable) {
        println("[CaptureDiagnostic] $msg")
    }
}

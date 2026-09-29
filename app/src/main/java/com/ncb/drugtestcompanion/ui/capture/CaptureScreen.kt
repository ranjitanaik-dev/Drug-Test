package com.ncb.drugtestcompanion.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture as CameraXImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.ncb.drugtestcompanion.cv.CardDetector
import kotlin.math.atan2
import kotlin.math.hypot
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ncb.drugtestcompanion.R
import com.ncb.drugtestcompanion.ui.newtest.StepperHeader
import com.ncb.drugtestcompanion.ui.qualitycheck.QualityCheckScreen
import com.ncb.drugtestcompanion.ui.theme.BorderGray
import com.ncb.drugtestcompanion.ui.theme.CanvasBackground
import com.ncb.drugtestcompanion.ui.theme.CardTintBlue
import com.ncb.drugtestcompanion.ui.theme.PrimaryNavy
import com.ncb.drugtestcompanion.ui.theme.StatusGreen
import com.ncb.drugtestcompanion.ui.theme.TextCharcoal
import com.ncb.drugtestcompanion.ui.theme.TextMuted
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
        color = CanvasBackground
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
    val scrollState = rememberScrollState()
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    // AR Capture Guidance Dynamic Analyzer & Real-Time OpenCV Card Detector
    val analyzer = remember { ArCaptureGuidanceAnalyzer() }
    val cardDetector = remember { CardDetector() }

    var pitchDegrees by remember { mutableStateOf(0f) }
    var rollDegrees by remember { mutableStateOf(0f) }

    var arGuidanceInfo by remember {
        mutableStateOf(
            ArGuidanceFrameInfo(
                cardDetected = false,
                distanceStatus = DistanceStatus.NOT_DETECTED,
                alignmentStatus = AlignmentStatus.NOT_DETECTED,
                guidanceState = ArCaptureGuidanceState.ReferenceCardNotDetected,
                isReadyToCapture = false
            )
        )
    }

    DisposableEffect(context, lifecycleOwner, previewView) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastUpdateTimestamp = 0L

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val now = System.currentTimeMillis()
                if (now - lastUpdateTimestamp < 40) return
                lastUpdateTimestamp = now

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                pitchDegrees = Math.toDegrees(atan2(y.toDouble(), hypot(x.toDouble(), z.toDouble()))).toFloat()
                rollDegrees = Math.toDegrees(atan2(x.toDouble(), hypot(y.toDouble(), z.toDouble()))).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        val currentPreviewView = previewView
        if (currentPreviewView != null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = currentPreviewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Live CameraX ImageAnalysis for real-time reference card detection
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var lastAnalysisMs = 0L

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val now = System.currentTimeMillis()
                    if (now - lastAnalysisMs > 250) {
                        lastAnalysisMs = now
                        val bitmap = imageProxy.toBitmap()
                        if (bitmap != null) {
                            val detectionResult = cardDetector.detectCard(bitmap)
                            val previewW = currentPreviewView.width.toFloat().coerceAtLeast(bitmap.width.toFloat())
                            val previewH = currentPreviewView.height.toFloat().coerceAtLeast(bitmap.height.toFloat())

                            val frameInfo = analyzer.analyzeGuidance(detectionResult, previewW, previewH)
                            val tiltMagnitude = hypot(pitchDegrees.toDouble(), rollDegrees.toDouble()).toFloat()

                            val alignmentStatus = when {
                                tiltMagnitude > 22f -> AlignmentStatus.POOR_ALIGNMENT
                                tiltMagnitude > 12f -> AlignmentStatus.NEED_STRAIGHTEN
                                else -> if (frameInfo.alignmentStatus != AlignmentStatus.NOT_DETECTED) frameInfo.alignmentStatus else AlignmentStatus.GOOD_ALIGNMENT
                            }

                            val finalReady = frameInfo.cardDetected &&
                                    frameInfo.distanceStatus == DistanceStatus.GOOD_DISTANCE &&
                                    alignmentStatus == AlignmentStatus.GOOD_ALIGNMENT

                            val finalState = when {
                                !frameInfo.cardDetected -> ArCaptureGuidanceState.ReferenceCardNotDetected
                                frameInfo.distanceStatus == DistanceStatus.TOO_FAR -> ArCaptureGuidanceState.TooFar(frameInfo.relativeCardAreaRatio)
                                frameInfo.distanceStatus == DistanceStatus.TOO_CLOSE -> ArCaptureGuidanceState.TooClose(frameInfo.relativeCardAreaRatio)
                                alignmentStatus == AlignmentStatus.POOR_ALIGNMENT -> ArCaptureGuidanceState.PoorAlignment("Straighten phone")
                                alignmentStatus == AlignmentStatus.NEED_STRAIGHTEN -> ArCaptureGuidanceState.AdjustPosition(frameInfo.distanceStatus, alignmentStatus)
                                finalReady -> ArCaptureGuidanceState.Ready(confidence = 0.95f)
                                else -> ArCaptureGuidanceState.AdjustPosition(frameInfo.distanceStatus, alignmentStatus)
                            }

                            arGuidanceInfo = ArGuidanceFrameInfo(
                                cardDetected = frameInfo.cardDetected,
                                distanceStatus = frameInfo.distanceStatus,
                                alignmentStatus = alignmentStatus,
                                guidanceState = finalState,
                                isReadyToCapture = finalReady,
                                cardCorners = frameInfo.cardCorners,
                                estimatedAngleDegrees = tiltMagnitude,
                                relativeCardAreaRatio = frameInfo.relativeCardAreaRatio
                            )
                        }
                    }
                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                    viewModel.onCameraReady(true)
                } catch (e: Exception) {
                    Log.e("CaptureScreen", "Camera binding failed", e)
                    viewModel.onCameraReady(false)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
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
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Navigation Header
                    Surface(
                        color = CanvasBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.resetCapture() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextCharcoal
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Guided Capture",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )
                                Text(
                                    text = "Kit: ${state.selectedProfile.cardTitle}",
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Horizontal Stepper Header (Step 2 Active)
                        StepperHeader(currentStep = 2)

                        // 2. Section Headline
                        Column {
                            Text(
                                text = "Capture Sample & Reference",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextCharcoal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Both color card and result bar must be in frame",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        // 3. Instructions Card (Matching Image 4)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InstructionStepRow("1", "Open test kit & prepare chemical reagents.")
                                InstructionStepRow("2", "Add sample to test well / ampoule device.")
                                InstructionStepRow("3", "Place reference color card next to reaction well.")
                                InstructionStepRow("4", "Align frame guides & hold phone steady.")
                            }
                        }

                        // 4. Camera Viewfinder Card Container (16dp rounded)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Camera View
                                AndroidView(
                                    factory = { ctx ->
                                        PreviewView(ctx).apply {
                                            scaleType = PreviewView.ScaleType.FIT_CENTER
                                        }.also { pv ->
                                            previewView = pv
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Camera Top Overlay Status Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(StatusGreen)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "LIVE CAMERAX • 1080P",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Info, contentDescription = "Flash", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // AR Capture Guidance Layer
                                ArCaptureGuidanceOverlay(
                                    guidanceInfo = arGuidanceInfo,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Dual Frame Overlay: Blue Reference Box (Left) + Green Reaction Box (Right)
                                DualFrameGuideOverlay(modifier = Modifier.fillMaxSize())

                                // Circular Shutter Button at bottom center of camera
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                ) {
                                    when (state) {
                                        is CaptureState.Idle -> {
                                            val isReady = arGuidanceInfo.isReadyToCapture && state.isCameraReady
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White,
                                                border = BorderStroke(
                                                    4.dp,
                                                    if (isReady) StatusGreen else Color.White.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clickable(enabled = state.isCameraReady) {
                                                        viewModel.capturePhoto()
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Capture Photo",
                                                        tint = if (isReady) StatusGreen else PrimaryNavy,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }

                                        is CaptureState.Capturing -> {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(54.dp))
                                        }

                                        else -> {}
                                    }
                                }
                            }
                        }

                        // 5. Test Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.capturePhoto() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderGray),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Sim Positive Test", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetCapture() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderGray),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Reset / Retake", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextCharcoal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStepRow(stepNum: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = CardTintBlue,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNum,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextCharcoal,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DualFrameGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left Blue Box: Reference Card
        val blueLeft = w * 0.08f
        val blueTop = h * 0.35f
        val boxWidth = w * 0.40f
        val boxHeight = h * 0.35f

        drawRoundRect(
            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
            topLeft = Offset(blueLeft, blueTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(blueLeft, blueTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Right Green Box: Reaction Well / Bar
        val greenLeft = w * 0.52f
        val greenTop = h * 0.35f

        drawRoundRect(
            color = Color(0xFF34D399).copy(alpha = 0.2f),
            topLeft = Offset(greenLeft, greenTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = Color(0xFF34D399),
            topLeft = Offset(greenLeft, greenTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 2.dp.toPx())
        )
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
            color = TextCharcoal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NiRIKSH requires camera access to capture test card images and location access for evidence recording.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Grant Permissions", color = Color.White)
        }
    }
}

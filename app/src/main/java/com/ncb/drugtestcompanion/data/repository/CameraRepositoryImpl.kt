package com.ncb.drugtestcompanion.data.repository

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.ncb.drugtestcompanion.domain.repository.CameraRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Concrete implementation of [CameraRepository] using CameraX [ImageCapture].
 * Saves captured JPEG images into app-private internal storage (`context.filesDir/captures`).
 */
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCapture: ImageCapture
) : CameraRepository {

    override suspend fun captureAndSaveImage(): Result<String> = suspendCancellableCoroutine { continuation ->
        val captureDir = File(context.filesDir, "captures").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val photoFile = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        val executor = ContextCompat.getMainExecutor(context)

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedPath = photoFile.absolutePath
                    logDiagnostic("1. Captured JPEG path saved: $savedPath")
                    logDiagnostic("10. ImageCapture configuration: targetRotation=${imageCapture.targetRotation}, captureMode=${imageCapture.captureMode}, flashMode=${imageCapture.flashMode}")
                    if (continuation.isActive) {
                        continuation.resume(Result.success(savedPath))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    logDiagnostic("ImageCapture onError: ${exception.message}")
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
            }
        )
    }

    private fun logDiagnostic(msg: String) {
        try {
            Log.d("CaptureDiagnostic", msg)
        } catch (_: Throwable) {
            println("[CaptureDiagnostic] $msg")
        }
    }
}

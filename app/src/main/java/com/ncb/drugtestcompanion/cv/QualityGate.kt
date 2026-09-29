package com.ncb.drugtestcompanion.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import com.ncb.drugtestcompanion.domain.model.QualityFailureReason
import com.ncb.drugtestcompanion.domain.model.ReferenceCardProfile
import com.ncb.drugtestcompanion.domain.model.RoiSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class QualityResult {

    data class ImageValid(
        val roiExtractionResult: RoiExtractionResult.Success? = null,
        val rawMlRoiBitmap: Bitmap? = null,
        val calibratedMlRoiBitmap: Bitmap? = null,
        val colorCalibrationResult: ColorCalibrationResult? = null
    ) : QualityResult()

    data class ImageInvalid(
        val reason: QualityFailureReason
    ) : QualityResult()
}

/**
 * Quality gate for captured field-test image.
 *
 * Processing order:
 * 1. Resolution check
 * 2. Blur check
 * 3. Exposure check
 * 4. Reference card detection (Mandatory!)
 * 5. Reference card 3x3 RGB color calibration transform calculation
 * 6. Test device detection & ML ROI extraction (386x133)
 * 7. Apply 3x3 RGB calibration matrix to ML ROI
 */
@Singleton
class QualityGate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardDetector: CardDetector,
    private val colorCalibrator: ColorCalibrator,
    private val roiExtractor: RoiExtractor? = null
) {

    private val testDeviceDetector = TestDeviceDetector()

    fun analyzeImage(
        imagePath: String,
        profile: ReferenceCardProfile? = null
    ): QualityResult {

        logDiagnostic("1. Captured JPEG path in QualityGate: $imagePath")

        val file = File(imagePath)
        if (!file.exists()) {
            logQuality("QualityGate: File does not exist at $imagePath")
            logQuality("QUALITY_FAILURE = LOW_RESOLUTION")
            return QualityResult.ImageInvalid(QualityFailureReason.LOW_RESOLUTION)
        }

        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)
            logDiagnostic("2. Captured JPEG raw file dimensions: ${options.outWidth}x${options.outHeight}")
        } catch (e: Throwable) {
            logDiagnostic("QualityGate: Failed to decode raw bounds: ${e.message}")
        }

        val bitmap = loadBitmapWithExifRotation(imagePath)
        if (bitmap == null) {
            logQuality("QualityGate: loadBitmapWithExifRotation returned null")
            logQuality("QUALITY_FAILURE = LOW_RESOLUTION")
            return QualityResult.ImageInvalid(QualityFailureReason.LOW_RESOLUTION)
        }

        logDiagnostic("3. Bitmap dimensions after EXIF rotation: ${bitmap.width}x${bitmap.height}")

        val sourceName = file.nameWithoutExtension
        return analyzeBitmap(
            bitmap = bitmap,
            profile = profile,
            sourceName = sourceName
        )
    }

    /**
     * Decode the JPEG and apply the EXIF rotation stored by the camera.
     */
    fun loadBitmapWithExifRotation(
        imagePath: String
    ): Bitmap? {

        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } catch (_: Throwable) {
            bitmap
        }
    }

    fun analyzeBitmap(
        bitmap: Bitmap,
        profile: ReferenceCardProfile? = null,
        sourceName: String = "capture"
    ): QualityResult {

        val width = bitmap.width
        val height = bitmap.height

        logQuality("Bitmap dimensions: ${width}x${height}")

        // ============================================================
        // 1. RESOLUTION CHECK
        // ============================================================
        if (width < Thresholds.MIN_RESOLUTION_WIDTH || height < Thresholds.MIN_RESOLUTION_HEIGHT) {
            logQuality("Resolution Check FAILED: ${width}x${height} < minimum ${Thresholds.MIN_RESOLUTION_WIDTH}x${Thresholds.MIN_RESOLUTION_HEIGHT}")
            logQuality("QUALITY_FAILURE = LOW_RESOLUTION")
            return QualityResult.ImageInvalid(QualityFailureReason.LOW_RESOLUTION)
        }
        logQuality("Resolution Check PASSED: ${width}x${height}")

        // ============================================================
        // 2. BLUR CHECK
        // ============================================================
        val laplacianVariance = calculateLaplacianVariance(bitmap)
        logQuality("Calculated Laplacian variance = $laplacianVariance (threshold = ${Thresholds.LAPLACIAN_VARIANCE_THRESHOLD})")

        if (laplacianVariance < Thresholds.LAPLACIAN_VARIANCE_THRESHOLD) {
            logQuality("Blur Check FAILED: variance $laplacianVariance < threshold ${Thresholds.LAPLACIAN_VARIANCE_THRESHOLD}")
            logQuality("QUALITY_FAILURE = BLUR")
            return QualityResult.ImageInvalid(QualityFailureReason.BLUR)
        }
        logQuality("Blur Check PASSED: variance $laplacianVariance >= threshold ${Thresholds.LAPLACIAN_VARIANCE_THRESHOLD}")

        // ============================================================
        // 3. EXPOSURE CHECK
        // ============================================================
        val meanIntensity = calculateMeanGrayscaleIntensity(bitmap)
        logQuality("Calculated mean grayscale intensity = $meanIntensity (underexposure threshold = ${Thresholds.UNDEREXPOSURE_MEAN_THRESHOLD}, overexposure threshold = ${Thresholds.OVEREXPOSURE_MEAN_THRESHOLD})")

        if (meanIntensity < Thresholds.UNDEREXPOSURE_MEAN_THRESHOLD) {
            logQuality("Underexposure Check FAILED: mean $meanIntensity < threshold ${Thresholds.UNDEREXPOSURE_MEAN_THRESHOLD}")
            logQuality("QUALITY_FAILURE = UNDEREXPOSED")
            return QualityResult.ImageInvalid(QualityFailureReason.UNDEREXPOSED)
        }

        if (meanIntensity > Thresholds.OVEREXPOSURE_MEAN_THRESHOLD) {
            logQuality("Overexposure Check FAILED: mean $meanIntensity > threshold ${Thresholds.OVEREXPOSURE_MEAN_THRESHOLD}")
            logQuality("QUALITY_FAILURE = OVEREXPOSED")
            return QualityResult.ImageInvalid(QualityFailureReason.OVEREXPOSED)
        }
        logQuality("Exposure Check PASSED: mean $meanIntensity")

        // ============================================================
        // 4. REFERENCE CARD DETECTION (Mandatory)
        // ============================================================
        logQuality("Executing CardDetector.detectCard(bitmap)...")
        val detection = cardDetector.detectCard(bitmap)

        when (detection) {
            is CardDetectionResult.NotDetected -> {
                logQuality("Card Detection FAILED: required reference-card markers were not fully detected")
                logQuality("QUALITY_FAILURE = CARD_NOT_VISIBLE")
                return QualityResult.ImageInvalid(QualityFailureReason.CARD_NOT_VISIBLE)
            }

            is CardDetectionResult.FramingError -> {
                logQuality("Framing Check FAILED: reference card is outside the allowed frame")
                logQuality("QUALITY_FAILURE = FRAMING_ERROR")
                return QualityResult.ImageInvalid(QualityFailureReason.FRAMING_ERROR)
            }

            is CardDetectionResult.Detected -> {
                logQuality("Card Detection PASSED: reference card detected")
                logMlPipeline("ML_PIPELINE: Reference card detected = YES")

                // ====================================================
                // 5. REFERENCE CARD COLOR CALIBRATION MATRIX
                // ====================================================
                val calibrationResult = colorCalibrator.computeCalibrationTransform(bitmap, detection.corners)

                // ====================================================
                // 6. TEST DEVICE DETECTION & 386x133 ML ROI
                // ====================================================
                var rawMlRoiBitmap: Bitmap? = null
                var calibratedMlRoiBitmap: Bitmap? = null

                try {
                    val testDeviceResult = testDeviceDetector.detectAndWarp(
                        bitmap = bitmap,
                        cardCorners = detection.corners,
                        context = context,
                        sourceImageName = sourceName
                    )
                    if (testDeviceResult != null) {
                        rawMlRoiBitmap = testDeviceResult.mlRoiBitmap
                        logMlPipeline("ML_PIPELINE: Test device detected = YES")
                        logTfliteDebug("testDeviceDetection=SUCCESS")
                        logTfliteDebug("roiExtraction=SUCCESS")
                        logTfliteDebug("deviceBitmap=${testDeviceResult.deviceBitmap.width}x${testDeviceResult.deviceBitmap.height}")
                        logTfliteDebug("resultROI=${rawMlRoiBitmap.width}x${rawMlRoiBitmap.height}")

                        // Apply 3x3 least-squares RGB color calibration matrix if available
                        if (calibrationResult.isCalibrated && calibrationResult.transformMatrix != null) {
                            calibratedMlRoiBitmap = colorCalibrator.applyCalibrationToBitmap(
                                rawMlRoiBitmap,
                                calibrationResult.transformMatrix
                            )
                            logTfliteDebug("colorCalibrationApplied=YES")
                        } else {
                            calibratedMlRoiBitmap = rawMlRoiBitmap
                            logTfliteDebug("colorCalibrationApplied=NO")
                        }
                    } else {
                        logMlPipeline("ML_PIPELINE: Test device detected = NO")
                        logTfliteDebug("testDeviceDetection=FAILED")
                        logTfliteDebug("roiExtraction=FAILED")
                    }
                } catch (e: Throwable) {
                    logMlPipeline("ML_PIPELINE: Test device detection error: ${e.message}")
                    logTfliteDebug("testDeviceDetection=FAILED")
                    logTfliteDebug("roiExtraction=FAILED")
                }

                // Optional legacy reference-card ROI extraction for backwards compatibility
                val kitRoiSpec = profile?.roiSpec ?: RoiSpec(xMin = 140, yMin = 250, width = 360, height = 160)
                val extraction = roiExtractor?.extractRoi(bitmap, detection.corners, kitRoiSpec)
                val extractedRoiSuccess = if (extraction is RoiExtractionResult.Success) extraction else null

                logQuality("ALL QUALITY CHECKS PASSED SUCCESSFULLY")
                return QualityResult.ImageValid(
                    roiExtractionResult = extractedRoiSuccess,
                    rawMlRoiBitmap = rawMlRoiBitmap,
                    calibratedMlRoiBitmap = calibratedMlRoiBitmap,
                    colorCalibrationResult = calibrationResult
                )
            }
        }
    }

    fun calculateLaplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 0.0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            gray[i] = 0.299 * r + 0.587 * g + 0.114 * b
        }

        val laplacianValues = DoubleArray((width - 2) * (height - 2))
        var index = 0
        var sum = 0.0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = gray[y * width + x]
                val top = gray[(y - 1) * width + x]
                val bottom = gray[(y + 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val right = gray[y * width + (x + 1)]

                val laplacian = top + bottom + left + right - 4.0 * center
                laplacianValues[index] = laplacian
                sum += laplacian
                index++
            }
        }

        if (index == 0) return 0.0
        val mean = sum / index
        var varianceSum = 0.0

        for (i in 0 until index) {
            val difference = laplacianValues[i] - mean
            varianceSum += difference * difference
        }

        return varianceSum / index
    }

    fun calculateMeanGrayscaleIntensity(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        if (pixels.isEmpty()) return 0.0

        var totalIntensity = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalIntensity += 0.299 * r + 0.587 * g + 0.114 * b
        }

        return totalIntensity / pixels.size
    }

    private fun logQuality(message: String) {
        try {
            Log.d("DrugTestQuality", message)
        } catch (_: Throwable) {
            println("[DrugTestQuality] $message")
        }
        logDiagnostic(message)
    }

    private fun logMlPipeline(msg: String) {
        try {
            Log.d("ML_PIPELINE", msg)
        } catch (_: Throwable) {
            println("[ML_PIPELINE] $msg")
        }
    }

    private fun logTfliteDebug(msg: String) {
        try {
            Log.d("TFLITE_DEBUG", msg)
        } catch (_: Throwable) {
            println("[TFLITE_DEBUG] $msg")
        }
    }

    private fun logDiagnostic(message: String) {
        try {
            Log.d("CaptureDiagnostic", message)
        } catch (_: Throwable) {
            println("[CaptureDiagnostic] $message")
        }
    }
}

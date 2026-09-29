package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class RgbColor(val r: Double, val g: Double, val b: Double)

data class LabColor(
    val l: Double, // Lightness: 0.0 to 100.0
    val a: Double, // a*: -128.0 (green) to +127.0 (red)
    val b: Double  // b*: -128.0 (blue) to +127.0 (yellow)
)

data class ColorCalibrationResult(
    val isCalibrated: Boolean,
    val transformMatrix: DoubleArray? = null, // 3x3 matrix in row-major order
    val sampledRgbValues: Map<String, RgbColor> = emptyMap(),
    val targetRgbValues: Map<String, RgbColor> = emptyMap(),
    val statusMessage: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorCalibrationResult

        if (isCalibrated != other.isCalibrated) return false
        if (transformMatrix != null) {
            if (other.transformMatrix == null) return false
            if (!transformMatrix.contentEquals(other.transformMatrix)) return false
        } else if (other.transformMatrix != null) return false
        if (sampledRgbValues != other.sampledRgbValues) return false
        if (targetRgbValues != other.targetRgbValues) return false
        if (statusMessage != other.statusMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCalibrated.hashCode()
        result = 31 * result + (transformMatrix?.contentHashCode() ?: 0)
        result = 31 * result + sampledRgbValues.hashCode()
        result = 31 * result + targetRgbValues.hashCode()
        result = 31 * result + statusMessage.hashCode()
        return result
    }
}

/**
 * Performs reference card color patch sampling and calculates a 3x3 least-squares RGB color transformation matrix
 * using synthetic training-derived targets for ML ROI color calibration.
 */
@Singleton
class ColorCalibrator @Inject constructor() {

    private val isNativeOpenCvLoaded: Boolean by lazy {
        try {
            OpenCVLoader.initDebug()
        } catch (_: Throwable) {
            try {
                System.loadLibrary("opencv_java4")
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    companion object {
        // SYNTHETIC TRAINING-DERIVED TARGETS (Not physical ground-truth values)
        val SYNTHETIC_RGB_TARGETS = mapOf(
            "White" to RgbColor(238.06, 239.70, 238.02),
            "Gray"  to RgbColor(126.22, 125.90, 125.68),
            "Black" to RgbColor(23.67, 23.13, 23.22),
            "Red"   to RgbColor(216.87, 27.75, 28.47),
            "Green" to RgbColor(28.24, 157.22, 57.53),
            "Blue"  to RgbColor(28.29, 77.70, 195.82)
        )

        // Canonical patch bounding boxes in 640x960 space: [xMin, yMin, width, height]
        val CANONICAL_PATCH_ROIS = mapOf(
            "White" to Rect(160, 432, 115, 115),
            "Gray"  to Rect(275, 432, 115, 115),
            "Black" to Rect(390, 432, 115, 115),
            "Red"   to Rect(160, 605, 115, 115),
            "Green" to Rect(275, 605, 115, 115),
            "Blue"  to Rect(390, 605, 115, 115)
        )
    }

    /**
     * Samples reference patches and computes a 3x3 least-squares RGB calibration transform matrix.
     */
    fun computeCalibrationTransform(
        bitmap: Bitmap,
        cardCorners: List<PointF>
    ): ColorCalibrationResult {
        val sampledRgb = if (cardCorners.size >= 4) {
            sampleRgbPatchesWarped(bitmap, cardCorners)
        } else {
            sampleRgbPatchesFromBitmap(bitmap)
        }

        // Must have at least 4 valid sampled patches
        if (sampledRgb.size < 4) {
            logCalibrationDebug("cardDetected=NO")
            logCalibrationDebug("Insufficient patches sampled (${sampledRgb.size}/6)")
            return ColorCalibrationResult(
                isCalibrated = false,
                statusMessage = "FAILED — Insufficient patches sampled (${sampledRgb.size}/6)"
            )
        }

        logCalibrationDebug("cardDetected=YES")
        logCalibrationDebug("whiteObserved=${sampledRgb["White"]}")
        logCalibrationDebug("grayObserved=${sampledRgb["Gray"]}")
        logCalibrationDebug("blackObserved=${sampledRgb["Black"]}")
        logCalibrationDebug("redObserved=${sampledRgb["Red"]}")
        logCalibrationDebug("greenObserved=${sampledRgb["Green"]}")
        logCalibrationDebug("blueObserved=${sampledRgb["Blue"]}")

        val transformMatrix = calculateLeastSquares3x3Matrix(sampledRgb, SYNTHETIC_RGB_TARGETS)

        if (transformMatrix == null) {
            return ColorCalibrationResult(
                isCalibrated = false,
                sampledRgbValues = sampledRgb,
                targetRgbValues = SYNTHETIC_RGB_TARGETS,
                statusMessage = "FAILED — Matrix solver singular or non-convergent"
            )
        }

        logCalibrationDebug("transform=[${transformMatrix.joinToString(", ") { String.format("%.4f", it) }}]")

        return ColorCalibrationResult(
            isCalibrated = true,
            transformMatrix = transformMatrix,
            sampledRgbValues = sampledRgb,
            targetRgbValues = SYNTHETIC_RGB_TARGETS,
            statusMessage = "SUCCESS — 3x3 Least-Squares RGB Calibration Matrix Calculated"
        )
    }

    /**
     * Calculates 3x3 least-squares RGB transformation matrix M such that Measured * M ≈ Targets.
     */
    fun calculateLeastSquares3x3Matrix(
        measuredRgbMap: Map<String, RgbColor>,
        targetRgbMap: Map<String, RgbColor>
    ): DoubleArray? {
        val patchNames = measuredRgbMap.keys.filter { targetRgbMap.containsKey(it) }
        val n = patchNames.size
        if (n < 4) return null

        var p00 = 0.0; var p01 = 0.0; var p02 = 0.0
        var p10 = 0.0; var p11 = 0.0; var p12 = 0.0
        var p20 = 0.0; var p21 = 0.0; var p22 = 0.0

        var t00 = 0.0; var t01 = 0.0; var t02 = 0.0
        var t10 = 0.0; var t11 = 0.0; var t12 = 0.0
        var t20 = 0.0; var t21 = 0.0; var t22 = 0.0

        for (name in patchNames) {
            val m = measuredRgbMap[name]!!
            val t = targetRgbMap[name]!!

            p00 += m.r * m.r; p01 += m.r * m.g; p02 += m.r * m.b
            p10 += m.g * m.r; p11 += m.g * m.g; p12 += m.g * m.b
            p20 += m.b * m.r; p21 += m.b * m.g; p22 += m.b * m.b

            t00 += m.r * t.r; t01 += m.r * t.g; t02 += m.r * t.b
            t10 += m.g * t.r; t11 += m.g * t.g; t12 += m.g * t.b
            t20 += m.b * t.r; t21 += m.b * t.g; t22 += m.b * t.b
        }

        val det = p00 * (p11 * p22 - p12 * p21) -
                  p01 * (p10 * p22 - p12 * p20) +
                  p02 * (p10 * p21 - p11 * p20)

        if (abs(det) < 1e-8) return null

        val invDet = 1.0 / det

        val inv00 = (p11 * p22 - p12 * p21) * invDet
        val inv01 = (p02 * p21 - p01 * p22) * invDet
        val inv02 = (p01 * p12 - p02 * p11) * invDet

        val inv10 = (p12 * p20 - p10 * p22) * invDet
        val inv11 = (p00 * p22 - p02 * p20) * invDet
        val inv12 = (p02 * p10 - p00 * p12) * invDet

        val inv20 = (p10 * p21 - p11 * p20) * invDet
        val inv21 = (p01 * p20 - p00 * p21) * invDet
        val inv22 = (p00 * p11 - p01 * p10) * invDet

        val m00 = inv00 * t00 + inv01 * t10 + inv02 * t20
        val m01 = inv00 * t01 + inv01 * t11 + inv02 * t21
        val m02 = inv00 * t02 + inv01 * t12 + inv02 * t22

        val m10 = inv10 * t00 + inv11 * t10 + inv12 * t20
        val m11 = inv10 * t01 + inv11 * t11 + inv12 * t21
        val m12 = inv10 * t02 + inv11 * t12 + inv12 * t22

        val m20 = inv20 * t00 + inv21 * t10 + inv22 * t20
        val m21 = inv20 * t01 + inv21 * t11 + inv22 * t21
        val m22 = inv20 * t02 + inv21 * t12 + inv22 * t22

        return doubleArrayOf(
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22
        )
    }

    /**
     * Applies a 3x3 RGB color transformation matrix M to an ML ROI Bitmap.
     * Preserves original dimensions and clamps pixels to 0..255.
     */
    fun applyCalibrationToBitmap(
        bitmap: Bitmap,
        matrixArray: DoubleArray
    ): Bitmap {
        if (matrixArray.size != 9) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        try {
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        } catch (_: Throwable) {
            return bitmap
        }

        val calibPixels = IntArray(width * height)

        val m00 = matrixArray[0]; val m01 = matrixArray[1]; val m02 = matrixArray[2]
        val m10 = matrixArray[3]; val m11 = matrixArray[4]; val m12 = matrixArray[5]
        val m20 = matrixArray[6]; val m21 = matrixArray[7]; val m22 = matrixArray[8]

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF).toDouble()
            val g = ((p shr 8) and 0xFF).toDouble()
            val b = (p and 0xFF).toDouble()

            val rCalib = (r * m00 + g * m10 + b * m20).coerceIn(0.0, 255.0).toInt()
            val gCalib = (r * m01 + g * m11 + b * m21).coerceIn(0.0, 255.0).toInt()
            val bCalib = (r * m02 + g * m12 + b * m22).coerceIn(0.0, 255.0).toInt()

            calibPixels[i] = (0xFF shl 24) or (rCalib shl 16) or (gCalib shl 8) or bCalib
        }

        return try {
            val calibBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            calibBitmap.setPixels(calibPixels, 0, width, 0, 0, width, height)
            calibBitmap
        } catch (_: Throwable) {
            bitmap
        }
    }

    fun sampleRgbPatchesWarped(
        bitmap: Bitmap,
        corners: List<PointF>
    ): Map<String, RgbColor> {
        if (!isNativeOpenCvLoaded) {
            return sampleRgbPatchesFromBitmap(bitmap)
        }

        return try {
            val srcMat = Mat()
            Utils.bitmapToMat(bitmap, srcMat)

            val srcPoints = MatOfPoint2f(
                Point(corners[0].x.toDouble(), corners[0].y.toDouble()),
                Point(corners[1].x.toDouble(), corners[1].y.toDouble()),
                Point(corners[2].x.toDouble(), corners[2].y.toDouble()),
                Point(corners[3].x.toDouble(), corners[3].y.toDouble())
            )

            val dstPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(640.0, 0.0),
                Point(640.0, 960.0),
                Point(0.0, 960.0)
            )

            val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
            val warpedMat = Mat(960, 640, CvType.CV_8UC4)
            Imgproc.warpPerspective(srcMat, warpedMat, transformMatrix, Size(640.0, 960.0))

            val warpedBitmap = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warpedMat, warpedBitmap)

            srcMat.release()
            srcPoints.release()
            dstPoints.release()
            transformMatrix.release()
            warpedMat.release()

            sampleRgbPatchesFromBitmap(warpedBitmap)
        } catch (_: Throwable) {
            sampleRgbPatchesFromBitmap(bitmap)
        }
    }

    fun sampleRgbPatchesFromBitmap(bitmap: Bitmap): Map<String, RgbColor> {
        val result = mutableMapOf<String, RgbColor>()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        try {
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        } catch (_: Throwable) {
            return result
        }

        for ((name, roi) in CANONICAL_PATCH_ROIS) {
            val normX = roi.x / 640.0
            val normY = roi.y / 960.0
            val normW = roi.width / 640.0
            val normH = roi.height / 960.0

            val px = (normX * width).toInt()
            val py = (normY * height).toInt()
            val pw = (normW * width).toInt()
            val ph = (normH * height).toInt()

            val insetX = (pw * 0.20).toInt()
            val insetY = (ph * 0.20).toInt()

            val startX = (px + insetX).coerceIn(0, width - 1)
            val endX = (px + pw - insetX).coerceIn(startX + 1, width)
            val startY = (py + insetY).coerceIn(0, height - 1)
            val endY = (py + ph - insetY).coerceIn(startY + 1, height)

            var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0

            for (y in startY until endY) {
                for (x in startX until endX) {
                    val p = pixels[y * width + x]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                    count++
                }
            }

            if (count > 0) {
                result[name] = RgbColor(
                    r = rSum.toDouble() / count,
                    g = gSum.toDouble() / count,
                    b = bSum.toDouble() / count
                )
            }
        }

        return result
    }

    fun rgbToLab(r: Int, g: Int, b: Int): LabColor {
        var rNorm = r / 255.0; var gNorm = g / 255.0; var bNorm = b / 255.0

        rNorm = if (rNorm > 0.04045) ((rNorm + 0.055) / 1.055).pow(2.4) else rNorm / 12.92
        gNorm = if (gNorm > 0.04045) ((gNorm + 0.055) / 1.055).pow(2.4) else gNorm / 12.92
        bNorm = if (bNorm > 0.04045) ((bNorm + 0.055) / 1.055).pow(2.4) else bNorm / 12.92

        val x = (rNorm * 0.4124564 + gNorm * 0.3575761 + bNorm * 0.1804375) / 0.95047
        val y = (rNorm * 0.2126729 + gNorm * 0.7151522 + bNorm * 0.0721750) / 1.00000
        val z = (rNorm * 0.0193339 + gNorm * 0.1191920 + bNorm * 0.9503041) / 1.08883

        val fx = if (x > 0.008856) x.pow(1.0 / 3.0) else (7.787 * x) + (16.0 / 116.0)
        val fy = if (y > 0.008856) y.pow(1.0 / 3.0) else (7.787 * y) + (16.0 / 116.0)
        val fz = if (z > 0.008856) z.pow(1.0 / 3.0) else (7.787 * z) + (16.0 / 116.0)

        val l = (116.0 * fy) - 16.0
        val a = 500.0 * (fx - fy)
        val bVal = 200.0 * (fy - fz)

        return LabColor(l = l.coerceIn(0.0, 100.0), a = a, b = bVal)
    }

    fun calculateDeltaE(c1: LabColor, c2: LabColor): Double {
        val dL = c1.l - c2.l; val da = c1.a - c2.a; val db = c1.b - c2.b
        return sqrt(dL * dL + da * da + db * db)
    }

    private fun logCalibrationDebug(msg: String) {
        try {
            Log.d("CALIBRATION_DEBUG", msg)
        } catch (_: Throwable) {
            println("[CALIBRATION_DEBUG] $msg")
        }
    }
}

package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
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
import kotlin.math.pow
import kotlin.math.sqrt

data class LabColor(
    val l: Double, // Lightness: 0.0 to 100.0
    val a: Double, // a*: -128.0 (green) to +127.0 (red)
    val b: Double  // b*: -128.0 (blue) to +127.0 (yellow)
)

data class CalibrationResult(
    val isCalibrated: Boolean,
    val meanDeltaE: Double,
    val sampledLabValues: Map<String, LabColor>,
    val correctedLabValues: Map<String, LabColor>,
    val statusMessage: String
)

/**
 * ColorCalibrator performs perspective transformation of detected card corners to canonical 640x960 px space,
 * samples patch colors from 6 safe inner center ROIs, and converts sRGB to CIE L*a*b*.
 *
 * NOTE: Full LAB correction gain matrix calculation is PENDING until physical reference patch target LAB values
 * are measured and provided.
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

    // Canonical patch bounding boxes in 640x960 space: [xMin, yMin, width, height]
    private val canonicalPatchRois = mapOf(
        "White" to Rect(160, 432, 115, 115),
        "Gray" to Rect(275, 432, 115, 115),
        "Black" to Rect(390, 432, 115, 115),
        "Red" to Rect(160, 605, 115, 115),
        "Green" to Rect(275, 605, 115, 115),
        "Blue" to Rect(390, 605, 115, 115)
    )

    fun calibrate(
        bitmap: Bitmap,
        cardCorners: List<PointF>
    ): CalibrationResult {
        val sampledLab = if (isNativeOpenCvLoaded && cardCorners.size >= 4) {
            samplePatchesWarped(bitmap, cardCorners)
        } else {
            samplePatchesFromBitmap(bitmap)
        }

        // LAB Target Reference Values remain PENDING / UNMEASURED
        return CalibrationResult(
            isCalibrated = false,
            meanDeltaE = -1.0,
            sampledLabValues = sampledLab,
            correctedLabValues = emptyMap(),
            statusMessage = "PENDING — PHYSICAL REFERENCE PATCH LAB TARGET VALUES UNAVAILABLE"
        )
    }

    /**
     * Warps detected card corners to canonical 640x960 px view using OpenCV perspective transformation
     * and samples the inner 60% center region of each reference patch.
     */
    fun samplePatchesWarped(
        bitmap: Bitmap,
        corners: List<PointF>
    ): Map<String, LabColor> {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        val srcPoints = MatOfPoint2f(
            Point(corners[0].x.toDouble(), corners[0].y.toDouble()), // TL
            Point(corners[1].x.toDouble(), corners[1].y.toDouble()), // TR
            Point(corners[2].x.toDouble(), corners[2].y.toDouble()), // BR
            Point(corners[3].x.toDouble(), corners[3].y.toDouble())  // BL
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

        return samplePatchesFromBitmap(warpedBitmap)
    }

    private fun samplePatchesFromBitmap(bitmap: Bitmap): Map<String, LabColor> {
        val result = mutableMapOf<String, LabColor>()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for ((name, roi) in canonicalPatchRois) {
            val normX = roi.x / 640.0
            val normY = roi.y / 960.0
            val normW = roi.width / 640.0
            val normH = roi.height / 960.0

            val px = (normX * width).toInt()
            val py = (normY * height).toInt()
            val pw = (normW * width).toInt()
            val ph = (normH * height).toInt()

            // Sample safe inner 60% center region (inset 20% on each side)
            val insetX = (pw * 0.20).toInt()
            val insetY = (ph * 0.20).toInt()

            val startX = (px + insetX).coerceIn(0, width - 1)
            val endX = (px + pw - insetX).coerceIn(startX + 1, width)
            val startY = (py + insetY).coerceIn(0, height - 1)
            val endY = (py + ph - insetY).coerceIn(startY + 1, height)

            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0

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
                val avgR = (rSum / count).toInt()
                val avgG = (gSum / count).toInt()
                val avgB = (bSum / count).toInt()
                result[name] = rgbToLab(avgR, avgG, avgB)
            }
        }

        return result
    }

    /**
     * Converts sRGB (0..255) to CIE L*a*b* (D65 illuminant, 2° observer).
     */
    fun rgbToLab(r: Int, g: Int, b: Int): LabColor {
        var rNorm = r / 255.0
        var gNorm = g / 255.0
        var bNorm = b / 255.0

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

    /**
     * Calculates CIE Delta E 1976 color difference.
     */
    fun calculateDeltaE(c1: LabColor, c2: LabColor): Double {
        val dL = c1.l - c2.l
        val da = c1.a - c2.a
        val db = c1.b - c2.b
        return sqrt(dL * dL + da * da + db * db)
    }
}

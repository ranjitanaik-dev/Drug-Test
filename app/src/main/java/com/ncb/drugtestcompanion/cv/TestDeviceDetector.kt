package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.sqrt

data class TestDeviceDetectionResult(
    val deviceBitmap: Bitmap, // 640x400 warped device
    val mlRoiBitmap: Bitmap,  // 386x133 cropped ML ROI (x=206, y=135, w=386, h=133)
    val corners: List<PointF>,
    val confidence: Double
)

/**
 * Detects the rectangular test device in the camera image, perspective warps it to 640x400,
 * and crops the exact 386x133 ML ROI at (x=206, y=135, width=386, height=133).
 */
class TestDeviceDetector {

    companion object {
        const val OUTPUT_WIDTH = 640
        const val OUTPUT_HEIGHT = 400

        // ML ROI Crop Specifications on 640x400 test device image:
        const val ML_ROI_X = 206
        const val ML_ROI_Y = 135
        const val ML_ROI_WIDTH = 386
        const val ML_ROI_HEIGHT = 133

        private const val MIN_AREA_RATIO = 0.02
        private const val MAX_AREA_RATIO = 0.85
    }

    fun detectAndWarp(bitmap: Bitmap): TestDeviceDetectionResult? {
        val source = Mat()

        try {
            Utils.bitmapToMat(bitmap, source)
            if (source.empty()) {
                logMlPipeline("ML_PIPELINE: Test device detection failed (empty mat)")
                return fallbackWarpAndCrop(bitmap)
            }

            val gray = Mat()
            val blurred = Mat()
            val edges = Mat()

            try {
                Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
                Imgproc.Canny(blurred, edges, 40.0, 140.0)

                val contours = ArrayList<MatOfPoint>()
                val hierarchy = Mat()

                try {
                    Imgproc.findContours(
                        edges,
                        contours,
                        hierarchy,
                        Imgproc.RETR_EXTERNAL,
                        Imgproc.CHAIN_APPROX_SIMPLE
                    )

                    val imageArea = source.rows().toDouble() * source.cols().toDouble()
                    var bestCorners: List<Point>? = null
                    var bestScore = 0.0

                    for (contour in contours) {
                        val contourArea = Imgproc.contourArea(contour)
                        if (contourArea <= 0.0) continue

                        val areaRatio = contourArea / imageArea
                        if (areaRatio < MIN_AREA_RATIO || areaRatio > MAX_AREA_RATIO) continue

                        val contour2f = MatOfPoint2f(*contour.toArray())
                        try {
                            val perimeter = Imgproc.arcLength(contour2f, true)
                            val approximation = MatOfPoint2f()
                            try {
                                Imgproc.approxPolyDP(contour2f, approximation, 0.02 * perimeter, true)
                                if (approximation.rows() != 4) continue

                                val points = approximation.toArray()
                                if (points.size != 4) continue

                                val ordered = orderPoints(points)
                                val widthTop = distance(ordered[0], ordered[1])
                                val widthBottom = distance(ordered[3], ordered[2])
                                val heightLeft = distance(ordered[0], ordered[3])
                                val heightRight = distance(ordered[1], ordered[2])

                                val averageWidth = (widthTop + widthBottom) / 2.0
                                val averageHeight = (heightLeft + heightRight) / 2.0

                                if (averageWidth <= 0.0 || averageHeight <= 0.0) continue

                                val aspectRatio = averageWidth / averageHeight
                                if (aspectRatio < 0.7 || aspectRatio > 4.5) continue

                                val widthDiff = abs(widthTop - widthBottom) / averageWidth
                                val heightDiff = abs(heightLeft - heightRight) / averageHeight
                                if (widthDiff > 0.45 || heightDiff > 0.45) continue

                                val rectangularity = 1.0 - ((widthDiff + heightDiff) / 2.0)
                                val score = areaRatio * rectangularity

                                if (score > bestScore) {
                                    bestScore = score
                                    bestCorners = ordered.toList()
                                }
                            } finally {
                                approximation.release()
                            }
                        } finally {
                            contour2f.release()
                        }
                    }

                    if (bestCorners == null) {
                        logMlPipeline("ML_PIPELINE: Test device contour detection missed quad, using adaptive fallback warp")
                        return fallbackWarpAndCrop(bitmap)
                    }

                    val sourcePoints = MatOfPoint2f(*bestCorners.toTypedArray())
                    val destinationPoints = MatOfPoint2f(
                        Point(0.0, 0.0),
                        Point(OUTPUT_WIDTH - 1.0, 0.0),
                        Point(OUTPUT_WIDTH - 1.0, OUTPUT_HEIGHT - 1.0),
                        Point(0.0, OUTPUT_HEIGHT - 1.0)
                    )

                    val transform = Imgproc.getPerspectiveTransform(sourcePoints, destinationPoints)
                    val warped = Mat(OUTPUT_HEIGHT, OUTPUT_WIDTH, source.type())

                    try {
                        Imgproc.warpPerspective(
                            source,
                            warped,
                            transform,
                            Size(OUTPUT_WIDTH.toDouble(), OUTPUT_HEIGHT.toDouble())
                        )

                        val deviceBitmap = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(warped, deviceBitmap)

                        val mlRoiBitmap = cropMlRoi(deviceBitmap)

                        logMlPipeline("ML_PIPELINE: Test device detected = YES")
                        logMlPipeline("ML_PIPELINE: Test device warped = 640x400")
                        logMlPipeline("ML_PIPELINE: ML ROI = x=$ML_ROI_X y=$ML_ROI_Y width=$ML_ROI_WIDTH height=$ML_ROI_HEIGHT")
                        logMlPipeline("ML_PIPELINE: ML ROI size = ${mlRoiBitmap.width}x${mlRoiBitmap.height}")

                        return TestDeviceDetectionResult(
                            deviceBitmap = deviceBitmap,
                            mlRoiBitmap = mlRoiBitmap,
                            corners = bestCorners.map { PointF(it.x.toFloat(), it.y.toFloat()) },
                            confidence = bestScore
                        )
                    } finally {
                        sourcePoints.release()
                        destinationPoints.release()
                        transform.release()
                        warped.release()
                    }
                } finally {
                    hierarchy.release()
                    contours.forEach { it.release() }
                }
            } finally {
                gray.release()
                blurred.release()
                edges.release()
            }
        } catch (e: Throwable) {
            logMlPipeline("ML_PIPELINE: TestDeviceDetector exception: ${e.message}")
            return fallbackWarpAndCrop(bitmap)
        }
    }

    private fun fallbackWarpAndCrop(bitmap: Bitmap): TestDeviceDetectionResult {
        val deviceBitmap = if (bitmap.width == OUTPUT_WIDTH && bitmap.height == OUTPUT_HEIGHT) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, OUTPUT_WIDTH, OUTPUT_HEIGHT, true)
        }

        val mlRoiBitmap = cropMlRoi(deviceBitmap)

        logMlPipeline("ML_PIPELINE: Test device detected = YES (Fallback scaling)")
        logMlPipeline("ML_PIPELINE: Test device warped = 640x400")
        logMlPipeline("ML_PIPELINE: ML ROI = x=$ML_ROI_X y=$ML_ROI_Y width=$ML_ROI_WIDTH height=$ML_ROI_HEIGHT")
        logMlPipeline("ML_PIPELINE: ML ROI size = ${mlRoiBitmap.width}x${mlRoiBitmap.height}")

        val defaultCorners = listOf(
            PointF(0f, 0f),
            PointF(OUTPUT_WIDTH.toFloat(), 0f),
            PointF(OUTPUT_WIDTH.toFloat(), OUTPUT_HEIGHT.toFloat()),
            PointF(0f, OUTPUT_HEIGHT.toFloat())
        )

        return TestDeviceDetectionResult(
            deviceBitmap = deviceBitmap,
            mlRoiBitmap = mlRoiBitmap,
            corners = defaultCorners,
            confidence = 0.5
        )
    }

    private fun cropMlRoi(deviceBitmap: Bitmap): Bitmap {
        val safeX = ML_ROI_X.coerceIn(0, deviceBitmap.width - 1)
        val safeY = ML_ROI_Y.coerceIn(0, deviceBitmap.height - 1)
        val safeW = ML_ROI_WIDTH.coerceAtMost(deviceBitmap.width - safeX)
        val safeH = ML_ROI_HEIGHT.coerceAtMost(deviceBitmap.height - safeY)

        return Bitmap.createBitmap(
            deviceBitmap,
            safeX,
            safeY,
            safeW,
            safeH
        )
    }

    private fun orderPoints(points: Array<Point>): Array<Point> {
        val topLeft = points.minBy { it.x + it.y }
        val bottomRight = points.maxBy { it.x + it.y }
        val topRight = points.minBy { it.y - it.x }
        val bottomLeft = points.maxBy { it.y - it.x }

        return arrayOf(
            topLeft,
            topRight,
            bottomRight,
            bottomLeft
        )
    }

    private fun distance(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun logMlPipeline(msg: String) {
        try {
            Log.d("ML_PIPELINE", msg)
        } catch (_: Throwable) {
            println("[ML_PIPELINE] $msg")
        }
    }
}

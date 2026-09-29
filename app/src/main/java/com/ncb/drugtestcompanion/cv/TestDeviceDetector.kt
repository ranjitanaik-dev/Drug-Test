package com.ncb.drugtestcompanion.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class TestDeviceCandidate(
    val corners: List<Point>,
    val areaRatio: Double,
    val aspectRatio: Double,
    val rectangularity: Double,
    val solidity: Double,
    val cardOverlapRatio: Double,
    val score: Double
)

data class TestDeviceDetectionResult(
    val deviceBitmap: Bitmap, // 640x400 warped device
    val mlRoiBitmap: Bitmap,  // 386x133 cropped ML ROI (x=206, y=135, w=386, h=133)
    val corners: List<PointF>,
    val confidence: Double,
    val candidatesCount: Int
)

/**
 * Anchors test-device quad search using reference-card geometry.
 * Evaluates candidate quadrilaterals, detects physical long-axis orientation (HORIZONTAL / VERTICAL),
 * perspective-warps the selected test device to 640x400, and crops the exact 386x133 ML ROI
 * at (x=206, y=135, width=386, height=133).
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

    fun detectAndWarp(
        bitmap: Bitmap,
        cardCorners: List<PointF>? = null,
        context: Context? = null,
        sourceImageName: String = "capture"
    ): TestDeviceDetectionResult {
        val source = Mat()

        try {
            Utils.bitmapToMat(bitmap, source)
            if (source.empty()) {
                logMlPipeline("ML_PIPELINE: Test device detection failed (empty mat)")
                return fallbackWarpAndCrop(bitmap, context, sourceImageName, cardCorners)
            }

            // Reference Card Bounding Box
            var cardMinX = Float.MAX_VALUE
            var cardMaxX = -Float.MAX_VALUE
            var cardMinY = Float.MAX_VALUE
            var cardMaxY = -Float.MAX_VALUE

            if (cardCorners != null && cardCorners.size >= 4) {
                cardMinX = cardCorners.minOf { it.x }
                cardMaxX = cardCorners.maxOf { it.x }
                cardMinY = cardCorners.minOf { it.y }
                cardMaxY = cardCorners.maxOf { it.y }
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
                    val candidates = mutableListOf<TestDeviceCandidate>()

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
                                if (aspectRatio < 0.2 || aspectRatio > 5.0) continue

                                val widthDiff = abs(widthTop - widthBottom) / averageWidth
                                val heightDiff = abs(heightLeft - heightRight) / averageHeight
                                if (widthDiff > 0.50 || heightDiff > 0.50) continue

                                // Overlap with Reference Card Box
                                var cardOverlapRatio = 0.0
                                if (cardCorners != null && cardCorners.size >= 4) {
                                    val candMinX = ordered.minOf { it.x.toFloat() }
                                    val candMaxX = ordered.maxOf { it.x.toFloat() }
                                    val candMinY = ordered.minOf { it.y.toFloat() }
                                    val candMaxY = ordered.maxOf { it.y.toFloat() }

                                    val interLeft = max(cardMinX, candMinX)
                                    val interRight = min(cardMaxX, candMaxX)
                                    val interTop = max(cardMinY, candMinY)
                                    val interBottom = min(cardMaxY, candMaxY)

                                    if (interLeft < interRight && interTop < interBottom) {
                                        val interArea = (interRight - interLeft) * (interBottom - interTop)
                                        val candArea = (candMaxX - candMinX) * (candMaxY - candMinY)
                                        if (candArea > 0) {
                                            cardOverlapRatio = (interArea / candArea).toDouble()
                                        }
                                    }
                                }

                                // Reject reference card itself
                                if (cardOverlapRatio > 0.35) continue

                                // Candidate Solidity
                                val hullInt = MatOfInt()
                                Imgproc.convexHull(contour, hullInt)
                                val hullPoints = contour.toArray().filterIndexed { idx, _ -> idx in hullInt.toArray() }
                                val hullArea = Imgproc.contourArea(MatOfPoint(*hullPoints.toTypedArray())).coerceAtLeast(1.0)
                                val solidity = (contourArea / hullArea).coerceIn(0.0, 1.0)

                                val rectangularity = 1.0 - ((widthDiff + heightDiff) / 2.0)
                                val maxDim = max(averageWidth, averageHeight)
                                val minDim = min(averageWidth, averageHeight).coerceAtLeast(1.0)
                                val trueAspect = maxDim / minDim
                                val aspectScore = 1.0 - (abs(trueAspect - 2.2) / 2.2).coerceIn(0.0, 1.0)

                                val score = areaRatio * rectangularity * solidity * aspectScore * (1.0 - cardOverlapRatio)

                                candidates.add(
                                    TestDeviceCandidate(
                                        corners = ordered.toList(),
                                        areaRatio = areaRatio,
                                        aspectRatio = trueAspect,
                                        rectangularity = rectangularity,
                                        solidity = solidity,
                                        cardOverlapRatio = cardOverlapRatio,
                                        score = score
                                    )
                                )
                            } finally {
                                approximation.release()
                            }
                        } finally {
                            contour2f.release()
                        }
                    }

                    logDeviceDetection("originalWidth=${bitmap.width}")
                    logDeviceDetection("originalHeight=${bitmap.height}")
                    logDeviceDetection("candidateCount=${candidates.size}")

                    val selectedCandidate = candidates.maxByOrNull { it.score }

                    if (selectedCandidate == null) {
                        logDeviceDetection("quadFound=NO")
                        logMlPipeline("ML_PIPELINE: Test device contour detection missed quad, using adaptive fallback warp")
                        return fallbackWarpAndCrop(bitmap, context, sourceImageName, cardCorners)
                    }

                    val bestCorners = selectedCandidate.corners

                    // Step 2 & 3: Calculate Edge Lengths and Physical Long-Axis Orientation
                    val topEdge = distance(bestCorners[0], bestCorners[1])
                    val bottomEdge = distance(bestCorners[3], bestCorners[2])
                    val leftEdge = distance(bestCorners[0], bestCorners[3])
                    val rightEdge = distance(bestCorners[1], bestCorners[2])

                    val avgW = (topEdge + bottomEdge) / 2.0
                    val avgH = (leftEdge + rightEdge) / 2.0
                    val isVertical = avgH > avgW

                    val orientationLabel = if (isVertical) "VERTICAL" else "HORIZONTAL"

                    logDeviceDetection("quadFound=YES")
                    logDeviceDetection("orientation=$orientationLabel")
                    logDeviceDetection("original edge lengths: top=${topEdge.toInt()}, bottom=${bottomEdge.toInt()}, left=${leftEdge.toInt()}, right=${rightEdge.toInt()}")
                    logDeviceDetection("selected corners=[${bestCorners.joinToString { "(${it.x.toInt()}, ${it.y.toInt()})" }}]")
                    logDeviceDetection("warped size=${OUTPUT_WIDTH}x$OUTPUT_HEIGHT")
                    logDeviceDetection("ROI coordinates: x=$ML_ROI_X, y=$ML_ROI_Y, width=$ML_ROI_WIDTH, height=$ML_ROI_HEIGHT")

                    // Step 4 & 5: Reorder source corner points for vertical/horizontal long axis alignment
                    val orientedSourceCorners = if (isVertical) {
                        // Cassette is vertical: rotate corners so vertical long axis becomes horizontal on 640x400 canvas
                        // [3]=BL -> (0,0), [0]=TL -> (639,0), [1]=TR -> (639,399), [2]=BR -> (0,399)
                        listOf(bestCorners[3], bestCorners[0], bestCorners[1], bestCorners[2])
                    } else {
                        // Cassette is horizontal: keep standard order [0]=TL, [1]=TR, [2]=BR, [3]=BL
                        bestCorners
                    }

                    val sourcePoints = MatOfPoint2f(*orientedSourceCorners.toTypedArray())
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

                        logMlRoi("x=$ML_ROI_X")
                        logMlRoi("y=$ML_ROI_Y")
                        logMlRoi("width=$ML_ROI_WIDTH")
                        logMlRoi("height=$ML_ROI_HEIGHT")
                        logMlRoi("roiWidth=${mlRoiBitmap.width}")
                        logMlRoi("roiHeight=${mlRoiBitmap.height}")

                        logMlPipeline("ML_PIPELINE: Test device detected = YES ($orientationLabel)")
                        logMlPipeline("ML_PIPELINE: Test device warped = 640x400")
                        logMlPipeline("ML_PIPELINE: ML ROI = x=$ML_ROI_X y=$ML_ROI_Y width=$ML_ROI_WIDTH height=$ML_ROI_HEIGHT")
                        logMlPipeline("ML_PIPELINE: ML ROI size = ${mlRoiBitmap.width}x${mlRoiBitmap.height}")

                        // Step 6: Save Visual Debug Images
                        if (context != null) {
                            saveDebugVisualImages(
                                context = context,
                                originalBitmap = bitmap,
                                cardCorners = cardCorners,
                                candidates = candidates,
                                selectedCandidate = selectedCandidate,
                                deviceBitmap = deviceBitmap,
                                sourceName = sourceImageName
                            )
                        }

                        return TestDeviceDetectionResult(
                            deviceBitmap = deviceBitmap,
                            mlRoiBitmap = mlRoiBitmap,
                            corners = bestCorners.map { PointF(it.x.toFloat(), it.y.toFloat()) },
                            confidence = selectedCandidate.score,
                            candidatesCount = candidates.size
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
            return fallbackWarpAndCrop(bitmap, context, sourceImageName, cardCorners)
        }
    }

    private fun fallbackWarpAndCrop(
        bitmap: Bitmap,
        context: Context? = null,
        sourceImageName: String = "capture",
        cardCorners: List<PointF>? = null
    ): TestDeviceDetectionResult {
        val deviceBitmap = if (bitmap.width == OUTPUT_WIDTH && bitmap.height == OUTPUT_HEIGHT) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, OUTPUT_WIDTH, OUTPUT_HEIGHT, true)
        }

        val mlRoiBitmap = cropMlRoi(deviceBitmap)

        logDeviceDetection("quadFound=FALLBACK_SCALED")
        logDeviceDetection("orientation=FALLBACK")
        logDeviceDetection("warped size=${OUTPUT_WIDTH}x$OUTPUT_HEIGHT")

        logMlRoi("x=$ML_ROI_X")
        logMlRoi("y=$ML_ROI_Y")
        logMlRoi("width=$ML_ROI_WIDTH")
        logMlRoi("height=$ML_ROI_HEIGHT")
        logMlRoi("roiWidth=${mlRoiBitmap.width}")
        logMlRoi("roiHeight=${mlRoiBitmap.height}")

        val defaultCorners = listOf(
            PointF(0f, 0f),
            PointF(OUTPUT_WIDTH.toFloat(), 0f),
            PointF(OUTPUT_WIDTH.toFloat(), OUTPUT_HEIGHT.toFloat()),
            PointF(0f, OUTPUT_HEIGHT.toFloat())
        )

        if (context != null) {
            saveDebugVisualImages(
                context = context,
                originalBitmap = bitmap,
                cardCorners = cardCorners,
                candidates = emptyList(),
                selectedCandidate = null,
                deviceBitmap = deviceBitmap,
                sourceName = sourceImageName
            )
        }

        return TestDeviceDetectionResult(
            deviceBitmap = deviceBitmap,
            mlRoiBitmap = mlRoiBitmap,
            corners = defaultCorners,
            confidence = 0.5,
            candidatesCount = 0
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

    private fun saveDebugVisualImages(
        context: Context,
        originalBitmap: Bitmap,
        cardCorners: List<PointF>?,
        candidates: List<TestDeviceCandidate>,
        selectedCandidate: TestDeviceCandidate?,
        deviceBitmap: Bitmap,
        sourceName: String
    ) {
        try {
            val debugDir = File(context.filesDir, "debug_ml_inputs").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = System.currentTimeMillis()

            // 1. ORIGINAL + reference card (blue) + ALL candidates (yellow)
            val img1 = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas1 = Canvas(img1)
            val paintBlue = Paint().apply { color = Color.BLUE; strokeWidth = 6f; style = Paint.Style.STROKE }
            val paintYellow = Paint().apply { color = Color.YELLOW; strokeWidth = 4f; style = Paint.Style.STROKE }

            if (cardCorners != null && cardCorners.size >= 4) {
                val path = Path().apply {
                    moveTo(cardCorners[0].x, cardCorners[0].y)
                    lineTo(cardCorners[1].x, cardCorners[1].y)
                    lineTo(cardCorners[2].x, cardCorners[2].y)
                    lineTo(cardCorners[3].x, cardCorners[3].y)
                    close()
                }
                canvas1.drawPath(path, paintBlue)
            }

            for (cand in candidates) {
                val path = Path().apply {
                    moveTo(cand.corners[0].x.toFloat(), cand.corners[0].y.toFloat())
                    lineTo(cand.corners[1].x.toFloat(), cand.corners[1].y.toFloat())
                    lineTo(cand.corners[2].x.toFloat(), cand.corners[2].y.toFloat())
                    lineTo(cand.corners[3].x.toFloat(), cand.corners[3].y.toFloat())
                    close()
                }
                canvas1.drawPath(path, paintYellow)
            }
            saveBitmapFile(File(debugDir, "debug_candidates_${timestamp}_$sourceName.png"), img1)

            // 2. ORIGINAL + reference card (blue) + SELECTED test device quad (green)
            val img2 = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas2 = Canvas(img2)
            val paintGreen = Paint().apply { color = Color.GREEN; strokeWidth = 6f; style = Paint.Style.STROKE }

            if (cardCorners != null && cardCorners.size >= 4) {
                val path = Path().apply {
                    moveTo(cardCorners[0].x, cardCorners[0].y)
                    lineTo(cardCorners[1].x, cardCorners[1].y)
                    lineTo(cardCorners[2].x, cardCorners[2].y)
                    lineTo(cardCorners[3].x, cardCorners[3].y)
                    close()
                }
                canvas2.drawPath(path, paintBlue)
            }

            if (selectedCandidate != null) {
                val path = Path().apply {
                    moveTo(selectedCandidate.corners[0].x.toFloat(), selectedCandidate.corners[0].y.toFloat())
                    lineTo(selectedCandidate.corners[1].x.toFloat(), selectedCandidate.corners[1].y.toFloat())
                    lineTo(selectedCandidate.corners[2].x.toFloat(), selectedCandidate.corners[2].y.toFloat())
                    lineTo(selectedCandidate.corners[3].x.toFloat(), selectedCandidate.corners[3].y.toFloat())
                    close()
                }
                canvas2.drawPath(path, paintGreen)
            }
            saveBitmapFile(File(debugDir, "debug_selected_${timestamp}_$sourceName.png"), img2)

            // 3. 640x400 warped device
            saveBitmapFile(File(debugDir, "debug_warped_${timestamp}_$sourceName.png"), deviceBitmap)

            // 4. 640x400 warped device + 386x133 ML ROI rectangle (red box)
            val img4 = deviceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas4 = Canvas(img4)
            val paintRed = Paint().apply { color = Color.RED; strokeWidth = 4f; style = Paint.Style.STROKE }
            canvas4.drawRect(
                ML_ROI_X.toFloat(),
                ML_ROI_Y.toFloat(),
                (ML_ROI_X + ML_ROI_WIDTH).toFloat(),
                (ML_ROI_Y + ML_ROI_HEIGHT).toFloat(),
                paintRed
            )
            saveBitmapFile(File(debugDir, "debug_roi_box_${timestamp}_$sourceName.png"), img4)

        } catch (e: Throwable) {
            logMlPipeline("ML_PIPELINE: Failed to save visual debug images: ${e.message}")
        }
    }

    private fun saveBitmapFile(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Throwable) {}
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

    private fun logDeviceDetection(msg: String) {
        try {
            Log.d("DEVICE_DETECTION", msg)
        } catch (_: Throwable) {
            println("[DEVICE_DETECTION] $msg")
        }
    }

    private fun logMlRoi(msg: String) {
        try {
            Log.d("ML_ROI", msg)
        } catch (_: Throwable) {
            println("[ML_ROI] $msg")
        }
    }

    private fun logMlPipeline(msg: String) {
        try {
            Log.d("ML_PIPELINE", msg)
        } catch (_: Throwable) {
            println("[ML_PIPELINE] $msg")
        }
    }
}

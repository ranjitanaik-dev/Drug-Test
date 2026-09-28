package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.DetectorParameters
import org.opencv.objdetect.Objdetect
import javax.inject.Inject
import javax.inject.Singleton

sealed class CardDetectionResult {
    data class Detected(
        val corners: List<PointF>, // 4 points: Top-Left, Top-Right, Bottom-Right, Bottom-Left
        val confidence: Float
    ) : CardDetectionResult()

    data object NotDetected : CardDetectionResult()
    data object FramingError : CardDetectionResult()
}

/**
 * CardDetector performs ArUco-marker-based reference card detection using OpenCV 4.9.0 [ArucoDetector].
 * Identifies markers 1 (Top-Left), 2 (Top-Right), 3 (Bottom-Left), and 4 (Bottom-Right) from DICT_4X4_50.
 */
@Singleton
class CardDetector @Inject constructor() {

    private val isNativeOpenCvLoaded: Boolean by lazy {
        var loaded = false
        var lastError: Throwable? = null

        // Explicitly load libc++_shared.so dependency if present
        try {
            System.loadLibrary("c++_shared")
            logDebug("CardDetector", "System.loadLibrary('c++_shared') succeeded")
        } catch (e: Throwable) {
            logDebug("CardDetector", "System.loadLibrary('c++_shared') note: ${e.message}")
        }

        // 1. Try OpenCVLoader.initLocal() (Recommended for OpenCV 4.5+ AAR)
        try {
            loaded = OpenCVLoader.initLocal()
            logDebug("CardDetector", "OpenCVLoader.initLocal() returned: $loaded")
        } catch (e: Throwable) {
            lastError = e
            logWarning("CardDetector", "OpenCVLoader.initLocal() threw exception: ${e.message}\n${e.stackTraceToString()}")
        }

        // 2. If initLocal() returns false, try OpenCVLoader.initDebug()
        if (!loaded) {
            try {
                loaded = OpenCVLoader.initDebug()
                logDebug("CardDetector", "OpenCVLoader.initDebug() returned: $loaded")
            } catch (e: Throwable) {
                lastError = e
                logWarning("CardDetector", "OpenCVLoader.initDebug() threw exception: ${e.message}\n${e.stackTraceToString()}")
            }
        }

        // 3. If still false, try direct System.loadLibrary("opencv_java4")
        if (!loaded) {
            try {
                System.loadLibrary("opencv_java4")
                logDebug("CardDetector", "System.loadLibrary('opencv_java4') succeeded directly")
                loaded = true
            } catch (e: Throwable) {
                lastError = e
                logWarning("CardDetector", "System.loadLibrary('opencv_java4') threw exception: ${e.message}\n${e.stackTraceToString()}")
            }
        }

        logDiagnostic("OpenCV native initialization result = $loaded")
        if (!loaded) {
            if (lastError != null) {
                logDiagnostic("OpenCV initialization error details:\n${lastError.stackTraceToString()}")
            } else {
                logDiagnostic("OpenCV initialization returned false without throwing an exception.")
            }
        }

        loaded
    }

    fun detectCard(bitmap: Bitmap): CardDetectionResult {
        logDebug("CardDetector", "detectCard called with image size: ${bitmap.width}x${bitmap.height}")
        logDiagnostic("4. Native OpenCV loaded: $isNativeOpenCvLoaded")

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        if (width < Thresholds.MIN_RESOLUTION_WIDTH || height < Thresholds.MIN_RESOLUTION_HEIGHT) {
            logWarning("CardDetector", "Image resolution below threshold: ${bitmap.width}x${bitmap.height}")
            return CardDetectionResult.NotDetected
        }

        if (isNativeOpenCvLoaded) {
            logDebug("CardDetector", "Using native OpenCV ArUco detector")
            return detectCardAruco(bitmap)
        }

        logWarning("CardDetector", "Native OpenCV not loaded; using fallback detector")
        return detectCardFallback(bitmap)
    }

    /**
     * Executes official OpenCV [ArucoDetector] on the input image using multi-stage parameters
     * suited for high-resolution camera captures (e.g. 3000x4000).
     */
    private fun detectCardAruco(bitmap: Bitmap): CardDetectionResult {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        logDebug("CardDetector", "Converted bitmap to Mat: cols=${mat.cols()}, rows=${mat.rows()}, channels=${mat.channels()}")

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        val minMax = Core.minMaxLoc(gray)
        val meanVal = Core.mean(gray).`val`[0]
        logDiagnostic("Grayscale image stats: width=${bitmap.width}, height=${bitmap.height}, min=${minMax.minVal}, max=${minMax.maxVal}, mean=$meanVal")

        val dictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50)

        // Attempt 1: Standard DetectorParameters
        val params1 = DetectorParameters()
        val detector1 = ArucoDetector(dictionary, params1)
        val corners1 = ArrayList<Mat>()
        val ids1 = Mat()
        val rejected1 = ArrayList<Mat>()
        detector1.detectMarkers(gray, corners1, ids1, rejected1)

        val idsList1 = extractIdsList(ids1)
        logDiagnostic("Attempt 1 (default params): detected ${corners1.size} markers $idsList1, rejected ${rejected1.size} candidates")

        var finalCorners = corners1
        var finalIdsList = idsList1

        // Attempt 2: Tuned parameters for high-resolution images if Attempt 1 is insufficient
        if (!hasRequiredMarkers(idsList1)) {
            val params2 = DetectorParameters().apply {
                set_adaptiveThreshWinSizeMin(3)
                set_adaptiveThreshWinSizeMax(103)
                set_adaptiveThreshWinSizeStep(10)
                set_useAruco3Detection(true)
                set_cornerRefinementMethod(Objdetect.CORNER_REFINE_SUBPIX)
            }
            val detector2 = ArucoDetector(dictionary, params2)
            val corners2 = ArrayList<Mat>()
            val ids2 = Mat()
            val rejected2 = ArrayList<Mat>()
            detector2.detectMarkers(gray, corners2, ids2, rejected2)

            val idsList2 = extractIdsList(ids2)
            logDiagnostic("Attempt 2 (tuned high-res params): detected ${corners2.size} markers $idsList2, rejected ${rejected2.size} candidates")

            if (hasRequiredMarkers(idsList2)) {
                finalCorners = corners2
                finalIdsList = idsList2
            }
        }

        // Attempt 3: Downscaled image (scale factor ~2x-3x to standard ~1200px max dimension) if still missing
        if (!hasRequiredMarkers(finalIdsList)) {
            val maxDim = maxOf(bitmap.width, bitmap.height)
            if (maxDim > 1200) {
                val scale = 1200.0 / maxDim
                val scaledGray = Mat()
                Imgproc.resize(gray, scaledGray, Size(), scale, scale, Imgproc.INTER_AREA)

                val params3 = DetectorParameters().apply {
                    set_cornerRefinementMethod(Objdetect.CORNER_REFINE_SUBPIX)
                }
                val detector3 = ArucoDetector(dictionary, params3)
                val corners3 = ArrayList<Mat>()
                val ids3 = Mat()
                val rejected3 = ArrayList<Mat>()
                detector3.detectMarkers(scaledGray, corners3, ids3, rejected3)

                val idsList3 = extractIdsList(ids3)
                logDiagnostic("Attempt 3 (downscaled scale=$scale): detected ${corners3.size} markers $idsList3, rejected ${rejected3.size} candidates")

                if (hasRequiredMarkers(idsList3)) {
                    val rescaledCorners = ArrayList<Mat>()
                    for (cMat in corners3) {
                        val origMat = Mat(cMat.rows(), cMat.cols(), cMat.type())
                        val pts = FloatArray(8)
                        cMat.get(0, 0, pts)
                        for (i in pts.indices) {
                            pts[i] = (pts[i] / scale).toFloat()
                        }
                        origMat.put(0, 0, pts)
                        rescaledCorners.add(origMat)
                    }
                    finalCorners = rescaledCorners
                    finalIdsList = idsList3
                }
            }
        }

        logDebug("CardDetector", "ArucoDetector final detected ${finalCorners.size} marker corners")

        if (finalCorners.size < 4 || !hasRequiredMarkers(finalIdsList)) {
            logWarning("CardDetector", "NotDetected: Insufficient markers detected (found=$finalIdsList)")
            logDiagnostic("5. ArUco marker IDs detected: Insufficient markers (found=$finalIdsList)")
            return CardDetectionResult.NotDetected
        }

        logDiagnostic("5. ArUco marker IDs detected: $finalIdsList")

        val idToCornersMap = mutableMapOf<Int, Mat>()
        for (i in finalIdsList.indices) {
            idToCornersMap[finalIdsList[i]] = finalCorners[i]
        }

        // Verify required marker IDs 1, 2, 3, 4 exist
        val mat1 = idToCornersMap[1]
        val mat2 = idToCornersMap[2]
        val mat3 = idToCornersMap[3]
        val mat4 = idToCornersMap[4]

        if (mat1 == null || mat2 == null || mat3 == null || mat4 == null) {
            val missingIds = mutableListOf<Int>()
            if (mat1 == null) missingIds.add(1)
            if (mat2 == null) missingIds.add(2)
            if (mat3 == null) missingIds.add(3)
            if (mat4 == null) missingIds.add(4)
            logWarning("CardDetector", "NotDetected: Missing required marker IDs: $missingIds. Found IDs: $finalIdsList")
            return CardDetectionResult.NotDetected
        }

        // Extract outer reference corners:
        // Top-Left corner of Marker 1
        val pt1 = getMatPoint(mat1, 0)
        // Top-Right corner of Marker 2
        val pt2 = getMatPoint(mat2, 1)
        // Bottom-Right corner of Marker 4
        val pt4 = getMatPoint(mat4, 2)
        // Bottom-Left corner of Marker 3
        val pt3 = getMatPoint(mat3, 3)

        val orderedCorners = listOf(
            PointF(pt1.x.toFloat(), pt1.y.toFloat()), // Top-Left
            PointF(pt2.x.toFloat(), pt2.y.toFloat()), // Top-Right
            PointF(pt4.x.toFloat(), pt4.y.toFloat()), // Bottom-Right
            PointF(pt3.x.toFloat(), pt3.y.toFloat())  // Bottom-Left
        )

        val cornersFormatted = orderedCorners.joinToString { "(${it.x}, ${it.y})" }
        logDebug("CardDetector", "Extracted Card Outer Corners [TL, TR, BR, BL]: [$cornersFormatted]")
        logDiagnostic("6. Ordered card corner coordinates [TL, TR, BR, BL]: [$cornersFormatted]")

        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()
        val cornerLabels = listOf("Top-Left", "Top-Right", "Bottom-Right", "Bottom-Left")
        orderedCorners.forEachIndexed { idx, pt ->
            val distLeft = pt.x
            val distTop = pt.y
            val distRight = imgW - pt.x
            val distBottom = imgH - pt.y
            logDiagnostic("7. Corner ${idx + 1} (${cornerLabels[idx]} at ${pt.x}, ${pt.y}) distances -> Left: $distLeft, Top: $distTop, Right: $distRight, Bottom: $distBottom")
        }

        // Check if card or markers intersect image frame boundaries (FramingError)
        val margin = 5f
        logDiagnostic("8. Framing margin threshold: $margin px (image bounds: ${bitmap.width}x${bitmap.height})")

        val clippedCorner = orderedCorners.firstOrNull {
            it.x <= margin || it.y <= margin || it.x >= bitmap.width - margin || it.y >= bitmap.height - margin
        }

        val isClipped = clippedCorner != null
        logDebug("CardDetector", "isClipped: $isClipped, clippedCorner=$clippedCorner (margin=$margin, bitmapSize=${bitmap.width}x${bitmap.height})")
        logDiagnostic("9. Final isClipped result: $isClipped (clippedCorner=$clippedCorner)")

        return if (isClipped) {
            val c = clippedCorner!!
            logWarning("CardDetector", "FramingError: Corner (${c.x}, ${c.y}) clipped by border margin=$margin (bitmap size=${bitmap.width}x${bitmap.height})")
            CardDetectionResult.FramingError
        } else {
            logDebug("CardDetector", "Card successfully detected with all 4 ArUco markers 1, 2, 3, 4!")
            CardDetectionResult.Detected(
                corners = orderedCorners,
                confidence = 0.95f
            )
        }
    }

    private fun extractIdsList(markerIds: Mat): List<Int> {
        if (markerIds.empty()) return emptyList()
        val idsArray = IntArray(markerIds.rows() * markerIds.cols())
        markerIds.get(0, 0, idsArray)
        return idsArray.toList()
    }

    private fun hasRequiredMarkers(idsList: List<Int>): Boolean {
        return idsList.contains(1) && idsList.contains(2) && idsList.contains(3) && idsList.contains(4)
    }

    /**
     * Fallback detector for pure JVM unit test environments where native .so shared library is not loaded.
     */
    private fun detectCardFallback(bitmap: Bitmap): CardDetectionResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var nonDarkCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val p = pixels[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val brightness = (r + g + b) / 3

                if (brightness > 40) {
                    nonDarkCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val cardArea = (maxX - minX).coerceAtLeast(0) * (maxY - minY).coerceAtLeast(0)
        val areaRatio = cardArea.toFloat() / (width * height)

        if (areaRatio < Thresholds.MIN_CARD_AREA_RATIO || nonDarkCount < 1000) {
            logWarning("CardDetector", "Fallback NotDetected: areaRatio=$areaRatio, nonDarkCount=$nonDarkCount")
            return CardDetectionResult.NotDetected
        }

        if (minX <= 5 || minY <= 5 || maxX >= width - 5 || maxY >= height - 5) {
            logWarning("CardDetector", "Fallback FramingError: minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY for bitmap size ${width}x${height}")
            return CardDetectionResult.FramingError
        }

        val corners = listOf(
            PointF(minX.toFloat(), minY.toFloat()),
            PointF(maxX.toFloat(), minY.toFloat()),
            PointF(maxX.toFloat(), maxY.toFloat()),
            PointF(minX.toFloat(), maxY.toFloat())
        )

        val cornersFormatted = corners.joinToString { "(${it.x}, ${it.y})" }
        logDebug("CardDetector", "Fallback Card Detected: [$cornersFormatted]")
        return CardDetectionResult.Detected(corners = corners, confidence = areaRatio.coerceIn(0.6f, 0.95f))
    }

    private fun getMatPoint(mat: Mat, cornerIndex: Int): Point {
        val pts = FloatArray(8)
        mat.get(0, 0, pts)
        val idx = cornerIndex * 2
        return Point(pts[idx].toDouble(), pts[idx + 1].toDouble())
    }

    private fun logDiagnostic(msg: String) {
        try {
            Log.d("CaptureDiagnostic", msg)
        } catch (_: Throwable) {
            println("[CaptureDiagnostic] $msg")
        }
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun logWarning(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] WARNING: $msg")
        }
    }
}

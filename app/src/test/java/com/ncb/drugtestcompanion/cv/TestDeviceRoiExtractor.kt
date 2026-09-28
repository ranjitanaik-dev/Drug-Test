package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class TestDeviceDetectionResult(
    val deviceBitmap: Bitmap,
    val corners: List<PointF>,
    val confidence: Double
)

class TestDeviceDetector {

    companion object {

        private const val TAG = "TestDeviceDetector"

        private const val OUTPUT_WIDTH = 640
        private const val OUTPUT_HEIGHT = 400

        private const val MIN_AREA_RATIO = 0.008
        private const val MAX_AREA_RATIO = 0.85

        private const val MIN_SIDE = 80.0

        private const val MIN_CONFIDENCE = 0.35
    }

    fun detectAndWarp(
        bitmap: Bitmap
    ): TestDeviceDetectionResult? {

        log(
            "START image=${bitmap.width}x${bitmap.height}"
        )

        if (
            bitmap.width < 200 ||
            bitmap.height < 200
        ) {
            log("FAILED: image too small")
            return null
        }

        val source = Mat()
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        val morphed = Mat()
        val hierarchy = Mat()

        try {

            Utils.bitmapToMat(
                bitmap,
                source
            )

            Imgproc.cvtColor(
                source,
                gray,
                Imgproc.COLOR_RGBA2GRAY
            )

            Imgproc.GaussianBlur(
                gray,
                blurred,
                Size(5.0, 5.0),
                0.0
            )

            Imgproc.Canny(
                blurred,
                edges,
                25.0,
                100.0
            )

            val kernel =
                Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    Size(9.0, 9.0)
                )

            Imgproc.morphologyEx(
                edges,
                morphed,
                Imgproc.MORPH_CLOSE,
                kernel
            )

            kernel.release()

            val contours =
                ArrayList<MatOfPoint>()

            Imgproc.findContours(
                morphed,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            log(
                "Edge contours = ${contours.size}"
            )

            var bestCorners: Array<Point>? = null
            var bestScore = 0.0

            for (contour in contours) {

                try {

                    val area =
                        Imgproc.contourArea(contour)

                    val imageArea =
                        bitmap.width.toDouble() *
                                bitmap.height.toDouble()

                    val areaRatio =
                        area / imageArea

                    if (
                        areaRatio < MIN_AREA_RATIO ||
                        areaRatio > MAX_AREA_RATIO
                    ) {
                        continue
                    }

                    val contour2f =
                        MatOfPoint2f(
                            *contour.toArray()
                        )

                    try {

                        val perimeter =
                            Imgproc.arcLength(
                                contour2f,
                                true
                            )

                        if (perimeter <= 0.0) {
                            continue
                        }

                        val epsilons =
                            doubleArrayOf(
                                0.01,
                                0.015,
                                0.02,
                                0.025,
                                0.03,
                                0.04
                            )

                        for (epsilon in epsilons) {

                            val approx =
                                MatOfPoint2f()

                            try {

                                Imgproc.approxPolyDP(
                                    contour2f,
                                    approx,
                                    epsilon * perimeter,
                                    true
                                )

                                val points =
                                    approx.toArray()

                                if (
                                    points.size != 4
                                ) {
                                    continue
                                }

                                val ordered =
                                    orderPoints(points)

                                val widthTop =
                                    distance(
                                        ordered[0],
                                        ordered[1]
                                    )

                                val widthBottom =
                                    distance(
                                        ordered[3],
                                        ordered[2]
                                    )

                                val heightLeft =
                                    distance(
                                        ordered[0],
                                        ordered[3]
                                    )

                                val heightRight =
                                    distance(
                                        ordered[1],
                                        ordered[2]
                                    )

                                val width =
                                    (widthTop + widthBottom) / 2.0

                                val height =
                                    (heightLeft + heightRight) / 2.0

                                if (
                                    width < MIN_SIDE ||
                                    height < MIN_SIDE
                                ) {
                                    continue
                                }

                                val aspect =
                                    width / height

                                val normalizedAspect =
                                    max(
                                        aspect,
                                        1.0 / aspect
                                    )

                                /*
                                 * The test device used by the
                                 * training pipeline is a landscape-
                                 * oriented rectangular object.
                                 *
                                 * Avoid candidates that are almost
                                 * square or extremely elongated.
                                 */
                                if (
                                    normalizedAspect < 1.15 ||
                                    normalizedAspect > 5.0
                                ) {
                                    continue
                                }

                                val sideConsistency =
                                    calculateSideConsistency(
                                        widthTop,
                                        widthBottom,
                                        heightLeft,
                                        heightRight
                                    )

                                val rectangularity =
                                    calculateRectangularity(
                                        ordered,
                                        area
                                    )

                                val sizeScore =
                                    calculateSizeScore(
                                        areaRatio
                                    )

                                val aspectScore =
                                    calculateAspectScore(
                                        normalizedAspect
                                    )

                                val score =
                                    sideConsistency * 0.30 +
                                            rectangularity * 0.30 +
                                            aspectScore * 0.25 +
                                            sizeScore * 0.15

                                if (
                                    score > bestScore
                                ) {

                                    bestScore =
                                        score

                                    bestCorners =
                                        ordered
                                }

                            } finally {

                                approx.release()
                            }
                        }

                    } finally {

                        contour2f.release()
                    }

                } catch (e: Throwable) {

                    log(
                        "Contour error: " +
                                "${e.javaClass.simpleName}: " +
                                "${e.message}"
                    )

                } finally {

                    contour.release()
                }
            }

            log(
                "BEST SCORE = $bestScore"
            )

            if (
                bestCorners == null ||
                bestScore < MIN_CONFIDENCE
            ) {

                log(
                    "Edge method did not find suitable device"
                )

                /*
                 * Second detection method:
                 * threshold-based rectangular objects.
                 */
                val thresholdResult =
                    detectUsingThresholds(
                        bitmap
                    )

                if (
                    thresholdResult != null
                ) {

                    log(
                        "THRESHOLD METHOD PASSED"
                    )

                    return thresholdResult
                }

                log(
                    "TEST DEVICE DETECTION FAILED"
                )

                return null
            }

            log(
                "EDGE METHOD PASSED"
            )

            log(
                "Corners = ${bestCorners.toList()}"
            )

            val warped =
                warpPerspective(
                    bitmap,
                    bestCorners
                )

            if (warped == null) {

                log(
                    "FAILED: warp returned null"
                )

                return null
            }

            log(
                "SUCCESS: warped device = " +
                        "${warped.width}x${warped.height}"
            )

            return TestDeviceDetectionResult(
                deviceBitmap = warped,
                corners = bestCorners.map {
                    PointF(
                        it.x.toFloat(),
                        it.y.toFloat()
                    )
                },
                confidence =
                    bestScore.coerceIn(
                        0.0,
                        1.0
                    )
            )

        } catch (e: Throwable) {

            log(
                "EXCEPTION: " +
                        "${e.javaClass.simpleName}: " +
                        "${e.message}"
            )

            return null

        } finally {

            source.release()
            gray.release()
            blurred.release()
            edges.release()
            morphed.release()
            hierarchy.release()
        }
    }

    private fun detectUsingThresholds(
        bitmap: Bitmap
    ): TestDeviceDetectionResult? {

        val source =
            Mat()

        val gray =
            Mat()

        try {

            Utils.bitmapToMat(
                bitmap,
                source
            )

            Imgproc.cvtColor(
                source,
                gray,
                Imgproc.COLOR_RGBA2GRAY
            )

            val thresholds =
                doubleArrayOf(
                    60.0,
                    90.0,
                    120.0,
                    150.0,
                    180.0,
                    210.0
                )

            var bestCorners:
                    Array<Point>? = null

            var bestScore =
                0.0

            for (threshold in thresholds) {

                val binary =
                    Mat()

                val hierarchy =
                    Mat()

                try {

                    Imgproc.threshold(
                        gray,
                        binary,
                        threshold,
                        255.0,
                        Imgproc.THRESH_BINARY
                    )

                    val kernel =
                        Imgproc.getStructuringElement(
                            Imgproc.MORPH_RECT,
                            Size(7.0, 7.0)
                        )

                    Imgproc.morphologyEx(
                        binary,
                        binary,
                        Imgproc.MORPH_CLOSE,
                        kernel
                    )

                    kernel.release()

                    val contours =
                        ArrayList<MatOfPoint>()

                    Imgproc.findContours(
                        binary,
                        contours,
                        hierarchy,
                        Imgproc.RETR_EXTERNAL,
                        Imgproc.CHAIN_APPROX_SIMPLE
                    )

                    for (contour in contours) {

                        try {

                            val area =
                                Imgproc.contourArea(
                                    contour
                                )

                            val imageArea =
                                bitmap.width.toDouble() *
                                        bitmap.height.toDouble()

                            val areaRatio =
                                area / imageArea

                            if (
                                areaRatio <
                                MIN_AREA_RATIO ||
                                areaRatio >
                                MAX_AREA_RATIO
                            ) {
                                continue
                            }

                            val contour2f =
                                MatOfPoint2f(
                                    *contour.toArray()
                                )

                            try {

                                val perimeter =
                                    Imgproc.arcLength(
                                        contour2f,
                                        true
                                    )

                                if (
                                    perimeter <= 0.0
                                ) {
                                    continue
                                }

                                val approx =
                                    MatOfPoint2f()

                                try {

                                    Imgproc.approxPolyDP(
                                        contour2f,
                                        approx,
                                        0.025 * perimeter,
                                        true
                                    )

                                    val points =
                                        approx.toArray()

                                    if (
                                        points.size != 4
                                    ) {
                                        continue
                                    }

                                    val ordered =
                                        orderPoints(points)

                                    val width =
                                        (
                                                distance(
                                                    ordered[0],
                                                    ordered[1]
                                                ) +
                                                        distance(
                                                            ordered[3],
                                                            ordered[2]
                                                        )
                                                ) / 2.0

                                    val height =
                                        (
                                                distance(
                                                    ordered[0],
                                                    ordered[3]
                                                ) +
                                                        distance(
                                                            ordered[1],
                                                            ordered[2]
                                                        )
                                                ) / 2.0

                                    if (
                                        width < MIN_SIDE ||
                                        height < MIN_SIDE
                                    ) {
                                        continue
                                    }

                                    val aspect =
                                        width / height

                                    val normalizedAspect =
                                        max(
                                            aspect,
                                            1.0 / aspect
                                        )

                                    if (
                                        normalizedAspect < 1.15 ||
                                        normalizedAspect > 5.0
                                    ) {
                                        continue
                                    }

                                    val rectangularity =
                                        calculateRectangularity(
                                            ordered,
                                            area
                                        )

                                    val score =
                                        rectangularity * 0.7 +
                                                calculateAspectScore(
                                                    normalizedAspect
                                                ) * 0.3

                                    if (
                                        score > bestScore
                                    ) {

                                        bestScore =
                                            score

                                        bestCorners =
                                            ordered
                                    }

                                } finally {

                                    approx.release()
                                }

                            } finally {

                                contour2f.release()
                            }

                        } finally {

                            contour.release()
                        }
                    }

                } finally {

                    binary.release()
                    hierarchy.release()
                }
            }

            if (
                bestCorners == null
            ) {
                return null
            }

            val warped =
                warpPerspective(
                    bitmap,
                    bestCorners
                ) ?: return null

            return TestDeviceDetectionResult(
                deviceBitmap = warped,
                corners = bestCorners.map {
                    PointF(
                        it.x.toFloat(),
                        it.y.toFloat()
                    )
                },
                confidence =
                    bestScore.coerceIn(
                        0.0,
                        1.0
                    )
            )

        } catch (e: Throwable) {

            log(
                "Threshold exception: " +
                        "${e.javaClass.simpleName}: " +
                        "${e.message}"
            )

            return null

        } finally {

            source.release()
            gray.release()
        }
    }

    private fun orderPoints(
        points: Array<Point>
    ): Array<Point> {

        val sortedBySum =
            points.sortedBy {
                it.x + it.y
            }

        val sortedByDifference =
            points.sortedBy {
                it.x - it.y
            }

        val topLeft =
            sortedBySum.first()

        val bottomRight =
            sortedBySum.last()

        val topRight =
            sortedByDifference.last()

        val bottomLeft =
            sortedByDifference.first()

        return arrayOf(
            topLeft,
            topRight,
            bottomRight,
            bottomLeft
        )
    }

    private fun distance(
        a: Point,
        b: Point
    ): Double {

        val dx =
            a.x - b.x

        val dy =
            a.y - b.y

        return sqrt(
            dx * dx + dy * dy
        )
    }

    private fun calculateSideConsistency(
        widthTop: Double,
        widthBottom: Double,
        heightLeft: Double,
        heightRight: Double
    ): Double {

        val widthAverage =
            (widthTop + widthBottom) / 2.0

        val heightAverage =
            (heightLeft + heightRight) / 2.0

        if (
            widthAverage <= 0.0 ||
            heightAverage <= 0.0
        ) {
            return 0.0
        }

        val widthDifference =
            abs(
                widthTop - widthBottom
            ) / widthAverage

        val heightDifference =
            abs(
                heightLeft - heightRight
            ) / heightAverage

        return (
                1.0 -
                        (
                                widthDifference +
                                        heightDifference
                                ) / 2.0
                ).coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateRectangularity(
        corners: Array<Point>,
        contourArea: Double
    ): Double {

        val width =
            (
                    distance(
                        corners[0],
                        corners[1]
                    ) +
                            distance(
                                corners[3],
                                corners[2]
                            )
                    ) / 2.0

        val height =
            (
                    distance(
                        corners[0],
                        corners[3]
                    ) +
                            distance(
                                corners[1],
                                corners[2]
                            )
                    ) / 2.0

        val rectangleArea =
            width * height

        if (
            rectangleArea <= 0.0
        ) {
            return 0.0
        }

        return (
                contourArea /
                        rectangleArea
                ).coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateAspectScore(
        normalizedAspect: Double
    ): Double {

        return when {

            normalizedAspect in 1.2..2.5 ->
                1.0

            normalizedAspect in 2.5..3.5 ->
                0.90

            normalizedAspect in 3.5..4.5 ->
                0.75

            normalizedAspect in 4.5..5.0 ->
                0.55

            else ->
                0.30
        }
    }

    private fun calculateSizeScore(
        areaRatio: Double
    ): Double {

        return when {

            areaRatio in 0.02..0.60 ->
                1.0

            areaRatio in 0.008..0.02 ->
                0.70

            areaRatio in 0.60..0.75 ->
                0.75

            else ->
                0.45
        }
    }

    private fun warpPerspective(
        bitmap: Bitmap,
        corners: Array<Point>
    ): Bitmap? {

        val source =
            Mat()

        val warped =
            Mat()

        val transform =
            Mat()

        try {

            Utils.bitmapToMat(
                bitmap,
                source
            )

            val sourcePoints =
                MatOfPoint2f(
                    corners[0],
                    corners[1],
                    corners[2],
                    corners[3]
                )

            val destinationPoints =
                MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(
                        (OUTPUT_WIDTH - 1).toDouble(),
                        0.0
                    ),
                    Point(
                        (OUTPUT_WIDTH - 1).toDouble(),
                        (OUTPUT_HEIGHT - 1).toDouble()
                    ),
                    Point(
                        0.0,
                        (OUTPUT_HEIGHT - 1).toDouble()
                    )
                )

            try {

                val matrix =
                    Imgproc.getPerspectiveTransform(
                        sourcePoints,
                        destinationPoints
                    )

                matrix.copyTo(transform)
                matrix.release()

                Imgproc.warpPerspective(
                    source,
                    warped,
                    transform,
                    Size(
                        OUTPUT_WIDTH.toDouble(),
                        OUTPUT_HEIGHT.toDouble()
                    )
                )

                val result =
                    Bitmap.createBitmap(
                        OUTPUT_WIDTH,
                        OUTPUT_HEIGHT,
                        Bitmap.Config.ARGB_8888
                    )

                Utils.matToBitmap(
                    warped,
                    result
                )

                return result

            } finally {

                sourcePoints.release()
                destinationPoints.release()
            }

        } catch (e: Throwable) {

            log(
                "Warp error: " +
                        "${e.javaClass.simpleName}: " +
                        "${e.message}"
            )

            return null

        } finally {

            source.release()
            warped.release()
            transform.release()
        }
    }

    private fun log(
        message: String
    ) {

        try {
            Log.d(
                TAG,
                message
            )
        } catch (_: Throwable) {
            println(
                "[$TAG] $message"
            )
        }
    }
}
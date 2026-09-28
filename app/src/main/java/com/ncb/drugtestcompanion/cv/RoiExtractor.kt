package com.ncb.drugtestcompanion.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.ncb.drugtestcompanion.domain.model.RoiSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class RoiFeatureVector(
    val averageLab: LabColor,
    val averageHsv: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RoiFeatureVector

        if (averageLab != other.averageLab) return false
        if (!averageHsv.contentEquals(other.averageHsv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = averageLab.hashCode()
        result = 31 * result + averageHsv.contentHashCode()
        return result
    }
}

sealed class RoiExtractionResult {

    data class Success(
        val roiBitmap: Bitmap,
        val featureVector: RoiFeatureVector,
        val canonicalRoiSpec: RoiSpec,
        val debugImagePath: String
    ) : RoiExtractionResult()

    data class Failure(
        val reason: String
    ) : RoiExtractionResult()
}

/**
 * RoiExtractor extracts the test-strip region of interest (ROI)
 * from perspective-corrected canonical 640x960 reference-card geometry.
 *
 * It also computes averaged CIE L*a*b* and HSV feature vectors
 * and saves a debug ROI image to app-private storage.
 */
@Singleton
class RoiExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val colorCalibrator: ColorCalibrator
) {

    private val isNativeOpenCvLoaded: Boolean by lazy {
        try {
            OpenCVLoader.initLocal()
        } catch (_: Throwable) {
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
    }

    fun extractRoi(
        bitmap: Bitmap,
        cardCorners: List<PointF>,
        roiSpec: RoiSpec
    ): RoiExtractionResult {

        // ------------------------------------------------------------
        // 1. Validate Card Geometry
        // ------------------------------------------------------------

        if (cardCorners.size < 4) {
            logWarning(
                "Missing or incomplete card corners " +
                        "(expected 4, found ${cardCorners.size})"
            )

            return RoiExtractionResult.Failure(
                "Invalid card corners: expected 4 points"
            )
        }

        // ------------------------------------------------------------
        // 2. Validate ROI Specification
        //    against canonical 640x960 bounds
        // ------------------------------------------------------------

        if (
            roiSpec.xMin < 0 ||
            roiSpec.yMin < 0 ||
            roiSpec.width <= 0 ||
            roiSpec.height <= 0 ||
            roiSpec.xMin + roiSpec.width > 640 ||
            roiSpec.yMin + roiSpec.height > 960
        ) {

            logWarning(
                "ROI spec exceeds canonical 640x960 bounds: $roiSpec"
            )

            return RoiExtractionResult.Failure(
                "ROI specification exceeds canonical 640x960 bounds"
            )
        }

        // ------------------------------------------------------------
        // 3. Perspective Warp to Canonical 640x960 Frame
        // ------------------------------------------------------------

        val warpedBitmap = if (isNativeOpenCvLoaded) {

            try {
                warpCardToCanonical(
                    bitmap,
                    cardCorners
                )
            } catch (e: Throwable) {

                logWarning(
                    "Native perspective warp failed: ${e.message}"
                )

                bitmap
            }

        } else {

            // Fallback for pure JVM unit test environment
            // where native OpenCV is unavailable.

            try {

                if (
                    bitmap.width == 640 &&
                    bitmap.height == 960
                ) {
                    bitmap
                } else {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        640,
                        960,
                        true
                    )
                }

            } catch (_: Throwable) {

                bitmap
            }
        }

        // ------------------------------------------------------------
        // 4. Crop ROI Sub-Bitmap
        // ------------------------------------------------------------

        val roiBitmap = try {

            if (
                roiSpec.xMin == 0 &&
                roiSpec.yMin == 0 &&
                roiSpec.width == warpedBitmap.width &&
                roiSpec.height == warpedBitmap.height
            ) {

                warpedBitmap

            } else {

                Bitmap.createBitmap(
                    warpedBitmap,
                    roiSpec.xMin,
                    roiSpec.yMin,
                    roiSpec.width,
                    roiSpec.height
                )
            }

        } catch (_: Throwable) {

            warpedBitmap
        }

        // ------------------------------------------------------------
        // NEW DIAGNOSTIC LOG
        // ------------------------------------------------------------
        //
        // This tells us exactly what Android is sending toward
        // the TFLite classifier.
        //
        // Expected currently:
        // x=140
        // y=250
        // width=360
        // height=160
        //
        // The classifier later resizes this to:
        // 386 x 133
        // ------------------------------------------------------------

        logDiagnostic(
            "ANDROID ROI: " +
                    "x=${roiSpec.xMin}, " +
                    "y=${roiSpec.yMin}, " +
                    "width=${roiBitmap.width}, " +
                    "height=${roiBitmap.height}"
        )

        // ------------------------------------------------------------
        // 5. Compute Averaged LAB and HSV Feature Vectors
        // ------------------------------------------------------------

        val featureVector =
            computeFeatureVector(roiBitmap)

        // ------------------------------------------------------------
        // 6. Save Debug ROI Image
        // ------------------------------------------------------------

        val debugPath =
            saveDebugRoiImage(roiBitmap)

        logDiagnostic(
            "Phase 4 ROI debug image saved: $debugPath"
        )

        // ------------------------------------------------------------
        // 7. Return Successful ROI Extraction
        // ------------------------------------------------------------

        return RoiExtractionResult.Success(
            roiBitmap = roiBitmap,
            featureVector = featureVector,
            canonicalRoiSpec = roiSpec,
            debugImagePath = debugPath
        )
    }

    // ================================================================
    // Perspective Correction
    // ================================================================

    private fun warpCardToCanonical(
        bitmap: Bitmap,
        corners: List<PointF>
    ): Bitmap {

        val srcMat = Mat()

        Utils.bitmapToMat(
            bitmap,
            srcMat
        )

        val srcPoints = MatOfPoint2f(

            Point(
                corners[0].x.toDouble(),
                corners[0].y.toDouble()
            ),

            Point(
                corners[1].x.toDouble(),
                corners[1].y.toDouble()
            ),

            Point(
                corners[2].x.toDouble(),
                corners[2].y.toDouble()
            ),

            Point(
                corners[3].x.toDouble(),
                corners[3].y.toDouble()
            )
        )

        val dstPoints = MatOfPoint2f(

            Point(0.0, 0.0),

            Point(
                640.0,
                0.0
            ),

            Point(
                640.0,
                960.0
            ),

            Point(
                0.0,
                960.0
            )
        )

        val transformMatrix =
            Imgproc.getPerspectiveTransform(
                srcPoints,
                dstPoints
            )

        val warpedMat =
            Mat(
                960,
                640,
                CvType.CV_8UC4
            )

        Imgproc.warpPerspective(
            srcMat,
            warpedMat,
            transformMatrix,
            Size(
                640.0,
                960.0
            )
        )

        val warpedBitmap =
            Bitmap.createBitmap(
                640,
                960,
                Bitmap.Config.ARGB_8888
            )

        Utils.matToBitmap(
            warpedMat,
            warpedBitmap
        )

        return warpedBitmap
    }

    // ================================================================
    // Feature Vector
    // ================================================================

    private fun computeFeatureVector(
        roiBitmap: Bitmap
    ): RoiFeatureVector {

        val width =
            roiBitmap.width

        val height =
            roiBitmap.height

        val pixels =
            IntArray(
                width * height
            )

        roiBitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L

        for (p in pixels) {

            rSum +=
                (p shr 16) and 0xFF

            gSum +=
                (p shr 8) and 0xFF

            bSum +=
                p and 0xFF
        }

        val count =
            pixels.size.coerceAtLeast(1)

        val avgR =
            (rSum / count).toInt()

        val avgG =
            (gSum / count).toInt()

        val avgB =
            (bSum / count).toInt()

        val avgLab =
            colorCalibrator.rgbToLab(
                avgR,
                avgG,
                avgB
            )

        val avgHsv =
            computeAverageHsv(
                roiBitmap,
                avgR,
                avgG,
                avgB
            )

        return RoiFeatureVector(
            averageLab = avgLab,
            averageHsv = avgHsv
        )
    }

    // ================================================================
    // HSV
    // ================================================================

    private fun computeAverageHsv(
        roiBitmap: Bitmap,
        avgR: Int,
        avgG: Int,
        avgB: Int
    ): FloatArray {

        if (isNativeOpenCvLoaded) {

            try {

                val roiMat =
                    Mat()

                Utils.bitmapToMat(
                    roiBitmap,
                    roiMat
                )

                val rgbMat =
                    Mat()

                Imgproc.cvtColor(
                    roiMat,
                    rgbMat,
                    Imgproc.COLOR_RGBA2RGB
                )

                val hsvMat =
                    Mat()

                Imgproc.cvtColor(
                    rgbMat,
                    hsvMat,
                    Imgproc.COLOR_RGB2HSV
                )

                val meanHsv =
                    Core.mean(
                        hsvMat
                    )

                val h =
                    (
                            meanHsv.`val`[0] * 2.0
                            ).toFloat()

                val s =
                    (
                            meanHsv.`val`[1] / 255.0
                            ).toFloat()

                val v =
                    (
                            meanHsv.`val`[2] / 255.0
                            ).toFloat()

                return floatArrayOf(
                    h,
                    s,
                    v
                )

            } catch (e: Throwable) {

                logWarning(
                    "Native HSV calculation failed: ${e.message}"
                )
            }
        }

        // Pure Kotlin fallback

        return rgbToHsvKotlin(
            avgR,
            avgG,
            avgB
        )
    }

    // ================================================================
    // RGB → HSV
    // ================================================================

    fun rgbToHsvKotlin(
        r: Int,
        g: Int,
        b: Int
    ): FloatArray {

        val rf =
            r / 255.0f

        val gf =
            g / 255.0f

        val bf =
            b / 255.0f

        val cmax =
            maxOf(
                rf,
                gf,
                bf
            )

        val cmin =
            minOf(
                rf,
                gf,
                bf
            )

        val delta =
            cmax - cmin

        var h =
            0.0f

        if (delta > 0.00001f) {

            when (cmax) {

                rf ->
                    h =
                        60.0f *
                                (((gf - bf) / delta) % 6.0f)

                gf ->
                    h =
                        60.0f *
                                (((bf - rf) / delta) + 2.0f)

                bf ->
                    h =
                        60.0f *
                                (((rf - gf) / delta) + 4.0f)
            }

            if (h < 0.0f) {
                h += 360.0f
            }
        }

        val s =
            if (cmax > 0.00001f) {
                delta / cmax
            } else {
                0.0f
            }

        val v =
            cmax

        return floatArrayOf(
            h,
            s,
            v
        )
    }

    // ================================================================
    // Save Debug ROI
    // ================================================================

    private fun saveDebugRoiImage(
        roiBitmap: Bitmap
    ): String {

        return try {

            val debugDir =
                File(
                    context.filesDir,
                    "debug_rois"
                ).apply {

                    if (!exists()) {
                        mkdirs()
                    }
                }

            val file =
                File(
                    debugDir,
                    "roi_${System.currentTimeMillis()}.png"
                )

            FileOutputStream(
                file
            ).use { out ->

                roiBitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
            }

            file.absolutePath

        } catch (e: Throwable) {

            logWarning(
                "Failed to save debug ROI image: ${e.message}"
            )

            "SAVE_FAILED"
        }
    }

    // ================================================================
    // Logging
    // ================================================================

    private fun logDiagnostic(
        msg: String
    ) {

        try {

            Log.d(
                "CaptureDiagnostic",
                msg
            )

        } catch (_: Throwable) {

            println(
                "[CaptureDiagnostic] $msg"
            )
        }
    }

    private fun logWarning(
        msg: String
    ) {

        try {

            Log.w(
                "RoiExtractor",
                msg
            )

        } catch (_: Throwable) {

            println(
                "[RoiExtractor] WARNING: $msg"
            )
        }
    }
}
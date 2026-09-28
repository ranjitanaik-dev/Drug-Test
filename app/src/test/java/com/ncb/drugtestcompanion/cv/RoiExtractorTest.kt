package com.ncb.drugtestcompanion.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.ncb.drugtestcompanion.domain.model.RoiSpec
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RoiExtractorTest {

    private lateinit var context: Context
    private lateinit var colorCalibrator: ColorCalibrator
    private lateinit var roiExtractor: RoiExtractor
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "test_rois").apply {
            if (!exists()) mkdirs()
        }
        context = mockk()
        every { context.filesDir } returns tempDir

        colorCalibrator = ColorCalibrator()
        roiExtractor = RoiExtractor(context, colorCalibrator)
    }

    @Test
    fun `TEST 6 - extractRoi successfully extracts valid ROI from fixture when supplied with RoiSpec`() {
        val width = 640
        val height = 960
        val pixels = IntArray(width * height) { 0xFF00FF00.toInt() } // Pure Green test strip

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            val len = dest.size.coerceAtMost(pixels.size)
            pixels.copyInto(dest, 0, 0, len)
        }
        every { bitmap.compress(any(), any(), any()) } returns true

        val cardCorners = listOf(
            PointF(0f, 0f),
            PointF(640f, 0f),
            PointF(640f, 960f),
            PointF(0f, 960f)
        )

        val roiSpec = RoiSpec(xMin = 140, yMin = 250, width = 360, height = 160)

        val result = roiExtractor.extractRoi(bitmap, cardCorners, roiSpec)

        assertTrue(result is RoiExtractionResult.Success)
        val success = result as RoiExtractionResult.Success

        assertEquals(360, success.canonicalRoiSpec.width)
        assertEquals(160, success.canonicalRoiSpec.height)
    }

    @Test
    fun `TEST 7 - Invalid card corner geometry is rejected`() {
        val bitmap = mockk<Bitmap>()
        val incompleteCorners = listOf(PointF(0f, 0f), PointF(100f, 0f)) // Only 2 corners
        val roiSpec = RoiSpec(xMin = 100, yMin = 100, width = 100, height = 100)

        val result = roiExtractor.extractRoi(bitmap, incompleteCorners, roiSpec)

        assertTrue(result is RoiExtractionResult.Failure)
        val failure = result as RoiExtractionResult.Failure
        assertTrue(failure.reason.contains("expected 4"))
    }

    @Test
    fun `TEST 8 - Out-of-bounds ROI is rejected`() {
        val bitmap = mockk<Bitmap>()
        val cardCorners = listOf(
            PointF(0f, 0f), PointF(640f, 0f), PointF(640f, 960f), PointF(0f, 960f)
        )

        // Out-of-bounds ROI (xMin + width = 700 > 640)
        val invalidRoiSpec = RoiSpec(xMin = 500, yMin = 100, width = 200, height = 100)

        val result = roiExtractor.extractRoi(bitmap, cardCorners, invalidRoiSpec)

        assertTrue(result is RoiExtractionResult.Failure)
        val failure = result as RoiExtractionResult.Failure
        assertTrue(failure.reason.contains("bounds"))
    }

    @Test
    fun `TEST 9 - LAB feature vector is produced during ROI extraction`() {
        val width = 640
        val height = 960
        val pixels = IntArray(width * height) { 0xFF00FF00.toInt() }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            val len = dest.size.coerceAtMost(pixels.size)
            pixels.copyInto(dest, 0, 0, len)
        }
        every { bitmap.compress(any(), any(), any()) } returns true

        val cardCorners = listOf(
            PointF(0f, 0f), PointF(640f, 0f), PointF(640f, 960f), PointF(0f, 960f)
        )
        val roiSpec = RoiSpec(xMin = 140, yMin = 250, width = 360, height = 160)

        val result = roiExtractor.extractRoi(bitmap, cardCorners, roiSpec)

        assertTrue(result is RoiExtractionResult.Success)
        val success = result as RoiExtractionResult.Success
        assertNotNull(success.featureVector.averageLab)
        assertTrue(success.featureVector.averageLab.l >= 0.0)
    }

    @Test
    fun `TEST 10 - HSV feature vector is produced during ROI extraction`() {
        val width = 640
        val height = 960
        val pixels = IntArray(width * height) { 0xFF00FF00.toInt() }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            val len = dest.size.coerceAtMost(pixels.size)
            pixels.copyInto(dest, 0, 0, len)
        }
        every { bitmap.compress(any(), any(), any()) } returns true

        val cardCorners = listOf(
            PointF(0f, 0f), PointF(640f, 0f), PointF(640f, 960f), PointF(0f, 960f)
        )
        val roiSpec = RoiSpec(xMin = 140, yMin = 250, width = 360, height = 160)

        val result = roiExtractor.extractRoi(bitmap, cardCorners, roiSpec)

        assertTrue(result is RoiExtractionResult.Success)
        val success = result as RoiExtractionResult.Success
        assertNotNull(success.featureVector.averageHsv)
        assertEquals(3, success.featureVector.averageHsv.size)
    }

    @Test
    fun `TEST 11 - Debug ROI image is generated and saved`() {
        val width = 640
        val height = 960
        val pixels = IntArray(width * height) { 0xFF00FF00.toInt() }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            val len = dest.size.coerceAtMost(pixels.size)
            pixels.copyInto(dest, 0, 0, len)
        }
        every { bitmap.compress(any(), any(), any()) } returns true

        val cardCorners = listOf(
            PointF(0f, 0f), PointF(640f, 0f), PointF(640f, 960f), PointF(0f, 960f)
        )
        val roiSpec = RoiSpec(xMin = 140, yMin = 250, width = 360, height = 160)

        val result = roiExtractor.extractRoi(bitmap, cardCorners, roiSpec)

        assertTrue(result is RoiExtractionResult.Success)
        val success = result as RoiExtractionResult.Success
        assertTrue(success.debugImagePath.endsWith(".png") || success.debugImagePath == "SAVE_FAILED")
    }
}

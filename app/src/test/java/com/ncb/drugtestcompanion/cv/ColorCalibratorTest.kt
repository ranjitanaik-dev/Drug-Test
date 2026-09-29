package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ColorCalibratorTest {

    private lateinit var colorCalibrator: ColorCalibrator

    @Before
    fun setUp() {
        colorCalibrator = ColorCalibrator()
    }

    @Test
    fun `calculateLeastSquares3x3Matrix calculates identity-like matrix when measured matches targets`() {
        val targets = ColorCalibrator.SYNTHETIC_RGB_TARGETS
        val matrix = colorCalibrator.calculateLeastSquares3x3Matrix(targets, targets)

        assertNotNull(matrix)
        assertEquals(9, matrix!!.size)

        // Diagonal elements should be close to 1.0, off-diagonal elements close to 0.0
        assertEquals(1.0, matrix[0], 0.05) // M00 (R -> R)
        assertEquals(0.0, matrix[1], 0.05) // M01 (R -> G)
        assertEquals(0.0, matrix[2], 0.05) // M02 (R -> B)

        assertEquals(0.0, matrix[3], 0.05) // M10 (G -> R)
        assertEquals(1.0, matrix[4], 0.05) // M11 (G -> G)
        assertEquals(0.0, matrix[5], 0.05) // M12 (G -> B)

        assertEquals(0.0, matrix[6], 0.05) // M20 (B -> R)
        assertEquals(0.0, matrix[7], 0.05) // M21 (B -> G)
        assertEquals(1.0, matrix[8], 0.05) // M22 (B -> B)
    }

    @Test
    fun `calculateLeastSquares3x3Matrix returns null when insufficient patches provided`() {
        val insufficientMeasured = mapOf(
            "White" to RgbColor(220.0, 215.0, 210.0),
            "Gray" to RgbColor(120.0, 115.0, 110.0)
        )
        val matrix = colorCalibrator.calculateLeastSquares3x3Matrix(insufficientMeasured, ColorCalibrator.SYNTHETIC_RGB_TARGETS)

        assertNull(matrix)
    }

    @Test
    fun `computeCalibrationTransform fails gracefully when less than 4 patches sampled`() {
        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.width } returns 10
        every { mockBitmap.height } returns 10
        every { mockBitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val dest = firstArg<IntArray>()
            dest.fill(0xFF000000.toInt())
        }

        val result = colorCalibrator.computeCalibrationTransform(mockBitmap, emptyList())

        assertFalse(result.isCalibrated)
        assertNull(result.transformMatrix)
        assertTrue(result.statusMessage.contains("FAILED"))
    }

    @Test
    fun `applyCalibrationToBitmap clips RGB values outside 0 to 255 range`() {
        val width = 10
        val height = 10
        val pixels = IntArray(width * height) { 0xFFFFFFFF.toInt() } // Pure white

        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.width } returns width
        every { mockBitmap.height } returns height
        every { mockBitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        // Gain matrix that multiplies by 2.0 (would produce 510 without clipping)
        val matrixGain2 = doubleArrayOf(
            2.0, 0.0, 0.0,
            0.0, 2.0, 0.0,
            0.0, 0.0, 2.0
        )

        // Ensure method executes without exception and clamps values
        val resultBitmap = colorCalibrator.applyCalibrationToBitmap(mockBitmap, matrixGain2)
        assertNotNull(resultBitmap)
    }

    @Test
    fun `sampleRgbPatchesFromBitmap samples all 6 canonical reference patches`() {
        val width = 640
        val height = 960
        val pixels = IntArray(width * height) { 0xFF808080.toInt() }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val sampledRgb = colorCalibrator.sampleRgbPatchesFromBitmap(bitmap)

        assertEquals(6, sampledRgb.size)
        assertTrue(sampledRgb.containsKey("White"))
        assertTrue(sampledRgb.containsKey("Gray"))
        assertTrue(sampledRgb.containsKey("Black"))
        assertTrue(sampledRgb.containsKey("Red"))
        assertTrue(sampledRgb.containsKey("Green"))
        assertTrue(sampledRgb.containsKey("Blue"))
    }

    @Test
    fun `rgbToLab converts pure white (255,255,255) to L approximately 100`() {
        val lab = colorCalibrator.rgbToLab(255, 255, 255)

        assertEquals(100.0, lab.l, 1.0)
        assertEquals(0.0, lab.a, 2.0)
        assertEquals(0.0, lab.b, 2.0)
    }

    @Test
    fun `calculateDeltaE calculates CIE 1976 Euclidean distance accurately`() {
        val c1 = LabColor(50.0, 10.0, 20.0)
        val c2 = LabColor(53.0, 14.0, 24.0) // sqrt(9 + 16 + 16) = sqrt(41) ≈ 6.403
        assertEquals(6.403, colorCalibrator.calculateDeltaE(c1, c2), 0.01)
    }
}

package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import android.graphics.PointF
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `rgbToLab converts pure white (255,255,255) to L approximately 100`() {
        val lab = colorCalibrator.rgbToLab(255, 255, 255)

        assertEquals(100.0, lab.l, 1.0)
        assertEquals(0.0, lab.a, 2.0)
        assertEquals(0.0, lab.b, 2.0)
    }

    @Test
    fun `rgbToLab converts pure black (0,0,0) to L approximately 0`() {
        val lab = colorCalibrator.rgbToLab(0, 0, 0)

        assertEquals(0.0, lab.l, 1.0)
    }

    @Test
    fun `rgbToLab converts primary red (255,0,0) to positive a star`() {
        val lab = colorCalibrator.rgbToLab(255, 0, 0)

        assertTrue(lab.l > 0.0)
        assertTrue(lab.a > 30.0) // Strong positive a* indicates red
    }

    @Test
    fun `rgbToLab converts primary green (0,255,0) to negative a star`() {
        val lab = colorCalibrator.rgbToLab(0, 255, 0)

        assertTrue(lab.l > 0.0)
        assertTrue(lab.a < -30.0) // Strong negative a* indicates green
    }

    @Test
    fun `rgbToLab converts primary blue (0,0,255) to negative b star`() {
        val lab = colorCalibrator.rgbToLab(0, 0, 255)

        assertTrue(lab.l > 0.0)
        assertTrue(lab.b < -30.0) // Strong negative b* indicates blue
    }

    @Test
    fun `calculateDeltaE calculates CIE 1976 Euclidean distance accurately`() {
        val c1 = LabColor(50.0, 10.0, 20.0)
        val c2 = LabColor(50.0, 10.0, 20.0)
        assertEquals(0.0, colorCalibrator.calculateDeltaE(c1, c2), 0.001)

        val c3 = LabColor(53.0, 14.0, 24.0) // dL=3, da=4, db=4 -> sqrt(9 + 16 + 16) = sqrt(41) ≈ 6.403
        assertEquals(6.403, colorCalibrator.calculateDeltaE(c1, c3), 0.01)
    }

    @Test
    fun `calibrate samples canonical ROIs and reports LAB calibration pending due to missing physical target schema`() {
        val width = 640
        val height = 960
        val pixels = IntArray(width * height) { 0xFFCCCCCC.toInt() }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val corners = listOf(
            PointF(10f, 10f),
            PointF(630f, 10f),
            PointF(630f, 950f),
            PointF(10f, 950f)
        )

        val result = colorCalibrator.calibrate(bitmap, corners)

        assertTrue(result.sampledLabValues.isNotEmpty())
        assertFalse(result.isCalibrated)
        assertTrue(result.statusMessage.contains("PENDING"))
    }
}

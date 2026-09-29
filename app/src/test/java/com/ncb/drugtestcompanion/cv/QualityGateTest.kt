package com.ncb.drugtestcompanion.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.ncb.drugtestcompanion.domain.model.QualityFailureReason
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QualityGateTest {

    private lateinit var context: Context
    private lateinit var cardDetector: CardDetector
    private lateinit var colorCalibrator: ColorCalibrator
    private lateinit var qualityGate: QualityGate

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        cardDetector = mockk()
        colorCalibrator = ColorCalibrator()
        every { cardDetector.detectCard(any()) } returns CardDetectionResult.Detected(
            corners = listOf(PointF(10f, 10f), PointF(400f, 10f), PointF(400f, 300f), PointF(10f, 300f)),
            confidence = 0.9f
        )
        qualityGate = QualityGate(context, cardDetector, colorCalibrator)
    }

    @Test
    fun `analyzeBitmap with low resolution returns ImageInvalid LOW_RESOLUTION`() {
        val lowResBitmap = mockk<Bitmap>()
        every { lowResBitmap.width } returns 200
        every { lowResBitmap.height } returns 200

        val result = qualityGate.analyzeBitmap(lowResBitmap)

        assertTrue(result is QualityResult.ImageInvalid)
        assertEquals(
            QualityFailureReason.LOW_RESOLUTION,
            (result as QualityResult.ImageInvalid).reason
        )
    }

    @Test
    fun `analyzeBitmap with blurry image returns ImageInvalid BLUR`() {
        val blurryBitmap = mockk<Bitmap>()
        val width = 500
        val height = 500
        val pixels = IntArray(width * height) { 0xFF808080.toInt() }

        every { blurryBitmap.width } returns width
        every { blurryBitmap.height } returns height
        every {
            blurryBitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = qualityGate.analyzeBitmap(blurryBitmap)

        assertTrue(result is QualityResult.ImageInvalid)
        assertEquals(
            QualityFailureReason.BLUR,
            (result as QualityResult.ImageInvalid).reason
        )
    }

    @Test
    fun `analyzeBitmap with underexposed image returns ImageInvalid UNDEREXPOSED`() {
        val width = 500
        val height = 500
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x / 10 + y / 10) % 2 == 0) 0xFF050505.toInt() else 0xFF191919.toInt()
        }

        val darkBitmap = mockk<Bitmap>()
        every { darkBitmap.width } returns width
        every { darkBitmap.height } returns height
        every {
            darkBitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = qualityGate.analyzeBitmap(darkBitmap)

        assertTrue(result is QualityResult.ImageInvalid)
        assertEquals(
            QualityFailureReason.UNDEREXPOSED,
            (result as QualityResult.ImageInvalid).reason
        )
    }

    @Test
    fun `analyzeBitmap with overexposed image returns ImageInvalid OVEREXPOSED`() {
        val width = 500
        val height = 500
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x / 10 + y / 10) % 2 == 0) 0xFFECECEC.toInt() else 0xFFFEFEFE.toInt()
        }

        val brightBitmap = mockk<Bitmap>()
        every { brightBitmap.width } returns width
        every { brightBitmap.height } returns height
        every {
            brightBitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = qualityGate.analyzeBitmap(brightBitmap)

        assertTrue(result is QualityResult.ImageInvalid)
        assertEquals(
            QualityFailureReason.OVEREXPOSED,
            (result as QualityResult.ImageInvalid).reason
        )
    }

    @Test
    fun `analyzeBitmap with missing card returns ImageInvalid CARD_NOT_VISIBLE`() {
        val width = 500
        val height = 500
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x / 10 + y / 10) % 2 == 0) 0xFF505050.toInt() else 0xFFB4B4B4.toInt()
        }

        val sharpBitmap = mockk<Bitmap>()
        every { sharpBitmap.width } returns width
        every { sharpBitmap.height } returns height
        every {
            sharpBitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        every { cardDetector.detectCard(sharpBitmap) } returns CardDetectionResult.NotDetected

        val result = qualityGate.analyzeBitmap(sharpBitmap)

        assertTrue(result is QualityResult.ImageInvalid)
        assertEquals(
            QualityFailureReason.CARD_NOT_VISIBLE,
            (result as QualityResult.ImageInvalid).reason
        )
    }

    @Test
    fun `analyzeBitmap with sharp well-exposed image with card returns ImageValid`() {
        val width = 500
        val height = 500
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x / 10 + y / 10) % 2 == 0) 0xFF505050.toInt() else 0xFFB4B4B4.toInt()
        }

        val sharpBitmap = mockk<Bitmap>()
        every { sharpBitmap.width } returns width
        every { sharpBitmap.height } returns height
        every {
            sharpBitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = qualityGate.analyzeBitmap(sharpBitmap)

        assertTrue(result is QualityResult.ImageValid)
    }
}

package com.ncb.drugtestcompanion.cv

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardDetectorTest {

    private lateinit var cardDetector: CardDetector

    @Before
    fun setUp() {
        cardDetector = CardDetector()
    }

    @Test
    fun `detectCard returns NotDetected for low resolution image`() {
        val lowResBitmap = mockk<Bitmap>()
        every { lowResBitmap.width } returns 200
        every { lowResBitmap.height } returns 200

        val result = cardDetector.detectCard(lowResBitmap)

        assertEquals(CardDetectionResult.NotDetected, result)
    }

    @Test
    fun `detectCard returns Detected for image with visible reference card bounds`() {
        val width = 600
        val height = 600
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (x in 100..500 && y in 100..400) 0xFFCCCCCC.toInt() else 0xFF101010.toInt()
        }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = cardDetector.detectCard(bitmap)

        assertTrue(result is CardDetectionResult.Detected)
        val detected = result as CardDetectionResult.Detected
        assertEquals(4, detected.corners.size)
        assertTrue(detected.confidence > 0.0f)
    }

    @Test
    fun `detectCard returns Detected for 4-3 ratio camera photo containing 2-3 reference card`() {
        val width = 1200 // 4:3 aspect ratio camera photo (1200 x 1600)
        val height = 1600
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            // 2:3 reference card placed inside (x in 200..1000, y in 200..1400)
            if (x in 200..1000 && y in 200..1400) 0xFFCCCCCC.toInt() else 0xFF101010.toInt()
        }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = cardDetector.detectCard(bitmap)

        assertTrue(result is CardDetectionResult.Detected)
        val detected = result as CardDetectionResult.Detected
        assertEquals(4, detected.corners.size)
    }

    @Test
    fun `detectCard returns FramingError when reference card is clipped at image border`() {
        val width = 600
        val height = 600
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (x in 0..500 && y in 0..400) 0xFFCCCCCC.toInt() else 0xFF101010.toInt()
        }

        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every {
            bitmap.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val dest = firstArg<IntArray>()
            pixels.copyInto(dest)
        }

        val result = cardDetector.detectCard(bitmap)

        assertEquals(CardDetectionResult.FramingError, result)
    }
}

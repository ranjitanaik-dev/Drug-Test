package com.ncb.drugtestcompanion.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class MlClassificationResult(
    val label: String,
    val confidence: Float,
    val probabilities: Map<String, Float>
)

/**
 * Classifier for evaluating [drug_test_classifier_float32.tflite] on ROI images using TensorFlow Lite Interpreter.
 */
class LiteRtImageClassifier(
    context: Context,
    modelFileName: String = MODEL_FILE
) {

    companion object {
        const val MODEL_FILE = "drug_test_classifier_float32.tflite"

        private const val INPUT_WIDTH = 386
        private const val INPUT_HEIGHT = 133
        private const val INPUT_CHANNELS = 3

        val LABELS = listOf(
            "NEGATIVE",
            "POSITIVE",
            "INCONCLUSIVE"
        )
    }

    private val interpreter: Interpreter

    init {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    fun classify(bitmap: Bitmap): MlClassificationResult {
        val resizedBitmap = if (bitmap.width == INPUT_WIDTH && bitmap.height == INPUT_HEIGHT) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(
                bitmap,
                INPUT_WIDTH,
                INPUT_HEIGHT,
                true
            )
        }

        val inputBuffer = ByteBuffer.allocateDirect(
            INPUT_WIDTH * INPUT_HEIGHT * INPUT_CHANNELS * 4
        ).order(ByteOrder.nativeOrder())

        for (y in 0 until INPUT_HEIGHT) {
            for (x in 0 until INPUT_WIDTH) {
                val pixel = resizedBitmap.getPixel(x, y)

                /*
                 * IMPORTANT:
                 * The TFLite model already contains its preprocessing layers (true_divide and subtract).
                 * Therefore, Android provides raw RGB values in the 0..255 range as float32.
                 */
                inputBuffer.putFloat(Color.red(pixel).toFloat())
                inputBuffer.putFloat(Color.green(pixel).toFloat())
                inputBuffer.putFloat(Color.blue(pixel).toFloat())
            }
        }

        inputBuffer.rewind()

        val outputBuffer = Array(1) { FloatArray(LABELS.size) }
        try {
            interpreter.run(inputBuffer, outputBuffer)
        } catch (_: Throwable) {}

        val probabilities = outputBuffer[0]
        var bestIndex = 0
        var maxProb = -1.0f
        val probabilityMap = mutableMapOf<String, Float>()

        for (i in LABELS.indices) {
            val prob = probabilities[i]
            probabilityMap[LABELS[i]] = prob
            if (prob > maxProb) {
                maxProb = prob
                bestIndex = i
            }
        }

        val result = MlClassificationResult(
            label = LABELS[bestIndex],
            confidence = maxProb,
            probabilities = probabilityMap
        )

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return result
    }

    fun close() {
        try {
            interpreter.close()
        } catch (_: Throwable) {}
    }
}

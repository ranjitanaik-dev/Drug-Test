package com.ncb.drugtestcompanion.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

data class MlClassificationResult(
    val label: String,
    val confidence: Float,
    val probabilities: Map<String, Float>
)

/**
 * Classifier for evaluating [roi_mobilenetv2_best_BASELINE_A.tflite] on ROI images using TensorFlow Lite Interpreter.
 */
class LiteRtImageClassifier(
    context: Context,
    modelFileName: String = MODEL_FILE
) {

    companion object {
        const val MODEL_FILE = "roi_mobilenetv2_best_BASELINE_A.tflite"
        private const val INPUT_SIZE = 224

        val LABELS = listOf(
            "INCONCLUSIVE",
            "NEGATIVE",
            "POSITIVE"
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
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF

            // MobileNetV2 preprocessing: (pixel / 127.5) - 1.0 -> [-1.0, 1.0]
            inputBuffer.putFloat((red / 127.5f) - 1.0f)
            inputBuffer.putFloat((green / 127.5f) - 1.0f)
            inputBuffer.putFloat((blue / 127.5f) - 1.0f)
        }

        inputBuffer.rewind()

        // Calculate tensor statistics for diagnostic logging
        val floatCount = 1 * INPUT_SIZE * INPUT_SIZE * 3
        val floatArray = FloatArray(floatCount)
        inputBuffer.asFloatBuffer().get(floatArray)
        inputBuffer.rewind()

        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE
        var sumVal = 0.0
        for (v in floatArray) {
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
            sumVal += v
        }
        val meanVal = (sumVal / floatCount).toFloat()
        var varianceSum = 0.0
        for (v in floatArray) {
            val diff = v - meanVal
            varianceSum += diff * diff
        }
        val stdVal = sqrt(varianceSum / floatCount).toFloat()

        logDiagnostic("========================================")
        logDiagnostic("ANDROID TENSOR DIAGNOSTIC BEFORE INFERENCE")
        logDiagnostic("========================================")
        logDiagnostic("Tensor Element Count: $floatCount")
        logDiagnostic("Total Byte Count: ${inputBuffer.capacity()}")
        logDiagnostic("ByteBuffer Position: ${inputBuffer.position()}")
        logDiagnostic("ByteBuffer Limit: ${inputBuffer.limit()}")
        logDiagnostic("ByteBuffer Capacity: ${inputBuffer.capacity()}")
        logDiagnostic("ByteBuffer Order: ${inputBuffer.order()}")
        logDiagnostic("Min Value: $minVal")
        logDiagnostic("Max Value: $maxVal")
        logDiagnostic("Mean Value: $meanVal")
        logDiagnostic("Std Dev: $stdVal")
        logDiagnostic("First 12 Floats: ${floatArray.take(12).joinToString(", ") { String.format("%.6f", it) }}")
        logDiagnostic("Last 12 Floats: ${floatArray.takeLast(12).joinToString(", ") { String.format("%.6f", it) }}")

        val outputBuffer = Array(1) { FloatArray(LABELS.size) }
        interpreter.run(inputBuffer, outputBuffer)

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
        interpreter.close()
    }

    private fun logDiagnostic(msg: String) {
        try {
            Log.d("AndroidTensorDiagnostic", msg)
        } catch (_: Throwable) {
            println("[AndroidTensorDiagnostic] $msg")
        }
    }
}

package com.ncb.drugtestcompanion.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.ncb.drugtestcompanion.domain.model.ClassificationResult
import com.ncb.drugtestcompanion.domain.model.TestResultCategory
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class DrugTestClassifier(
    private val context: Context
) {

    companion object {
        private const val MODEL_FILE = "drug_test_classifier_float32.tflite"

        private const val INPUT_WIDTH = 386
        private const val INPUT_HEIGHT = 133
        private const val INPUT_CHANNELS = 3

        private const val NUM_CLASSES = 3
    }

    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile())
    }

    // Exact class labels corresponding to TFLite model output indices:
    // index 0 = NEGATIVE
    // index 1 = POSITIVE
    // index 2 = INCONCLUSIVE
    private val labels = arrayOf(
        TestResultCategory.NEGATIVE,
        TestResultCategory.POSITIVE,
        TestResultCategory.INCONCLUSIVE
    )

    fun classify(bitmap: Bitmap): ClassificationResult {
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
                 * Therefore, Android must provide raw RGB values in the 0..255 range as float32.
                 */
                inputBuffer.putFloat(Color.red(pixel).toFloat())
                inputBuffer.putFloat(Color.green(pixel).toFloat())
                inputBuffer.putFloat(Color.blue(pixel).toFloat())
            }
        }

        inputBuffer.rewind()

        val output = Array(1) {
            FloatArray(NUM_CLASSES)
        }

        interpreter.run(inputBuffer, output)

        val probabilities = output[0]

        var maxIndex = 0
        for (i in 1 until NUM_CLASSES) {
            if (probabilities[i] > probabilities[maxIndex]) {
                maxIndex = i
            }
        }

        val predictedCategory = labels[maxIndex]
        val maxConfidence = probabilities[maxIndex]

        logMlPipeline("ML_PIPELINE: TFLite probabilities = NEG=${probabilities[0]}, POS=${probabilities[1]}, INC=${probabilities[2]}")
        logMlPipeline("ML_PIPELINE: FINAL RESULT = ${predictedCategory.name}")
        logMlPipeline("ML_PIPELINE: FINAL CONFIDENCE = $maxConfidence")

        val componentScores = mapOf(
            "negative_prob" to probabilities[0].toDouble(),
            "positive_prob" to probabilities[1].toDouble(),
            "inconclusive_prob" to probabilities[2].toDouble(),
            "calculated_confidence" to maxConfidence.toDouble()
        )

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return ClassificationResult(
            result = predictedCategory,
            confidence = maxConfidence,
            distance = 0.0,
            targetCentroidName = "TFLITE_MOBILENETV2",
            componentScores = componentScores,
            statusMessage = "TFLite ML classification completed successfully"
        )
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }

    fun close() {
        try {
            interpreter.close()
        } catch (_: Throwable) {}
    }

    private fun logMlPipeline(msg: String) {
        try {
            Log.d("ML_PIPELINE", msg)
        } catch (_: Throwable) {
            println("[ML_PIPELINE] $msg")
        }
    }
}

package com.ncb.drugtestcompanion.domain.model

/**
 * Baseline classification outcome category.
 */
enum class TestResultCategory {
    POSITIVE,
    NEGATIVE,
    INCONCLUSIVE
}

/**
 * Domain model representing the classification result (TFLite ML or classical fallback).
 */
data class ClassificationResult(
    val result: TestResultCategory,
    val confidence: Float,
    val distance: Double = 0.0,
    val targetCentroidName: String = "TFLITE_MOBILENETV2",
    val componentScores: Map<String, Double> = emptyMap(),
    val statusMessage: String = "TFLite ML Classification Completed"
)

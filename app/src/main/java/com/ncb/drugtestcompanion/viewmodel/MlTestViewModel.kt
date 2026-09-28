package com.ncb.drugtestcompanion.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncb.drugtestcompanion.data.ml.LiteRtImageClassifier
import com.ncb.drugtestcompanion.data.ml.MlClassificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for managing ML Test screen state and evaluating [LiteRtImageClassifier] on [test_rois/positive_test_roi.jpg].
 */
@HiltViewModel
class MlTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _result = MutableStateFlow<MlClassificationResult?>(null)
    val result: StateFlow<MlClassificationResult?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun runMlTest() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val classificationResult = withContext(ioDispatcher) {
                    val assetManager = context.assets
                    val roiPath = "test_rois/positive_test_roi.jpg"
                    val inputStream = assetManager.open(roiPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap == null) {
                        throw IllegalStateException("Failed to decode synthetic ROI bitmap from asset: $roiPath")
                    }

                    val classifier = LiteRtImageClassifier(context)
                    val classification = classifier.classify(bitmap)
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                    classification
                }
                _result.value = classificationResult
            } catch (e: Throwable) {
                _error.value = e.message ?: "An unexpected error occurred during ML evaluation"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

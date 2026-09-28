package com.ncb.drugtestcompanion.viewmodel

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class MlTestViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var viewModel: MlTestViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        viewModel = MlTestViewModel(context).apply {
            ioDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has null result, null error, and false isLoading`() {
        assertNull(viewModel.result.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `runMlTest sets error state when asset is missing or unreadable`() = runTest {
        every { context.assets.open("test_rois/positive_test_roi.jpg") } throws FileNotFoundException("Asset missing")

        viewModel.runMlTest()

        assertNull(viewModel.result.value)
        assertEquals("Asset missing", viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
}

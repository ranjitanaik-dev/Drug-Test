package com.ncb.drugtestcompanion.di

import androidx.camera.core.ImageCapture
import com.ncb.drugtestcompanion.data.repository.CameraRepositoryImpl
import com.ncb.drugtestcompanion.domain.repository.CameraRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing CameraX dependencies and binding repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        impl: CameraRepositoryImpl
    ): CameraRepository

    companion object {
        @Provides
        @Singleton
        fun provideImageCapture(): ImageCapture {
            return ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        }
    }
}

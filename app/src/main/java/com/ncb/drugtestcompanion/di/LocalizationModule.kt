package com.ncb.drugtestcompanion.di

import com.ncb.drugtestcompanion.localization.GoogleTranslationService
import com.ncb.drugtestcompanion.localization.TranslationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalizationModule {

    @Binds
    @Singleton
    abstract fun bindTranslationService(
        googleTranslationService: GoogleTranslationService
    ): TranslationService
}

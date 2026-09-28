package com.ncb.drugtestcompanion.di

import com.ncb.drugtestcompanion.data.repository.TestRecordRepositoryImpl
import com.ncb.drugtestcompanion.domain.repository.TestRecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTestRecordRepository(
        impl: TestRecordRepositoryImpl
    ): TestRecordRepository
}

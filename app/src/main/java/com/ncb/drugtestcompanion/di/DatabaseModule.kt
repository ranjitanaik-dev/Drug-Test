package com.ncb.drugtestcompanion.di

import android.content.Context
import androidx.room.Room
import com.ncb.drugtestcompanion.data.local.DrugTestDatabase
import com.ncb.drugtestcompanion.data.local.dao.TestRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDrugTestDatabase(
        @ApplicationContext context: Context
    ): DrugTestDatabase {
        return Room.databaseBuilder(
            context,
            DrugTestDatabase::class.java,
            "drug_test_companion.db"
        )
            .addMigrations(
                DrugTestDatabase.MIGRATION_1_2,
                DrugTestDatabase.MIGRATION_2_3,
                DrugTestDatabase.MIGRATION_3_4
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTestRecordDao(database: DrugTestDatabase): TestRecordDao {
        return database.testRecordDao()
    }
}

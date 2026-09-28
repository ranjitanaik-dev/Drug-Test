package com.ncb.drugtestcompanion.di



import android.content.Context
import com.ncb.drugtestcompanion.ml.DrugTestClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideDrugTestClassifier(
        @ApplicationContext context: Context
    ): DrugTestClassifier {
        return DrugTestClassifier(context)
    }
}
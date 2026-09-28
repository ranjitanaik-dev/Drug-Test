package com.ncb.drugtestcompanion

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class DrugTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (!OpenCVLoader.initLocal()) {
                OpenCVLoader.initDebug()
            }
        } catch (e: Throwable) {
            Log.e("DrugTestApplication", "OpenCV initialization warning: ${e.message}")
        }
    }
}

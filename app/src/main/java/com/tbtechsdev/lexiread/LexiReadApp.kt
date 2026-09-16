package com.tbtechsdev.lexiread

import android.app.Application
import android.util.Log
import com.tbtechsdev.lexiread.data.db.AppDatabase
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "LexiReadApp"

@HiltAndroidApp
class LexiReadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "PDFBoxResourceLoader init error: ${e.message}", e)
        }

        if (AppDatabase.hasDictionaryAsset(this)) {
            Log.i(TAG, "dictionary.db asset detected in assets.")
        } else {
            Log.i(TAG, "dictionary.db not found in assets, database will run with starter schema.")
        }
    }
}

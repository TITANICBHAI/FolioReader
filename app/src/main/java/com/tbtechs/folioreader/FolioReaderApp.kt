package com.tbtechs.folioreader

import android.app.Application
import android.util.Log
import com.tbtechs.folioreader.data.db.AppDatabase
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "FolioReaderApp"

@HiltAndroidApp
class FolioReaderApp : Application() {
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

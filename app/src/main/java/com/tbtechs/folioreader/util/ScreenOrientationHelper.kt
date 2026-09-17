package com.tbtechs.folioreader.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo

enum class ScreenOrientationMode(
    val key: String,
    val title: String,
    val description: String
) {
    SENSOR(
        key = "sensor",
        title = "Auto-Rotate (Sensor)",
        description = "Screen rotates automatically with device orientation sensor"
    ),
    PORTRAIT(
        key = "portrait",
        title = "Lock Portrait",
        description = "Screen stays locked in vertical portrait orientation"
    ),
    LANDSCAPE(
        key = "landscape",
        title = "Lock Landscape",
        description = "Screen stays locked in horizontal landscape orientation"
    );

    companion object {
        fun fromKey(key: String?): ScreenOrientationMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SENSOR
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Activity.applyOrientation(mode: ScreenOrientationMode) {
    val requested = when (mode) {
        ScreenOrientationMode.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    if (requestedOrientation != requested) {
        requestedOrientation = requested
    }
}

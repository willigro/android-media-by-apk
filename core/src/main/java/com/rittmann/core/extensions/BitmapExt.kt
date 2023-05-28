package com.rittmann.core.extensions

import android.graphics.Bitmap
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun Bitmap.saveTo(file: File): String? {
    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(file)

        this.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    } catch (e: Exception) {
        track(e)
        e.printStackTrace()
    } finally {
        try {
            fos?.close()
        } catch (e: IOException) {
            track(e)
            e.printStackTrace()
        }
    }
    return file.absolutePath
}
package com.rittmann.core.extensions

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


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

fun BitmapExif?.saveTo(
    resolver: ContentResolver,
    contentValues: ContentValues,
): Uri {
    var stream: OutputStream? = null
    var uri: Uri? = null

    return try {
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        uri = resolver.insert(contentUri, contentValues)

        if (uri == null) {
            throw IOException("Failed to create new MediaStore record.")
        }

        stream = resolver.openOutputStream(uri)

        if (stream == null) {
            throw IOException("Failed to get output stream.")
        }

        if (this?.bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream) == false) {
            throw IOException("Failed to save bitmap.")
        }

        uri
    } catch (e: IOException) {
        if (uri != null) {
            // Don't leave an orphan entry in the MediaStore
            resolver.delete(uri, null, null)
        }

        throw e
    } finally {
        stream?.close()
    }
}
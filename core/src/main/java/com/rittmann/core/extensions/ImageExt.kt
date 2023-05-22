package com.rittmann.core.extensions

import android.graphics.BitmapFactory
import android.media.Image
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.android.Exif
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.tracker.track
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

fun Image.toBitmapExif(): BitmapExif? {
    val buffer: ByteBuffer = planes[0].buffer
    return try {
        val bytes = ByteArray(buffer.capacity())

        track("Image=$this, buffer=$buffer, bytes=${bytes.size}")

        buffer.get(bytes)

        val exif = ExifInterface(ByteArrayInputStream(bytes))

        val bitmap = Exif.fixBitmapOrientation(
            exif,
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null),
        )

        BitmapExif(bitmap, exif)
    } catch (e: Exception) {
        track(e)
        null
    } finally {
        // Without it, the pos is not going to be 0, and then it will break
        // due to BufferUnderflowException
        buffer.clear()
    }
}
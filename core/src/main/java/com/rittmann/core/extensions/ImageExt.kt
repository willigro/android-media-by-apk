package com.rittmann.core.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import com.rittmann.core.tracker.track
import java.nio.ByteBuffer

fun Image.toBitmap(): Bitmap? {
    val buffer: ByteBuffer = planes[0].buffer
    return try {
        val bytes = ByteArray(buffer.capacity())
        track("Image=$this, buffer=$buffer, bytes=${bytes.size}")
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    } catch (e: Exception) {
        track(e)
        null
    } finally {
        // Without it, the pos is not going to be 0, and then it will break
        // due to BufferUnderflowException
        buffer.clear()
    }
}
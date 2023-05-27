package com.rittmann.core.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.Image
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.exif.Exif
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.tracker.track
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import kotlin.random.Random


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

fun Bitmap.applyRandomFilter(): Bitmap {
    val width: Int = this.width
    val height: Int = this.height
    val pixels = IntArray(width * height)
    this.getPixels(pixels, 0, width, 0, 0, width, height)

    for (x in pixels.indices) {
        pixels[x] = if (Random.nextBoolean()) colors.random() else pixels[x]
    }

    val result = Bitmap.createBitmap(width, height, this.config)
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

val colors = arrayListOf(
    Color.BLUE,
    Color.RED,
    Color.WHITE,
    Color.YELLOW,
    Color.GRAY,
    Color.GREEN,
    Color.CYAN,
    Color.MAGENTA,
    Color.GRAY,
    Color.BLACK,
)
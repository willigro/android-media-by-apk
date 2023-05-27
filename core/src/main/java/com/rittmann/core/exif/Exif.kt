package com.rittmann.core.exif

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.tracker.track
import java.io.File

object Exif {

    fun fixBitmapOrientation(exifInterface: ExifInterface?, bitmap: Bitmap?): Bitmap? {
        val matrix = Matrix()

        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_FLIP_HORIZONTAL")
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
                track("ExifInterface.ORIENTATION_ROTATE_180")
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_FLIP_VERTICAL")
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_TRANSPOSE")
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
                track("ExifInterface.ORIENTATION_ROTATE_90")
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_TRANSVERSE")
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(-90f)
                track("ExifInterface.ORIENTATION_ROTATE_270")
            }

            ExifInterface.ORIENTATION_UNDEFINED -> {
//                matrix.setRotate(-90f)
                track("ExifInterface.ORIENTATION_UNDEFINED")
            }
        }

        if (bitmap == null) return null

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    fun saveExif(exifInterface: ExifInterface?, path: String?) {
        path?.also {
            val newExif = ExifInterface(File(path))

            newExif.setAttribute(
                ExifInterface.TAG_DATETIME,
                exifInterface?.getAttribute(ExifInterface.TAG_DATETIME)
            )

            newExif.setAttribute(
                ExifInterface.TAG_IMAGE_WIDTH,
                exifInterface?.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            )

            newExif.saveAttributes()
        }
    }
}
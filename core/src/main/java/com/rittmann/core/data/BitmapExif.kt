package com.rittmann.core.data

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface

class BitmapExif(
    val bitmap: Bitmap?,
    val exifInterface: ExifInterface?,
)
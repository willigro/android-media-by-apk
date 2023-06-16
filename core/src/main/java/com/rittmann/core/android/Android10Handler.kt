package com.rittmann.core.android

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.R
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.exif.Exif
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.Q)
class Android10Handler(
    context: Context
) : CentralHandler(context) {

    companion object {
        const val INTERNAL_DIRECTORY = "imageDir"
    }

    init {
        track()

        // TODO: mocking, remove me later
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val fileName = saveToInternalStorage(
                    BitmapFactory.decodeResource(context.resources, R.drawable.untitled)
                )
                track(
                    "mocking bitmap=${
                        fileName
                    }"
                )

                fileName?.also {
                    val bitmap = ThumbnailUtils.createImageThumbnail(
                        File(fileName),
                        Size(120, 120),
                        null,
                    )

                    track(bitmap)
                }

            } else {
                track("Cannot mock bitmap")
            }
        } catch (e: IOException) {
            track(e)
            e.printStackTrace()
        }
    }

    override fun loadInternalMedia() {
        queueExecution.clear()

        val cw = ContextWrapper(context)

        val directory: File = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)
        val files = directory.listFiles()

        val imageList = mutableListOf<Image>()

        files?.filter { file ->
            file.canRead()
        }?.map { file ->
            imageList += Image(
                uri = Uri.fromFile(file),
                name = file.name,
                id = null,
                storage = Storage.INTERNAL,
            )
        } ?: listOf()

        mediaImageList.value = imageList

        lastExecution = QueueExecution.RETRIEVE_INTERNAL_MEDIA

        track("imageList=$imageList")
    }

    override fun loadExternalMedia() {

    }

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10

    override fun loadThumbnail(image: Image): Bitmap {
        return ThumbnailUtils.createImageThumbnail(
            File(image.uri.path!!),
            Size(200, 300),
            null,
        )
    }

    override fun loadBitmap(image: Image): Bitmap {
        return loadBitmap(image.uri)
    }

    override fun loadBitmapExif(image: Image): BitmapExif? {
        return try {
            if (image.uri.path == null) return null

            val exifInterface = ExifInterface(File(image.uri.path!!))

            BitmapExif(
                bitmap = Exif.fixBitmapOrientation(exifInterface, loadBitmap(image.uri)),
                exifInterface = exifInterface,
            )
        } catch (e: IOException) {
            track(e)
            null
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        track(uri)
        return MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri,
        )
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap): String? {
        val cw = ContextWrapper(context)

        val directory: File = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

        val myPath = File(directory, "profile.jpg")

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(myPath)

            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: Exception) {
            track(e)
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                track(e)
                e.printStackTrace()
            }
        }
        return myPath.absolutePath
    }
}
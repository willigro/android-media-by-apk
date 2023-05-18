package com.rittmann.core.android

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rittmann.core.R
import com.rittmann.core.data.Image
import com.rittmann.core.extensions.arePermissionsGranted
import com.rittmann.core.extensions.arePermissionsGrated
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class Android9Handler(private val context: Context) : AndroidHandler {

    init {
        track()

        // TODO: mocking, remove me later
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                track(
                    "mocking bitmap=${
                        saveToInternalStorage(
                            BitmapFactory.decodeResource(context.resources, R.drawable.tilemap)
                        )
                    }"
                )
            } else {
                track("Cannot mock bitmap")
            }
        } catch (e: IOException) {
            track(e)
            e.printStackTrace()
        }
    }

    companion object {
        private val PERMISSIONS_STORAGE = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        const val INTERNAL_DIRECTORY = "imageDir"
    }

    private var activityResultLauncherPermissions: ActivityResultLauncher<Array<String>>? = null
    private var activityResultLauncherSettings: ActivityResultLauncher<Intent>? = null
    private val requestPermissionsLiveData: MutableLiveData<Unit> = MutableLiveData()
    private val queueExecution: Queue<QueueExecution> = LinkedList()

    private val _mediaUris: MutableStateFlow<List<Image>> = MutableStateFlow(arrayListOf())

    override val permissionIsDenied: ConflatedEventBus<Boolean> = ConflatedEventBus()

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_9

    override fun loadInternalMedia() {
        queueExecution.clear()

        if (checkPermissionsAndScheduleExecutionCaseNeeded(
                QueueExecution.RETRIEVE_INTERNAL_MEDIA
            ).not()
        ) {
            return
        }

        val cw = ContextWrapper(context)

        val directory: File = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)
        val files = directory.listFiles()

        val imageList = mutableListOf<Image>()

        val result = files?.filter { file ->
            file.canRead()
        }?.map { file ->
//            val imageBytes = it.readBytes()
//            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageList += Image(uri = Uri.fromFile(file), name = file.name, id = null)
        } ?: listOf()

        _mediaUris.value = imageList

        track(result)
    }

    override fun loadExternalMedia() {
        queueExecution.clear()

        if (checkPermissionsAndScheduleExecutionCaseNeeded(
                QueueExecution.RETRIEVE_EXTERNAL_MEDIA
            ).not()
        ) {
            return
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
        )

        val selection = ""
        val selectionArgs = arrayOf<String>()

        val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        val imageList = mutableListOf<Image>()

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    collection,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                imageList += Image(uri = contentUri, name = name, id = id)
            }
        }

        _mediaUris.value = imageList
        track(imageList)
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
        return directory.absolutePath
    }

    override fun registerPermissions(componentActivity: ComponentActivity) {
        registerLauncherPermissions(componentActivity)
        registerLauncherSettings(componentActivity)
    }

    override fun requestPermissions() {
        activityResultLauncherPermissions?.launch(
            PERMISSIONS_STORAGE.toTypedArray()
        )
    }

    override fun permissionObserver(): LiveData<Unit> = requestPermissionsLiveData

    override fun mediaList(): StateFlow<List<Image>> = _mediaUris

    override fun loadThumbnailFor(media: Image): Bitmap {
        if (media.id == null) {
            return loadBitmapFor(media)
        }

        return MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            media.id,
            MediaStore.Images.Thumbnails.MINI_KIND,
            null,
        )
    }

    override fun loadBitmapFor(media: Image): Bitmap {
        return MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            media.uri,
        )
    }

    private fun checkPermissionsAndScheduleExecutionCaseNeeded(
        execution: QueueExecution,
    ): Boolean {
        val hasPermission = PERMISSIONS_STORAGE.arePermissionsGrated(context)

        if (hasPermission.not()) {
            requestPermissionsLiveData.value = Unit
            queueExecution.add(execution)
        }

        track("hasPermission=$hasPermission, execution=$execution")

        return hasPermission
    }

    private fun registerLauncherSettings(componentActivity: ComponentActivity) {
        activityResultLauncherSettings = componentActivity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (PERMISSIONS_STORAGE.arePermissionsGrated(context)) {
                executeNextOnQueue()
            } else {
                permissionIsDenied.send(true)
            }
            track(result)
        }
    }

    private fun registerLauncherPermissions(componentActivity: ComponentActivity) {
        activityResultLauncherPermissions = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.entries.arePermissionsGranted()) {
                executeNextOnQueue()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    track("try check each permission")
                    for (permission in PERMISSIONS_STORAGE) {
                        if (componentActivity.shouldShowRequestPermissionRationale(
                                permission
                            ).not()
                        ) {
                            track("blocked, open settings")
                            // Permission was blocked, request for the settings
                            openSettingsScreen()

                            return@registerForActivityResult
                        }
                    }

                    track("permission was just denied")
                    // Permission was not blocked, try request again
                    permissionIsDenied.send(true)
                } else {
                    track("normal, open settings")
                    // Permission was blocked, request for the settings
                    openSettingsScreen()
                }
            }
        }
    }

    private fun executeNextOnQueue() {
        track(queueExecution)
        when (queueExecution.remove()) {
            QueueExecution.RETRIEVE_INTERNAL_MEDIA -> loadInternalMedia()
            QueueExecution.RETRIEVE_EXTERNAL_MEDIA -> loadExternalMedia()
            else -> {}
        }
    }

    private fun openSettingsScreen() {
        activityResultLauncherSettings?.launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}
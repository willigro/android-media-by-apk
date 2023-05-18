package com.rittmann.core.android

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rittmann.core.data.Image
import java.util.*
import kotlinx.coroutines.flow.StateFlow

class Android12Handler: AndroidHandler {

    private val requestPermissionsLiveData: MutableLiveData<Unit> = MutableLiveData()
    private val queueExecution: Queue<QueueExecution> = LinkedList()

    override val permissionIsDenied: ConflatedEventBus<Boolean> = ConflatedEventBus(false)

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_12
    override fun registerPermissions(componentActivity: ComponentActivity) {
        TODO("Not yet implemented")
    }

    override fun requestPermissions() {
        TODO("Not yet implemented")
    }

    override fun permissionObserver(): LiveData<Unit> = requestPermissionsLiveData
    override fun mediaList(): StateFlow<List<Image>> {
        TODO("Not yet implemented")
    }

    override fun loadThumbnailFor(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmapFor(media: Image): Bitmap {
        TODO("Not yet implemented")
    }
}
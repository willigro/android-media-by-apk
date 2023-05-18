package com.rittmann.core.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

class Android10Handler: AndroidHandler {

    private val queueExecution: Queue<QueueExecution> = LinkedList()
    private val requestPermissionsLiveData: MutableLiveData<Unit> = MutableLiveData()

    override val permissionIsDenied: ConflatedEventBus<Boolean> = ConflatedEventBus(false)

    override fun permissionObserver(): LiveData<Unit> = requestPermissionsLiveData
    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10
}
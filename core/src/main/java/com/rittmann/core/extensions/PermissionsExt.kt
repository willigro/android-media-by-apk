package com.rittmann.core.extensions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.rittmann.core.tracker.track

fun Set<Map.Entry<String, Boolean>>.arePermissionsGranted(): Boolean {
    for (permission in this) {
        val permissionName = permission.key
        val isGranted = permission.value
        track("permissionName=$permissionName, isGranted=$isGranted")
        if (isGranted.not()) {
            return false
        }
    }

    return true
}

fun MutableList<String>.arePermissionsGrated(context: Context) : Boolean {
    track()
    for (permission in this) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }

    return true
}
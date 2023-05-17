package com.rittmann.core.android


interface AndroidHandler {
    fun version(): AndroidVersion
}

object AndroidHandlerFactory {

    fun create(): AndroidHandler {
        val sdk = android.os.Build.VERSION.SDK_INT
        return when {
            sdk == android.os.Build.VERSION_CODES.Q -> Android10Handler()
            sdk == android.os.Build.VERSION_CODES.R -> Android11Handler()
            sdk >= android.os.Build.VERSION_CODES.S -> Android12Handler()
            else -> Android9Handler()
        }
    }
}

enum class AndroidVersion {
    ANDROID_9, ANDROID_10, ANDROID_11, ANDROID_12
}
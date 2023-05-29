package com.rittmann.core.android

import android.content.Context

class Android10Handler(
    context: Context
): CentralHandler(context) {

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10
}
package com.rittmann.core.data

import android.net.Uri
import com.rittmann.core.android.Storage


data class Image(
    val uri: Uri,
    val name: String,
    val id: Long?,
    val storage: Storage,
)
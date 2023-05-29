package com.rittmann.core.data

import com.rittmann.core.android.Storage

data class StorageUri(
    val uri: String,
    val storage: Storage,
    val mediaId: Long?,
)
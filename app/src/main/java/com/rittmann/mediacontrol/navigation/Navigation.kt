package com.rittmann.mediacontrol.navigation

import com.rittmann.core.android.Storage


sealed class Navigation(val destination: String) {
    object Medias : Navigation("medias")
    object Create : Navigation("create")
    object Update : Navigation("update/{uri}/{storage}/{mediaId}") {
        const val URI = "uri"
        const val STORAGE = "storage"
        const val MEDIA_ID = "mediaId"
        fun transformDestination(uri: String, storage: Storage, mediaId: Long?) =
            "update/${uri.replace("/", "*")}/${storage.value}/${mediaId ?: "0"}"
    }

    companion object {
        const val ROUTE = "media"
    }
}
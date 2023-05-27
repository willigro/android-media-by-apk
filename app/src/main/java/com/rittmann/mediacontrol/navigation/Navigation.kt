package com.rittmann.mediacontrol.navigation

import com.rittmann.core.android.Storage


sealed class Navigation(val destination: String) {
    object Medias : Navigation("medias")
    object Create : Navigation("create")
    object Update : Navigation("update/{uri}/{storage}") {
        const val URI = "uri"
        const val STORAGE = "storage"
        fun transformDestination(uri: String, storage: Storage) = "update/${uri.replace("/", "*")}/${storage.value}"
    }

    companion object {
        const val ROUTE = "media"
    }
}
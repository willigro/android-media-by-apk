package com.rittmann.mediacontrol.navigation


sealed class Navigation(val destination: String) {
    object Medias : Navigation("medias")
    object Create : Navigation("create")

    companion object {
        const val ROUTE = "media"
    }
}
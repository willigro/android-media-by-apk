package com.rittmann.mediacontrol.navigation


sealed class Navigation(val destination: String) {
    object Medias : Navigation("medias")

    companion object {
        const val ROUTE = "media"
    }
}
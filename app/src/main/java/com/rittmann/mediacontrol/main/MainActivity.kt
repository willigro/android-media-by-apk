package com.rittmann.mediacontrol.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.rittmann.components.theme.GitHubTheme
import com.rittmann.components.theme.appLightColors
import com.rittmann.core.android.Android9Handler
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.tracker.track
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var androidHandler: AndroidHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidHandler.registerPermissions(this)

        androidHandler.permissionObserver().observe(this) {
            track()
            androidHandler.requestPermissions()
        }

        setContent {
            GitHubTheme {
                MainScreenRoot(androidHandler)
            }
        }
    }
}
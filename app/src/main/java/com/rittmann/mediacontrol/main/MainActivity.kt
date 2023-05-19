package com.rittmann.mediacontrol.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rittmann.components.theme.GitHubTheme
import com.rittmann.core.android.AndroidHandler
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var androidHandler: AndroidHandler

    @Inject
    lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO I don't wanna to pass the executor this way, find a better way where only the
        //  Camera screen will see it
        androidHandler.registerPermissions(this)

        setContent {
            GitHubTheme {
                MainScreenRoot(androidHandler)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
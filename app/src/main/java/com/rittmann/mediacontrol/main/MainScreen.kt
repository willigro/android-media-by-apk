package com.rittmann.mediacontrol.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rittmann.components.ui.alert.AlertComposeConfirm
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.tracker.track
import com.rittmann.mediacontrol.navigation.Navigation
import com.rittmann.mediacontrol.medias.mediaGraph
import kotlinx.coroutines.flow.consumeAsFlow


private val dialogConfirm = AlertComposeConfirm()

@Composable
fun MainScreenRoot(androidHandler: AndroidHandler) {
    NavigationGraph(rememberNavController(), androidHandler)
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    androidHandler: AndroidHandler,
) {
    NavHost(navController, startDestination = Navigation.ROUTE) {
        navigation(
            startDestination = Navigation.Medias.destination,
            route = Navigation.ROUTE,
        ) {
            mediaGraph(navController)
        }
    }

    RequestPermissionDeniedDialog(androidHandler)
}

@Composable
fun RequestPermissionDeniedDialog(androidHandler: AndroidHandler) {
    val permissionIsDenied = androidHandler.permissionIsDenied.flow.collectAsState(
        initial = 0 to false
    ).value

    track(permissionIsDenied)
    dialogConfirm.PresentDialog()

    if (permissionIsDenied.second == true) {
        // TODO show when permission is denied but not blocked
        dialogConfirm.updateAndShow(
            title = "Title",
            message = "Message",
            confirmCallback = androidHandler::requestPermissions,
        )
    }
}

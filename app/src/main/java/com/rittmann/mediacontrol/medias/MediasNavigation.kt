package com.rittmann.mediacontrol.medias

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rittmann.mediacontrol.create.CreateMediaScreenRoot
import com.rittmann.mediacontrol.navigation.Navigation
import java.util.concurrent.ExecutorService


fun NavGraphBuilder.mediaGraph(navController: NavController) {
    composable(Navigation.Medias.destination) {
        MediasScreenRoot(navController = navController)
    }
    composable(Navigation.Create.destination) {
        CreateMediaScreenRoot(navController = navController)
    }
}
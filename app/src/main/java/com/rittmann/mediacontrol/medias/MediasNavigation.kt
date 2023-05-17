package com.rittmann.mediacontrol.medias

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rittmann.mediacontrol.navigation.Navigation


fun NavGraphBuilder.mediaGraph(navController: NavController) {
    composable(Navigation.Medias.destination) {
        MediasScreenRoot(navController = navController)
    }
}
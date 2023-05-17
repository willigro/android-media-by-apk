package com.rittmann.mediacontrol.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rittmann.mediacontrol.navigation.Navigation
import com.rittmann.mediacontrol.medias.mediaGraph


@Preview
@Composable
fun MainScreenRoot() {
    NavigationGraph(rememberNavController())
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Navigation.ROUTE) {
        navigation(
            startDestination = Navigation.Medias.destination,
            route = Navigation.ROUTE,
        ) {
            mediaGraph(navController)
        }
    }
}
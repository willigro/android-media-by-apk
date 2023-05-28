package com.rittmann.mediacontrol.medias

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rittmann.core.android.Storage
import com.rittmann.core.android.StorageUri
import com.rittmann.mediacontrol.create.CreateMediaScreenArguments
import com.rittmann.mediacontrol.create.CreateMediaScreenRoot
import com.rittmann.mediacontrol.navigation.Navigation


fun NavGraphBuilder.mediaGraph(navController: NavController) {
    composable(Navigation.Medias.destination) {
        MediasScreenRoot(navController = navController)
    }
    composable(Navigation.Create.destination) {
        CreateMediaScreenRoot(navController = navController)
    }
    composable(Navigation.Update.destination) { backStackEntry ->

        val uri = backStackEntry.arguments?.getString(Navigation.Update.URI)
        val storage = backStackEntry.arguments?.getString(Navigation.Update.STORAGE)
        val mediaId = backStackEntry.arguments?.getString(Navigation.Update.MEDIA_ID)

        val storageUri = if (uri == null || storage == null) null else {
            StorageUri(
                uri = uri.replace("*", "/"),
                storage = Storage.values().first { it.value == storage },
            )
        }

        CreateMediaScreenRoot(
            navController = navController,
            createMediaScreenArguments = CreateMediaScreenArguments(
                storageUri = storageUri,
                mediaId = mediaId?.toLongOrNull(),
            ),
        )
    }
}
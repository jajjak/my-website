package com.linnan.hayaophoto.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linnan.hayaophoto.ui.screens.AddEditPersonScreen
import com.linnan.hayaophoto.ui.screens.CameraScreen
import com.linnan.hayaophoto.ui.screens.ChangePasswordScreen
import com.linnan.hayaophoto.ui.screens.HomeScreen
import com.linnan.hayaophoto.ui.screens.MediaViewerScreen
import com.linnan.hayaophoto.ui.screens.PersonDetailScreen

object Routes {
    const val HOME = "home"
    const val ADD_EDIT_PERSON = "add_edit_person"
    const val PERSON_DETAIL = "person_detail"
    const val CAMERA = "camera"
    const val MEDIA_VIEWER = "media_viewer"
    const val CHANGE_PASSWORD = "change_password"

    fun addEditPerson(personId: Long = -1L) = "$ADD_EDIT_PERSON?personId=$personId"
    fun personDetail(personId: Long) = "$PERSON_DETAIL/$personId"
    fun camera(personId: Long = -1L) = "$CAMERA?personId=$personId"
    fun mediaViewer(personId: Long, startId: Long) = "$MEDIA_VIEWER/$personId/$startId"
}

@Composable
fun HayaoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(
            route = "${Routes.ADD_EDIT_PERSON}?personId={personId}",
            arguments = listOf(navArgument("personId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: -1L
            AddEditPersonScreen(
                personId = if (personId >= 0) personId else null,
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Routes.PERSON_DETAIL}/{personId}",
            arguments = listOf(navArgument("personId") { type = NavType.LongType })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
            PersonDetailScreen(
                personId = personId,
                navController = navController
            )
        }
        composable(
            route = "${Routes.CAMERA}?personId={personId}",
            arguments = listOf(navArgument("personId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: -1L
            CameraScreen(
                preselectedPersonId = if (personId >= 0) personId else null,
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Routes.MEDIA_VIEWER}/{personId}/{startId}",
            arguments = listOf(
                navArgument("personId") { type = NavType.LongType },
                navArgument("startId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
            val startId = backStackEntry.arguments?.getLong("startId") ?: return@composable
            MediaViewerScreen(
                personId = personId,
                startMediaId = startId,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onDone = { navController.popBackStack() })
        }
    }
}

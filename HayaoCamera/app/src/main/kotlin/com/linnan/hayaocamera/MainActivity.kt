package com.linnan.hayaocamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linnan.hayaocamera.ui.camera.CameraScreen
import com.linnan.hayaocamera.ui.camera.CameraViewModel
import com.linnan.hayaocamera.ui.gallery.GalleryScreen
import com.linnan.hayaocamera.ui.gallery.GalleryViewModel
import com.linnan.hayaocamera.ui.gallery.MediaViewerScreen
import com.linnan.hayaocamera.ui.permissions.PermissionGate
import com.linnan.hayaocamera.ui.settings.AboutScreen
import com.linnan.hayaocamera.ui.settings.SettingsScreen
import com.linnan.hayaocamera.ui.theme.HayaoCameraTheme

private const val ROUTE_CAMERA = "camera"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_GALLERY_GRAPH = "gallery_graph"
private const val ROUTE_GALLERY_LIST = "gallery_list"
private const val ROUTE_GALLERY_VIEWER = "gallery_viewer/{index}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HayaoCameraTheme {
                PermissionGate {
                    HayaoCameraNavHost()
                }
            }
        }
    }
}

@Composable
private fun HayaoCameraNavHost() {
    val navController = rememberNavController()
    val cameraViewModel: CameraViewModel = viewModel()

    NavHost(navController = navController, startDestination = ROUTE_CAMERA) {
        composable(ROUTE_CAMERA) {
            CameraScreen(
                viewModel = cameraViewModel,
                onOpenGallery = { navController.navigate(ROUTE_GALLERY_GRAPH) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = cameraViewModel,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(ROUTE_ABOUT) }
            )
        }
        composable(ROUTE_ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        navigation(startDestination = ROUTE_GALLERY_LIST, route = ROUTE_GALLERY_GRAPH) {
            composable(ROUTE_GALLERY_LIST) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(ROUTE_GALLERY_GRAPH) }
                val galleryViewModel: GalleryViewModel = viewModel(parentEntry)
                GalleryScreen(
                    viewModel = galleryViewModel,
                    onBack = {
                        navController.popBackStack()
                        cameraViewModel.refreshThumbnail()
                    },
                    onOpenItem = { index -> navController.navigate("gallery_viewer/$index") }
                )
            }
            composable(
                route = ROUTE_GALLERY_VIEWER,
                arguments = listOf(navArgument("index") { type = NavType.IntType })
            ) { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(ROUTE_GALLERY_GRAPH) }
                val galleryViewModel: GalleryViewModel = viewModel(parentEntry)
                val index = entry.arguments?.getInt("index") ?: 0
                MediaViewerScreen(
                    index = index,
                    viewModel = galleryViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

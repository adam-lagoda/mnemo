package com.mnemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mnemo.ui.detail.DetailScreen
import com.mnemo.ui.gallery.GalleryScreen
import com.mnemo.ui.graph.GraphScreen
import com.mnemo.ui.indexing.IndexingScreen
import com.mnemo.ui.model.ModelScreen
import com.mnemo.ui.profile.ProfileScreen
import com.mnemo.ui.search.SearchScreen
import com.mnemo.ui.setup.SetupScreen

object Routes {
    const val GALLERY = "gallery"
    const val SEARCH = "search"
    const val GRAPH = "graph"
    const val MODEL = "model"
    const val PROFILE = "profile"
    // Sub-screens pushed from Profile
    const val SETUP = "setup"
    const val INDEXING = "indexing"
    const val DETAIL = "detail/{screenshotId}"
    fun detail(id: String) = "detail/$id"
}

@Composable
fun MnemoNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.GALLERY) {
        composable(Routes.GALLERY) {
            GalleryScreen(
                onScreenshotClick = { id -> navController.navigate(Routes.detail(id)) },
                onPendingClick = { navController.navigate(Routes.INDEXING) }
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(onResultClick = { id ->
                navController.navigate(Routes.detail(id))
            })
        }
        composable(Routes.GRAPH) {
            GraphScreen(onScreenshotOpen = { id ->
                navController.navigate(Routes.detail(id))
            })
        }
        composable(Routes.MODEL) {
            ModelScreen()
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onSetupClick = { navController.navigate(Routes.SETUP) },
                onIndexingClick = { navController.navigate(Routes.INDEXING) }
            )
        }
        composable(Routes.SETUP) {
            SetupScreen()
        }
        composable(Routes.INDEXING) {
            IndexingScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("screenshotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("screenshotId") ?: return@composable
            DetailScreen(screenshotId = id, onBack = { navController.popBackStack() })
        }
    }
}

package com.mnemo.ui.navigation

import android.net.Uri
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
import com.mnemo.ui.topic.TopicDetailScreen

object Routes {
    const val GALLERY = "gallery"
    const val SEARCH = "search"
    const val GRAPH = "graph"
    const val MODEL = "model"
    const val PROFILE = "profile"
    const val SETUP = "setup"
    const val INDEXING = "indexing"
    const val DETAIL = "detail/{screenshotId}"
    const val TOPIC = "topic/{topicKey}"

    fun detail(id: String) = "detail/$id"
    fun topic(key: String) = "topic/${Uri.encode(key)}"
}

@Composable
fun MnemoNavGraph(navController: NavHostController, onSearchClick: () -> Unit) {
    NavHost(navController = navController, startDestination = Routes.GALLERY) {
        composable(Routes.GALLERY) {
            GalleryScreen(
                onScreenshotClick = { id -> navController.navigate(Routes.detail(id)) },
                onPendingClick = { navController.navigate(Routes.INDEXING) },
                onSearchClick = onSearchClick,
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(onResultClick = { id -> navController.navigate(Routes.detail(id)) })
        }
        composable(Routes.GRAPH) {
            GraphScreen(
                onScreenshotOpen = { id -> navController.navigate(Routes.detail(id)) },
                onTopicOpen = { key -> navController.navigate(Routes.topic(key)) },
            )
        }
        composable(Routes.MODEL) { ModelScreen() }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onSetupClick = { navController.navigate(Routes.SETUP) },
                onIndexingClick = { navController.navigate(Routes.INDEXING) }
            )
        }
        composable(Routes.SETUP) { SetupScreen() }
        composable(Routes.INDEXING) {
            IndexingScreen(
                onBack = { navController.popBackStack() },
                onScreenshotClick = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("screenshotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("screenshotId") ?: return@composable
            DetailScreen(
                screenshotId = id,
                onBack = { navController.popBackStack() },
                onOpen = { newId -> navController.navigate(Routes.detail(newId)) },
            )
        }
        composable(
            route = Routes.TOPIC,
            arguments = listOf(navArgument("topicKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("topicKey") ?: return@composable
            TopicDetailScreen(
                topicKey = key,
                onBack = { navController.popBackStack() },
                onScreenshotOpen = { id -> navController.navigate(Routes.detail(id)) },
                onTopicOpen = { newKey -> navController.navigate(Routes.topic(newKey)) },
            )
        }
    }
}

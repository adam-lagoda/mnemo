package com.mnemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mnemo.ui.navigation.MnemoNavGraph
import com.mnemo.ui.navigation.Routes
import com.mnemo.ui.theme.Background
import com.mnemo.ui.theme.MnemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MnemoTheme {
                MnemoScaffold()
            }
        }
    }
}

@Composable
private fun MnemoScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val topLevelRoutes = listOf(Routes.GALLERY, Routes.SEARCH, Routes.GRAPH)
    val showBottomBar = topLevelRoutes.any { currentRoute?.startsWith(it.substringBefore('/')) == true }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Background),
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Background) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.GALLERY,
                        onClick = { navController.navigate(Routes.GALLERY) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.GridView, null) },
                        label = { Text("Gallery") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SEARCH,
                        onClick = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.GRAPH,
                        onClick = { navController.navigate(Routes.GRAPH) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Hub, null) },
                        label = { Text("Graph") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MnemoNavGraph(navController = navController)
        }
    }
}

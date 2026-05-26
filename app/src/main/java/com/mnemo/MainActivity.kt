package com.mnemo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mnemo.ui.navigation.MnemoNavGraph
import com.mnemo.ui.navigation.Routes
import com.mnemo.ui.theme.*

class MainActivity : ComponentActivity() {

    private val requestMediaPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored — MediaStore queries will return results if granted */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMediaPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            MnemoTheme {
                MnemoScaffold()
            }
        }
    }

    private fun requestMediaPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestMediaPermission.launch(permission)
    }
}

@Composable
private fun MnemoScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val topLevelRoutes = listOf(Routes.GALLERY, Routes.GRAPH, Routes.MODEL, Routes.PROFILE)
    val showBottomBar = topLevelRoutes.any { currentRoute?.startsWith(it.substringBefore('/')) == true }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Background),
        containerColor = Background,
        // Floating pill lives in bottomBar so Scaffold handles content bottom padding automatically
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    MnemoBottomNav(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) { launchSingleTop = true }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()) {
            MnemoNavGraph(
                navController = navController,
                onSearchClick = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Floating pill bottom nav
// ─────────────────────────────────────────────────────────────

private data class NavEntry(
    val route: String,
    val label: String,
    val isGraphTab: Boolean = false,
    val drawIcon: (@Composable (color: Color) -> Unit)? = null,
)

private val navEntries = listOf(
    NavEntry(Routes.GALLERY, "Gallery") { c -> GalleryIcon(c) },
    NavEntry(Routes.GRAPH,   "Graph",  isGraphTab = true),
    NavEntry(Routes.MODEL,   "Model")   { c -> ModelIcon(c) },
    NavEntry(Routes.PROFILE, "Profile") { c -> ProfileIcon(c) },
)

@Composable
private fun MnemoBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xBF0C0C0C))  // rgba(12,12,12,0.75)
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(999.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        navEntries.forEach { entry ->
            val isActive = currentRoute?.startsWith(entry.route.substringBefore('/')) == true
            val iconColor = if (isActive) Accent else Color(0x8CFDFDFD)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Tab) { onNavigate(entry.route) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                // Active indicator dot
                Box(Modifier.size(4.dp)) {
                    if (isActive) {
                        Box(Modifier.fillMaxSize().background(Accent, CircleShape))
                    }
                }

                // Icon
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    if (entry.isGraphTab) {
                        MnemoMark(color = iconColor, size = 14.dp)
                    } else {
                        entry.drawIcon?.invoke(iconColor)
                    }
                }

                // Label
                Text(
                    entry.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                    color = if (isActive) Accent else Color(0x72FDFDFD),
                )
            }
        }
    }
}

// ── Nav icons (drawn with Canvas)

@Composable
private fun GalleryIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = size.width / 24f
        listOf(3f to 3f, 14f to 3f, 3f to 14f, 14f to 14f).forEach { (x, y) ->
            drawRoundRect(
                color = color,
                topLeft = Offset(x * s, y * s),
                size = Size(7f * s, 7f * s),
                cornerRadius = CornerRadius(1f * s),
            )
        }
    }
}

@Composable
private fun ModelIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = size.width / 24f
        val cx = 12f * s; val cy = 12f * s
        drawCircle(
            color = color,
            radius = 9f * s,
            center = Offset(cx, cy),
            style = Stroke(width =1.6f * s),
        )
        drawCircle(color = color, radius = 3f * s, center = Offset(cx, cy))
    }
}

@Composable
private fun ProfileIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = size.width / 24f
        drawCircle(
            color = color,
            radius = 4f * s,
            center = Offset(12f * s, 8f * s),
            style = Stroke(width =1.6f * s),
        )
        drawPath(
            path = Path().apply {
                moveTo(4f * s, 21f * s)
                cubicTo(4f * s, 17f * s, 7.6f * s, 14f * s, 12f * s, 14f * s)
                cubicTo(16.4f * s, 14f * s, 20f * s, 17f * s, 20f * s, 21f * s)
            },
            color = color,
            style = Stroke(width =1.6f * s, cap = StrokeCap.Round),
        )
    }
}

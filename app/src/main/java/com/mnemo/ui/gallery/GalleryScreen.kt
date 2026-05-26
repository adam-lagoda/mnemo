package com.mnemo.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.ui.theme.Background
import com.mnemo.ui.theme.Outline
import com.mnemo.ui.theme.communityColor

@Composable
fun GalleryScreen(
    onScreenshotClick: (String) -> Unit,
    vm: GalleryViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        if (state.screenshots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No indexed screenshots yet", style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(state.screenshots, key = { it.id }) { screenshot ->
                ScreenshotTile(screenshot = screenshot, onClick = { onScreenshotClick(screenshot.id) })
            }
        }
    }
}

@Composable
private fun ScreenshotTile(screenshot: ScreenshotEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(com.mnemo.ui.theme.SurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(screenshot.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Community color indicator
        if (screenshot.communityId >= 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(communityColor(screenshot.communityId))
            )
        }
        // Unreviewed badge
        if (!screenshot.reviewed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(6.dp)
                    .background(
                        com.mnemo.ui.theme.Accent,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

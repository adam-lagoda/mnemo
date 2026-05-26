package com.mnemo.ui.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import com.mnemo.ui.theme.*

@Composable
fun GalleryScreen(
    onScreenshotClick: (String) -> Unit,
    vm: GalleryViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isSelectionMode) {
                Text(
                    "${state.selectedIds.size} selected",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::exitSelectionMode) {
                    Text("Cancel", color = Accent)
                }
            } else {
                Text(
                    "Mnemo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface
                )
                Spacer(Modifier.weight(1f))
                if (state.pendingCount > 0) {
                    Surface(
                        color = SurfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "${state.pendingCount} not indexed",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }
        if (state.screenshots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "No indexed screenshots yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        "Go to Profile → Indexing Settings to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            return@Column
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(state.screenshots, key = { it.id }) { screenshot ->
                    ScreenshotTile(
                        screenshot = screenshot,
                        isSelectionMode = state.isSelectionMode,
                        isSelected = screenshot.id in state.selectedIds,
                        onClick = {
                            if (state.isSelectionMode) vm.toggleSelection(screenshot.id)
                            else onScreenshotClick(screenshot.id)
                        },
                        onLongClick = { vm.enterSelectionMode(screenshot.id) }
                    )
                }
            }

            if (state.isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = SurfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    Box(Modifier.padding(16.dp)) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            enabled = state.selectedIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Error)
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val count = state.selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove screenshots") },
            text = {
                Text("Remove $count ${if (count == 1) "screenshot" else "screenshots"} from Mnemo? Files stay on your device.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.deleteSelected()
                }) { Text("Remove", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenshotTile(
    screenshot: ScreenshotEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(SurfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
        if (screenshot.communityId >= 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(communityColor(screenshot.communityId))
            )
        }
        if (!screenshot.reviewed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(6.dp)
                    .background(Accent, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Accent,
                    uncheckedColor = OnSurfaceVariant,
                    checkmarkColor = Background
                )
            )
        }
    }
}

package com.lagoda.mnemo.ui.gallery

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagoda.mnemo.data.db.entities.ScreenshotEntity
import com.lagoda.mnemo.ui.theme.*

@Composable
fun GalleryScreen(
    onScreenshotClick: (String) -> Unit,
    onPendingClick: () -> Unit,
    onSearchClick: () -> Unit,
    vm: GalleryViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // ── Sticky header
        GalleryHeader(
            isSelectionMode = state.isSelectionMode,
            selectedCount = state.selectedIds.size,
            totalCount = state.allScreenshots.size,
            pendingCount = state.pendingCount,
            isIndexingActive = state.isIndexingActive,
            onSelectAll = vm::selectAll,
            onCancelSelection = vm::exitSelectionMode,
            onPendingClick = onPendingClick,
            onSearchClick = onSearchClick,
        )

        // ── Filter pills
        if (!state.isSelectionMode) {
            FilterPillRow(
                filter = state.filter,
                totalCount = state.allScreenshots.size,
                tags = state.availableTags,
                tagCounts = state.tagCounts,
                onFilter = vm::setFilter,
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 1.5.dp)
            }
            return@Column
        }
        if (state.screenshots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "No screenshots here",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextTertiary,
                    )
                    Text(
                        "Try a different filter",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
            return@Column
        }

        // ── Grid
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 1.dp, top = 1.dp, end = 1.dp, bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
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
                        onLongClick = { vm.enterSelectionMode(screenshot.id) },
                    )
                }
            }

            // Selection mode action bar
            if (state.isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(SurfaceVariant),
                ) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            enabled = state.selectedIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Error),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Remove ${state.selectedIds.size} selected")
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
            containerColor = SurfaceVariant,
            title = { Text("Remove screenshots", color = TextPrimary) },
            text = {
                Text(
                    "Remove $count ${if (count == 1) "screenshot" else "screenshots"} from Mnemo? Files stay on your device.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.deleteSelected()
                }) { Text("Remove", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────

@Composable
private fun GalleryHeader(
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    pendingCount: Int,
    isIndexingActive: Boolean,
    onSelectAll: () -> Unit,
    onCancelSelection: () -> Unit,
    onPendingClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD9050505))  // 85% opaque
            .border(
                width = 0.5.dp,
                color = Color(0x0DFFFFFF),  // 5% white
                shape = RoundedCornerShape(0.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Text(
                    "$selectedCount selected",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSelectAll) {
                    Text("Select all", color = Accent, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onCancelSelection) {
                    Text("Cancel", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                // Brand lockup
                MnemoMark(color = Accent, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "MNEMO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp,
                    ),
                    color = Background,
                )
                Spacer(Modifier.weight(1f))

                // Status badge — only animate when worker is actually running
                when {
                    isIndexingActive -> Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable(onClick = onPendingClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        PulsingDot(color = Accent, size = 5.dp)
                        Text(
                            "INDEXING · $pendingCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                        )
                    }
                    pendingCount > 0 -> Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable(onClick = onPendingClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(Modifier.size(5.dp).background(Color(0x8CFDFDFD), CircleShape))
                        Text(
                            "$pendingCount pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0x8CFDFDFD),
                        )
                    }
                    else -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Box(Modifier.size(5.dp).background(Accent, CircleShape))
                        Text(
                            "LIVE · $totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0x8CFDFDFD),
                        )
                    }
                }

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0x8CFDFDFD),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Filter pill row
// ─────────────────────────────────────────────────────────────

@Composable
private fun FilterPillRow(
    filter: String?,
    totalCount: Int,
    tags: List<String>,
    tagCounts: Map<String, Int>,
    onFilter: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterPill(
            label = "All",
            count = totalCount,
            active = filter == null,
            dotColor = null,
            onClick = { onFilter(null) },
        )
        tags.forEach { tag ->
            FilterPill(
                label = formatSourceType(tag),
                count = tagCounts[tag],
                active = filter == tag,
                dotColor = null,
                onClick = { onFilter(tag) },
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String?,
    count: Int?,
    active: Boolean,
    dotColor: Color?,
    onClick: () -> Unit,
) {
    val bgColor = if (active) Color(0x0FFFFFFF) else Color.Transparent
    val borderColor = if (active) Color(0x33FFFFFF) else Color(0x14FFFFFF)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (dotColor != null) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape)
                )
            }
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (active) TextPrimary else TextSecondary,
                )
            }
            if (count != null) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Screenshot tile
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenshotTile(
    screenshot: ScreenshotEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val communityColor = communityColor(screenshot.communityId)
    val showCommunityBar = screenshot.communityId >= 0

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(SurfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // Screenshot image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(screenshot.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1.0f to Color(0x8C000000),
                    )
                )
        )

        // Community accent bar — bottom edge, horizontal
        if (showCommunityBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(communityColor),
            )
        }

        // Unreviewed pulsing dot — top-right
        if (!screenshot.reviewed) {
            Box(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
            ) {
                PulsingDot(color = Accent, size = 6.dp)
            }
        }

        // Selection checkbox — top-left
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Accent,
                    uncheckedColor = TextTertiary,
                    checkmarkColor = Background,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Shared: pulsing indicator dot
// ─────────────────────────────────────────────────────────────

@Composable
fun PulsingDot(color: Color, size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )
    Box(
        modifier = Modifier
            .size(size)
            .background(color.copy(alpha = alpha), CircleShape),
    )
}

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

private fun formatSourceType(type: String): String =
    type.replace('_', ' ')
        .split(' ')
        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }


package com.mnemo.ui.graph

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.graph.GraphNode
import com.mnemo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GraphScreen(
    onScreenshotOpen: (String) -> Unit,
    vm: GraphViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Mnemo",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.graphData.nodes.isEmpty() -> Text(
                        "No graph data yet — index some screenshots first",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    else -> ForceDirectedCanvas(
                        nodes = state.graphData.nodes,
                        edges = state.graphData.edges,
                        selectedNodeId = state.selectedNode?.id,
                        onNodeTap = vm::onNodeTap
                    )
                }
            }
        }

        state.selectedNode?.let { node ->
            NodePopup(
                node = node,
                neighborScreenshots = state.neighborScreenshots,
                neighborTopics = state.neighborTopics,
                onDismiss = vm::dismissPopup,
                onOpen = if (!node.isTopicNode) ({ onScreenshotOpen(node.id) }) else null,
                onScreenshotOpen = onScreenshotOpen,
                onTopicTap = vm::onNodeTap,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun NodePopup(
    node: GraphNode,
    neighborScreenshots: List<ScreenshotEntity>,
    neighborTopics: List<GraphNode>,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)?,
    onScreenshotOpen: (String) -> Unit,
    onTopicTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = SurfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (node.isTopicNode) Accent.copy(alpha = 0.15f)
                                    else BrandPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (node.isTopicNode) "TOPIC" else "SCREENSHOT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (node.isTopicNode) Accent else BrandPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (node.isTopicNode) {
                            Text(
                                "${node.screenshotCount} screenshot${if (node.screenshotCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        } else if (node.timestamp > 0L) {
                            Text(
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(node.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(node.label, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onOpen != null) {
                        IconButton(onClick = onOpen) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Accent)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Screenshot summary (non-topic nodes) ───────────────────────
            if (!node.isTopicNode && node.summary.isNotBlank()) {
                Text(
                    node.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            // ── Screenshot thumbnail strip (topic nodes) ───────────────────
            if (node.isTopicNode && neighborScreenshots.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(neighborScreenshots, key = { it.id }) { entity ->
                        ScreenshotThumb(entity = entity, onClick = { onScreenshotOpen(entity.id) })
                    }
                }
            }

            // ── Chips: topics/entities (screenshot nodes) or co-topics (topic nodes) ──
            val chips = when {
                node.isTopicNode -> neighborTopics.map { it.label }
                else -> (node.topics + node.entities).distinct().take(8)
            }
            if (chips.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(chips) { chip ->
                        val isNeighborTopic = node.isTopicNode
                        Surface(
                            color = if (isNeighborTopic) BrandPrimary.copy(alpha = 0.1f)
                                    else Accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = if (isNeighborTopic) Modifier.clickable {
                                onTopicTap("topic:${chip.lowercase().trim()}")
                            } else Modifier
                        ) {
                            Text(
                                chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNeighborTopic) BrandPrimary else Accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotThumb(entity: ScreenshotEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(entity.uri))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Community accent bar at bottom, matching gallery tile pattern
        if (entity.communityId >= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(communityColor(entity.communityId))
            )
        }
    }
}

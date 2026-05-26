package com.mnemo.ui.graph

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.prefs.AppConfig
import com.mnemo.graph.GraphNode
import com.mnemo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GraphScreen(
    onScreenshotOpen: (String) -> Unit,
    onTopicOpen: (String) -> Unit,
    vm: GraphViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "mnemo",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            // Date filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                items(AppConfig.DAY_FILTER_OPTIONS) { days ->
                    FilterChip(
                        selected = state.dateFilterDays == days,
                        onClick = { vm.setDateFilter(days) },
                        label = { Text(AppConfig.dayFilterLabel(days), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            selectedLabelColor = Background,
                        )
                    )
                }
            }

            // Topic filter chips — shown once graph has topic nodes
            if (state.availableTopics.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.topicFilter == null,
                            onClick = { vm.setTopicFilter(null) },
                            label = { Text("All topics", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent,
                                selectedLabelColor = Background,
                            )
                        )
                    }
                    items(state.availableTopics) { topic ->
                        FilterChip(
                            selected = state.topicFilter == topic,
                            onClick = { vm.setTopicFilter(if (state.topicFilter == topic) null else topic) },
                            label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent,
                                selectedLabelColor = Background,
                            )
                        )
                    }
                }
            }

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
                onDismiss = vm::dismissPopup,
                onOpen = if (node.isTopicNode) ({ onTopicOpen(node.label) })
                         else ({ onScreenshotOpen(node.id) }),
                onScreenshotOpen = onScreenshotOpen,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun NodePopup(
    node: GraphNode,
    neighborScreenshots: List<ScreenshotEntity>,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)?,
    onScreenshotOpen: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                            color = if (node.isTopicNode) Accent.copy(alpha = 0.2f) else Color(0xFF37474F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (node.isTopicNode) "TOPIC" else "SCREENSHOT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (node.isTopicNode) Accent else OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (node.isTopicNode) {
                            Text(
                                "${node.screenshotCount} screenshot${if (node.screenshotCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        } else if (node.timestamp > 0L) {
                            Text(
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(node.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(node.label, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onOpen != null) {
                        IconButton(onClick = onOpen) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint = Accent
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("✕", color = OnSurfaceVariant)
                    }
                }
            }

            if (node.summary.isNotBlank()) {
                Text(
                    node.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 2
                )
            }

            val chips = (node.topics + node.entities).distinct().take(8)
            if (chips.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(chips) { chip ->
                        Surface(
                            color = Accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = Accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


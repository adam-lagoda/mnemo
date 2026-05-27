package com.lagoda.mnemo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
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
import com.lagoda.mnemo.ui.theme.*

@Composable
fun DetailScreen(
    screenshotId: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit = {},
    vm: DetailViewModel = viewModel()
) {
    LaunchedEffect(screenshotId) {
        vm.load(screenshotId)
        vm.markReviewed()
    }
    val state by vm.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Error)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val screenshot = state.screenshot ?: return@Column
        val extraction = state.extraction

        // Image with community accent bar at bottom
        val cColor = communityColor(screenshot.communityId)
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(screenshot.uri)
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            )
            if (screenshot.communityId >= 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(cColor),
                )
            }
        }

        // Extracted data
        if (extraction != null) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(extraction.source_type.uppercase())
                    Chip(extraction.sentiment)
                    if (extraction.urgency > 0.5f) Chip("URGENT")
                }
                if (extraction.title.isNotBlank()) {
                    Text(extraction.title, style = SerifTitle, color = TextPrimary)
                }
                if (extraction.summary.isNotBlank()) {
                    Text(extraction.summary, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
                if (extraction.topics.isNotEmpty()) {
                    Section("Topics") { FlowRow(extraction.topics) }
                }
                if (extraction.entities.isNotEmpty()) {
                    Section("Entities") { FlowRow(extraction.entities) }
                }
                if (extraction.action_items.isNotEmpty()) {
                    Section("Action Items") {
                        extraction.action_items.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Related screenshots — same community, tappable
        if (state.related.isNotEmpty()) {
            Text(
                "Related",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.related, key = { it.id }) { related ->
                    val relatedColor = communityColor(related.communityId)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariant)
                            .clickable { onOpen(related.id) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(related.uri)
                                .crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (related.communityId >= 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(relatedColor)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove screenshot") },
            text = { Text("Remove this screenshot from Mnemo? The file stays on your device.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onBack)
                }) { Text("Remove", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Chip(label: String) {
    Surface(color = SurfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Accent
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        content()
    }
}

@Composable
private fun FlowRow(items: List<String>) {
    Column {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { item -> Chip(item) }
            }
        }
    }
}

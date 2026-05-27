package com.lagoda.mnemo.ui.topic

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagoda.mnemo.data.db.entities.ScreenshotEntity
import com.lagoda.mnemo.ui.theme.*

@Composable
fun TopicDetailScreen(
    topicKey: String,
    onBack: () -> Unit,
    onScreenshotOpen: (String) -> Unit,
    onTopicOpen: (String) -> Unit,
    vm: TopicDetailViewModel = viewModel(),
) {
    LaunchedEffect(topicKey) { vm.load(topicKey) }
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar — matches DetailScreen
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }

        // Header — topic name in the same SerifTitle as the screenshot title
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "TOPIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(topicKey, style = SerifTitle, color = TextPrimary)
            Text(
                "${state.screenshots.size} screenshot${if (state.screenshots.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Outline)
        Spacer(Modifier.height(16.dp))

        // Screenshots — same thumbnail row as DetailScreen's "Related" section
        if (state.screenshots.isNotEmpty()) {
            Text(
                "Screenshots",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(state.screenshots, key = { it.id }) { entity ->
                    ScreenshotTile(entity = entity, onClick = { onScreenshotOpen(entity.id) })
                }
            }
        }

        // Related topics — co-occurring topics as tappable chips
        if (state.relatedTopics.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Related topics",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(state.relatedTopics) { topic ->
                    Surface(
                        color = Accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onTopicOpen(topic) }
                    ) {
                        Text(
                            topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(88.dp)) // nav bar clearance
    }
}

@Composable
private fun ScreenshotTile(entity: ScreenshotEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(entity.uri))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (entity.communityId >= 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(communityColor(entity.communityId))
            )
        }
    }
}

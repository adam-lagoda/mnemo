package com.mnemo.ui.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mnemo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SearchScreen(
    onResultClick: (String) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            placeholder = {
                Text(
                    "Search by meaning…",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Accent) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Color(0x14FFFFFF),
                cursorColor = Accent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = Color(0x0AFFFFFF),
                unfocusedContainerColor = Color(0x0AFFFFFF),
            ),
        )

        when {
            state.isSearching -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            state.query.isBlank() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Type to search your screenshots", color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
            state.results.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No results for \"${state.query}\"", color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.results, key = { it.entity.id }) { result ->
                    SearchResultCard(result, onClick = { onResultClick(result.entity.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResult, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        color = SurfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(result.entity.uri))
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .background(Background, RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        result.sourceType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent
                    )
                    Text(
                        SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(result.entity.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    result.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    maxLines = 2
                )
                if (result.summary.isNotBlank()) {
                    Text(
                        result.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 2
                    )
                }
                val chips = (result.topics + result.entities).distinct().take(5)
                if (chips.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(chips) { chip ->
                            Surface(
                                color = Accent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    chip,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

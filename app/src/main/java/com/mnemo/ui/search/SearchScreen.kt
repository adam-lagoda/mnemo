package com.mnemo.ui.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
fun SearchScreen(
    onResultClick: (String) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text("Search screenshots…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Outline,
                cursorColor = Accent
            )
        )
        when {
            state.isSearching -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.results.isEmpty() && state.query.isNotBlank() -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) { Text("No results for \"${state.query}\"") }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.results, key = { it.id }) { screenshot ->
                    SearchResultRow(screenshot, onClick = { onResultClick(screenshot.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(screenshot: ScreenshotEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(screenshot.uri)
                .crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).background(SurfaceVariant)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = screenshot.sourceType.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Accent
            )
            val summary = screenshot.extractedJson?.let { jsonStr ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<com.mnemo.data.model.ExtractionResult>(jsonStr).title
                } catch (e: Exception) { null }
            } ?: "Unindexed"
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}

package com.mnemo.ui.indexing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
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
import com.mnemo.data.model.ScreenshotCandidate
import com.mnemo.data.prefs.AppConfig
import com.mnemo.ui.theme.*

@Composable
fun IndexingScreen(
    onBack: () -> Unit,
    onScreenshotClick: (String) -> Unit = {},
    vm: IndexingViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { vm.setTreeUri(it) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Text("Indexing", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        }

        // Folder row
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            color = SurfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.hasFolderSelected) state.folderDisplayName else "Tap to select screenshot folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.hasFolderSelected) OnSurface else OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Text(if (state.hasFolderSelected) "Change" else "Select", color = Accent)
                }
            }
        }

        if (!state.hasFolderSelected) return@Column

        // Day filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppConfig.DAY_FILTER_OPTIONS.forEach { days ->
                FilterChip(
                    selected = state.dayFilter == days,
                    onClick = { vm.setDayFilter(days) },
                    label = { Text(AppConfig.dayFilterLabel(days)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent,
                        selectedLabelColor = Background
                    )
                )
            }
        }

        // Progress bar
        val progress = if (state.totalCount > 0) state.indexedCount.toFloat() / state.totalCount else 0f
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Accent,
                trackColor = SurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.indexedCount} / ${state.totalCount} indexed",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Background,
            contentColor = Accent,
            divider = { HorizontalDivider(color = Outline) }
        ) {
            val pendingCount = state.pendingNew.size + state.pendingQueued.size
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(
                    "Pending ($pendingCount)",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(
                    "Indexed (${state.indexedCount})",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (state.isScanning) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }

        // Grid
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> PendingGrid(
                    pendingNew = state.pendingNew,
                    pendingQueued = state.pendingQueued,
                    selectedUris = state.selectedUris,
                    inFlightUri = state.inFlightUri,
                    onToggle = vm::toggleSelection
                )
                1 -> IndexedGrid(
                    indexed = state.indexed,
                    uriToDbId = state.uriToDbId,
                    onScreenshotClick = onScreenshotClick,
                )
            }
        }

        // Progress card — shown while extraction is running
        state.progress?.let { progress ->
            ExtractionProgressCard(progress = progress)
        }

        // Bottom action bar (Pending tab only, hidden while extracting)
        if (selectedTab == 0 && state.progress == null &&
            (state.pendingNew.isNotEmpty() || state.selectedCount > 0)) {
            Surface(color = Surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (state.selectedCount == state.pendingNew.size) vm.deselectAll() else vm.selectAll() }
                    ) {
                        Text(
                            if (state.selectedCount == state.pendingNew.size && state.pendingNew.isNotEmpty())
                                "Deselect All" else "Select All",
                            color = OnSurfaceVariant
                        )
                    }
                    Button(
                        onClick = vm::indexSelected,
                        enabled = state.selectedCount > 0 && !state.isIndexing,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)
                    ) {
                        Text("Index ${if (state.selectedCount > 0) state.selectedCount else ""} selected".trim())
                    }
                }
            }
        }
    }
}

private fun formatEta(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s left" else "${s}s left"
}

@Composable
private fun ExtractionProgressCard(progress: ExtractionProgress) {
    val pulseTransition = rememberInfiniteTransition(label = "step_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "current_bar_alpha",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = SurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!progress.isPreparing && progress.uri.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(progress.uri))
                            .crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Background, RoundedCornerShape(6.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (!progress.isPreparing) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Image ${progress.itemIndex} of ${progress.itemTotal}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                            if (progress.remainingSeconds >= 0) {
                                Text(
                                    formatEta(progress.remainingSeconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        progress.stepLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Accent,
                    strokeWidth = 2.dp
                )
            }

            // Step bars — only shown once worker is actually running
            if (!progress.isPreparing) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val stepLabels = listOf("Load", "Extract", "Save")
                    stepLabels.forEachIndexed { idx, label ->
                        val stepNum = idx + 1
                        val isDone = stepNum < progress.stepNum
                        val isCurrent = stepNum == progress.stepNum
                        val barColor = when {
                            isDone    -> Accent.copy(alpha = 0.35f)
                            isCurrent -> Accent.copy(alpha = pulseAlpha)
                            else      -> Color(0x14FFFFFF)
                        }
                        val labelColor = when {
                            isCurrent -> TextPrimary
                            isDone    -> OnSurfaceVariant.copy(alpha = 0.5f)
                            else      -> OnSurfaceVariant.copy(alpha = 0.3f)
                        }
                        Surface(
                            color = barColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(4.dp).weight(1f)
                        ) {}
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                        )
                        if (idx < stepLabels.lastIndex) Spacer(Modifier.width(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingGrid(
    pendingNew: List<ScreenshotCandidate>,
    pendingQueued: List<ScreenshotCandidate>,
    selectedUris: Set<Uri>,
    inFlightUri: String?,
    onToggle: (Uri) -> Unit
) {
    val all = pendingQueued + pendingNew
    if (all.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("All screenshots indexed", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Queued items first (spinners, not selectable)
        items(pendingQueued, key = { "q_${it.id}" }) { candidate ->
            val isInFlight = candidate.uri.toString() == inFlightUri
            CandidateTile(
                candidate = candidate,
                isSelected = false,
                isQueued = true,
                isInFlight = isInFlight,
                onClick = {}
            )
        }
        // New items (selectable with checkboxes)
        items(pendingNew, key = { "n_${it.id}" }) { candidate ->
            CandidateTile(
                candidate = candidate,
                isSelected = candidate.uri in selectedUris,
                isQueued = false,
                isInFlight = false,
                onClick = { onToggle(candidate.uri) }
            )
        }
    }
}

@Composable
private fun IndexedGrid(
    indexed: List<ScreenshotCandidate>,
    uriToDbId: Map<String, String>,
    onScreenshotClick: (String) -> Unit,
) {
    if (indexed.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No indexed screenshots yet", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(indexed, key = { it.id }) { candidate ->
            val dbId = uriToDbId[candidate.uri.toString()]
            CandidateTile(
                candidate = candidate,
                isSelected = false,
                isQueued = false,
                isInFlight = false,
                showCheckmark = true,
                onClick = { if (dbId != null) onScreenshotClick(dbId) }
            )
        }
    }
}

@Composable
private fun CandidateTile(
    candidate: ScreenshotCandidate,
    isSelected: Boolean,
    isQueued: Boolean,
    isInFlight: Boolean,
    showCheckmark: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(SurfaceVariant)
            .clickable(enabled = !isQueued, onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, Accent)
                else Modifier
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(candidate.uri)
                .crossfade(true)
                .build(),
            contentDescription = candidate.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dim overlay for queued
        if (isQueued) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }
        // Spinner for in-flight
        if (isInFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).align(Alignment.Center),
                color = Accent,
                strokeWidth = 2.dp
            )
        }
        // Checkbox overlay for new items
        if (!isQueued && !showCheckmark) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(
                        if (isSelected) Accent else Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Background,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // Checkmark for indexed
        if (showCheckmark) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
            }
        }
    }
}

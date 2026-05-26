package com.mnemo.ui.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnemo.model.ModelDownloadState
import com.mnemo.model.ModelId
import com.mnemo.model.ModelSpec
import com.mnemo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SetupScreen(vm: SetupViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { vm.setTreeUri(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Setup", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            Spacer(Modifier.height(4.dp))
        }

        // ── Models ───────────────────────────────────────────────
        item {
            Text(
                "Models",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            GemmaModelCard(
                state = state.modelStates[ModelId.GEMMA] ?: ModelDownloadState.Absent,
                savedToken = state.savedHfToken,
                onDownload = { token -> vm.downloadModel(ModelId.GEMMA, token) },
                onCancel = { vm.cancelDownload(ModelId.GEMMA) },
                onRetry = { vm.retryDownload(ModelId.GEMMA) },
                onVerify = { vm.verifyModel(ModelId.GEMMA) }
            )
        }

        item {
            SimpleModelCard(
                spec = ModelSpec.ALL[ModelId.GTE_SMALL]!!,
                state = state.modelStates[ModelId.GTE_SMALL] ?: ModelDownloadState.Absent,
                onDownload = { vm.downloadModel(ModelId.GTE_SMALL) },
                onCancel = { vm.cancelDownload(ModelId.GTE_SMALL) },
                onRetry = { vm.retryDownload(ModelId.GTE_SMALL) },
                onVerify = { vm.verifyModel(ModelId.GTE_SMALL) }
            )
        }

        // ── Screenshot Folder ─────────────────────────────────────
        item {
            Text(
                "Screenshot Folder",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        item {
            Surface(
                color = SurfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.hasFolderSelected) state.folderDisplayName
                               else "Tap to select screenshot folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.hasFolderSelected) OnSurface else OnSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Text(if (state.hasFolderSelected) "Change" else "Select", color = Accent)
                    }
                }
            }
        }

        // ── Indexing Status ───────────────────────────────────────
        item {
            Text(
                "Indexing",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        item {
            Surface(
                color = SurfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${state.indexedCount} / ${state.candidateCount} indexed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface
                        )
                        val lastAt = state.lastIndexedAt
                        Text(
                            text = if (lastAt != null) {
                                "Last: ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(lastAt))}"
                            } else "Never indexed",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    TextButton(onClick = vm::reIndexNow) {
                        Text("Re-index", color = Accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun GemmaModelCard(
    state: ModelDownloadState,
    savedToken: String?,
    onDownload: (String?) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onVerify: () -> Unit
) {
    var showTokenInput by remember(state) { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(savedToken ?: "") }

    ModelCard(
        name = "Gemma 3n E2B",
        sizeLabel = "3.14 GB",
        state = state,
        onCancel = onCancel,
        onRetry = onRetry,
        onVerify = onVerify,
        downloadButton = {
            when (state) {
                is ModelDownloadState.Absent, is ModelDownloadState.Failed -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTokenInput = !showTokenInput }) {
                            Text(
                                if (showTokenInput) "Hide token" else "Add token",
                                color = OnSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onDownload(tokenInput.takeIf { it.isNotBlank() }) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Background
                            )
                        ) { Text("Download") }
                    }
                }
                else -> {}
            }
        },
        extraContent = {
            AnimatedVisibility(visible = showTokenInput) {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = {
                        Text(
                            "HuggingFace token (optional, faster downloads)",
                            color = OnSurfaceVariant
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface
                    )
                )
            }
        }
    )
}

@Composable
private fun SimpleModelCard(
    spec: ModelSpec,
    state: ModelDownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onVerify: () -> Unit
) {
    ModelCard(
        name = spec.name,
        sizeLabel = "34 MB",
        state = state,
        onCancel = onCancel,
        onRetry = onRetry,
        onVerify = onVerify,
        downloadButton = {
            when (state) {
                is ModelDownloadState.Absent, is ModelDownloadState.Failed -> {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Background
                        )
                    ) { Text("Download") }
                }
                else -> {}
            }
        }
    )
}

@Composable
private fun ModelCard(
    name: String,
    sizeLabel: String,
    state: ModelDownloadState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onVerify: () -> Unit,
    downloadButton: @Composable () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    val isDownloaded = state is ModelDownloadState.Ready
        || state is ModelDownloadState.Verifying
        || state is ModelDownloadState.Verified
    val green = Color(0xFF4CAF50)

    Surface(
        color = SurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    // Only show gray size while not yet downloaded
                    if (!isDownloaded) {
                        Text(sizeLabel, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                }
                when (state) {
                    is ModelDownloadState.Absent -> downloadButton()
                    is ModelDownloadState.Downloading -> {
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                        ) { Text("Cancel") }
                    }
                    is ModelDownloadState.Validating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Accent,
                            strokeWidth = 2.dp
                        )
                    }
                    is ModelDownloadState.Ready -> {
                        // Tapping the checkmark loads the model into memory to verify it
                        IconButton(onClick = onVerify, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Tap to verify model loads",
                                tint = green,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    is ModelDownloadState.Verifying -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = green,
                            strokeWidth = 2.dp
                        )
                    }
                    is ModelDownloadState.Verified -> {
                        Icon(
                            Icons.Default.TaskAlt,
                            contentDescription = "Verified",
                            tint = green,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    is ModelDownloadState.Failed -> downloadButton()
                }
            }
            if (state is ModelDownloadState.Downloading) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Accent,
                        trackColor = Outline
                    )
                    Text(
                        "${(state.progress * 100).toInt()}%",

                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            if (state is ModelDownloadState.Validating) {
                Text("Verifying…", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            if (state is ModelDownloadState.Failed) {
                Text(state.reason, style = MaterialTheme.typography.bodySmall, color = Error)
            }
            // Show size once in green when downloaded; verified state also shows "Loaded"
            if (isDownloaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(sizeLabel, style = MaterialTheme.typography.bodySmall, color = green)
                    if (state is ModelDownloadState.Verified) {
                        Text("· Loaded", style = MaterialTheme.typography.bodySmall, color = green)
                    }
                }
            }
            extraContent()
        }
    }
}

package com.mnemo.ui.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnemo.ui.theme.*

@Composable
fun ModelScreen(vm: ModelViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Mnemo",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
            )
            if (state.messages.isNotEmpty()) {
                IconButton(onClick = vm::clearChat) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear chat",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (!state.modelReady) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Gemma model not downloaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )
                    Text(
                        "Go to Profile → Setup to download it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
        } else {
            if (state.messages.isEmpty() && !state.isStreaming) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ask Gemma anything…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(state.messages) { index, message ->
                        MessageBubble(
                            message = message,
                            isStreamingLast = state.isStreaming && index == state.messages.lastIndex,
                        )
                    }
                }
            }

            if (state.error != null) {
                Text(
                    state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            state.tokensPerSecond?.let { tps ->
                Text(
                    "%.1f tok/s".format(tps),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isStreaming) AccentDim else OnSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = vm::onPromptChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type your question…", color = OnSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { vm.ask() }),
                    minLines = 1,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                    ),
                )
                IconButton(
                    onClick = vm::ask,
                    enabled = state.prompt.isNotBlank() && !state.isStreaming,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Ask",
                        tint = if (state.prompt.isNotBlank() && !state.isStreaming) Accent else OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isStreamingLast: Boolean) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) Accent else SurfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.text.ifEmpty { if (isStreamingLast) " " else "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Background else OnSurface,
                )
                if (isStreamingLast) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

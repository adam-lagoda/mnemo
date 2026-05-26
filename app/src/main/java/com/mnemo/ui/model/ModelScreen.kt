package com.mnemo.ui.model

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mnemo.ui.theme.*
import com.mnemo.ui.theme.MnemoMark

@Composable
fun ModelScreen(vm: ModelViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isRetrieving) {
        val target = state.messages.size + if (state.isRetrieving) 1 else 0
        if (target > 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MnemoMark(color = Accent, size = 20.dp)
                Text("Ask Mnemo", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            }
            if (state.messages.isNotEmpty()) {
                IconButton(onClick = vm::clearChat) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear chat", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (!state.modelReady) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gemma model not downloaded", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    Text("Go to Profile → Setup to download it.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        } else {
            // Message list
            if (state.messages.isEmpty() && !state.isStreaming && !state.isRetrieving) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MnemoMark(color = TextTertiary, size = 32.dp)
                        Text(
                            if (state.isRagMode) "Ask about your screenshots" else "Ask anything",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(state.messages) { index, message ->
                        MessageBubble(
                            message = message,
                            isStreamingLast = state.isStreaming && index == state.messages.lastIndex,
                        )
                    }
                    if (state.isRetrieving) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Accent)
                                Spacer(Modifier.width(8.dp))
                                Text("Searching screenshots…", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                        }
                    }
                }
            }

            // Error + tps
            if (state.error != null) {
                Text(state.error!!, style = MaterialTheme.typography.bodySmall, color = Error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            state.tokensPerSecond?.let { tps ->
                Text("%.1f tok/s".format(tps), style = MaterialTheme.typography.labelSmall, color = if (state.isStreaming) AccentDim else OnSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 2.dp))
            }

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // RAG toggle chip
                val ragBorder = if (state.isRagMode) Accent else TextMuted.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.5.dp, ragBorder, RoundedCornerShape(8.dp))
                        .background(if (state.isRagMode) Accent.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { vm.toggleRagMode() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "RAG",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isRagMode) Accent else TextMuted,
                    )
                }

                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = vm::onPromptChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Type your question…", color = TextMuted)
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { vm.ask() }),
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent,
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                    ),
                )
                IconButton(
                    onClick = vm::ask,
                    enabled = state.prompt.isNotBlank() && !state.isStreaming && !state.isRetrieving,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Ask",
                        tint = if (state.prompt.isNotBlank() && !state.isStreaming && !state.isRetrieving) Accent else TextMuted,
                    )
                }
            }
        }
    }
}

private fun parseMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val src = text.trimEnd().replace(Regex("^[*-] ", RegexOption.MULTILINE), "• ")
    var i = 0
    while (i < src.length) {
        when {
            src.startsWith("**", i) -> {
                val end = src.indexOf("**", i + 2)
                if (end != -1) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(src.substring(i + 2, end)) }; i = end + 2 }
                else { append(src[i]); i++ }
            }
            src.startsWith("*", i) -> {
                val end = src.indexOf("*", i + 1)
                if (end != -1) { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(src.substring(i + 1, end)) }; i = end + 1 }
                else { append(src[i]); i++ }
            }
            src.startsWith("_", i) -> {
                val end = src.indexOf("_", i + 1)
                if (end != -1) { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(src.substring(i + 1, end)) }; i = end + 1 }
                else { append(src[i]); i++ }
            }
            src.startsWith("`", i) -> {
                val end = src.indexOf("`", i + 1)
                if (end != -1) { withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(src.substring(i + 1, end)) }; i = end + 1 }
                else { append(src[i]); i++ }
            }
            else -> { append(src[i]); i++ }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isStreamingLast: Boolean) {
    val isUser = message.role == "user"
    Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = if (isUser) {
                            AnnotatedString(message.text.trimEnd().ifEmpty { if (isStreamingLast) " " else "" })
                        } else {
                            parseMarkdown(message.text.trimEnd().ifEmpty { if (isStreamingLast) " " else "" })
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Background else OnSurface,
                    )
                    if (isStreamingLast) {
                        Spacer(Modifier.width(6.dp))
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = OnSurfaceVariant)
                    }
                }
            }
        }

        // Sources row — shown below model messages that used RAG
        if (!isUser && message.sources.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Sources", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(message.sources, key = { it.entity.id }) { src ->
                    Column(
                        modifier = Modifier.width(64.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Uri.parse(src.entity.uri))
                                .crossfade(true).build(),
                            contentDescription = src.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariant),
                        )
                        Text(src.title, style = MaterialTheme.typography.labelSmall, color = TextTertiary, maxLines = 2)
                    }
                }
            }
        }
    }
}

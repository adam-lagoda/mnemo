package com.mnemo.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.di.AppModule
import com.mnemo.extraction.GemmaExtractor
import com.mnemo.model.ModelSpec
import com.mnemo.model.ModelId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

data class ChatMessage(val role: String, val text: String)

data class ModelUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val tokensPerSecond: Float? = null,
    val modelReady: Boolean = false,
    val error: String? = null,
)

class ModelViewModel(app: Application) : AndroidViewModel(app) {
    private val gemmaExtractor =
        AppModule.getInstance(app).vlmExtractor as? GemmaExtractor

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        checkModelReady()
    }

    private fun checkModelReady() {
        val spec = ModelSpec.ALL[ModelId.GEMMA] ?: return
        val file = File(getApplication<Application>().filesDir, spec.filename)
        _uiState.value = _uiState.value.copy(
            modelReady = file.exists() && file.length() > 0
        )
    }

    fun onPromptChange(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text, error = null)
    }

    fun ask() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank() || _uiState.value.isStreaming) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages +
                    ChatMessage("user", prompt) +
                    ChatMessage("model", ""),
            prompt = "",
            isStreaming = true,
            error = null,
        )

        viewModelScope.launch {
            var tokenCount = 0
            var startMs = 0L
            try {
                val stream = gemmaExtractor?.generateStream(prompt)
                if (stream == null) {
                    updateLastMessage("No model loaded.")
                } else {
                    stream.collect { token ->
                        if (tokenCount == 0) startMs = System.currentTimeMillis()
                        tokenCount++
                        val elapsedSec = max(1L, System.currentTimeMillis() - startMs) / 1000f
                        val msgs = _uiState.value.messages.toMutableList()
                        val last = msgs.last()
                        msgs[msgs.lastIndex] = last.copy(text = last.text + token)
                        _uiState.value = _uiState.value.copy(
                            messages = msgs,
                            tokensPerSecond = tokenCount / elapsedSec,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: e.javaClass.simpleName
                )
            } finally {
                _uiState.value = _uiState.value.copy(isStreaming = false)
            }
        }
    }

    fun clearChat() {
        gemmaExtractor?.clearChatHistory()
        _uiState.value = _uiState.value.copy(messages = emptyList(), error = null, tokensPerSecond = null)
    }

    private fun updateLastMessage(text: String) {
        val msgs = _uiState.value.messages.toMutableList()
        if (msgs.isNotEmpty()) msgs[msgs.lastIndex] = msgs.last().copy(text = text)
        _uiState.value = _uiState.value.copy(messages = msgs)
    }

    override fun onCleared() {
        super.onCleared()
        gemmaExtractor?.clearChatHistory()
    }
}

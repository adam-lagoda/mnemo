package com.mnemo.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import com.mnemo.di.AppModule
import com.mnemo.embedding.toEmbeddingBlob
import com.mnemo.embedding.toFloatVector
import com.mnemo.extraction.GemmaExtractor
import com.mnemo.model.ModelSpec
import com.mnemo.model.ModelId
import com.mnemo.scheduling.ExtractionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class RagSource(
    val entity: ScreenshotEntity,
    val result: ExtractionResult?,
    val score: Float,
) {
    val title: String get() = result?.title?.takeIf { it.isNotBlank() } ?: "Untitled"
}

data class ChatMessage(
    val role: String,
    val text: String,
    val sources: List<RagSource> = emptyList(),
)

data class ModelUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val isRetrieving: Boolean = false,
    val tokensPerSecond: Float? = null,
    val modelReady: Boolean = false,
    val isRagMode: Boolean = false,
    val error: String? = null,
)

class ModelViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val gemmaExtractor = appModule.vlmExtractor as? GemmaExtractor
    private val embeddingEngine = appModule.embeddingEngine
    private val onnx = appModule.onnxEmbeddingEngine
    private val repo = appModule.screenshotRepository

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init { checkModelReady() }

    private fun checkModelReady() {
        val spec = ModelSpec.ALL[ModelId.GEMMA] ?: return
        val file = File(getApplication<Application>().filesDir, spec.filename)
        _uiState.value = _uiState.value.copy(modelReady = file.exists() && file.length() > 0)
    }

    fun onPromptChange(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text, error = null)
    }

    fun toggleRagMode() {
        _uiState.value = _uiState.value.copy(isRagMode = !_uiState.value.isRagMode)
    }

    fun ask() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank() || _uiState.value.isStreaming || _uiState.value.isRetrieving) return
        if (_uiState.value.isRagMode) askWithRag(prompt) else askDirect(prompt)
    }

    private fun askDirect(prompt: String) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages +
                    ChatMessage("user", prompt) +
                    ChatMessage("model", ""),
            prompt = "",
            isStreaming = true,
            error = null,
        )
        viewModelScope.launch { streamResponse(prompt) }
    }

    private fun askWithRag(prompt: String) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage("user", prompt),
            prompt = "",
            isRetrieving = true,
            error = null,
        )
        viewModelScope.launch {
            val sources = retrieveRelevant(prompt)
            val contextPrompt = buildRagPrompt(prompt, sources)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + ChatMessage("model", "", sources),
                isRetrieving = false,
                isStreaming = true,
            )
            streamResponse(contextPrompt)
        }
    }

    private suspend fun streamResponse(prompt: String) {
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
            _uiState.value = _uiState.value.copy(error = e.message ?: e.javaClass.simpleName)
        } finally {
            _uiState.value = _uiState.value.copy(isStreaming = false)
        }
    }

    private suspend fun retrieveRelevant(query: String): List<RagSource> =
        withContext(Dispatchers.IO) {
            if (onnx.isReady) retrieveWithOnnx(query) else retrieveWithTfIdf(query)
        }

    private suspend fun retrieveWithOnnx(query: String): List<RagSource> {
        val queryVec = onnx.embed(query)
        if (queryVec.isEmpty()) return retrieveWithTfIdf(query)

        return repo.getEmbedded()
            .map { entity ->
                val docVec = entity.embeddingBlob!!.toFloatVector()
                val score = onnx.similarity(queryVec, docVec)
                Triple(entity, entity.extractedJson?.let { parseExtraction(it) }, score)
            }
            .filter { it.third > 0.1f }
            .sortedByDescending { it.third }
            .take(5)
            .map { (entity, result, score) -> RagSource(entity, result, score) }
    }

    private suspend fun retrieveWithTfIdf(query: String): List<RagSource> {
        val queryVec = embeddingEngine.embed(query)
        if (queryVec.isEmpty()) return emptyList()

        return repo.getAll()
            .filter { it.extractedJson != null }
            .map { entity ->
                val result = parseExtraction(entity.extractedJson!!)
                val docText = ExtractionWorker.buildDocText(result ?: ExtractionResult())
                val score = embeddingEngine.similarity(queryVec, embeddingEngine.embed(docText))
                Triple(entity, result, score)
            }
            .filter { it.third > 0.05f }
            .sortedByDescending { it.third }
            .take(5)
            .map { (entity, result, score) -> RagSource(entity, result, score) }
    }

    private fun buildRagPrompt(query: String, sources: List<RagSource>): String {
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return buildString {
            appendLine("You are a personal memory assistant. The user has indexed screenshots from their phone.")
            if (sources.isEmpty()) {
                appendLine("No relevant screenshots were found for this question.")
            } else {
                appendLine("Most relevant screenshots:")
                appendLine()
                sources.forEachIndexed { i, src ->
                    appendLine("[${i + 1}] ${dateFmt.format(Date(src.entity.timestamp))} — ${src.result?.source_type ?: "unknown"}")
                    appendLine("Title: ${src.title}")
                    src.result?.summary?.takeIf { it.isNotBlank() }?.let { appendLine("Summary: $it") }
                    src.result?.topics?.takeIf { it.isNotEmpty() }?.let { appendLine("Topics: ${it.joinToString(", ")}") }
                    src.result?.entities?.takeIf { it.isNotEmpty() }?.let { appendLine("Entities: ${it.joinToString(", ")}") }
                    appendLine("---")
                }
            }
            appendLine()
            appendLine("User question: $query")
        }
    }

    private fun parseExtraction(jsonStr: String): ExtractionResult? =
        try { json.decodeFromString<ExtractionResult>(jsonStr) } catch (_: Exception) { null }

    private fun updateLastMessage(text: String) {
        val msgs = _uiState.value.messages.toMutableList()
        if (msgs.isNotEmpty()) msgs[msgs.lastIndex] = msgs.last().copy(text = text)
        _uiState.value = _uiState.value.copy(messages = msgs)
    }

    fun clearChat() {
        gemmaExtractor?.clearChatHistory()
        _uiState.value = _uiState.value.copy(messages = emptyList(), error = null, tokensPerSecond = null)
    }

    override fun onCleared() {
        super.onCleared()
        gemmaExtractor?.clearChatHistory()
    }
}

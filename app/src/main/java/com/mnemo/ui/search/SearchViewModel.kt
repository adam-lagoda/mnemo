package com.mnemo.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import com.mnemo.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

data class SearchResult(
    val entity: ScreenshotEntity,
    val title: String,
    val summary: String,
    val topics: List<String>,
    val entities: List<String>,
    val sourceType: String,
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppModule.getInstance(app).screenshotRepository
    private val embeddingEngine = AppModule.getInstance(app).embeddingEngine
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _query = MutableStateFlow("")

    private val allParsed: Flow<List<SearchResult>> = repo.observeAll()
        .map { screenshots -> screenshots.mapNotNull { parse(it) } }

    val uiState: StateFlow<SearchUiState> = combine(
        _query.debounce(250).distinctUntilChanged(),
        allParsed,
    ) { q, parsed ->
        if (q.isBlank()) return@combine SearchUiState(query = q)

        val queryVec = embeddingEngine.embed(q)
        val results = if (queryVec.isEmpty()) {
            // Corpus not ready yet — fall back to keyword match
            val lower = q.lowercase()
            parsed.filter { r ->
                r.title.lowercase().contains(lower) ||
                r.summary.lowercase().contains(lower) ||
                r.topics.any { it.lowercase().contains(lower) } ||
                r.entities.any { it.lowercase().contains(lower) }
            }.sortedByDescending { it.entity.timestamp }
        } else {
            parsed
                .map { r ->
                    val docText = "${r.title} ${r.summary} ${r.topics.joinToString(" ")} ${r.entities.joinToString(" ")}"
                    r to embeddingEngine.similarity(queryVec, embeddingEngine.embed(docText))
                }
                .filter { it.second > 0.02f }
                .sortedByDescending { it.second }
                .map { it.first }
        }
        SearchUiState(query = q, results = results)
    }
    .flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(q: String) { _query.value = q }

    private fun parse(entity: ScreenshotEntity): SearchResult? {
        val r = entity.extractedJson?.let {
            try { json.decodeFromString<ExtractionResult>(it) } catch (_: Exception) { null }
        } ?: return null
        return SearchResult(
            entity = entity,
            title = r.title,
            summary = r.summary,
            topics = r.topics,
            entities = r.entities,
            sourceType = r.source_type,
        )
    }
}

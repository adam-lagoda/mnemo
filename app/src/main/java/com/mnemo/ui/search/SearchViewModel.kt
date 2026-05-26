package com.mnemo.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import com.mnemo.di.AppModule
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
    val sourceType: String
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppModule.getInstance(app).screenshotRepository
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _query = MutableStateFlow("")

    // All indexed screenshots as parsed results — rebuilt when DB changes
    private val allParsed: Flow<List<SearchResult>> = repo.observeAll()
        .map { screenshots -> screenshots.mapNotNull { parse(it) } }

    val uiState: StateFlow<SearchUiState> = combine(
        _query.debounce(250).distinctUntilChanged(),
        allParsed
    ) { q, parsed ->
        if (q.isBlank()) {
            SearchUiState(query = q, results = emptyList())
        } else {
            val lower = q.lowercase()
            val results = parsed.filter { r ->
                r.title.lowercase().contains(lower) ||
                r.summary.lowercase().contains(lower) ||
                r.topics.any { it.lowercase().contains(lower) } ||
                r.entities.any { it.lowercase().contains(lower) } ||
                r.sourceType.lowercase().contains(lower)
            }.sortedByDescending { it.entity.timestamp }
            SearchUiState(query = q, results = results, isSearching = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(q: String) { _query.value = q }

    private fun parse(entity: ScreenshotEntity): SearchResult? {
        val r = entity.extractedJson?.let {
            try { json.decodeFromString<ExtractionResult>(it) } catch (e: Exception) { null }
        } ?: return null
        return SearchResult(
            entity = entity,
            title = r.title,
            summary = r.summary,
            topics = r.topics,
            entities = r.entities,
            sourceType = r.source_type
        )
    }
}

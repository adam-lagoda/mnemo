package com.mnemo.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import com.mnemo.di.AppModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class DetailUiState(
    val screenshot: ScreenshotEntity? = null,
    val extraction: ExtractionResult? = null,
    val related: List<ScreenshotEntity> = emptyList(),
    val isLoading: Boolean = true
)

class DetailViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val screenshotRepo = appModule.screenshotRepository
    private val graphRepo = appModule.graphRepository
    private val analytics = appModule.graphAnalytics
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _screenshotId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DetailUiState> = _screenshotId
        .filterNotNull()
        .flatMapLatest { id ->
            flow {
                val screenshot = screenshotRepo.getById(id)
                val extraction = screenshot?.extractedJson?.let { j ->
                    try { json.decodeFromString<ExtractionResult>(j) } catch (e: Exception) { null }
                }
                val edges = graphRepo.getEdgesForNode(id)
                val relatedIds = analytics.getRelated(id, edges.map {
                    com.mnemo.data.db.entities.GraphEdgeEntity(it.sourceId, it.targetId, it.weight, it.edgeType)
                })
                val related = relatedIds.mapNotNull { screenshotRepo.getById(it) }
                emit(DetailUiState(screenshot = screenshot, extraction = extraction, related = related, isLoading = false))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun load(id: String) { _screenshotId.value = id }

    fun markReviewed() {
        viewModelScope.launch {
            _screenshotId.value?.let { screenshotRepo.markReviewed(it) }
        }
    }
}

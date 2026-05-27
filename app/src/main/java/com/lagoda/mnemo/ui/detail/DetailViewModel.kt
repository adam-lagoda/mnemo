package com.lagoda.mnemo.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.lagoda.mnemo.data.db.entities.ScreenshotEntity
import com.lagoda.mnemo.data.model.ExtractionResult
import com.lagoda.mnemo.data.repository.GraphRepository
import com.lagoda.mnemo.data.repository.ScreenshotRepository
import com.lagoda.mnemo.di.AppModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class DetailUiState(
    val screenshot: ScreenshotEntity? = null,
    val extraction: ExtractionResult? = null,
    val related: List<ScreenshotEntity> = emptyList(),
    val isLoading: Boolean = true
)

class DetailViewModel(
    app: Application,
    private val screenshotRepo: ScreenshotRepository,
    private val graphRepo: GraphRepository
) : AndroidViewModel(app) {

    constructor(app: Application) : this(
        app,
        AppModule.getInstance(app).screenshotRepository,
        AppModule.getInstance(app).graphRepository
    )

    private val analytics = AppModule.getInstance(app).graphAnalytics
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _screenshotId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = _screenshotId
        .filterNotNull()
        .flatMapLatest { id ->
            flow {
                val screenshot = screenshotRepo.getById(id)
                val extraction = screenshot?.extractedJson?.let { j ->
                    try { json.decodeFromString<ExtractionResult>(j) } catch (e: Exception) { null }
                }
                // Related = same community, excluding self, ordered by timestamp desc
                val related: List<ScreenshotEntity> = if (screenshot != null && screenshot.communityId >= 0) {
                    screenshotRepo.getByCommunity(screenshot.communityId)
                        .filter { it.id != id }
                        .sortedByDescending { it.timestamp }
                        .take(12)
                } else {
                    emptyList()
                }
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

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _screenshotId.value?.let { id ->
                screenshotRepo.deleteById(id)
                graphRepo.deleteEdgesForNode(id)
            }
            onDeleted()
        }
    }
}

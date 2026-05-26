package com.mnemo.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.di.AppModule
import com.mnemo.util.DateUtils
import com.mnemo.util.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GalleryUiState(
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val communityFilter: Int? = null,
    val isLoading: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val pendingCount: Int = 0
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppModule.getInstance(app).screenshotRepository
    private val graphRepo = AppModule.getInstance(app).graphRepository
    private val config = AppModule.getInstance(app).appConfig
    private val _filter = MutableStateFlow<Int?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _candidateCount = MutableStateFlow(0)

    val uiState: StateFlow<GalleryUiState> = combine(
        repo.observeAll(),
        _filter,
        _selectedIds,
        _isSelectionMode,
        _candidateCount
    ) { screenshots: List<ScreenshotEntity>, filter: Int?, selectedIds: Set<String>, isSelectionMode: Boolean, candidateCount: Int ->
        val filtered = if (filter == null) screenshots else screenshots.filter { it.communityId == filter }
        val indexedCount = screenshots.count { it.extractedJson != null }
        val pendingCount = maxOf(0, candidateCount - indexedCount)
        GalleryUiState(
            screenshots = filtered,
            communityFilter = filter,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            pendingCount = pendingCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState(isLoading = true))

    init {
        scanCandidates()
    }

    private fun scanCandidates() {
        val relativePath = config.relativePath ?: return
        val sinceMillis = if (config.dayFilter == -1) 0L else DateUtils.millisSince(config.dayFilter)
        viewModelScope.launch(Dispatchers.IO) {
            _candidateCount.value = MediaStoreScanner.query(
                getApplication<Application>().contentResolver,
                relativePath,
                sinceMillis
            ).size
        }
    }

    fun setFilter(communityId: Int?) { _filter.value = communityId }

    fun markReviewed(id: String) { viewModelScope.launch { repo.markReviewed(id) } }

    fun enterSelectionMode(id: String) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelection(id: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
    }

    fun selectAll() {
        val current = uiState.value.screenshots
        _selectedIds.value = current.map { it.id }.toSet()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                repo.deleteById(id)
                graphRepo.deleteEdgesForNode(id)
            }
            exitSelectionMode()
        }
    }
}

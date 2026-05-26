package com.mnemo.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.di.AppModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GalleryUiState(
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val communityFilter: Int? = null,
    val isLoading: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val unextractedCount: Int = 0
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppModule.getInstance(app).screenshotRepository
    private val graphRepo = AppModule.getInstance(app).graphRepository
    private val _filter = MutableStateFlow<Int?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)

    val uiState: StateFlow<GalleryUiState> = combine(
        repo.observeAll(),
        _filter,
        _selectedIds,
        _isSelectionMode,
        repo.observeUnextractedCount()
    ) { screenshots: List<ScreenshotEntity>, filter: Int?, selectedIds: Set<String>, isSelectionMode: Boolean, unextractedCount: Int ->
        val filtered = if (filter == null) screenshots else screenshots.filter { it.communityId == filter }
        GalleryUiState(
            screenshots = filtered,
            communityFilter = filter,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            unextractedCount = unextractedCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState(isLoading = true))

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

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
    val isLoading: Boolean = false
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppModule.getInstance(app).screenshotRepository
    private val _filter = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<GalleryUiState> = combine(
        repo.observeAll(),
        _filter
    ) { screenshots, filter ->
        val filtered = if (filter == null) screenshots
        else screenshots.filter { it.communityId == filter }
        GalleryUiState(screenshots = filtered, communityFilter = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState(isLoading = true))

    fun setFilter(communityId: Int?) {
        _filter.value = communityId
    }

    fun markReviewed(id: String) {
        viewModelScope.launch { repo.markReviewed(id) }
    }
}

package com.lagoda.mnemo.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lagoda.mnemo.data.db.entities.ScreenshotEntity
import com.lagoda.mnemo.scheduling.ExtractionWorker
import com.lagoda.mnemo.data.model.ExtractionResult
import com.lagoda.mnemo.di.AppModule
import com.lagoda.mnemo.util.DateUtils
import com.lagoda.mnemo.util.MediaStoreScanner
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Filter values:
 *   null        → All indexed screenshots
 *   non-null    → source_type value (e.g. "article", "reddit_post")
 */
data class GalleryUiState(
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val allScreenshots: List<ScreenshotEntity> = emptyList(),
    val filter: String? = null,
    val availableTags: List<String> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val pendingCount: Int = 0,
    /** True only when the extraction worker is actually RUNNING. */
    val isIndexingActive: Boolean = false,
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun tagsFor(entity: ScreenshotEntity): List<String> {
        val result = entity.extractedJson?.let {
            try { json.decodeFromString<ExtractionResult>(it) } catch (_: Exception) { null }
        } ?: return emptyList()
        return buildList {
            if (entity.sourceType.isNotBlank()) add(entity.sourceType)
            if (result.sentiment.isNotBlank()) add(result.sentiment)
            addAll(result.topics.filter { it.isNotBlank() })
        }
    }

    private val repo      = AppModule.getInstance(app).screenshotRepository
    private val graphRepo = AppModule.getInstance(app).graphRepository
    private val config    = AppModule.getInstance(app).appConfig
    private val _filter          = MutableStateFlow<String?>(null)
    private val _selectedIds     = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _candidateCount  = MutableStateFlow(0)

    // Live WorkManager state — true when extraction worker is running or enqueued
    private val _isIndexingActive: Flow<Boolean> =
        WorkManager.getInstance(app)
            .getWorkInfosForUniqueWorkFlow(ExtractionWorker.WORK_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
            .distinctUntilChanged()

    val uiState: StateFlow<GalleryUiState> = combine(
        repo.observeAll(),
        _filter,
        _selectedIds,
        _isSelectionMode,
        combine(_candidateCount, _isIndexingActive) { count, active -> count to active },
    ) { all: List<ScreenshotEntity>, filter: String?, selectedIds: Set<String>, isSelectionMode: Boolean, countAndActive: Pair<Int, Boolean> ->
        val (candidateCount, isIndexingActive) = countAndActive
        val indexed = all.filter { it.extractedJson != null }
        val entityTags = indexed.associateWith { tagsFor(it) }
        val filtered = if (filter == null) indexed
                       else indexed.filter { filter in (entityTags[it] ?: emptyList()) }
        val pendingCount = maxOf(0, candidateCount - indexed.size)
        val allTags = entityTags.values.flatten().distinct().sorted()
        val tagCounts = allTags.associateWith { tag -> entityTags.values.count { tag in it } }
        GalleryUiState(
            screenshots     = filtered,
            allScreenshots  = indexed,
            filter          = filter,
            availableTags   = allTags,
            tagCounts       = tagCounts,
            selectedIds     = selectedIds,
            isSelectionMode = isSelectionMode,
            pendingCount    = pendingCount,
            isIndexingActive = isIndexingActive,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState(isLoading = true))

    init { scanCandidates() }

    private fun scanCandidates() {
        val relativePath = config.relativePath ?: return
        val sinceMillis  = if (config.dayFilter == -1) 0L else DateUtils.millisSince(config.dayFilter)
        viewModelScope.launch(Dispatchers.IO) {
            _candidateCount.value = MediaStoreScanner.query(
                getApplication<Application>().contentResolver,
                relativePath,
                sinceMillis,
            ).size
        }
    }

    fun setFilter(filter: String?) { _filter.value = filter }

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
        _selectedIds.value = uiState.value.screenshots.map { it.id }.toSet()
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

package com.mnemo.ui.indexing

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ScreenshotCandidate
import com.mnemo.data.prefs.AppConfig
import com.mnemo.di.AppModule
import com.mnemo.scheduling.ExtractionWorker
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_ITEM_INDEX
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_ITEM_TOTAL
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_REMAINING_SECONDS
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_STEP_LABEL
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_STEP_NUM
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_STEP_TOTAL
import com.mnemo.scheduling.ExtractionWorker.Companion.KEY_URI
import com.mnemo.util.DateUtils
import com.mnemo.util.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.UUID

data class ExtractionProgress(
    val uri: String,          // empty while preparing
    val stepLabel: String,
    val stepNum: Int = 1,
    val stepTotal: Int = 3,
    val itemIndex: Int = 0,
    val itemTotal: Int = 0,
    val remainingSeconds: Int = -1,
    val isPreparing: Boolean = false  // true while worker is enqueued but not yet running
)

data class IndexingUiState(
    val folderDisplayName: String = "",
    val hasFolderSelected: Boolean = false,
    val dayFilter: Int = 30,
    /** Candidates not yet in DB — selectable */
    val pendingNew: List<ScreenshotCandidate> = emptyList(),
    /** Candidates in DB but extractedJson == null — queued/processing */
    val pendingQueued: List<ScreenshotCandidate> = emptyList(),
    /** Candidates in DB with extractedJson != null — done */
    val indexed: List<ScreenshotCandidate> = emptyList(),
    /** URI string → DB entity id, for navigating to detail on tap */
    val uriToDbId: Map<String, String> = emptyMap(),
    val selectedUris: Set<Uri> = emptySet(),
    val progress: ExtractionProgress? = null,
    val isScanning: Boolean = false,
    val isIndexing: Boolean = false,
    val autoWatchEnabled: Boolean = false,
) {
    val indexedCount: Int get() = indexed.size
    val totalCount: Int get() = pendingNew.size + pendingQueued.size + indexed.size
    val selectedCount: Int get() = selectedUris.size
    val inFlightUri: String? get() = progress?.uri
}

class IndexingViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val config = appModule.appConfig
    private val screenshotRepo = appModule.screenshotRepository

    private val _scanResult = MutableStateFlow<List<ScreenshotCandidate>>(emptyList())
    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    private val _isScanning = MutableStateFlow(false)
    private val _isIndexing = MutableStateFlow(false)
    private val _preparingProgress = MutableStateFlow<ExtractionProgress?>(null)
    private val _autoWatchEnabled = MutableStateFlow(config.autoWatchEnabled)

    private val _progress: Flow<ExtractionProgress?> = WorkManager.getInstance(app)
        .getWorkInfosForUniqueWorkFlow(ExtractionWorker.WORK_NAME)
        .map { infos ->
            val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            val enqueued = infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
            if (running != null) {
                val data = running.progress
                val uri = data.getString(KEY_URI)
                if (uri != null) {
                    ExtractionProgress(
                        uri = uri,
                        stepLabel = data.getString(KEY_STEP_LABEL) ?: "",
                        stepNum = data.getInt(KEY_STEP_NUM, 1),
                        stepTotal = data.getInt(KEY_STEP_TOTAL, 3),
                        itemIndex = data.getInt(KEY_ITEM_INDEX, 0),
                        itemTotal = data.getInt(KEY_ITEM_TOTAL, 0),
                        remainingSeconds = data.getInt(KEY_REMAINING_SECONDS, -1)
                    )
                } else {
                    ExtractionProgress(uri = "", stepLabel = "Starting…", isPreparing = true)
                }
            } else if (enqueued != null) {
                // Worker queued but not yet running — keep the card visible
                ExtractionProgress(uri = "", stepLabel = "Starting…", isPreparing = true)
            } else {
                null
            }
        }
        .catch { emit(null) }

    // Effective progress: WorkManager progress takes over once available, else manual preparing state
    private val _effectiveProgress: Flow<ExtractionProgress?> = combine(_progress, _preparingProgress) { wm, manual ->
        wm ?: manual
    }

    val uiState: StateFlow<IndexingUiState> = combine(
        combine(_scanResult, screenshotRepo.observeAll(), _selectedUris) { candidates, entities, selected ->
            Triple(candidates, entities, selected)
        },
        combine(_effectiveProgress, _isScanning, _isIndexing) { progress, scanning, indexing ->
            Triple(progress, scanning, indexing)
        },
        _autoWatchEnabled
    ) { (candidates, entities, selected), (progress, scanning, indexing), autoWatch ->
        val dbByUri = (entities as List<ScreenshotEntity>).associateBy { it.uri }
        val candidateList = candidates as List<ScreenshotCandidate>

        val pendingNew = candidateList.filter { it.uri.toString() !in dbByUri }
        val pendingQueued = candidateList.filter { c ->
            dbByUri[c.uri.toString()]?.extractedJson == null && c.uri.toString() in dbByUri
        }
        val indexed = candidateList.filter { c ->
            dbByUri[c.uri.toString()]?.extractedJson != null
        }

        val uriToDbId = (entities as List<ScreenshotEntity>)
            .associate { it.uri to it.id }
        IndexingUiState(
            folderDisplayName = config.displayName,
            hasFolderSelected = config.treeUri != null,
            dayFilter = config.dayFilter,
            pendingNew = pendingNew,
            pendingQueued = pendingQueued,
            indexed = indexed,
            uriToDbId = uriToDbId,
            selectedUris = selected as Set<Uri>,
            progress = progress as? ExtractionProgress,
            isScanning = scanning as Boolean,
            isIndexing = indexing as Boolean,
            autoWatchEnabled = autoWatch as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IndexingUiState(
        folderDisplayName = config.displayName,
        hasFolderSelected = config.treeUri != null,
        dayFilter = config.dayFilter,
        autoWatchEnabled = config.autoWatchEnabled,
    ))

    init { scan() }

    fun setTreeUri(uri: Uri) {
        getApplication<Application>().contentResolver
            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        config.treeUri = uri
        scan()
    }

    fun setAutoWatchEnabled(enabled: Boolean) {
        config.autoWatchEnabled = enabled
        _autoWatchEnabled.value = enabled
    }

    fun setDayFilter(days: Int) {
        config.dayFilter = days
        scan()
    }

    fun toggleSelection(uri: Uri) {
        _selectedUris.value = _selectedUris.value.toMutableSet().apply {
            if (contains(uri)) remove(uri) else add(uri)
        }
    }

    fun selectAll() {
        _selectedUris.value = uiState.value.pendingNew.map { it.uri }.toSet()
    }

    fun deselectAll() {
        _selectedUris.value = emptySet()
    }

    fun indexSelected() {
        val toInsert = _selectedUris.value.toList()
        if (toInsert.isEmpty()) return
        val candidates = uiState.value.pendingNew

        viewModelScope.launch {
            _isIndexing.value = true
            _selectedUris.value = emptySet()
            _preparingProgress.value = ExtractionProgress(
                uri = "", stepLabel = "Queuing images…",
                itemTotal = toInsert.size, isPreparing = true
            )
            withContext(Dispatchers.IO) {
                toInsert.chunked(50).forEach { batch ->
                    val entities = batch.mapNotNull { uri ->
                        candidates.find { it.uri == uri }?.let { c ->
                            ScreenshotEntity(
                                id = UUID.randomUUID().toString(),
                                uri = c.uri.toString(),
                                timestamp = c.timestamp
                            )
                        }
                    }
                    screenshotRepo.insertAll(entities)
                    yield()
                }
            }
            ExtractionWorker.enqueue(getApplication())
            _isIndexing.value = false
            _preparingProgress.value = null
        }
    }

    private fun scan() {
        val relativePath = config.relativePath ?: return
        val sinceMillis = if (config.dayFilter == -1) 0L else DateUtils.millisSince(config.dayFilter)

        viewModelScope.launch {
            _isScanning.value = true
            _scanResult.value = withContext(Dispatchers.IO) {
                MediaStoreScanner.query(
                    getApplication<Application>().contentResolver,
                    relativePath,
                    sinceMillis
                )
            }
            _isScanning.value = false
        }
    }
}

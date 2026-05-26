package com.mnemo.ui.setup

import ai.onnxruntime.OrtEnvironment
import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.mnemo.di.AppModule
import com.mnemo.util.DateUtils
import com.mnemo.util.MediaStoreScanner
import com.mnemo.model.DownloadQueryResult
import com.mnemo.model.ModelDownloadManager
import com.mnemo.model.ModelDownloadState
import com.mnemo.model.ModelId
import com.mnemo.model.ModelSpec
import com.mnemo.scheduling.ExtractionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.seconds

data class SetupUiState(
    val modelStates: Map<ModelId, ModelDownloadState> = emptyMap(),
    val savedHfToken: String? = null,
    val folderDisplayName: String = "",
    val hasFolderSelected: Boolean = false,
    val candidateCount: Int = 0,
    val indexedCount: Int = 0,
    val lastIndexedAt: Long? = null,
    val autoWatchEnabled: Boolean = false,
)

class SetupViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val config = appModule.appConfig
    private val screenshotRepo = appModule.screenshotRepository

    private val _modelStates = MutableStateFlow<Map<ModelId, ModelDownloadState>>(emptyMap())
    private val _candidateCount = MutableStateFlow(0)
    private val _autoWatchEnabled = MutableStateFlow(config.autoWatchEnabled)
    private var pollingJob: Job? = null

    val uiState: StateFlow<SetupUiState> = combine(
        _modelStates,
        _candidateCount,
        screenshotRepo.observeAll(),
        _autoWatchEnabled
    ) { modelStates, candidateCount, entities, autoWatch ->
        val indexed = entities.filter { it.extractedJson != null }
        SetupUiState(
            modelStates = modelStates,
            savedHfToken = config.hfToken,
            folderDisplayName = config.displayName,
            hasFolderSelected = config.treeUri != null,
            candidateCount = candidateCount,
            indexedCount = indexed.size,
            lastIndexedAt = indexed.maxOfOrNull { it.timestamp },
            autoWatchEnabled = autoWatch
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SetupUiState(
            savedHfToken = config.hfToken,
            folderDisplayName = config.displayName,
            hasFolderSelected = config.treeUri != null,
            autoWatchEnabled = config.autoWatchEnabled
        )
    )

    init {
        initModelStates()
        scanCandidates()
    }

    private fun scanCandidates() {
        val relativePath = config.relativePath ?: return
        val sinceMillis = if (config.dayFilter == -1) 0L else DateUtils.millisSince(config.dayFilter)
        viewModelScope.launch(Dispatchers.IO) {
            val count = MediaStoreScanner.query(
                getApplication<Application>().contentResolver,
                relativePath,
                sinceMillis
            ).size
            _candidateCount.value = count
        }
    }

    private fun initModelStates() {
        val app = getApplication<Application>()
        _modelStates.value = ModelSpec.ALL.mapValues { (_, spec) ->
            val file = File(app.filesDir, spec.filename)
            if (file.exists() && file.length() > 0) ModelDownloadState.Ready
            else ModelDownloadState.Absent
        }
    }

    fun downloadModel(modelId: ModelId, hfToken: String? = null) {
        val spec = ModelSpec.ALL[modelId] ?: return
        val app = getApplication<Application>()
        if (hfToken != null && hfToken.isNotBlank()) config.hfToken = hfToken
        viewModelScope.launch(Dispatchers.IO) {
            val downloadId = ModelDownloadManager.enqueue(app, spec, hfToken ?: config.hfToken)
            _modelStates.update { it + (modelId to ModelDownloadState.Downloading(0f, downloadId)) }
            maybeStartPolling()
        }
    }

    fun cancelDownload(modelId: ModelId) {
        val state = _modelStates.value[modelId] as? ModelDownloadState.Downloading ?: return
        ModelDownloadManager.cancel(getApplication(), state.downloadId)
        _modelStates.update { it + (modelId to ModelDownloadState.Absent) }
        if (_modelStates.value.values.none { it is ModelDownloadState.Downloading }) {
            pollingJob?.cancel()
        }
    }

    fun retryDownload(modelId: ModelId) {
        _modelStates.update { it + (modelId to ModelDownloadState.Absent) }
    }

    fun setTreeUri(uri: Uri) {
        getApplication<Application>().contentResolver
            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        config.treeUri = uri
        scanCandidates()
    }

    fun reIndexNow() {
        ExtractionWorker.enqueue(getApplication())
    }

    fun setAutoWatchEnabled(enabled: Boolean) {
        config.autoWatchEnabled = enabled
        _autoWatchEnabled.value = enabled
    }

    fun verifyModel(modelId: ModelId) {
        val spec = ModelSpec.ALL[modelId] ?: return
        val app = getApplication<Application>()
        _modelStates.update { it + (modelId to ModelDownloadState.Verifying) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                when (modelId) {
                    ModelId.GEMMA -> {
                        val config = EngineConfig(
                            modelPath = "${app.filesDir.absolutePath}/${spec.filename}",
                            backend = Backend.CPU(),
                            cacheDir = app.cacheDir.path
                        )
                        Engine(config).also { it.initialize() }.close()
                    }
                    ModelId.GTE_SMALL -> {
                        val session = OrtEnvironment.getEnvironment()
                            .createSession("${app.filesDir.absolutePath}/${spec.filename}")
                        session.close()
                    }
                }
            }
            val next = if (result.isSuccess) ModelDownloadState.Verified
                       else ModelDownloadState.Failed("Cannot load model: ${result.exceptionOrNull()?.message}")
            _modelStates.update { it + (modelId to next) }
        }
    }

    private fun maybeStartPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (_modelStates.value.values.any { it is ModelDownloadState.Downloading }) {
                delay(1.seconds)
                checkDownloadProgress()
            }
        }
    }

    private suspend fun checkDownloadProgress() {
        val app = getApplication<Application>()
        val downloading = _modelStates.value.entries
            .filter { (_, state) -> state is ModelDownloadState.Downloading }
            .map { (id, state) -> id to state as ModelDownloadState.Downloading }

        for ((modelId, state) in downloading) {
            val result: DownloadQueryResult = ModelDownloadManager.queryStatus(app, state.downloadId)
                ?: continue
            when (result.status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                    val progress = if (result.totalBytes > 0) {
                        result.bytesDownloaded.toFloat() / result.totalBytes
                    } else 0f
                    _modelStates.update { it + (modelId to state.copy(progress = progress)) }
                }
                DownloadManager.STATUS_SUCCESSFUL -> moveAndValidate(modelId)
                DownloadManager.STATUS_FAILED -> {
                    val reason = if (result.reason in 400..599) "HTTP ${result.reason}"
                                 else "Error code ${result.reason}"
                    _modelStates.update {
                        it + (modelId to ModelDownloadState.Failed(reason))
                    }
                }
            }
        }
    }

    private suspend fun moveAndValidate(modelId: ModelId) {
        val spec = ModelSpec.ALL[modelId] ?: return
        val app = getApplication<Application>()
        _modelStates.update { it + (modelId to ModelDownloadState.Validating) }

        withContext(Dispatchers.IO) {
            val src = File(app.getExternalFilesDir(null), spec.filename)
            val dst = File(app.filesDir, spec.filename)
            if (!src.renameTo(dst)) {
                src.inputStream().use { input ->
                    dst.outputStream().use { output -> input.copyTo(output) }
                }
                src.delete()
            }
            val valid = dst.exists() && dst.length() > 0
            _modelStates.update {
                it + (modelId to if (valid) ModelDownloadState.Ready
                               else ModelDownloadState.Failed("File missing after download"))
            }
        }
    }
}

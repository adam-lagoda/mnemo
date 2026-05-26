package com.mnemo.ui.topic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import com.mnemo.di.AppModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

data class TopicDetailUiState(
    val topicKey: String = "",
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val relatedTopics: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

class TopicDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppModule.getInstance(app).screenshotRepository
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _topicKey = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TopicDetailUiState> = _topicKey
        .filterNotNull()
        .flatMapLatest { topicKey ->
            flow {
                val all = repo.getAll()

                val screenshots = all
                    .filter { entity ->
                        parseExtraction(entity.extractedJson)
                            ?.topics
                            ?.any { it.lowercase().trim() == topicKey }
                            ?: false
                    }
                    .sortedByDescending { it.timestamp }

                // Topics that co-occur in the same screenshots, ranked by frequency
                val relatedTopics = screenshots
                    .flatMap { entity ->
                        parseExtraction(entity.extractedJson)
                            ?.topics
                            ?.map { it.lowercase().trim() }
                            ?.filter { it != topicKey }
                            ?: emptyList()
                    }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .take(12)

                emit(
                    TopicDetailUiState(
                        topicKey = topicKey,
                        screenshots = screenshots,
                        relatedTopics = relatedTopics,
                        isLoading = false,
                    )
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TopicDetailUiState())

    fun load(topicKey: String) {
        _topicKey.value = topicKey
    }

    private fun parseExtraction(jsonStr: String?): ExtractionResult? {
        jsonStr ?: return null
        return try { json.decodeFromString<ExtractionResult>(jsonStr) } catch (e: Exception) { null }
    }
}

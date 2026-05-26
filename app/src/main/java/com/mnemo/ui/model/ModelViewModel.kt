package com.mnemo.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.di.AppModule
import com.mnemo.extraction.GemmaExtractor
import com.mnemo.model.ModelSpec
import com.mnemo.model.ModelId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ModelUiState(
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val modelReady: Boolean = false,
    val error: String? = null,
)

class ModelViewModel(app: Application) : AndroidViewModel(app) {
    private val gemmaExtractor =
        AppModule.getInstance(app).vlmExtractor as? GemmaExtractor

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        checkModelReady()
    }

    private fun checkModelReady() {
        val spec = ModelSpec.ALL[ModelId.GEMMA] ?: return
        val file = File(getApplication<Application>().filesDir, spec.filename)
        _uiState.value = _uiState.value.copy(
            modelReady = file.exists() && file.length() > 0
        )
    }

    fun onPromptChange(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text, error = null)
    }

    fun ask() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank() || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, response = "", error = null)
            try {
                val result = gemmaExtractor?.generate(prompt) ?: "No model loaded."
                _uiState.value = _uiState.value.copy(isLoading = false, response = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: e.javaClass.simpleName
                )
            }
        }
    }
}

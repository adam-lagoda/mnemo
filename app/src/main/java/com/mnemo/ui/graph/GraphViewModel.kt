package com.mnemo.ui.graph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.di.AppModule
import com.mnemo.graph.GraphEdge
import com.mnemo.graph.GraphNode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GraphUiState(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val isLoading: Boolean = true
)

class GraphViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val screenshotRepo = appModule.screenshotRepository
    private val graphRepo = appModule.graphRepository
    private val analytics = appModule.graphAnalytics

    val uiState: StateFlow<GraphUiState> = combine(
        screenshotRepo.observeAll(),
        graphRepo.observeAll()
    ) { screenshots, dbEdges ->
        val nodes = analytics.buildGraphNodes(screenshots, dbEdges)
        val edges = dbEdges.map { GraphEdge(it.sourceId, it.targetId, it.weight, it.edgeType) }
        GraphUiState(nodes = nodes, edges = edges, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphUiState())
}

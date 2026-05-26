package com.mnemo.ui.graph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.di.AppModule
import com.mnemo.graph.GraphData
import com.mnemo.graph.GraphNode
import kotlinx.coroutines.flow.*

data class GraphUiState(
    val graphData: GraphData = GraphData(emptyList(), emptyList()),
    val selectedNode: GraphNode? = null,
    val isLoading: Boolean = true
)

class GraphViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val screenshotRepo = appModule.screenshotRepository
    private val graphRepo = appModule.graphRepository
    private val analytics = appModule.graphAnalytics

    private val _selectedNode = MutableStateFlow<GraphNode?>(null)

    val uiState: StateFlow<GraphUiState> = combine(
        screenshotRepo.observeAll(),
        graphRepo.observeAll(),
        _selectedNode
    ) { screenshots, dbEdges, selected ->
        val graphData = analytics.buildGraph(screenshots, dbEdges)
        GraphUiState(graphData = graphData, selectedNode = selected, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphUiState())

    fun onNodeTap(nodeId: String) {
        val node = uiState.value.graphData.nodes.find { it.id == nodeId }
        _selectedNode.value = if (_selectedNode.value?.id == nodeId) null else node
    }

    fun dismissPopup() {
        _selectedNode.value = null
    }
}

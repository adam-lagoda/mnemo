package com.mnemo.ui.graph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.di.AppModule
import com.mnemo.graph.GraphData
import com.mnemo.graph.GraphNode
import kotlinx.coroutines.flow.*

data class GraphUiState(
    val graphData: GraphData = GraphData(emptyList(), emptyList()),
    val selectedNode: GraphNode? = null,
    val neighborScreenshots: List<ScreenshotEntity> = emptyList(),
    val neighborTopics: List<GraphNode> = emptyList(),
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

        val neighborScreenshots: List<ScreenshotEntity>
        val neighborTopics: List<GraphNode>

        if (selected?.isTopicNode == true) {
            // Membership edges: source=screenshotId, target="topic:X"
            val memberIds = graphData.edges
                .filter { it.target == selected.id && it.edgeType == "membership" }
                .map { it.source }
                .toSet()
            neighborScreenshots = screenshots
                .filter { it.id in memberIds }
                .sortedByDescending { it.timestamp }

            // Co-occurrence edges: topic ↔ topic
            val coocIds = graphData.edges
                .filter { it.edgeType == "cooccurrence" &&
                          (it.source == selected.id || it.target == selected.id) }
                .map { if (it.source == selected.id) it.target else it.source }
                .toSet()
            neighborTopics = graphData.nodes.filter { it.id in coocIds }
        } else {
            neighborScreenshots = emptyList()
            neighborTopics = emptyList()
        }

        GraphUiState(
            graphData = graphData,
            selectedNode = selected,
            neighborScreenshots = neighborScreenshots,
            neighborTopics = neighborTopics,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphUiState())

    fun onNodeTap(nodeId: String) {
        val node = uiState.value.graphData.nodes.find { it.id == nodeId }
        _selectedNode.value = if (_selectedNode.value?.id == nodeId) null else node
    }

    fun dismissPopup() {
        _selectedNode.value = null
    }
}

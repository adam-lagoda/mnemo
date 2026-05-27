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
    val isLoading: Boolean = true,
    val dateFilterDays: Int = -1,
    val topicFilter: Set<String> = emptySet(),
    val availableTopics: List<String> = emptyList(),
)

class GraphViewModel(app: Application) : AndroidViewModel(app) {
    private val appModule = AppModule.getInstance(app)
    private val screenshotRepo = appModule.screenshotRepository
    private val graphRepo = appModule.graphRepository
    private val analytics = appModule.graphAnalytics

    private val _selectedNode = MutableStateFlow<GraphNode?>(null)
    private val _dateFilter = MutableStateFlow(-1)
    private val _topicFilter = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<GraphUiState> = combine(
        screenshotRepo.observeAll(),
        graphRepo.observeAll(),
        combine(_selectedNode, _dateFilter, _topicFilter) { a, b, c -> Triple(a, b, c) }
    ) { screenshots, dbEdges, (selected, dateDays, topicFilter) ->
        // 1. Date filter
        val now = System.currentTimeMillis()
        val filteredScreenshots = if (dateDays == -1) screenshots
            else screenshots.filter { now - it.timestamp <= dateDays * 86_400_000L }

        // 2. Build graph from date-filtered screenshots
        val rawGraph = analytics.buildGraph(filteredScreenshots, dbEdges)

        // 3. Available topic labels for the filter chips
        val availableTopics = rawGraph.nodes
            .filter { it.isTopicNode }
            .map { it.label }
            .sorted()

        // 4. Apply topic filter — union of each selected topic's members + co-occurring topics
        val graphData = if (topicFilter.isEmpty()) rawGraph else {
            val topicNodes = rawGraph.nodes.filter { it.isTopicNode && it.label in topicFilter }
            val keepIds = mutableSetOf<String>()
            for (topicNode in topicNodes) {
                keepIds += topicNode.id
                rawGraph.edges
                    .filter { it.target == topicNode.id && it.edgeType == "membership" }
                    .mapTo(keepIds) { it.source }
                rawGraph.edges
                    .filter { it.edgeType == "cooccurrence" &&
                               (it.source == topicNode.id || it.target == topicNode.id) }
                    .mapTo(keepIds) { if (it.source == topicNode.id) it.target else it.source }
            }
            GraphData(
                nodes = rawGraph.nodes.filter { it.id in keepIds },
                edges = rawGraph.edges.filter { it.source in keepIds && it.target in keepIds }
            )
        }

        // 5. Neighbor computation for selected node popup
        val neighborScreenshots: List<ScreenshotEntity>
        val neighborTopics: List<GraphNode>
        if (selected?.isTopicNode == true) {
            val memberIds = graphData.edges
                .filter { it.target == selected.id && it.edgeType == "membership" }
                .map { it.source }.toSet()
            neighborScreenshots = filteredScreenshots
                .filter { it.id in memberIds }
                .sortedByDescending { it.timestamp }
            val coocIds = graphData.edges
                .filter { it.edgeType == "cooccurrence" &&
                           (it.source == selected.id || it.target == selected.id) }
                .map { if (it.source == selected.id) it.target else it.source }.toSet()
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
            isLoading = false,
            dateFilterDays = dateDays,
            topicFilter = topicFilter,
            availableTopics = availableTopics,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphUiState())

    fun onNodeTap(nodeId: String) {
        val node = uiState.value.graphData.nodes.find { it.id == nodeId }
        _selectedNode.value = if (_selectedNode.value?.id == nodeId) null else node
    }

    fun dismissPopup() { _selectedNode.value = null }

    fun setDateFilter(days: Int) {
        _dateFilter.value = days
        _selectedNode.value = null
    }

    fun toggleTopicFilter(topic: String) {
        _topicFilter.value = _topicFilter.value.toMutableSet().apply {
            if (!add(topic)) remove(topic)
        }
        _selectedNode.value = null
    }

    fun clearTopicFilter() {
        _topicFilter.value = emptySet()
        _selectedNode.value = null
    }
}

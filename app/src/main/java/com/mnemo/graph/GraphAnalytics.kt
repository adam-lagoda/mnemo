package com.mnemo.graph

import com.mnemo.data.db.entities.GraphEdgeEntity
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import kotlinx.serialization.json.Json

data class GraphNode(
    val id: String,
    val communityId: Int,
    val degree: Int,
    val label: String
)

data class GraphEdge(
    val source: String,
    val target: String,
    val weight: Float,
    val edgeType: String
)

class GraphAnalytics {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun buildGraphNodes(
        screenshots: List<ScreenshotEntity>,
        edges: List<GraphEdgeEntity>
    ): List<GraphNode> {
        val degreeMap = mutableMapOf<String, Int>()
        edges.forEach { e ->
            degreeMap[e.sourceId] = (degreeMap[e.sourceId] ?: 0) + 1
            degreeMap[e.targetId] = (degreeMap[e.targetId] ?: 0) + 1
        }
        return screenshots.map { s ->
            GraphNode(
                id = s.id,
                communityId = s.communityId,
                degree = degreeMap[s.id] ?: 0,
                label = extractLabel(s)
            )
        }
    }

    fun getCommunityLabels(
        screenshots: List<ScreenshotEntity>
    ): Map<Int, String> {
        val byCommunity = screenshots.groupBy { it.communityId }
        return byCommunity.mapValues { (_, members) ->
            val topicCounts = mutableMapOf<String, Int>()
            members.forEach { s ->
                extractTopics(s).forEach { topic ->
                    topicCounts[topic] = (topicCounts[topic] ?: 0) + 1
                }
            }
            topicCounts.entries.maxByOrNull { it.value }?.key
                ?: "Group ${members.first().communityId}"
        }
    }

    fun getRelated(
        nodeId: String,
        edges: List<GraphEdgeEntity>,
        limit: Int = 10
    ): List<String> =
        edges.filter { it.sourceId == nodeId || it.targetId == nodeId }
            .sortedByDescending { it.weight }
            .take(limit)
            .map { if (it.sourceId == nodeId) it.targetId else it.sourceId }

    private fun extractLabel(entity: ScreenshotEntity): String {
        val jsonStr = entity.extractedJson ?: return entity.id.take(8)
        return try {
            json.decodeFromString<ExtractionResult>(jsonStr).title.take(40)
        } catch (e: Exception) { entity.id.take(8) }
    }

    private fun extractTopics(entity: ScreenshotEntity): List<String> {
        val jsonStr = entity.extractedJson ?: return emptyList()
        return try {
            json.decodeFromString<ExtractionResult>(jsonStr).topics
        } catch (e: Exception) { emptyList() }
    }
}

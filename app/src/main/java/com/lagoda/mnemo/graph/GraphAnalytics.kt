package com.lagoda.mnemo.graph

import com.lagoda.mnemo.data.db.entities.GraphEdgeEntity
import com.lagoda.mnemo.data.db.entities.ScreenshotEntity
import com.lagoda.mnemo.data.model.ExtractionResult
import kotlinx.serialization.json.Json

data class GraphNode(
    val id: String,
    val communityId: Int,
    val degree: Int,
    val label: String,
    val topics: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val summary: String = "",
    val timestamp: Long = 0L,
    val isTopicNode: Boolean = false,
    val screenshotCount: Int = 1   // for topic nodes: # of screenshots containing this topic
)

data class GraphEdge(
    val source: String,
    val target: String,
    val weight: Float,
    val edgeType: String           // "semantic" | "entity" | "temporal" | "membership" | "cooccurrence"
)

data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)

class GraphAnalytics {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun buildGraph(
        screenshots: List<ScreenshotEntity>,
        dbEdges: List<GraphEdgeEntity>
    ): GraphData {
        val indexed = screenshots.filter { it.extractedJson != null }

        // Degree map from stored ss-ss edges
        val degreeMap = mutableMapOf<String, Int>()
        dbEdges.forEach { e ->
            degreeMap[e.sourceId] = (degreeMap[e.sourceId] ?: 0) + 1
            degreeMap[e.targetId] = (degreeMap[e.targetId] ?: 0) + 1
        }

        // Parse extraction results
        data class Parsed(val entity: ScreenshotEntity, val result: ExtractionResult)
        val parsed = indexed.mapNotNull { s ->
            val r = parse(s) ?: return@mapNotNull null
            Parsed(s, r)
        }

        // Screenshot nodes
        val ssNodes = screenshots.map { s ->
            val r = parse(s)
            GraphNode(
                id = s.id,
                communityId = s.communityId,
                degree = degreeMap[s.id] ?: 0,
                label = r?.title?.take(40) ?: s.id.take(8),
                topics = r?.topics ?: emptyList(),
                entities = r?.entities ?: emptyList(),
                summary = r?.summary ?: "",
                timestamp = s.timestamp,
                isTopicNode = false
            )
        }

        // Topic aggregation: topic → list of screenshot IDs
        val topicToScreenshots = mutableMapOf<String, MutableList<String>>()
        parsed.forEach { (s, r) ->
            r.topics.forEach { topic ->
                val key = topic.lowercase().trim()
                topicToScreenshots.getOrPut(key) { mutableListOf() }.add(s.id)
            }
        }

        // Topic nodes (only topics that appear in at least 1 screenshot)
        val topicNodes = topicToScreenshots.map { (topic, ssIds) ->
            GraphNode(
                id = "topic:$topic",
                communityId = -1,
                degree = ssIds.size,
                label = topic,
                topics = listOf(topic),
                entities = emptyList(),
                summary = "${ssIds.size} screenshot${if (ssIds.size == 1) "" else "s"}",
                timestamp = 0L,
                isTopicNode = true,
                screenshotCount = ssIds.size
            )
        }

        // Edges: existing ss-ss edges
        val ssEdges = dbEdges.map { GraphEdge(it.sourceId, it.targetId, it.weight, it.edgeType) }

        // Membership edges: screenshot → topic
        val membershipEdges = parsed.flatMap { (s, r) ->
            r.topics.map { topic ->
                GraphEdge(
                    source = s.id,
                    target = "topic:${topic.lowercase().trim()}",
                    weight = 0.5f,
                    edgeType = "membership"
                )
            }
        }

        // Co-occurrence edges: topic ↔ topic (appear in same screenshot)
        val coocEdges = mutableListOf<GraphEdge>()
        parsed.forEach { (_, r) ->
            val topics = r.topics.map { it.lowercase().trim() }.distinct()
            for (i in topics.indices) {
                for (j in i + 1 until topics.size) {
                    coocEdges.add(
                        GraphEdge(
                            source = "topic:${topics[i]}",
                            target = "topic:${topics[j]}",
                            weight = 0.6f,
                            edgeType = "cooccurrence"
                        )
                    )
                }
            }
        }

        return GraphData(
            nodes = ssNodes + topicNodes,
            edges = ssEdges + membershipEdges + coocEdges
        )
    }

    fun getCommunityLabels(screenshots: List<ScreenshotEntity>): Map<Int, String> {
        val byCommunity = screenshots.groupBy { it.communityId }
        return byCommunity.mapValues { (_, members) ->
            val topicCounts = mutableMapOf<String, Int>()
            members.forEach { s ->
                parse(s)?.topics?.forEach { topic ->
                    topicCounts[topic] = (topicCounts[topic] ?: 0) + 1
                }
            }
            topicCounts.entries.maxByOrNull { it.value }?.key
                ?: "Group ${members.first().communityId}"
        }
    }

    fun getRelated(nodeId: String, edges: List<GraphEdgeEntity>, limit: Int = 10): List<String> =
        edges.filter { it.sourceId == nodeId || it.targetId == nodeId }
            .sortedByDescending { it.weight }
            .take(limit)
            .map { if (it.sourceId == nodeId) it.targetId else it.sourceId }

    private fun parse(entity: ScreenshotEntity): ExtractionResult? {
        val jsonStr = entity.extractedJson ?: return null
        return try { json.decodeFromString<ExtractionResult>(jsonStr) } catch (e: Exception) { null }
    }
}

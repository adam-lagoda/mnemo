package com.mnemo.graph

import com.mnemo.data.db.entities.GraphEdgeEntity
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.model.ExtractionResult
import com.mnemo.data.repository.ScreenshotRepository
import com.mnemo.embedding.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GraphBuilder(
    private val embeddingEngine: EmbeddingEngine,
    private val screenshotRepository: ScreenshotRepository
) {
    companion object {
        const val SEMANTIC_THRESHOLD = 0.7f
        const val TEMPORAL_WINDOW_MS = 30 * 60 * 1000L
        const val TEMPORAL_WEIGHT = 0.3f
        const val MIN_EDGE_WEIGHT = 0.05f
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun buildEdges(): List<GraphEdgeEntity> = withContext(Dispatchers.Default) {
        val screenshots = screenshotRepository.getAll()
        if (screenshots.size < 2) return@withContext emptyList()

        val embeddings = screenshots.map { s ->
            s.id to embeddingEngine.embed(buildEmbedText(s))
        }

        val edges = mutableListOf<GraphEdgeEntity>()
        for (i in screenshots.indices) {
            for (j in i + 1 until screenshots.size) {
                val a = screenshots[i]; val b = screenshots[j]
                val embA = embeddings[i].second; val embB = embeddings[j].second

                val semantic = if (embA.isNotEmpty() && embB.isNotEmpty())
                    embeddingEngine.similarity(embA, embB) else 0f

                val sharedEntities = extractEntities(a).intersect(extractEntities(b)).size
                val entity = minOf(1f, sharedEntities * 0.4f)

                val timeDiff = kotlin.math.abs(a.timestamp - b.timestamp)
                val temporal = if (timeDiff <= TEMPORAL_WINDOW_MS) TEMPORAL_WEIGHT else 0f

                val weight = maxOf(semantic, entity) + temporal
                if (weight < MIN_EDGE_WEIGHT) continue

                val edgeType = when {
                    semantic >= SEMANTIC_THRESHOLD -> "semantic"
                    entity > 0f -> "entity"
                    else -> "temporal"
                }
                edges.add(GraphEdgeEntity(a.id, b.id, weight, edgeType))
            }
        }
        edges
    }

    private fun buildEmbedText(entity: ScreenshotEntity): String {
        val jsonStr = entity.extractedJson ?: return ""
        return try {
            val r = json.decodeFromString<ExtractionResult>(jsonStr)
            (listOf(r.title, r.summary) + r.topics + r.entities).joinToString(" ")
        } catch (e: Exception) { "" }
    }

    private fun extractEntities(entity: ScreenshotEntity): Set<String> {
        val jsonStr = entity.extractedJson ?: return emptySet()
        return try {
            json.decodeFromString<ExtractionResult>(jsonStr)
                .entities.map { it.lowercase().trim() }.toSet()
        } catch (e: Exception) { emptySet() }
    }
}

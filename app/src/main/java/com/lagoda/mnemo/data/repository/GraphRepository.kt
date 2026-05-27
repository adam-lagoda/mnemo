package com.lagoda.mnemo.data.repository

import com.lagoda.mnemo.data.db.GraphEdgeDao
import com.lagoda.mnemo.data.db.entities.GraphEdgeEntity
import kotlinx.coroutines.flow.Flow

class GraphRepository(private val dao: GraphEdgeDao) {
    fun observeAll(): Flow<List<GraphEdgeEntity>> = dao.observeAll()
    suspend fun getAll(): List<GraphEdgeEntity> = dao.getAll()
    suspend fun getEdgesForNode(nodeId: String): List<GraphEdgeEntity> =
        dao.getEdgesForNode(nodeId)
    suspend fun insertAll(edges: List<GraphEdgeEntity>) = dao.insertAll(edges)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun deleteEdgesForNode(nodeId: String) = dao.deleteEdgesForNode(nodeId)
    suspend fun count(): Int = dao.count()
}

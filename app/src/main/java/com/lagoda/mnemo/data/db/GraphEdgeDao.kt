package com.lagoda.mnemo.data.db

import androidx.room.*
import com.lagoda.mnemo.data.db.entities.GraphEdgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphEdgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(edge: GraphEdgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(edges: List<GraphEdgeEntity>)

    @Query("SELECT * FROM graph_edges")
    suspend fun getAll(): List<GraphEdgeEntity>

    @Query("SELECT * FROM graph_edges")
    fun observeAll(): Flow<List<GraphEdgeEntity>>

    @Query("SELECT * FROM graph_edges WHERE sourceId = :nodeId OR targetId = :nodeId")
    suspend fun getEdgesForNode(nodeId: String): List<GraphEdgeEntity>

    @Query("DELETE FROM graph_edges")
    suspend fun deleteAll()

    @Query("DELETE FROM graph_edges WHERE sourceId = :nodeId OR targetId = :nodeId")
    suspend fun deleteEdgesForNode(nodeId: String)

    @Query("SELECT COUNT(*) FROM graph_edges")
    suspend fun count(): Int
}

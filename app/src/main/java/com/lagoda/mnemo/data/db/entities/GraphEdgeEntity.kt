package com.lagoda.mnemo.data.db.entities

import androidx.room.Entity

@Entity(tableName = "graph_edges", primaryKeys = ["sourceId", "targetId"])
data class GraphEdgeEntity(
    val sourceId: String,
    val targetId: String,
    val weight: Float,
    val edgeType: String // "semantic" | "entity" | "temporal"
)

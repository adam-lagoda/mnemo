package com.lagoda.mnemo.graph

import kotlin.math.abs

/**
 * Pure Kotlin Louvain community detection.
 * Single-phase (no super-node contraction) — sufficient for <10k nodes.
 */
class LouvainClustering {

    fun cluster(
        nodes: List<String>,
        edges: List<Triple<String, String, Float>>
    ): Map<String, Int> {
        if (nodes.isEmpty()) return emptyMap()
        if (nodes.size == 1) return mapOf(nodes[0] to 0)

        // Build adjacency
        val adj = mutableMapOf<String, MutableMap<String, Float>>()
        nodes.forEach { adj[it] = mutableMapOf() }
        var totalWeight = 0.0
        edges.forEach { (u, v, w) ->
            adj.getOrPut(u) { mutableMapOf() }[v] = (adj[u]?.get(v) ?: 0f) + w
            adj.getOrPut(v) { mutableMapOf() }[u] = (adj[v]?.get(u) ?: 0f) + w
            totalWeight += w
        }
        if (totalWeight == 0.0) return nodes.mapIndexed { i, n -> n to i }.toMap()

        val communities = nodes.associateWith { it }.toMutableMap()

        var improved = true
        var iterations = 0
        while (improved && iterations < 100) {
            improved = false
            iterations++
            for (node in nodes) {
                val currentCommunity = communities[node] ?: continue
                val neighborCommunities = (adj[node]?.keys ?: emptySet())
                    .mapNotNull { communities[it] }
                    .toSet()

                var bestCommunity = currentCommunity
                var bestGain = 0.0

                for (candidate in neighborCommunities) {
                    if (candidate == currentCommunity) continue
                    val gain = modularityGain(node, candidate, communities, adj, totalWeight)
                    if (gain > bestGain + 1e-6) {
                        bestGain = gain
                        bestCommunity = candidate
                    }
                }
                if (bestCommunity != currentCommunity) {
                    communities[node] = bestCommunity
                    improved = true
                }
            }
        }

        // Re-index community IDs to 0..n
        val uniqueIds = communities.values.distinct().sorted()
        val idMap = uniqueIds.mapIndexed { idx, id -> id to idx }.toMap()
        return communities.mapValues { (_, v) -> idMap[v] ?: 0 }
    }

    private fun modularityGain(
        node: String,
        targetCommunity: String,
        communities: Map<String, String>,
        adj: Map<String, Map<String, Float>>,
        totalWeight: Double
    ): Double {
        val nodeEdges = adj[node] ?: return 0.0
        val kNode = nodeEdges.values.sum().toDouble()
        val sumIn = nodeEdges.entries
            .filter { communities[it.key] == targetCommunity }
            .sumOf { it.value.toDouble() }
        val sumTot = adj.entries
            .filter { communities[it.key] == targetCommunity }
            .sumOf { (_, nbrs) -> nbrs.values.sum().toDouble() }
        return (sumIn - kNode * sumTot / (2.0 * totalWeight)) / totalWeight
    }
}

package com.mnemo.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.mnemo.graph.GraphEdge
import com.mnemo.graph.GraphNode
import com.mnemo.ui.theme.Accent
import com.mnemo.ui.theme.OnSurfaceVariant
import com.mnemo.ui.theme.communityColor
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

private val EdgeColors = mapOf(
    "semantic"     to Color(0xFF4FC3F7),   // light blue
    "entity"       to Color(0xFFFFB74D),   // amber
    "temporal"     to Color(0xFF90A4AE),   // grey
    "membership"   to Color(0xFF81C784),   // green
    "cooccurrence" to Color(0xFFCE93D8)    // purple
)

data class NodeState(
    val id: String,
    val communityId: Int,
    val degree: Int,
    val isTopicNode: Boolean,
    val x: Float = 0f,
    val y: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f
)

@Composable
fun ForceDirectedCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    selectedNodeId: String?,
    onNodeTap: (String) -> Unit
) {
    val nodeStates = remember(nodes.map { it.id }) {
        mutableStateListOf(*nodes.mapIndexed { i, n ->
            NodeState(
                id = n.id,
                communityId = n.communityId,
                degree = n.degree,
                isTopicNode = n.isTopicNode,
                x = (i * 137.508f % 600f) - 300f,
                y = (i * 97.3f % 600f) - 300f
            )
        }.toTypedArray())
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(nodes.map { it.id }, edges) {
        while (isActive) {
            withFrameMillis {
                for (i in nodeStates.indices) {
                    val a = nodeStates[i]
                    var fx = 0f; var fy = 0f

                    // Repulsion — topic nodes repel more strongly
                    for (j in nodeStates.indices) {
                        if (i == j) continue
                        val b = nodeStates[j]
                        val dx = a.x - b.x; val dy = a.y - b.y
                        val dist2 = dx * dx + dy * dy + 0.01f
                        val dist = sqrt(dist2)
                        val strength = if (a.isTopicNode || b.isTopicNode) 8000f else 4000f
                        val force = strength / dist2
                        fx += force * dx / dist; fy += force * dy / dist
                    }

                    // Attraction along edges
                    for (edge in edges) {
                        if (edge.source != a.id && edge.target != a.id) continue
                        val otherId = if (edge.source == a.id) edge.target else edge.source
                        val b = nodeStates.find { it.id == otherId } ?: continue
                        val dx = b.x - a.x; val dy = b.y - a.y
                        val dist = sqrt(dx * dx + dy * dy) + 0.01f
                        val force = 0.008f * edge.weight * dist
                        fx += force * dx / dist; fy += force * dy / dist
                    }

                    // Center gravity
                    fx -= a.x * 0.002f; fy -= a.y * 0.002f

                    val newVx = (a.vx + fx) * 0.85f
                    val newVy = (a.vy + fy) * 0.85f
                    nodeStates[i] = a.copy(vx = newVx, vy = newVy, x = a.x + newVx, y = a.y + newVy)
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .transformable(rememberTransformableState { zoom, pan, _ ->
                scale = (scale * zoom).coerceIn(0.1f, 8f)
                offset += pan
            })
            .pointerInput(nodeStates.size) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val wx = (tap.x - offset.x - cx) / scale
                    val wy = (tap.y - offset.y - cy) / scale
                    val nearest = nodeStates.minByOrNull { n ->
                        val dx = n.x - wx; val dy = n.y - wy; dx * dx + dy * dy
                    } ?: return@detectTapGestures
                    val dx = nearest.x - wx; val dy = nearest.y - wy
                    val radius = nodeRadius(nearest)
                    if (sqrt(dx * dx + dy * dy) <= radius * 1.5f) onNodeTap(nearest.id)
                }
            }
    ) {
        val cx = size.width / 2f; val cy = size.height / 2f
        withTransform({
            translate(offset.x + cx, offset.y + cy)
            scale(scale, scale, Offset.Zero)
        }) {
            // Edges
            for (edge in edges) {
                val src = nodeStates.find { it.id == edge.source } ?: continue
                val tgt = nodeStates.find { it.id == edge.target } ?: continue
                val edgeColor = EdgeColors[edge.edgeType] ?: OnSurfaceVariant
                val alpha = when (edge.edgeType) {
                    "membership"   -> 0.25f
                    "cooccurrence" -> 0.45f
                    else           -> (edge.weight * 0.5f).coerceIn(0.05f, 0.4f)
                }
                drawLine(
                    color = edgeColor.copy(alpha = alpha),
                    start = Offset(src.x, src.y),
                    end = Offset(tgt.x, tgt.y),
                    strokeWidth = if (edge.edgeType == "cooccurrence") 1.5f else 1f
                )
            }

            // Nodes
            for (node in nodeStates) {
                val radius = nodeRadius(node)
                val isSelected = node.id == selectedNodeId

                if (node.isTopicNode) {
                    // Topic nodes: filled accent circle with ring
                    val color = Accent
                    drawCircle(color = color.copy(alpha = 0.15f), radius = radius + 6f, center = Offset(node.x, node.y))
                    drawCircle(color = color, radius = radius, center = Offset(node.x, node.y))
                    if (isSelected) {
                        drawCircle(color = Color.White, radius = radius, center = Offset(node.x, node.y), style = Stroke(2.5f))
                    }
                } else {
                    // Screenshot nodes: community color
                    val color = communityColor(node.communityId)
                    drawCircle(color = color.copy(alpha = 0.15f), radius = radius + 4f, center = Offset(node.x, node.y))
                    drawCircle(color = color, radius = radius, center = Offset(node.x, node.y))
                    drawCircle(color = color.copy(alpha = 0.6f), radius = radius, center = Offset(node.x, node.y), style = Stroke(1f))
                    if (isSelected) {
                        drawCircle(color = Color.White, radius = radius, center = Offset(node.x, node.y), style = Stroke(2.5f))
                    }
                }
            }
        }
    }
}

private fun nodeRadius(node: NodeState): Float = when {
    node.isTopicNode -> (12f + node.degree * 3f).coerceIn(14f, 36f)
    else             -> (7f + node.degree * 1.5f).coerceAtMost(20f)
}

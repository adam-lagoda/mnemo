package com.mnemo.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.mnemo.graph.GraphEdge
import com.mnemo.graph.GraphNode
import com.mnemo.ui.theme.OnSurfaceVariant
import com.mnemo.ui.theme.communityColor
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

data class NodeState(
    val id: String,
    val communityId: Int,
    val degree: Int,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f
)

@Composable
fun ForceDirectedCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    onNodeTap: (String) -> Unit
) {
    val nodeStates = remember(nodes) {
        nodes.mapIndexed { i, n ->
            NodeState(
                id = n.id,
                communityId = n.communityId,
                degree = n.degree,
                x = (i * 137.508f % 600f) - 300f,
                y = (i * 97.3f % 600f) - 300f
            )
        }.toMutableList()
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(nodes, edges) {
        while (isActive) {
            withFrameMillis {
                for (i in nodeStates.indices) {
                    val a = nodeStates[i]
                    var fx = 0f; var fy = 0f

                    // Repulsion (Coulomb) — brute force, fine for <500 nodes
                    for (j in nodeStates.indices) {
                        if (i == j) continue
                        val b = nodeStates[j]
                        val dx = a.x - b.x; val dy = a.y - b.y
                        val dist2 = dx * dx + dy * dy + 0.01f
                        val dist = sqrt(dist2)
                        val force = 4000f / dist2
                        fx += force * dx / dist; fy += force * dy / dist
                    }

                    // Attraction (Hooke) along edges
                    for (edge in edges) {
                        val otherId = when {
                            edge.source == a.id -> edge.target
                            edge.target == a.id -> edge.source
                            else -> continue
                        }
                        val b = nodeStates.find { it.id == otherId } ?: continue
                        val dx = b.x - a.x; val dy = b.y - a.y
                        val dist = sqrt(dx * dx + dy * dy) + 0.01f
                        val force = 0.008f * edge.weight * dist
                        fx += force * dx / dist; fy += force * dy / dist
                    }

                    // Center gravity
                    fx -= a.x * 0.002f; fy -= a.y * 0.002f

                    a.vx = (a.vx + fx) * 0.85f
                    a.vy = (a.vy + fy) * 0.85f
                    a.x += a.vx; a.y += a.vy
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
                        val dx = n.x - wx; val dy = n.y - wy
                        dx * dx + dy * dy
                    } ?: return@detectTapGestures
                    val dx = nearest.x - wx; val dy = nearest.y - wy
                    val radius = (8f + nearest.degree * 1.5f).coerceAtMost(24f)
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
                drawLine(
                    color = OnSurfaceVariant.copy(alpha = (edge.weight * 0.5f).coerceIn(0.05f, 0.4f)),
                    start = Offset(src.x, src.y),
                    end = Offset(tgt.x, tgt.y),
                    strokeWidth = 1f
                )
            }
            // Nodes
            for (node in nodeStates) {
                val color = communityColor(node.communityId)
                val radius = (8f + node.degree * 1.5f).coerceAtMost(24f)
                drawCircle(color = color.copy(alpha = 0.2f), radius = radius + 4f, center = Offset(node.x, node.y))
                drawCircle(color = color, radius = radius, center = Offset(node.x, node.y))
                drawCircle(
                    color = color.copy(alpha = 0.6f), radius = radius,
                    center = Offset(node.x, node.y), style = Stroke(1f)
                )
            }
        }
    }
}

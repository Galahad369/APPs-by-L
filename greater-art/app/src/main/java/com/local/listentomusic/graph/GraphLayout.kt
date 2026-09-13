package com.local.listentomusic.graph

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.*
import java.util.Random

internal fun graphLinkTargetDistance(strength: Float, linkDistance: Float): Float =
    (38f + 215f * (1f - strength.coerceIn(0f, 1f)).pow(1.35f)) * linkDistance

internal fun strongestGraphHub(nodeCount: Int, edges: List<GraphEdge>): Int {
    if (nodeCount <= 0) return 0
    val degree = FloatArray(nodeCount)
    edges.forEach { edge -> if (edge.a in degree.indices && edge.b in degree.indices) {
        degree[edge.a] += edge.strength; degree[edge.b] += edge.strength
    } }
    return degree.indices.maxByOrNull { degree[it] } ?: 0
}

/** Finite background solve, not an endless UI-frame physics loop. Local-grid repulsion is bounded. */
object GraphLayout {
    suspend fun settle(nodes: List<GraphInput>, edges: List<GraphEdge>, linkDistance: Float = 1f,
        repulsion: Float = 1f, bounce: Float = .82f, initial: List<GraphPoint>? = null): List<GraphPoint> {
        val n = nodes.size
        if (n == 0) return emptyList()
        val rng = Random(42)
        val radius = max(120f, sqrt(n.toFloat()) * 32f)
        val weightedDegree = FloatArray(n)
        edges.forEach { edge ->
            if (edge.a in 0 until n && edge.b in 0 until n) {
                weightedDegree[edge.a] += edge.strength
                weightedDegree[edge.b] += edge.strength
            }
        }
        val hub = strongestGraphHub(n, edges)
        val maxDegree = weightedDegree.maxOrNull()?.coerceAtLeast(.001f) ?: 1f
        val x = FloatArray(n) { initial?.getOrNull(it)?.x ?: (rng.nextFloat() - 0.5f) * radius * 2 }
        val y = FloatArray(n) { initial?.getOrNull(it)?.y ?: (rng.nextFloat() - 0.5f) * radius * 2 }
        if (initial == null) { x[hub] = 0f; y[hub] = 0f }
        val vx = FloatArray(n); val vy = FloatArray(n)
        val cell = 80f
        fun key(cx: Int, cy: Int) = (cx.toLong() shl 32) xor (cy.toLong() and 0xffffffffL)
        repeat(180) { step ->
            coroutineContext.ensureActive()
            val grid = HashMap<Long, MutableList<Int>>()
            for (i in 0 until n) grid.getOrPut(key(floor(x[i] / cell).toInt(), floor(y[i] / cell).toInt())) { mutableListOf() }.add(i)
            val fx = FloatArray(n); val fy = FloatArray(n)
            for (i in 0 until n) {
                if (i % 128 == 0) coroutineContext.ensureActive()
                val cx = floor(x[i] / cell).toInt(); val cy = floor(y[i] / cell).toInt()
                for (gx in cx - 1..cx + 1) for (gy in cy - 1..cy + 1) {
                    val bucket = grid[key(gx, gy)] ?: continue
                    // Dense clusters must not turn repulsion back into O(N²).
                    repeat(minOf(bucket.size, 32)) { offset ->
                        val j = bucket[(offset + i) % bucket.size]
                        if (i != j) {
                            val dx = x[i] - x[j]; val dy = y[i] - y[j]
                            val d2 = (dx * dx + dy * dy).coerceAtLeast(4f)
                            fx[i] += dx * 150f * repulsion / d2; fy[i] += dy * 150f * repulsion / d2
                        }
                    }
                }
            }
            edges.forEach { e ->
                val dx = x[e.b] - x[e.a]; val dy = y[e.b] - y[e.a]
                val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                // Strong matches form tight clusters; weak retained links stay visibly long.
                val target = graphLinkTargetDistance(e.strength, linkDistance)
                val force = ((d - target) * (0.015f + 0.06f * e.strength)) / d
                fx[e.a] += dx * force; fy[e.a] += dy * force
                fx[e.b] -= dx * force; fy[e.b] -= dy * force
            }
            val cooling = 1f - step / 210f
            for (i in 0 until n) {
                val centrality = weightedDegree[i] / maxDegree
                val gravity = .0007f + centrality * .0032f + if (i == hub) .012f else 0f
                vx[i] = (vx[i] * bounce + (fx[i] - x[i] * gravity) * cooling).coerceIn(-12f, 12f)
                vy[i] = (vy[i] * bounce + (fy[i] - y[i] * gravity) * cooling).coerceIn(-12f, 12f)
                x[i] += vx[i]; y[i] += vy[i]
            }
        }
        // Exact invariant: the strongest weighted hub is the graph origin.
        val hubX = x[hub]; val hubY = y[hub]
        return nodes.indices.map { GraphPoint(x[it] - hubX, y[it] - hubY) }
    }
}

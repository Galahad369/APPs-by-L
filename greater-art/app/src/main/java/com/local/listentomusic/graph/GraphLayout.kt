package com.local.listentomusic.graph

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.*
import java.util.Random

/** Finite background solve, not an endless UI-frame physics loop. Local-grid repulsion is bounded. */
object GraphLayout {
    suspend fun settle(nodes: List<GraphInput>, edges: List<GraphEdge>): List<GraphPoint> {
        val n = nodes.size
        if (n == 0) return emptyList()
        val rng = Random(42)
        val radius = max(120f, sqrt(n.toFloat()) * 32f)
        val x = FloatArray(n) { (rng.nextFloat() - 0.5f) * radius * 2 }
        val y = FloatArray(n) { (rng.nextFloat() - 0.5f) * radius * 2 }
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
                            fx[i] += dx * 150f / d2; fy[i] += dy * 150f / d2
                        }
                    }
                }
            }
            edges.forEach { e ->
                val dx = x[e.b] - x[e.a]; val dy = y[e.b] - y[e.a]
                val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val target = 30f + 130f * (1f - e.strength)
                val force = ((d - target) * (0.015f + 0.06f * e.strength)) / d
                fx[e.a] += dx * force; fy[e.a] += dy * force
                fx[e.b] -= dx * force; fy[e.b] -= dy * force
            }
            val cooling = 1f - step / 210f
            for (i in 0 until n) {
                x[i] += (fx[i] - x[i] * 0.001f).coerceIn(-12f, 12f) * cooling
                y[i] += (fy[i] - y[i] * 0.001f).coerceIn(-12f, 12f) * cooling
            }
        }
        return nodes.indices.map { GraphPoint(x[it], y[it]) }
    }
}

package com.local.listentomusic.graph

import kotlin.math.ln

data class GraphOptions(val threshold: Float = 0.08f, val nodeSize: Float = 1f,
    val edgeOpacity: Float = 0.35f, val showLabels: Boolean = true,
    val hideIsolated: Boolean = false, val sizeByConnections: Boolean = true,
    val linkDistance: Float = 1f, val repulsion: Float = 1f, val bounce: Float = 0.82f) {
    fun encode() = listOf(threshold, nodeSize, edgeOpacity, showLabels, hideIsolated, sizeByConnections,
        linkDistance, repulsion, bounce).joinToString("|")
    companion object {
        fun decode(raw: String?): GraphOptions {
            val p = raw.orEmpty().split('|')
            fun number(i: Int, fallback: Float, low: Float, high: Float) = p.getOrNull(i)?.toFloatOrNull()
                ?.takeIf { it.isFinite() }?.coerceIn(low, high) ?: fallback
            return GraphOptions(number(0, .08f, 0f, 1f), number(1, 1f, .6f, 2f), number(2, .35f, .05f, 1f),
                p.getOrNull(3)?.toBooleanStrictOrNull() ?: true, p.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
                p.getOrNull(5)?.toBooleanStrictOrNull() ?: true,
                number(6, 1f, .55f, 1.8f), number(7, 1f, .35f, 2f), number(8, .82f, .55f, .94f))
        }
    }
}

data class GraphPresentation(val edges: List<GraphEdge>, val importance: List<Float>, val visibleNodes: Set<Int>)
fun presentGraph(graph: LibraryGraph, options: GraphOptions): GraphPresentation {
    val edges = graph.edges.filter { it.strength >= options.threshold }
    val weights = FloatArray(graph.nodes.size)
    edges.forEach { weights[it.a] += it.strength; weights[it.b] += it.strength }
    val largest = weights.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val importance = weights.map { ln(1f + it) / ln(1f + largest) }
    return GraphPresentation(edges, importance,
        graph.nodes.indices.filter { !options.hideIsolated || weights[it] > 0 }.toSet())
}

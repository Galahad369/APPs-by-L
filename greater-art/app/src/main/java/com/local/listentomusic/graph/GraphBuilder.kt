package com.local.listentomusic.graph

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class GraphInput(val id: String, val filename: String)
data class GraphEdge(val a: Int, val b: Int, val strength: Float)
data class GraphPoint(val x: Float, val y: Float)
data class LibraryGraph(val nodes: List<GraphInput>, val edges: List<GraphEdge>, val points: List<GraphPoint>)

object GraphBuilder {
    const val NEIGHBORS = 6
    const val EXACT_LIMIT = 1000
    private const val CANDIDATE_LIMIT = 192

    /** Exact top-K for ordinary libraries; bounded, deterministic approximate search above 1,000. */
    suspend fun edges(nodes: List<GraphInput>): List<GraphEdge> {
        val f = nodes.map { FilenameSimilarity.features(it.filename) }
        val postings = mutableMapOf<String, MutableList<Int>>()
        if (nodes.size > EXACT_LIMIT) f.forEachIndexed { i, item ->
            (item.tokens.map { "t:$it" } + item.bigrams.keys.map { "b:$it" } + item.chars.keys.map { "c:$it" })
                .forEach { postings.getOrPut(it) { mutableListOf() }.add(i) }
        }
        val result = linkedMapOf<Long, GraphEdge>()
        nodes.indices.forEach { i ->
            coroutineContext.ensureActive()
            val candidates = if (nodes.size <= EXACT_LIMIT) nodes.indices.toList() else {
                val item = f[i]
                val keys = (item.tokens.map { "t:$it" } + item.bigrams.keys.map { "b:$it" } + item.chars.keys.map { "c:$it" })
                    .sortedWith(compareBy<String> { postings[it]?.size ?: 0 }.thenBy { it })
                val chosen = linkedSetOf<Int>()
                for (key in keys) {
                    val bucket = postings.getValue(key)
                    // Rotate frequent postings so the first files do not monopolize all links.
                    val start = (i.toLong() * 31 % bucket.size).toInt()
                    repeat(minOf(bucket.size, 32)) { offset -> chosen.add(bucket[(start + offset) % bucket.size]) }
                    if (chosen.size >= CANDIDATE_LIMIT) break
                }
                chosen.take(CANDIDATE_LIMIT)
            }
            candidates.asSequence().filter { it != i }
                .map { it to FilenameSimilarity.score(f[i], f[it]).toFloat() }
                .filter { it.second > 0f }
                .sortedWith(compareByDescending<Pair<Int, Float>> { it.second }.thenBy { it.first })
                .take(NEIGHBORS).forEach { (j, strength) ->
                    val a = minOf(i, j); val b = maxOf(i, j)
                    result[(a.toLong() shl 32) or b.toLong()] = GraphEdge(a, b, strength)
                }
        }
        return result.values.toList()
    }
}

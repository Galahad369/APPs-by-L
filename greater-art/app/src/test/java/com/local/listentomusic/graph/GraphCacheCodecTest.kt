package com.local.listentomusic.graph

import java.io.*
import org.junit.Assert.*
import org.junit.Test

class GraphCacheCodecTest {
    private val nodes = listOf(GraphInput("a", "Song.mp3"), GraphInput("b", "Song live.mp4"))
    private val graph = LibraryGraph(nodes, listOf(GraphEdge(0, 1, 0.5f)), listOf(GraphPoint(0f, 1f), GraphPoint(2f, 3f)))
    @Test fun fingerprintIgnoresScannerOrderButInvalidatesRenamesAndMembership() {
        val key = GraphCacheCodec.fingerprint(nodes)
        assertEquals(key, GraphCacheCodec.fingerprint(nodes.reversed()))
        assertNotEquals(key, GraphCacheCodec.fingerprint(nodes.dropLast(1)))
        assertNotEquals(key, GraphCacheCodec.fingerprint(nodes.map { it.copy(filename = "Renamed.mp3") }))
    }
    private fun bytes(g: LibraryGraph = graph) = ByteArrayOutputStream().also {
        GraphCacheCodec.write(DataOutputStream(it), "key", g)
    }.toByteArray()
    @Test fun roundTripRetainsEdgesAndSettledPositions() {
        assertEquals(graph, GraphCacheCodec.read(DataInputStream(ByteArrayInputStream(bytes())), "key", nodes))
    }
    @Test fun truncatedCacheIsRejected() {
        assertTrue(runCatching { GraphCacheCodec.read(DataInputStream(ByteArrayInputStream(bytes().take(8).toByteArray())), "key", nodes) }.isFailure)
    }
    @Test fun staleKeyAndInvalidPositionsAreRejected() {
        assertTrue(runCatching { GraphCacheCodec.read(DataInputStream(ByteArrayInputStream(bytes())), "stale", nodes) }.isFailure)
        val corrupt = graph.copy(points = listOf(GraphPoint(Float.NaN, 0f), GraphPoint(0f, 0f)))
        assertTrue(runCatching { GraphCacheCodec.read(DataInputStream(ByteArrayInputStream(bytes(corrupt))), "key", nodes) }.isFailure)
    }
}

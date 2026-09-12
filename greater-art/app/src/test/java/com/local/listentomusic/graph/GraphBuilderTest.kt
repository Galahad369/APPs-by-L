package com.local.listentomusic.graph

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GraphBuilderTest {
    @Test fun sharedLetterExampleHasAnEdge() = runBlocking {
        val edges = GraphBuilder.edges(listOf(GraphInput("a", "Caravan.mp3"), GraphInput("b", "Gambling.mp4")))
        assertEquals(1, edges.size)
        assertTrue(edges.single().strength > 0)
    }
    @Test fun sparseUndirectedDeterministicGraph() = runBlocking {
        val nodes = List(80) { GraphInput("$it", "Artist ${it % 8} song $it.mp3") }
        val edges = GraphBuilder.edges(nodes)
        assertTrue(edges.size <= nodes.size * GraphBuilder.NEIGHBORS)
        assertEquals(edges, GraphBuilder.edges(nodes))
        assertEquals(edges.size, edges.map { it.a to it.b }.toSet().size)
        assertTrue(edges.all { it.a < it.b && it.strength in 0f..1f })
    }
    @Test fun layoutKeepsIsolatedNodesAndFiniteCoordinates() = runBlocking {
        val nodes = listOf(GraphInput("a", "abc"), GraphInput("b", "漢字"), GraphInput("c", ""))
        val edges = GraphBuilder.edges(nodes)
        assertTrue(edges.isEmpty())
        val positions = GraphLayout.settle(nodes, edges)
        assertEquals(3, positions.size)
        assertTrue(positions.all { it.x.isFinite() && it.y.isFinite() })
        assertEquals(positions, GraphLayout.settle(nodes, edges))
    }
    @Test fun emptyAndSingleLibraryWork() = runBlocking {
        assertTrue(GraphLayout.settle(emptyList(), emptyList()).isEmpty())
        assertTrue(GraphBuilder.edges(listOf(GraphInput("a", "one"))).isEmpty())
    }
    @Test fun largeLibraryCandidateSearchStaysBounded() = runBlocking {
        val nodes = List(1200) { GraphInput("$it", "Artist ${it % 20} track $it.mp3") }
        val edges = GraphBuilder.edges(nodes)
        assertTrue(edges.size <= nodes.size * GraphBuilder.NEIGHBORS)
        assertTrue(edges.all { it.a in nodes.indices && it.b in nodes.indices })
        assertEquals(nodes.size, GraphLayout.settle(nodes, edges).size)
    }
}

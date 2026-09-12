package com.local.listentomusic.graph
import org.junit.Assert.*
import org.junit.Test

class GraphOptionsTest {
    @Test fun preferencesRoundTrip() {
        val options = GraphOptions(.5f, 1.5f, .8f, false, true, false)
        assertEquals(options, GraphOptions.decode(options.encode()))
    }
    @Test fun malformedNumbersAreSafe() {
        val options = GraphOptions.decode("NaN|Infinity|-5|bad|bad|bad")
        assertEquals(.08f, options.threshold, .0001f); assertEquals(1f, options.nodeSize, .0001f)
        assertEquals(.05f, options.edgeOpacity, .0001f)
    }
    @Test fun strongConnectionsIncreaseNodeImportance() {
        val graph = LibraryGraph(List(4) { GraphInput("$it", "$it") },
            listOf(GraphEdge(0,1,.9f),GraphEdge(0,2,.8f),GraphEdge(1,2,.1f)), emptyList())
        val p = presentGraph(graph, GraphOptions(threshold=.5f,hideIsolated=true))
        assertEquals(2,p.edges.size); assertTrue(p.importance[0] > p.importance[1]); assertFalse(3 in p.visibleNodes)
    }
}

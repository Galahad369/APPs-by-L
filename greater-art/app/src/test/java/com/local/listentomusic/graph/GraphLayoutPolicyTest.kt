package com.local.listentomusic.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphLayoutPolicyTest {
    @Test fun strongerLinksHaveShorterTargetDistance() {
        assertTrue(graphLinkTargetDistance(.9f, 1f) < graphLinkTargetDistance(.5f, 1f))
        assertTrue(graphLinkTargetDistance(.5f, 1f) < graphLinkTargetDistance(.1f, 1f))
    }

    @Test fun strongestWeightedNodeBecomesHub() {
        val edges = listOf(GraphEdge(0, 1, .8f), GraphEdge(0, 2, .7f), GraphEdge(1, 3, .2f))
        assertEquals(0, strongestGraphHub(4, edges))
    }
}

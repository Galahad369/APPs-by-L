package com.local.listentomusic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueSnapshotTest {
    @Test fun unscannedEntriesStayInSessionOrder() {
        val ids = listOf("b", "a", "c")
        assertEquals(listOf("session-b", "scanned-a", "session-c"),
            reconcileSessionQueue(ids, mapOf("a" to "scanned-a")) { "session-${ids[it]}" })
    }
    @Test fun coldScannerDoesNotEmptyQueue() {
        val ids = listOf("one", "two")
        assertEquals(ids, reconcileSessionQueue(ids, emptyMap()) { ids[it] })
    }
    @Test fun duplicateSessionEntriesArePreserved() {
        val ids = listOf("a", "a")
        assertEquals(listOf("known", "known"), reconcileSessionQueue(ids, mapOf("a" to "known")) { "fallback" })
    }
}

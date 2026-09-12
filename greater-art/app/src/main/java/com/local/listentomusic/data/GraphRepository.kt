package com.local.listentomusic.data

import android.content.Context
import android.util.AtomicFile
import com.local.listentomusic.graph.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*

class GraphRepository(context: Context) {
    private val cache = AtomicFile(File(context.cacheDir, "filename-graph-v1.bin"))
    private val mutex = Mutex()
    private var memoryKey: String? = null
    private var memory: LibraryGraph? = null

    suspend fun load(input: List<GraphInput>): LibraryGraph = mutex.withLock {
        val nodes = withContext(Dispatchers.Default) { input.distinctBy { it.id }.sortedBy { it.id } }
        val key = withContext(Dispatchers.Default) { GraphCacheCodec.fingerprint(nodes) }
        if (memoryKey == key) memory?.let { return@withLock it }
        val disk = withContext(Dispatchers.IO) { read(key, nodes) }
        val graph = disk ?: withContext(Dispatchers.Default) {
            val edges = GraphBuilder.edges(nodes)
            LibraryGraph(nodes, edges, GraphLayout.settle(nodes, edges))
        }
        if (disk == null) withContext(Dispatchers.IO) { write(key, graph) }
        memoryKey = key; memory = graph
        graph
    }

    private fun read(key: String, nodes: List<GraphInput>): LibraryGraph? = runCatching {
        DataInputStream(BufferedInputStream(cache.openRead())).use { input ->
            GraphCacheCodec.read(input, key, nodes)
        }
    }.getOrNull()

    private fun write(key: String, graph: LibraryGraph) {
        var stream: FileOutputStream? = null
        try {
            stream = cache.startWrite()
            val output = DataOutputStream(BufferedOutputStream(stream))
            GraphCacheCodec.write(output, key, graph)
            output.flush(); cache.finishWrite(stream)
        } catch (_: IOException) { stream?.let(cache::failWrite) }
    }
}

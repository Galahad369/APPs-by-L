package com.local.listentomusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.graph.LibraryGraph
import kotlin.math.*

/** Nodes is the left page of Library. Canvas consumes graph gestures; the header still pages. */
@Composable
fun NodesScreen(graph: LibraryGraph?, loading: Boolean, error: String?, currentPath: String?,
    contentPadding: PaddingValues, onLibrary: () -> Unit, onRetry: () -> Unit, onPlay: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Nodes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            TextButton(onClick = onLibrary) { Text("Library →") }
        }
        when {
            error != null -> { Text(error, Modifier.padding(16.dp)); TextButton(onClick = onRetry) { Text("Retry") } }
            graph == null -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparing filename connections…", Modifier.padding(16.dp)) }
            graph.nodes.isEmpty() -> Text("Your library is empty. Add media in Download, then refresh Library.", Modifier.padding(16.dp))
            else -> {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                GraphCanvas(graph, currentPath, onPlay, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GraphCanvas(graph: LibraryGraph, currentPath: String?, onPlay: (String) -> Unit, modifier: Modifier) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(graph) { mutableFloatStateOf(1f) }
    var pan by remember(graph) { mutableStateOf(Offset.Zero) }
    val moved = remember(graph) { mutableStateMapOf<Int, Offset>() }
    var selected by remember(graph) { mutableIntStateOf(-1) }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val color = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val labelPaint = remember(ink) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = ink.toArgb(); textSize = 28f
    } }
    val onPlayCurrent by rememberUpdatedState(onPlay)
    fun fit() {
        val w = (graph.points.maxOf { it.x } - graph.points.minOf { it.x } + 100f).coerceAtLeast(100f)
        val h = (graph.points.maxOf { it.y } - graph.points.minOf { it.y } + 100f).coerceAtLeast(100f)
        scale = min(viewport.width / w, viewport.height / h).coerceIn(0.08f, 3f)
        pan = Offset(-(graph.points.maxOf { it.x } + graph.points.minOf { it.x }) / 2 * scale,
            -(graph.points.maxOf { it.y } + graph.points.minOf { it.y }) / 2 * scale)
        moved.clear()
    }
    LaunchedEffect(graph, viewport) { if (viewport.width > 0 && viewport.height > 0) fit() }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { fit() }) { Text("Fit graph") }
            TextButton(onClick = { picker = true }) { Text("Find / play a node") }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).onSizeChanged { viewport = it }
            .semantics { contentDescription = "Filename similarity graph. Pan or pinch to explore, drag nodes, tap to play. Use Find / play a node for a text list." }
            .pointerInput(graph) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    fun position(i: Int) = moved[i] ?: Offset(graph.points[i].x, graph.points[i].y)
                    val hit = graph.nodes.indices.minByOrNull { (position(it) * scale + pan + center - down.position).getDistanceSquared() }
                        ?.takeIf { (position(it) * scale + pan + center - down.position).getDistance() < 28.dp.toPx() }
                    var travelled = 0f
                    var multi = false
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val delta = event.calculatePan()
                        multi = multi || event.changes.count { it.pressed } > 1
                        travelled += delta.getDistance()
                        if (multi) {
                            val old = scale
                            scale = (scale * zoom).coerceIn(0.08f, 8f)
                            val pointers = event.changes.filter { it.pressed }
                            val focus = if (pointers.isEmpty()) center else pointers.map { it.position }.reduce(Offset::plus) / pointers.size.toFloat()
                            pan = (pan - (focus - center)) * (scale / old) + (focus - center) + delta
                        } else if (travelled > viewConfiguration.touchSlop) {
                            if (hit != null) { moved[hit] = position(hit) + delta / scale; selected = hit }
                            else pan += delta
                        }
                        if (multi || travelled > viewConfiguration.touchSlop) event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (!multi && travelled <= viewConfiguration.touchSlop && hit != null) {
                        selected = hit; onPlayCurrent(graph.nodes[hit].id)
                    }
                }
            }) {
            val center = Offset(size.width / 2, size.height / 2)
            val positions = graph.points.mapIndexed { i, p -> (moved[i] ?: Offset(p.x, p.y)) * scale + pan + center }
            fun visible(p: Offset) = p.x in -40f..size.width + 40 && p.y in -40f..size.height + 40
            graph.edges.forEach { edge ->
                val a = positions[edge.a]; val b = positions[edge.b]
                if (visible(a) || visible(b)) drawLine(color.copy(alpha = 0.08f + edge.strength * 0.45f), a, b,
                    (0.6f + edge.strength * 2.5f) * density)
            }
            var labels = 0
            graph.nodes.forEachIndexed { i, node ->
                val p = positions[i]
                if (visible(p)) {
                    val active = node.id == currentPath || i == selected
                    drawCircle(color.copy(alpha = if (active) 0.18f else 0.06f), if (active) 15.dp.toPx() else 9.dp.toPx(), p)
                    drawCircle(if (active) color else ink.copy(alpha = 0.75f), (if (active) 6f else 3.5f).dp.toPx(), p)
                    if (active || scale > 1.3f && labels++ < 35) drawIntoCanvas {
                        labelPaint.textSize = 12.dp.toPx()
                        it.nativeCanvas.drawText(node.filename.take(38), p.x + 10.dp.toPx(), p.y - 10.dp.toPx(), labelPaint)
                    }
                }
            }
        }
        Text("Pinch to zoom · Drag to move · Tap to play", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Find a node") }, text = {
        Column {
            OutlinedTextField(query, { query = it }, label = { Text("Filename") }, singleLine = true)
            val found = remember(graph, query) { graph.nodes.filter { it.filename.contains(query, ignoreCase = true) } }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(found, key = { it.id }) { node -> TextButton(onClick = { picker = false; onPlay(node.id) }) { Text(node.filename) } }
            }
        }
    }, confirmButton = { TextButton(onClick = { picker = false }) { Text("Close") } })
}

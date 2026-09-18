package com.local.listentomusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.graph.LibraryGraph
import com.local.listentomusic.graph.GraphOptions
import com.local.listentomusic.graph.presentGraph
import com.local.listentomusic.graph.GraphLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.*

/** Nodes is to the right of Library: swipe left to enter. The header still pages. */
@Composable
fun NodesScreen(graph: LibraryGraph?, loading: Boolean, error: String?, currentPath: String?,
    contentPadding: PaddingValues, onLibrary: () -> Unit, onRetry: () -> Unit, onPlay: (String) -> Unit,
    options: GraphOptions, onOptions: (GraphOptions) -> Unit, language: com.local.listentomusic.data.AppLanguage) {
    Column(Modifier.fillMaxSize().inspectElement("NODES_SCREEN", "Filename-similarity graph page")
        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
        .padding(contentPadding).consumeWindowInsets(contentPadding).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(uiText(language, "Nodes", "Nodes"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            TextButton(onClick = onLibrary) { Text(uiText(language, "← Library", "← Library")) }
        }
        when {
            error != null -> { Text(error, Modifier.padding(16.dp)); TextButton(onClick = onRetry) { Text(uiText(language, "Retry", "Retry")) } }
            graph == null -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(uiText(language, "Preparing filename connections…", "Preparing filename connections…"), Modifier.padding(16.dp)) }
            graph.nodes.isEmpty() -> Text(uiText(language, "Your library is empty. Add media in Download, then refresh Library.", "Your library is empty. Add media in Download, then refresh Library."), Modifier.padding(16.dp))
            else -> {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                GraphCanvas(graph, currentPath?.let { com.local.listentomusic.model.sourceMediaPath(it) }, onPlay, Modifier.weight(1f), options, onOptions, language)
            }
        }
    }
}

@Composable
private fun GraphCanvas(graph: LibraryGraph, currentPath: String?, onPlay: (String) -> Unit, modifier: Modifier,
    options: GraphOptions, onOptions: (GraphOptions) -> Unit, language: com.local.listentomusic.data.AppLanguage) {
    val presentation = remember(graph, options) { presentGraph(graph, options) }
    val points by produceState(initialValue = graph.points, graph, options.linkDistance, options.repulsion, options.bounce) {
        value = if (options.linkDistance == 1f && options.repulsion == 1f && options.bounce == .82f) graph.points
        else withContext(Dispatchers.Default) {
            GraphLayout.settle(graph.nodes, presentation.edges, options.linkDistance, options.repulsion, options.bounce, graph.points)
        }
    }
    var controls by remember { mutableStateOf(false) }
    val reveal = remember(graph) { Animatable(1f) }
    val organicMotion = rememberInfiniteTransition(label = "graph-organic-motion")
    val jigglePhase by organicMotion.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2f).toFloat(),
        animationSpec = infiniteRepeatable(tween(7_500, easing = LinearEasing), RepeatMode.Restart),
        label = "graph-jiggle-phase",
    )
    val animationScope = rememberCoroutineScope()
    val revealRank = remember(presentation.importance) {
        IntArray(graph.nodes.size).also { rank ->
            graph.nodes.indices.sortedByDescending { presentation.importance[it] }
                .forEachIndexed { order, node -> rank[node] = order }
        }
    }
    val current = remember(graph, currentPath) { graph.nodes.indexOfFirst { it.id == currentPath } }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(graph) { mutableFloatStateOf(1f) }
    var pan by remember(graph) { mutableStateOf(Offset.Zero) }
    val moved = remember(graph) { mutableStateMapOf<Int, Offset>() }
    val flex = remember(graph) { mutableStateMapOf<Int, Offset>() }
    var flexTarget by remember(graph) { mutableFloatStateOf(0f) }
    val flexAmount by animateFloatAsState(
        targetValue = flexTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        finishedListener = { if (it == 0f) flex.clear() },
        label = "graph neighbor flex",
    )
    var selected by remember(graph) { mutableIntStateOf(-1) }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val color = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val ink = MaterialTheme.colorScheme.onSurface
    val labelPaint = remember(ink) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = ink.toArgb(); textSize = 28f
    } }
    val onPlayCurrent by rememberUpdatedState(onPlay)
    fun fit() {
        // GraphLayout guarantees the strongest weighted hub is (0,0). Fit around
        // that origin instead of the bounding-box midpoint so the hub stays centered.
        val w = (points.maxOf { abs(it.x) } * 2f + 100f).coerceAtLeast(100f)
        val h = (points.maxOf { abs(it.y) } * 2f + 100f).coerceAtLeast(100f)
        scale = min(viewport.width / w, viewport.height / h).coerceIn(0.08f, 3f)
        pan = Offset.Zero
        moved.clear()
        flex.clear()
    }
    LaunchedEffect(points, viewport) { if (viewport.width > 0 && viewport.height > 0) fit() }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    animationScope.launch {
                        reveal.snapTo(0f)
                        reveal.animateTo(1f, tween(1_100))
                    }
                },
                modifier = Modifier.inspectElement("GRAPH_MAGIC_WAND", "Finite hub-first node reveal and bounce"),
            ) { Icon(Icons.Rounded.AutoFixHigh, uiText(language, "Animate nodes", "展開節點")) }
            TextButton(onClick = { fit() }, modifier = Modifier.inspectElement("GRAPH_FIT_BUTTON", "Fits all visible nodes")) { Text(uiText(language, "Fit", "Fit")) }
            TextButton(onClick = { picker = true }, modifier = Modifier.inspectElement("GRAPH_FIND_BUTTON", "Find a node by filename")) { Text(uiText(language, "Find", "Find")) }
            TextButton(onClick = {
                if (current >= 0) { scale = 2f; pan = Offset(-points[current].x * scale, -points[current].y * scale) }
            }, enabled = current >= 0, modifier = Modifier.inspectElement("GRAPH_PLAYING_BUTTON", "Centers the currently playing node")) { Text(uiText(language, "Playing", "Playing")) }
            TextButton(onClick = { controls = true }, modifier = Modifier.inspectElement("GRAPH_CONTROLS_BUTTON", "Graph display and force settings")) { Text(uiText(language, "Controls", "Controls")) }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).inspectElement("NODES_CANVAS", "Pan, pinch, drag, or tap a media node")
            .onSizeChanged { viewport = it }
            .semantics { contentDescription = "Filename similarity graph. Pan or pinch to explore, drag nodes, tap to play. Use Find / play a node for a text list." }
            .pointerInput(graph, presentation.visibleNodes, current) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    fun basePosition(i: Int) = moved[i] ?: Offset(points[i].x, points[i].y)
                    fun position(i: Int) = basePosition(i) + (flex[i] ?: Offset.Zero) * flexAmount
                    val hit = (presentation.visibleNodes + listOfNotNull(current.takeIf { it >= 0 })).minByOrNull { (position(it) * scale + pan + center - down.position).getDistanceSquared() }
                        ?.takeIf { (position(it) * scale + pan + center - down.position).getDistance() < 28.dp.toPx() }
                    var travelled = 0f
                    var multi = false
                    var dragTotal = Offset.Zero
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
                            if (hit != null) {
                                dragTotal += delta / scale
                                moved[hit] = basePosition(hit) + delta / scale
                                selected = hit
                                presentation.edges.asSequence().filter { it.a == hit || it.b == hit }.take(8).forEach { edge ->
                                    val neighbor = if (edge.a == hit) edge.b else edge.a
                                    flex[neighbor] = dragTotal * (edge.strength * .28f)
                                }
                                flexTarget = 1f
                            }
                            else pan += delta
                        }
                        if (multi || travelled > viewConfiguration.touchSlop) event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (!multi && travelled <= viewConfiguration.touchSlop && hit != null) {
                        selected = hit; onPlayCurrent(graph.nodes[hit].id)
                    }
                    if (flex.isNotEmpty()) {
                        flexTarget = 0f
                    }
                }
            }) {
            val center = Offset(size.width / 2, size.height / 2)
            fun nodePhase(i: Int): Float {
                val count = graph.nodes.size.coerceAtLeast(1)
                val delay = revealRank[i].toFloat() / count * .72f
                return ((reveal.value - delay) / .28f).coerceIn(0f, 1f)
            }
            val positions = points.mapIndexed { i, p ->
                val importance = presentation.importance.getOrElse(i) { 0f }
                // Small deterministic drift gives the settled force graph Obsidian-like
                // life without restarting the expensive physics solver on every frame.
                val amplitude = (1.1f + (1f - importance) * 1.7f).dp.toPx()
                val phase = jigglePhase + i * 1.618f
                val jiggle = Offset(sin(phase) * amplitude, cos(phase + i * .37f) * amplitude)
                val settled = ((moved[i] ?: Offset(p.x, p.y)) + (flex[i] ?: Offset.Zero) * flexAmount) * scale + pan + center + jiggle
                center + (settled - center) * nodePhase(i)
            }
            fun visible(p: Offset) = p.x in -40f..size.width + 40 && p.y in -40f..size.height + 40
            presentation.edges.forEach { edge ->
                val a = positions[edge.a]; val b = positions[edge.b]
                val connected = edge.a == current || edge.b == current || edge.a == selected || edge.b == selected
                val revealAlpha = min(nodePhase(edge.a), nodePhase(edge.b))
                if (revealAlpha > 0f && (visible(a) || visible(b))) drawLine((if (connected) color else ink).copy(alpha =
                    (revealAlpha * options.edgeOpacity * (if (connected) 1.4f else .65f) * (.35f + edge.strength)).coerceIn(.02f, 1f)), a, b,
                    (0.45f + edge.strength * (if (connected) 1.5f else .75f)) * density)
            }
            var labels = 0
            graph.nodes.forEachIndexed { i, node ->
                val p = positions[i]
                if (visible(p) && (i in presentation.visibleNodes || i == current)) {
                    val playing = i == current
                    val active = playing || i == selected
                    val importance = if (options.sizeByConnections) presentation.importance[i] else 0f
                    val phase = nodePhase(i)
                    if (phase <= 0f) return@forEachIndexed
                    val pop = phase + .18f * sin(phase * PI).toFloat()
                    // Weighted hubs must read at a glance, not as a one-pixel difference.
                    val radius = ((3.2f + importance.pow(.68f) * 10.8f) * options.nodeSize).dp.toPx() * pop
                    if (active) drawCircle(color.copy(alpha = .14f), radius + 8.dp.toPx(), p)
                    drawCircle(if (active) color else ink.copy(alpha = .48f + importance * .4f), radius, p)
                    if (playing) {
                        drawCircle(color, radius + 4.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
                        drawPath(Path().apply { moveTo(p.x-2.dp.toPx(), p.y-3.dp.toPx()); lineTo(p.x+3.dp.toPx(),p.y); lineTo(p.x-2.dp.toPx(),p.y+3.dp.toPx()); close() }, onAccent)
                    }
                    if (active || options.showLabels && scale > .8f && labels++ < 45) drawIntoCanvas {
                        labelPaint.textSize = 12.dp.toPx()
                        labelPaint.isFakeBoldText = active || importance > .7f
                        it.nativeCanvas.drawText(node.filename.substringBeforeLast('.').take(38), p.x + radius + 5.dp.toPx(), p.y + 4.dp.toPx(), labelPaint)
                    }
                }
            }
        }
        Text(uiText(language, "Pinch to zoom · Drag to move · Tap to play", "Pinch to zoom · Drag to move · Tap to play"), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
    }
    if (controls) GraphControls(options, { onOptions(it); controls = false }, { controls = false }, language)
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text(uiText(language, "Find a node", "Find a node")) }, text = {
        Column {
            OutlinedTextField(query, { query = it }, label = { Text(uiText(language, "Filename", "Filename")) }, singleLine = true)
            val found = remember(graph, query) { graph.nodes.filter { it.filename.contains(query, ignoreCase = true) } }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(found, key = { it.id }) { node -> TextButton(onClick = { picker = false; onPlay(node.id) }) { Text(node.filename) } }
            }
        }
    }, confirmButton = { TextButton(onClick = { picker = false }) { Text(uiText(language, "Close", "Close")) } })
}

@Composable
private fun GraphControls(options: GraphOptions, onSave: (GraphOptions) -> Unit, onClose: () -> Unit, language: com.local.listentomusic.data.AppLanguage) {
    var edit by remember { mutableStateOf(options) }
    AlertDialog(onDismissRequest = onClose, title = { Text(uiText(language, "Graph controls", "Graph controls")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(uiText(language, "Connection strength", "關聯強度") + " · ${(edit.threshold * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(edit.threshold, { edit = edit.copy(threshold = it) }, valueRange = 0f..1f)
            Text(uiText(language, "Node size", "Node size"), style = MaterialTheme.typography.labelLarge)
            Slider(edit.nodeSize, { edit = edit.copy(nodeSize = it) }, valueRange = .6f..2f)
            Text(uiText(language, "Link visibility", "Link visibility"), style = MaterialTheme.typography.labelLarge)
            Slider(edit.edgeOpacity, { edit = edit.copy(edgeOpacity = it) }, valueRange = .05f..1f)
            Text(uiText(language, "Link length", "Link length"), style = MaterialTheme.typography.labelLarge)
            Slider(edit.linkDistance, { edit = edit.copy(linkDistance = it) }, valueRange = .55f..1.8f)
            Text(uiText(language, "Node repulsion", "Node repulsion"), style = MaterialTheme.typography.labelLarge)
            Slider(edit.repulsion, { edit = edit.copy(repulsion = it) }, valueRange = .35f..2f)
            Text(uiText(language, "Elasticity", "Elasticity"), style = MaterialTheme.typography.labelLarge)
            Slider(edit.bounce, { edit = edit.copy(bounce = it) }, valueRange = .55f..0.94f)
            GraphToggle(uiText(language, "Filename labels", "Filename labels"), edit.showLabels) { edit = edit.copy(showLabels = it) }
            GraphToggle(uiText(language, "Hide isolated nodes", "Hide isolated nodes"), edit.hideIsolated) { edit = edit.copy(hideIsolated = it) }
            GraphToggle(uiText(language, "Size by strong connections", "Size by strong connections"), edit.sizeByConnections) { edit = edit.copy(sizeByConnections = it) }
            Text(uiText(language, "The double-ring node is playing. Larger nodes have more strong filename connections.", "The double-ring node is playing. Larger nodes have more strong filename connections."), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { edit = GraphOptions() }) { Text(uiText(language, "Restore graph defaults", "Restore graph defaults")) }
        }
    }, confirmButton = { TextButton(onClick = { onSave(edit) }) { Text(uiText(language, "Apply", "Apply")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(uiText(language, "Cancel", "Cancel")) } })
}

@Composable
private fun GraphToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange)
    }
}

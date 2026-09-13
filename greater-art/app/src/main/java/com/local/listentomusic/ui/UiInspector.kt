package com.local.listentomusic.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

internal data class InspectorRegion(
    val label: String,
    val detail: String,
    val bounds: Rect,
    val order: Long,
)

@Stable
internal class UiInspectorState {
    internal val regions = mutableStateMapOf<Any, InspectorRegion>()
    var armed by mutableStateOf(false)
    var selected by mutableStateOf<InspectorRegion?>(null)
        private set
    var lastTouch by mutableStateOf(Offset.Zero)
        private set
    private var order = 0L

    fun update(key: Any, label: String, detail: String, bounds: Rect) {
        if (!bounds.left.isFinite() || !bounds.top.isFinite() || bounds.width <= 0f || bounds.height <= 0f) return
        val previous = regions[key]
        if (previous?.label == label && previous.detail == detail && previous.bounds == bounds) return
        regions[key] = InspectorRegion(label, detail, bounds, previous?.order ?: ++order)
    }

    fun remove(key: Any) { regions.remove(key) }
    fun arm() { selected = null; armed = true }
    fun cancel() { armed = false }
    fun clearSelection() { selected = null }
    fun clear() { armed = false; selected = null; regions.clear() }

    fun pick(point: Offset) {
        lastTouch = point
        selected = regions.values.asSequence()
            .filter { it.bounds.contains(point) }
            .sortedWith(compareBy<InspectorRegion> { it.bounds.width * it.bounds.height }.thenByDescending { it.order })
            .firstOrNull()
            ?: InspectorRegion("UNREGISTERED_AREA", "No tagged Compose element at this point", Rect(point, 1f), Long.MAX_VALUE)
        armed = false
    }
}

private val LocalUiInspector = compositionLocalOf<UiInspectorState?> { null }

internal fun Modifier.inspectElement(label: String, detail: String = ""): Modifier = composed {
    val inspector = LocalUiInspector.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(inspector, key) { onDispose { inspector.remove(key) } }
    onGloballyPositioned { inspector.update(key, label, detail, it.boundsInRoot()) }
}

@Composable
internal fun UiInspectorHost(
    enabled: Boolean,
    state: UiInspectorState,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    // Inspector colors are independent of the selected app theme. Debug text must
    // remain readable even when a custom background or broken palette is under it.
    val inspectorError = Color(0xFFFF5C68)
    val inspectorAccent = Color(0xFF75EBD4)
    LaunchedEffect(enabled) { if (!enabled) state.clear() }
    BackHandler(enabled && state.armed) { state.cancel() }
    CompositionLocalProvider(LocalUiInspector provides state) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (enabled && state.armed) {
                Box(
                    Modifier.matchParentSize().zIndex(100f)
                        .background(Color.Black.copy(alpha = 0.08f))
                        .pointerInput(Unit) { detectTapGestures { state.pick(it) } },
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).zIndex(101f),
                    color = Color.Black.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TAP AN ELEMENT", Modifier.padding(horizontal = 12.dp), color = Color.White,
                            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = state::cancel) { Icon(Icons.Rounded.Close, "Cancel inspector", tint = Color.White) }
                    }
                }
            }
            if (enabled) state.selected?.let { region ->
                Canvas(Modifier.matchParentSize().zIndex(98f)) {
                    drawRect(inspectorError.copy(alpha = 0.10f), region.bounds.topLeft, region.bounds.size)
                    drawRect(inspectorError, region.bounds.topLeft, region.bounds.size,
                        style = Stroke(2.dp.toPx()))
                    drawLine(inspectorAccent, Offset(state.lastTouch.x - 10.dp.toPx(), state.lastTouch.y),
                        Offset(state.lastTouch.x + 10.dp.toPx(), state.lastTouch.y), 1.dp.toPx())
                    drawLine(inspectorAccent, Offset(state.lastTouch.x, state.lastTouch.y - 10.dp.toPx()),
                        Offset(state.lastTouch.x, state.lastTouch.y + 10.dp.toPx()), 1.dp.toPx())
                }
                val report = remember(region, state.lastTouch, density.density) {
                    buildString {
                        appendLine(region.label)
                        if (region.detail.isNotBlank()) appendLine(region.detail)
                        appendLine("boundsPx=${region.bounds.left.toInt()},${region.bounds.top.toInt()} → ${region.bounds.right.toInt()},${region.bounds.bottom.toInt()}")
                        appendLine("sizePx=${region.bounds.width.toInt()}×${region.bounds.height.toInt()}")
                        appendLine("touchPx=${state.lastTouch.x.toInt()},${state.lastTouch.y.toInt()}")
                        append("sizeDp=${(region.bounds.width / density.density).toInt()}×${(region.bounds.height / density.density).toInt()}")
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).zIndex(99f),
                    color = Color(0xFF090D0E).copy(alpha = 0.98f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 6.dp,
                ) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(report, Modifier.weight(1f), color = Color.White, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = {
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Greater Art element", report))
                        }) { Text("COPY", color = inspectorAccent) }
                        IconButton(onClick = state::clearSelection) { Icon(Icons.Rounded.Close, "Close selection", tint = Color.White) }
                    }
                }
            }
        }
    }
}

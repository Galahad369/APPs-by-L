package com.local.pixelmeasure

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class LoadedImage(val bitmap: Bitmap, val sourceWidth: Int, val sourceHeight: Int)
private enum class DisplayUnit { PX, DP }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PixelMeasureTheme { PixelMeasureScreen() } }
    }
}

@Composable
private fun PixelMeasureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6EE7D8), secondary = Color(0xFFFFCC66),
            background = Color(0xFF090C0D), surface = Color(0xFF111719),
            onPrimary = Color(0xFF06110F), onBackground = Color(0xFFE9F0EF),
            onSurface = Color(0xFFE9F0EF),
        ), content = content,
    )
}

@Composable
fun PixelMeasureScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var loaded by remember { mutableStateOf<LoadedImage?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pointA by remember { mutableStateOf<Offset?>(null) }
    var pointB by remember { mutableStateOf<Offset?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var showGrid by remember { mutableStateOf(true) }
    var unit by remember { mutableStateOf(DisplayUnit.PX) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = uri; pointA = null; pointB = null
        }
    }
    LaunchedEffect(imageUri) {
        val uri = imageUri ?: run { loaded = null; return@LaunchedEffect }
        loadError = null
        loaded = runCatching { withContext(Dispatchers.IO) { loadScaledBitmap(context, uri) } }
            .onFailure { loadError = "Could not read that image" }.getOrNull()
    }
    fun reset() { pointA = null; pointB = null }
    fun report() = measurementReport(pointA, pointB, viewport, loaded, density)

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)) {
        val landscape = maxWidth > maxHeight
        val canvas: @Composable (Modifier) -> Unit = { modifier ->
            MeasurementCanvas(loaded, pointA, pointB, showGrid, modifier, { viewport = it }) { p ->
                when {
                    pointA == null -> pointA = p
                    pointB == null -> pointB = p
                    else -> { pointA = p; pointB = null }
                }
            }
        }
        val controls: @Composable (Modifier) -> Unit = { modifier ->
            MeasurePanel(modifier, pointA, pointB, viewport, loaded, density, unit, showGrid,
                { unit = it }, { showGrid = it }, ::reset,
                { picker.launch(arrayOf("image/*")) },
                { imageUri = null; loaded = null; reset() },
                { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Pixel measurement", report())) })
        }
        if (landscape) Row(Modifier.fillMaxSize()) {
            canvas(Modifier.weight(1f).fillMaxHeight())
            controls(Modifier.widthIn(min = 280.dp, max = 360.dp).fillMaxHeight())
        } else Column(Modifier.fillMaxSize()) {
            canvas(Modifier.weight(1f).fillMaxWidth())
            controls(Modifier.fillMaxWidth().heightIn(max = 300.dp))
        }
        loadError?.let { Snackbar(Modifier.align(Alignment.BottomCenter).padding(12.dp)) { Text(it) } }
    }
}

@Composable
private fun MeasurementCanvas(image: LoadedImage?, a: Offset?, b: Offset?, showGrid: Boolean,
    modifier: Modifier, onSize: (IntSize) -> Unit, onTap: (Offset) -> Unit) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .10f)
    val accent = MaterialTheme.colorScheme.primary
    val second = MaterialTheme.colorScheme.secondary
    val imageBitmap = remember(image) { image?.bitmap?.asImageBitmap() }
    Canvas(modifier.background(Color(0xFF060809)).onSizeChanged(onSize)
        .pointerInput(image) { detectTapGestures(onTap = onTap) }) {
        imageBitmap?.let { bitmap ->
            val rect = fittedRect(size.width, size.height, image!!.sourceWidth, image.sourceHeight)
            drawImage(bitmap, dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
                dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()), filterQuality = FilterQuality.Medium)
            drawRect(Color.White.copy(alpha = .18f), rect.topLeft, rect.size, style = Stroke(1f))
        }
        if (showGrid) {
            val minor = 24.dp.toPx()
            var x = minor
            while (x < size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f); x += minor }
            var y = minor
            while (y < size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f); y += minor }
            drawLine(gridColor.copy(alpha = .8f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1f)
            drawLine(gridColor.copy(alpha = .8f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
        }
        fun marker(point: Offset, color: Color, label: String) {
            drawLine(color.copy(alpha=.75f), Offset(point.x - 18.dp.toPx(), point.y), Offset(point.x + 18.dp.toPx(), point.y), 1.dp.toPx())
            drawLine(color.copy(alpha=.75f), Offset(point.x, point.y - 18.dp.toPx()), Offset(point.x, point.y + 18.dp.toPx()), 1.dp.toPx())
            drawCircle(color, 5.dp.toPx(), point, style = Stroke(2.dp.toPx()))
            drawContext.canvas.nativeCanvas.drawText(label, point.x + 9.dp.toPx(), point.y - 9.dp.toPx(),
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); textSize = 13.dp.toPx(); isFakeBoldText = true })
        }
        a?.let { marker(it, accent, "A") }
        b?.let { marker(it, second, "B") }
        if (a != null && b != null) {
            drawLine(accent, a, b, 2.dp.toPx())
            drawCircle(accent.copy(alpha=.12f), 10.dp.toPx(), (a + b) / 2f)
        }
    }
}

@Composable
private fun MeasurePanel(modifier: Modifier, a: Offset?, b: Offset?, viewport: IntSize, image: LoadedImage?, density: Float,
    unit: DisplayUnit, grid: Boolean, onUnit: (DisplayUnit) -> Unit, onGrid: (Boolean) -> Unit,
    onReset: () -> Unit, onLoad: () -> Unit, onClearImage: () -> Unit, onCopy: () -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PIXEL MEASURE", style = MaterialTheme.typography.titleMedium)
                    Text("Tap A, then B. Third tap starts over.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onLoad) { Icon(Icons.Rounded.Image, "Load screenshot") }
                if (image != null) IconButton(onClick = onClearImage) { Icon(Icons.Rounded.Clear, "Remove screenshot") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(unit == DisplayUnit.PX, { onUnit(DisplayUnit.PX) }, { Text("PX") })
                FilterChip(unit == DisplayUnit.DP, { onUnit(DisplayUnit.DP) }, { Text("DP") })
                FilterChip(grid, { onGrid(!grid) }, { Text("GRID") })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(formatMeasurement(a, b, viewport, image, density, unit), fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onReset, enabled = a != null) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(6.dp)); Text("RESET") }
                Button(onClick = onCopy, enabled = a != null) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("COPY") }
            }
            if (image != null) Text("Source image ${image.sourceWidth}×${image.sourceHeight}px · decoded safely for display",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val topLeft get() = Offset(left, top)
    val size get() = androidx.compose.ui.geometry.Size(width, height)
}

private fun fittedRect(vw: Float, vh: Float, iw: Int, ih: Int): FitRect {
    if (vw <= 0 || vh <= 0 || iw <= 0 || ih <= 0) return FitRect(0f, 0f, vw, vh)
    val scale = min(vw / iw, vh / ih)
    val w = iw * scale; val h = ih * scale
    return FitRect((vw - w) / 2f, (vh - h) / 2f, w, h)
}

private fun sourcePoint(point: Offset, viewport: IntSize, image: LoadedImage): Offset {
    val rect = fittedRect(viewport.width.toFloat(), viewport.height.toFloat(), image.sourceWidth, image.sourceHeight)
    return Offset(((point.x - rect.left) / rect.width * image.sourceWidth).coerceIn(0f, image.sourceWidth.toFloat()),
        ((point.y - rect.top) / rect.height * image.sourceHeight).coerceIn(0f, image.sourceHeight.toFloat()))
}

private fun formatMeasurement(a: Offset?, b: Offset?, viewport: IntSize, image: LoadedImage?, density: Float, unit: DisplayUnit): String {
    if (a == null) return "Viewport  ${viewport.width} × ${viewport.height} px\n\nTap the first point."
    val divisor = if (unit == DisplayUnit.DP) density else 1f
    val suffix = unit.name.lowercase()
    fun f(value: Float) = if (unit == DisplayUnit.PX) value.roundToInt().toString() else "%.1f".format(value / divisor)
    return buildString {
        appendLine("Viewport  ${viewport.width} × ${viewport.height} px")
        appendLine("A         ${f(a.x)}, ${f(a.y)} $suffix")
        appendLine("Edges     L ${f(a.x)} · T ${f(a.y)} · R ${f(viewport.width-a.x)} · B ${f(viewport.height-a.y)}")
        image?.let { val p = sourcePoint(a, viewport, it); appendLine("A source  ${p.x.roundToInt()}, ${p.y.roundToInt()} px") }
        if (b != null) {
            val dx = b.x-a.x; val dy = b.y-a.y
            appendLine("B         ${f(b.x)}, ${f(b.y)} $suffix")
            appendLine("Δ         ${f(kotlin.math.abs(dx))} × ${f(kotlin.math.abs(dy))} $suffix")
            appendLine("Distance  ${f(hypot(dx,dy))} $suffix")
            image?.let { val p = sourcePoint(b, viewport, it); appendLine("B source  ${p.x.roundToInt()}, ${p.y.roundToInt()} px") }
        }
    }.trimEnd()
}

private fun measurementReport(a: Offset?, b: Offset?, viewport: IntSize, image: LoadedImage?, density: Float) =
    formatMeasurement(a, b, viewport, image, density, DisplayUnit.PX) + "\n" +
        formatMeasurement(a, b, viewport, image, density, DisplayUnit.DP)

private fun loadScaledBitmap(context: android.content.Context, uri: Uri): LoadedImage {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
    var sample = 1
    while (ceil(max(bounds.outWidth, bounds.outHeight).toDouble() / sample).toInt() > 4096) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("Unsupported image")
    return LoadedImage(bitmap, bounds.outWidth, bounds.outHeight)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb((alpha*255).roundToInt(), (red*255).roundToInt(), (green*255).roundToInt(), (blue*255).roundToInt())

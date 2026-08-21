package com.local.localkit.ui.features

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.local.localkit.core.DocumentOperations
import com.local.localkit.core.FileOperations
import com.local.localkit.core.PdfPageSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageCompressorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var width by rememberSaveable { mutableFloatStateOf(1600f) }
    var quality by rememberSaveable { mutableFloatStateOf(82f) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        uri = picked; status = null
        picked?.let { busy = true; scope.launch { bitmap = withContext(Dispatchers.IO) { DocumentOperations.loadBitmap(context.contentResolver, it, 4096) }; bitmap?.let { width = minOf(1600, it.width).toFloat() }; busy = false } }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { output ->
        val source = bitmap
        if (output != null && source != null) {
            busy = true; scope.launch {
                status = runCatching { withContext(Dispatchers.IO) { val scaled = DocumentOperations.scaled(source, width.toInt()); DocumentOperations.writeBitmap(context.contentResolver, output, scaled, Bitmap.CompressFormat.JPEG, quality.toInt()); if (scaled !== source) scaled.recycle() }; "Saved resized JPEG" }.getOrElse { "Export failed: ${it.message}" }
                busy = false
            }
        }
    }
    DisposableEffect(Unit) { onDispose { bitmap?.recycle() } }

    ToolPage("Control the actual export", "Choose dimensions and JPEG quality. The original is never modified.") {
        Button(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) { Text("Choose image") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        bitmap?.let { source ->
            BitmapPreview(source)
            Text("Original: ${source.width} × ${source.height}")
            Text("Maximum width: ${width.toInt()} px")
            Slider(width, { width = it }, valueRange = 320f..source.width.coerceAtLeast(320).toFloat())
            Text("JPEG quality: ${quality.toInt()}%")
            Slider(quality, { quality = it }, valueRange = 20f..100f)
            Button(onClick = { saver.launch("LocalKit-compressed.jpg") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save copy") }
        }
        status?.let { ResultCard(it) }
    }
}

@Composable
fun ImageConverterScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val formats = listOf("PNG", "JPEG", "WebP lossless")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { busy = true; scope.launch { bitmap = withContext(Dispatchers.IO) { DocumentOperations.loadBitmap(context.contentResolver, it) }; busy = false } }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { output ->
        val source = bitmap ?: return@rememberLauncherForActivityResult
        if (output != null) {
            busy = true; scope.launch {
                val format = when (formatIndex) {
                    0 -> Bitmap.CompressFormat.PNG
                    1 -> Bitmap.CompressFormat.JPEG
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                status = runCatching { withContext(Dispatchers.IO) { DocumentOperations.writeBitmap(context.contentResolver, output, source, format, 95) }; "Image converted" }.getOrElse { "Conversion failed: ${it.message}" }
                busy = false
            }
        }
    }

    ToolPage("Convert image format", "PNG and lossless WebP preserve transparency; JPEG does not.") {
        Button(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) { Text("Choose image") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        bitmap?.let { BitmapPreview(it); ChoiceDropdown("Output", formats, formatIndex) { formatIndex = it }; Button(onClick = { saver.launch("LocalKit-converted.${listOf("png", "jpg", "webp")[formatIndex]}") }, Modifier.fillMaxWidth()) { Text("Save converted copy") } }
        status?.let { ResultCard(it) }
    }
}

@Composable
fun MetadataCleanerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var metadata by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { busy = true; scope.launch { val result = withContext(Dispatchers.IO) { DocumentOperations.loadBitmap(context.contentResolver, it) to DocumentOperations.exifSummary(context.contentResolver, it) }; bitmap = result.first; metadata = result.second; busy = false } }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { output ->
        val source = bitmap ?: return@rememberLauncherForActivityResult
        if (output != null) { busy = true; scope.launch { status = runCatching { withContext(Dispatchers.IO) { DocumentOperations.writeBitmap(context.contentResolver, output, source, Bitmap.CompressFormat.JPEG, 96) }; "Saved a re-encoded copy without EXIF" }.getOrElse { "Export failed: ${it.message}" }; busy = false } }
    }

    ToolPage("See what the image reveals", "Cleaning creates a re-encoded JPEG copy. The original remains untouched.") {
        Button(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) { Text("Choose image") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        bitmap?.let { BitmapPreview(it) }
        if (bitmap != null) {
            if (metadata.isEmpty()) ResultCard("No selected EXIF fields found") else metadata.forEach { (label, value) -> Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) } }
            Button(onClick = { saver.launch("LocalKit-clean.jpg") }, Modifier.fillMaxWidth()) { Text("Save metadata-free copy") }
        }
        status?.let { ResultCard(it) }
    }
}

@Composable
fun ImagesToPdfScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { images = it; status = null }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { output ->
        if (output != null && images.isNotEmpty()) { busy = true; scope.launch { status = runCatching { withContext(Dispatchers.IO) { DocumentOperations.imagesToPdf(context.contentResolver, images, output) }; "Created ${images.size}-page PDF" }.getOrElse { "PDF failed: ${it.message}" }; busy = false } }
    }
    ToolPage("Photos become pages", "Images retain their order from the system picker and are fitted onto clean pages.") {
        Button(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) { Text(if (images.isEmpty()) "Choose images" else "${images.size} images selected") }
        Button(onClick = { saver.launch("LocalKit-images.pdf") }, enabled = images.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Save PDF") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        status?.let { ResultCard(it) }
    }
}

@Composable
fun PdfReaderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked -> uri = picked; page = 0 }

    LaunchedEffect(uri, page) {
        val source = uri ?: return@LaunchedEffect
        busy = true
        runCatching { withContext(Dispatchers.IO) { DocumentOperations.pageCount(context.contentResolver, source) to DocumentOperations.renderPdfPage(context.contentResolver, source, page) } }
            .onSuccess { count = it.first; bitmap?.recycle(); bitmap = it.second }
        busy = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Open PDF") }
            if (count > 0) {
                IconButton(onClick = { page-- }, enabled = page > 0) { Icon(Icons.Outlined.ChevronLeft, "Previous") }
                Text("${page + 1} / $count")
                IconButton(onClick = { page++ }, enabled = page < count - 1) { Icon(Icons.Outlined.ChevronRight, "Next") }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        bitmap?.let { Image(it.asImageBitmap(), "PDF page ${page + 1}", Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit) }
        if (uri == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Choose a PDF to begin") }
    }
}

@Composable
fun PdfOrganizerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = remember { mutableStateListOf<PdfPageSpec>() }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
        if (files.isNotEmpty()) {
            busy = true; scope.launch {
                val loaded = withContext(Dispatchers.IO) { files.flatMap { uri -> val name = FileOperations.describe(context.contentResolver, uri).name; List(DocumentOperations.pageCount(context.contentResolver, uri)) { page -> PdfPageSpec(uri, page, "$name · page ${page + 1}") } } }
                pages.clear(); pages.addAll(loaded); busy = false; status = null
            }
        }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { output ->
        if (output != null && pages.isNotEmpty()) { busy = true; scope.launch { status = runCatching { withContext(Dispatchers.IO) { DocumentOperations.organizePdf(context.contentResolver, pages.toList(), output) }; "Saved ${pages.size} pages" }.getOrElse { "Export failed: ${it.message}" }; busy = false } }
    }

    ToolPage("Arrange the exact output", "Add one or more PDFs, reorder/rotate/remove pages, then export. Compatibility mode rasterizes pages, so searchable text is not retained.") {
        Button(onClick = { picker.launch(arrayOf("application/pdf")) }, Modifier.fillMaxWidth()) { Text("Choose PDF files") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        pages.forEachIndexed { index, spec ->
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("${index + 1}. ${spec.label}", style = MaterialTheme.typography.bodySmall); if (spec.rotation != 0) Text("Rotated ${spec.rotation}°", style = MaterialTheme.typography.labelSmall) }
                    IconButton(onClick = { pages.move(index, index - 1) }, enabled = index > 0) { Icon(Icons.Outlined.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { pages.move(index, index + 1) }, enabled = index < pages.lastIndex) { Icon(Icons.Outlined.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { pages[index] = spec.copy(rotation = (spec.rotation + 90) % 360) }) { Icon(Icons.AutoMirrored.Outlined.RotateRight, "Rotate") }
                    IconButton(onClick = { pages.removeAt(index) }) { Icon(Icons.Outlined.Close, "Remove") }
                }
            }
        }
        Button(onClick = { saver.launch("LocalKit-organized.pdf") }, enabled = pages.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Save organized PDF") }
        status?.let { ResultCard(it) }
    }
}

@Composable
private fun BitmapPreview(bitmap: Bitmap) {
    Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit)
}

private fun <T> SnapshotStateList<T>.move(from: Int, to: Int) {
    if (from !in indices || to !in indices) return
    val item = removeAt(from)
    add(to, item)
}

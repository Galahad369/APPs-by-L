@file:androidx.annotation.OptIn(markerClass = [androidx.camera.core.ExperimentalGetImage::class])

package com.local.localkit.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.local.localkit.core.DocumentOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrGeneratorScreen() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    val bitmap = remember(text) { if (text.isBlank()) null else runCatching { qrBitmap(text) }.getOrNull() }
    var status by remember { mutableStateOf<String?>(null) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { output ->
        if (output != null && bitmap != null) status = runCatching { DocumentOperations.writeBitmap(context.contentResolver, output, bitmap, Bitmap.CompressFormat.PNG, 100); "QR code saved" }.getOrElse { "Save failed: ${it.message}" }
    }
    ToolPage("Make a clean QR code", "Text stays on this device. Always inspect a QR payload before sharing it.") {
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Text, URL, contact or Wi-Fi payload") }, minLines = 3)
        bitmap?.let {
            Surface(shape = MaterialTheme.shapes.large, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                Image(it.asImageBitmap(), "Generated QR code", Modifier.padding(18.dp), contentScale = ContentScale.Fit)
            }
            Button(onClick = { saver.launch("LocalKit-QR.png") }, Modifier.fillMaxWidth()) { Text("Save PNG") }
        }
        status?.let { ResultCard(it) }
    }
}

@Composable
fun QrScannerScreen() {
    CameraPermissionPage("Scan locally", "The bundled scanner recognizes standard QR and barcodes without a connection.") {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var result by remember { mutableStateOf<Pair<String, String>?>(null) }
        val scanner = remember { BarcodeScanning.getClient() }
        val executor = remember { Executors.newSingleThreadExecutor() }
        val processing = remember { AtomicBoolean(false) }

        DisposableEffect(Unit) { onDispose { scanner.close(); executor.shutdown() } }

        Column(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            analysis.setAnalyzer(executor) { proxy ->
                                val image = proxy.image
                                if (image == null || !processing.compareAndSet(false, true)) { proxy.close(); return@setAnalyzer }
                                scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                                    .addOnSuccessListener { codes -> codes.firstOrNull()?.let { code -> result = code.rawValue.orEmpty() to barcodeName(code.format) } }
                                    .addOnCompleteListener { processing.set(false); proxy.close() }
                            }
                            runCatching { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            result?.let { (value, format) ->
                Surface(Modifier.fillMaxWidth().padding(12.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(format, style = MaterialTheme.typography.labelLarge)
                        Text(value, fontFamily = FontFamily.Monospace)
                        Text("Review before opening links or joining Wi-Fi.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { result = null }) { Text("Scan another") }
                    }
                }
            }
        }
    }
}

@Composable
fun OcrScreen() {
    val context = LocalContext.current
    var result by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var chinese by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true; result = ""
        val recognizer = if (chinese) TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) else TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        runCatching { InputImage.fromFilePath(context, uri) }.onSuccess { image ->
            recognizer.process(image).addOnSuccessListener { result = it.text }.addOnFailureListener { result = "OCR failed: ${it.message}" }.addOnCompleteListener { busy = false; recognizer.close() }
        }.onFailure { result = "Could not read image: ${it.message}"; busy = false; recognizer.close() }
    }
    ToolPage("Extract text on-device", "Latin and Traditional/Simplified Chinese models are bundled into the app.") {
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(chinese, { chinese = it }); Spacer(Modifier.width(8.dp)); Text(if (chinese) "Chinese + Latin" else "Latin") }
        Button(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) { Text("Choose image") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (result.isNotBlank()) OutlinedTextField(result, { result = it }, Modifier.fillMaxWidth().heightIn(min = 260.dp), label = { Text("Recognized text") }, minLines = 10)
    }
}

@Composable
fun DocumentScannerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    var grayscale by rememberSaveable { mutableStateOf(true) }
    var contrast by rememberSaveable { mutableFloatStateOf(1.15f) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val enhanced = remember(captured, grayscale, contrast) { captured?.let { DocumentOperations.enhanceDocument(it, grayscale, contrast) } }
    val pdfSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { output ->
        val source = enhanced ?: return@rememberLauncherForActivityResult
        if (output != null) { busy = true; scope.launch { status = runCatching { withContext(Dispatchers.IO) { DocumentOperations.bitmapToPdf(context.contentResolver, source, output) }; "Saved scan as PDF" }.getOrElse { "Save failed: ${it.message}" }; busy = false } }
    }
    val imageSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { output ->
        val source = enhanced ?: return@rememberLauncherForActivityResult
        if (output != null) { busy = true; scope.launch { status = runCatching { withContext(Dispatchers.IO) { DocumentOperations.writeBitmap(context.contentResolver, output, source, Bitmap.CompressFormat.JPEG, 95) }; "Saved scan as JPEG" }.getOrElse { "Save failed: ${it.message}" }; busy = false } }
    }

    if (captured == null) {
        CameraPermissionPage("Capture a document", "Fill the frame and hold steady. Cleanup happens locally after capture.") {
            CameraCapturePanel(zoom = 1f, onCaptured = { captured = limitBitmap(it, 2400) })
        }
    } else {
        ToolPage("Clean and export", "Adjust contrast before creating a PDF or JPEG copy.") {
            enhanced?.let { Image(it.asImageBitmap(), "Document preview", Modifier.fillMaxWidth().heightIn(max = 420.dp), contentScale = ContentScale.Fit) }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(grayscale, { grayscale = it }); Spacer(Modifier.width(8.dp)); Text("Grayscale") }
            Text("Contrast ${"%.2f".format(contrast)}")
            Slider(contrast, { contrast = it }, valueRange = .7f..1.8f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { captured = null }, Modifier.weight(1f)) { Text("Retake") }
                Button(onClick = { imageSaver.launch("LocalKit-scan.jpg") }, Modifier.weight(1f)) { Text("JPEG") }
                Button(onClick = { pdfSaver.launch("LocalKit-scan.pdf") }, Modifier.weight(1f)) { Text("PDF") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            status?.let { ResultCard(it) }
        }
    }
}

@Composable
fun MagnifierColorScreen() {
    var zoom by rememberSaveable { mutableFloatStateOf(2f) }
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    if (captured == null) {
        CameraPermissionPage("Magnify and sample", "Zoom the live camera, then capture a frame to inspect its center color.") {
            Column(Modifier.fillMaxSize()) {
                CameraCapturePanel(zoom, { captured = limitBitmap(it, 1800) }, Modifier.weight(1f))
                Text("Zoom ${"%.1f".format(zoom)}×", Modifier.padding(horizontal = 18.dp))
                Slider(zoom, { zoom = it }, Modifier.padding(horizontal = 18.dp), valueRange = 1f..6f)
            }
        }
    } else {
        val color = remember(captured) { averageCenterColor(captured!!) }
        val hex = "#%02X%02X%02X".format(android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
        ToolPage("Captured detail", "The sampled color is averaged from the center square to reduce camera noise.") {
            Image(captured!!.asImageBitmap(), "Magnified frame", Modifier.fillMaxWidth().heightIn(max = 440.dp), contentScale = ContentScale.Fit)
            Surface(Modifier.fillMaxWidth().height(90.dp), color = androidx.compose.ui.graphics.Color(color), shape = MaterialTheme.shapes.large) {}
            ResultCard(hex, monospace = true)
            OutlinedButton(onClick = { captured = null }, Modifier.fillMaxWidth()) { Text("Return to camera") }
        }
    }
}

@Composable
private fun CameraPermissionPage(title: String, subtitle: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) content() else ToolPage(title, subtitle) {
        ResultCard("Camera permission is used only while this tool is visible")
        Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth()) { Text("Allow camera") }
    }
}

@Composable
private fun CameraCapturePanel(zoom: Float, onCaptured: (Bitmap) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(zoom, camera) { camera?.cameraControl?.setZoomRatio(zoom) }
    Box(modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                ProcessCameraProvider.getInstance(ctx).also { future ->
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                        val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                        capture = imageCapture
                        runCatching { provider.unbindAll(); camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        }, modifier = Modifier.fillMaxSize())
        FloatingActionButton(
            onClick = {
                capture?.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val raw = image.toBitmap()
                        val rotation = image.imageInfo.rotationDegrees
                        image.close()
                        val rotated = if (rotation == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true).also { if (it !== raw) raw.recycle() }
                        onCaptured(rotated)
                    }
                })
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        ) { Icon(Icons.Outlined.CameraAlt, "Capture") }
    }
}

private fun qrBitmap(value: String): Bitmap {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 1024, 1024, mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    val pixels = IntArray(matrix.width * matrix.height) { index -> if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}

private fun barcodeName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR CODE"; Barcode.FORMAT_EAN_13 -> "EAN-13"; Barcode.FORMAT_EAN_8 -> "EAN-8"; Barcode.FORMAT_UPC_A -> "UPC-A"; Barcode.FORMAT_UPC_E -> "UPC-E"; Barcode.FORMAT_CODE_128 -> "CODE 128"; Barcode.FORMAT_PDF417 -> "PDF417"; Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"; else -> "BARCODE"
}

private fun limitBitmap(source: Bitmap, maxSide: Int): Bitmap {
    val longest = maxOf(source.width, source.height)
    if (longest <= maxSide) return source
    val scale = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true).also { source.recycle() }
}

private fun averageCenterColor(bitmap: Bitmap): Int {
    val radius = minOf(bitmap.width, bitmap.height, 40) / 2
    val cx = bitmap.width / 2; val cy = bitmap.height / 2
    var red = 0L; var green = 0L; var blue = 0L; var count = 0
    for (y in cy - radius until cy + radius) for (x in cx - radius until cx + radius) {
        val color = bitmap.getPixel(x, y); red += android.graphics.Color.red(color); green += android.graphics.Color.green(color); blue += android.graphics.Color.blue(color); count++
    }
    return android.graphics.Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
}

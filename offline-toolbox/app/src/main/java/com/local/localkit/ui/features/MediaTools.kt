@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.local.localkit.ui.features

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.local.localkit.core.FileOperations
import com.local.localkit.core.MediaInfo
import com.local.localkit.core.MediaOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VideoEditorScreen() = MediaEditorScreen(video = true)

@Composable
fun AudioEditorScreen() = MediaEditorScreen(video = false)

@Composable
private fun MediaEditorScreen(video: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf<Uri?>(null) }
    var info by remember { mutableStateOf<MediaInfo?>(null) }
    var startText by rememberSaveable { mutableStateOf("0") }
    var endText by rememberSaveable { mutableStateOf("") }
    var mute by rememberSaveable { mutableStateOf(false) }
    var heightIndex by rememberSaveable { mutableIntStateOf(0) }
    var transformer by remember { mutableStateOf<Transformer?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val heights = listOf("Keep resolution", "1080p", "720p", "480p")

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        input = uri; status = null
        uri?.let { scope.launch { info = withContext(Dispatchers.IO) { MediaOperations.inspect(context, it) }; endText = "%.2f".format((info?.durationMs ?: 0) / 1000.0) } }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(if (video) "video/mp4" else "audio/mp4")) { output ->
        val source = input
        if (output != null && source != null) {
            busy = true; progress = 0; status = null
            val start = ((startText.toDoubleOrNull() ?: 0.0) * 1000).toLong()
            val end = ((endText.toDoubleOrNull() ?: 0.0) * 1000).toLong()
            val target = if (video) listOf(null, 1080, 720, 480)[heightIndex] else null
            transformer = runCatching {
                MediaOperations.export(context, source, output, start, end, audioOnly = !video, mute = mute, targetHeight = target) { result ->
                    busy = false; transformer = null; status = result.fold({ "Export complete" }, { "Export failed: ${it.message}" })
                }
            }.getOrElse { busy = false; status = "Could not start export: ${it.message}"; null }
        }
    }
    LaunchedEffect(busy, transformer) {
        val holder = ProgressHolder()
        while (busy) {
            transformer?.let { if (it.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) progress = holder.progress }
            delay(300)
        }
    }
    DisposableEffect(Unit) { onDispose { transformer?.cancel() } }

    ToolPage(
        if (video) "Trim and transcode video" else "Trim and convert audio",
        if (video) "Exports H.264 video with AAC audio using device codecs. Originals are untouched." else "Exports an AAC/M4A copy locally. Support depends on the device decoder."
    ) {
        Button(onClick = { picker.launch(arrayOf(if (video) "video/*" else "audio/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (input == null) "Choose ${if (video) "video" else "audio"}" else "Choose another") }
        info?.let {
            ResultCard("${formatDuration(it.durationMs)}${if (video) " · ${it.width} × ${it.height}" else ""}")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(startText, { startText = it }, Modifier.weight(1f), label = { Text("Start seconds") }, singleLine = true)
                OutlinedTextField(endText, { endText = it }, Modifier.weight(1f), label = { Text("End seconds") }, singleLine = true)
            }
            if (video) {
                ChoiceDropdown("Resolution", heights, heightIndex) { heightIndex = it }
                Row { Checkbox(mute, { mute = it }); Text("Remove audio") }
            }
            Button(onClick = { saver.launch(if (video) "LocalKit-video.mp4" else "LocalKit-audio.m4a") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose output and export") }
        }
        if (busy) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            Text("Exporting locally · $progress%")
            OutlinedButton(onClick = { transformer?.cancel(); transformer = null; busy = false; status = "Export cancelled" }, Modifier.fillMaxWidth()) { Text("Cancel") }
        }
        status?.let { ResultCard(it) }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

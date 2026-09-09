package com.local.listentomusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.playback.ParallelPlayback

@Composable
fun ParallelMixerDialog(files: List<MediaFile>, language: AppLanguage, onLoadThumbnail: suspend (MediaFile) -> android.graphics.Bitmap?, onDismiss: () -> Unit) {
    val layers by ParallelPlayback.layers.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(files, query) { files.filter { it.name.contains(query, true) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiText(language, "Parallel playback", "並行播放"), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss) { Text(uiText(language, "Close", "關閉")) }
                }
                Text("${layers.size + 1} / 10", style = MaterialTheme.typography.labelLarge)
                Text(uiText(language, "The main track plus nine audio layers. Video layers play sound only. Mixing lowers each layer to leave headroom.", "主曲目加九個音訊層，影片只播放聲音。混音會降低各層音量以保留餘裕。"), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { layers.forEach { ParallelPlayback.remove(it.id) } }, enabled = layers.isNotEmpty()) { Text(uiText(language, "Remove all layers", "移除所有音訊層")) }
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text(uiText(language, "Search", "搜尋")) }, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f)) {
                    items(layers, key = { "layer-${it.id}" }) { layer ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                files.firstOrNull { it.path == layer.path }?.let { QueueThumbnail(it, onLoadThumbnail); Spacer(Modifier.width(10.dp)) }
                                Text(files.firstOrNull { it.path == layer.path }?.name ?: layer.title, maxLines = 2, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row {
                                TextButton(onClick = { ParallelPlayback.toggle(layer.id) }) { Text(uiText(language, if (layer.playing) "Pause" else "Play", if (layer.playing) "暫停" else "播放")) }
                                TextButton(onClick = { ParallelPlayback.volume(layer.id, if (layer.volume == 0f) 1f else 0f) }) { Text(uiText(language, if (layer.volume == 0f) "Unmute" else "Mute", if (layer.volume == 0f) "取消靜音" else "靜音")) }
                                TextButton(onClick = { ParallelPlayback.remove(layer.id) }) { Text(uiText(language, "Remove", "移除")) }
                            }
                            Slider(layer.volume, { ParallelPlayback.volume(layer.id, it) }, valueRange = 0f..1f)
                            HorizontalDivider()
                        }
                    }
                    items(filtered, key = { "file-${it.path}" }) { file ->
                        ListItem(headlineContent = { Text(file.name, maxLines = 2) },
                            leadingContent = { QueueThumbnail(file, onLoadThumbnail) },
                            trailingContent = { TextButton(enabled = layers.size < ParallelPlayback.MAX_EXTRA_LAYERS, onClick = { ParallelPlayback.add(file.path) }) { Text(uiText(language, "Add", "加入")) } },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
                    }
                }
            }
        }
    }
}

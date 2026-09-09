package com.local.listentomusic.ui

import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.local.listentomusic.model.MediaFile
import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest

@Composable
fun DuplicateDialog(files: List<MediaFile>, currentPath: String?, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val mixLayers by com.local.listentomusic.playback.ParallelPlayback.layers.collectAsState()
    val protectedPaths = (mixLayers.map { com.local.listentomusic.model.sourceMediaPath(it.path) } + listOfNotNull(currentPath?.let { com.local.listentomusic.model.sourceMediaPath(it) })).toSet()
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<List<MediaFile>>?>(null) }
    var message by remember { mutableStateOf("Checking same-size files locally…") }
    val delete = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == android.app.Activity.RESULT_OK) { onChanged(); onDismiss() }
    }
    LaunchedEffect(Unit) {
        groups = withContext(Dispatchers.IO) {
            files.distinctBy { it.sourcePath }.map { it.copy(path = it.sourcePath) }.filter { it.sizeBytes > 0 }.groupBy { it.sizeBytes }.values.filter { it.size > 1 }.flatMap { candidates ->
                candidates.mapNotNull { media ->
                    ensureActive()
                    runCatching {
                        val digest = MessageDigest.getInstance("SHA-256")
                        File(media.path).inputStream().buffered().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                digest.update(buffer, 0, count)
                            }
                        }
                        digest.digest().joinToString("") { "%02x".format(it) } to media
                    }.getOrElse { if (it is CancellationException) throw it; null }
                }.groupBy({ it.first }, { it.second }).values.filter { it.size > 1 }
            }
        }
        message = "Exact file matches. Review each path before deleting. The currently playing file is protected."
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Duplicate files") }, text = {
        Column {
            Text(message)
            if (groups == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            else if (groups!!.isEmpty()) Text("No exact duplicates found.")
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                groups.orEmpty().forEach { group ->
                    item { HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
                    group.forEach { file -> item {
                        Text(file.path, style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = file.path !in protectedPaths, onClick = {
                            scope.launch {
                                if (Build.VERSION.SDK_INT < 30) { message = "System-confirmed deletion requires Android 11 or newer."; return@launch }
                                val uri = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val collection = MediaStore.Files.getContentUri("external")
                                        context.contentResolver.query(collection, arrayOf(MediaStore.Files.FileColumns._ID),
                                            "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(file.path), null)?.use { cursor ->
                                            if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
                                        }
                                    }.getOrNull()
                                }
                                if (uri == null) message = "Android has not indexed this file. Delete it using your Files app."
                                else runCatching {
                                    delete.launch(IntentSenderRequest.Builder(MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender).build())
                                }.onFailure { message = "Android could not open deletion confirmation. No file was deleted." }
                            }
                        }) { Text("Ask Android to delete", color = MaterialTheme.colorScheme.error) }
                    } }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

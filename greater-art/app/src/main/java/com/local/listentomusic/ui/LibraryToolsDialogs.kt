package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.*

@Composable
internal fun DisplayOverrideDialog(file: MediaFile, saved: LocalOverride?, language: AppLanguage,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember(file.path) { mutableStateOf(saved?.title.orEmpty()) }
    var cover by remember(file.path) { mutableStateOf(saved?.coverUri.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            .onSuccess { cover = uri.toString(); error = null }
            .onFailure { error = uiText(language, "This image provider cannot keep access. Choose a local image.", "無法保留此圖片的存取權，請選擇本機圖片。") }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(uiText(language, "Local title & cover", "本機標題與封面")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(uiText(language, "Only changes this app. Your original file stays untouched.", "只更改此應用程式內的顯示，不會修改原始檔案。"))
            OutlinedTextField(title, { title = it.take(300) }, label = { Text(uiText(language, "Display title (blank = original)", "顯示標題（空白＝原始標題）")) }, modifier = Modifier.fillMaxWidth())
            QueueThumbnail(file.copy(coverUri = cover), onLoadThumbnail)
            TextButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(uiText(language, "Choose image", "選擇圖片")) }
            TextButton(onClick = { title = ""; cover = "" }) { Text(uiText(language, "Use original title and artwork", "使用原始標題及封面")) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = { onSave(title, cover); onDismiss() }) { Text(uiText(language, "Save", "儲存")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } })
}

@Composable
internal fun RulePlaylistDialog(language: AppLanguage, onCreate: (String, PlaylistRule) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }; var folder by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("") }; var text by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && folder.replace('\\', '/').split('/').none { it == ".." }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(uiText(language, "Rule-based playlist", "規則播放清單")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(uiText(language, "Updates after scanning. All filled rules must match. No listening history is used.", "掃描後自動更新，所有已填規則都須符合，不使用播放紀錄。"), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it.take(60) }, label = { Text(uiText(language, "Playlist name", "播放清單名稱")) }, singleLine = true)
            OutlinedTextField(folder, { folder = it.take(300) }, label = { Text(uiText(language, "Folder inside Download (optional)", "Download 內的資料夾（選填）")) }, placeholder = { Text("Concerts") }, singleLine = true)
            OutlinedTextField(extension, { extension = it.take(12) }, label = { Text(uiText(language, "Format (optional)", "格式（選填）")) }, placeholder = { Text("flac") }, singleLine = true)
            OutlinedTextField(text, { text = it.take(100) }, label = { Text(uiText(language, "Title contains (optional)", "標題包含（選填）")) }, singleLine = true)
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = { onCreate(name, PlaylistRule(folder.trim(), extension.trim(), text.trim())); onDismiss() }) { Text(uiText(language, "Create", "建立")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } })
}

package com.local.listentomusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.LibraryStatus
import com.local.listentomusic.LibraryUiState
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.ui.components.LiquidMetalSurface
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    preferences: UserPreferences,
    currentPath: String?,
    contentPadding: PaddingValues,
    onGrantStorageAccess: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistAndSeed: (String, String?, String?) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onRemoveFromPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onOpenSettings: () -> Unit,
    onPlay: (MediaFile) -> Unit,
) {
    val language = preferences.appLanguage
    val activePlaylist = preferences.playlists.firstOrNull { it.id == preferences.activePlaylistId }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var playlistMenuOpen by remember { mutableStateOf(false) }
    var createPlaylistOpen by remember { mutableStateOf(false) }
    var createSeedPath by remember { mutableStateOf<String?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var seedKeyword by remember { mutableStateOf("") }
    var songListFile by remember { mutableStateOf<MediaFile?>(null) }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val markShape = RoundedCornerShape(9.dp)
                        LiquidMetalSurface(
                            modifier = Modifier.size(36.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, markShape),
                            shape = markShape,
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                null,
                                Modifier.size(22.dp),
                                MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text("Greater Art", fontWeight = FontWeight.ExtraBold)
                            Text(
                                uiText(language, "${state.files.size} files • offline", "${state.files.size} 個檔案 • 離線"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, uiText(language, "Settings", "設定"))
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, uiText(language, "Scan again", "重新掃描"))
                    }
                    if (activePlaylist == null) Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, uiText(language, "Sort", "排序"))
                        }
                        DropdownMenu(sortMenuOpen, { sortMenuOpen = false }) {
                            SortMode.entries.forEach { mode ->
                                val label = when (mode) {
                                    SortMode.CUSTOM -> uiText(language, "Custom order", "自訂排序")
                                    SortMode.NAME_ASC -> uiText(language, "Name A–Z", "名稱 A–Z")
                                    SortMode.NAME_DESC -> uiText(language, "Name Z–A", "名稱 Z–A")
                                }
                                DropdownMenuItem(
                                    text = { Text(if (mode == state.sortMode) "✓  $label" else label) },
                                    onClick = { sortMenuOpen = false; onSortChange(mode) },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                ),
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(uiText(language, "Filter library", "篩選音樂庫")) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    Button(onClick = { playlistMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, null)
                        Spacer(Modifier.width(8.dp))
                        Text(activePlaylist?.name ?: uiText(language, "All songs", "所有歌曲"), maxLines = 1)
                    }
                    DropdownMenu(playlistMenuOpen, { playlistMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(uiText(language, "All songs", "所有歌曲")) },
                            onClick = { playlistMenuOpen = false; onSelectPlaylist(null) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                        )
                        preferences.playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text("${playlist.name}  (${playlist.paths.size})") },
                                onClick = { playlistMenuOpen = false; onSelectPlaylist(playlist.id) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(uiText(language, "Play this list", "播放此清單")) },
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                            onClick = { playlistMenuOpen = false; activePlaylist?.let { onPlayPlaylist(it.id) } },
                            enabled = activePlaylist != null && activePlaylist.paths.isNotEmpty(),
                        )
                        DropdownMenuItem(
                            text = { Text(uiText(language, "Create playlist", "建立播放清單")) },
                            onClick = { playlistMenuOpen = false; createSeedPath = null; createPlaylistOpen = true },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                        )
                        activePlaylist?.let { pl ->
                            DropdownMenuItem(
                                text = { Text(uiText(language, "Delete list", "刪除清單")) },
                                onClick = { playlistMenuOpen = false; onDeletePlaylist(pl.id) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            )
                        }
                    }
                }
                if (activePlaylist != null) {
                    Text(
                        uiText(language, "Hold + drag to reorder", "長按拖曳排序"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            when (state.status) {
                LibraryStatus.NEEDS_PERMISSION -> MessageState(
                    uiText(language, "Allow local file access", "允許本機檔案存取"),
                    uiText(language, "Android will open settings. Enable all-files access, then return here.", "Android 將開啟設定。請允許存取所有檔案，然後返回。"),
                    uiText(language, "Open settings", "開啟設定"),
                    onGrantStorageAccess,
                )
                LibraryStatus.SCANNING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                LibraryStatus.FOLDER_MISSING -> MessageState(
                    uiText(language, "Download folder unavailable", "無法使用 Download 資料夾"),
                    uiText(language, "The app could not open:\n${state.targetPath}", "應用程式無法開啟：\n${state.targetPath}"),
                    uiText(language, "Scan again", "重新掃描"),
                    onRefresh,
                )
                LibraryStatus.CANNOT_READ -> MessageState(
                    uiText(language, "Folder access was lost", "資料夾存取權已失效"),
                    uiText(language, "Grant file access again, then return to the app.", "請重新授予檔案存取權，然後返回應用程式。"),
                    uiText(language, "Open settings", "開啟設定"),
                    onGrantStorageAccess,
                )
                LibraryStatus.READY -> if (state.files.isEmpty()) {
                    MessageState(
                        if (state.query.isBlank()) uiText(language, "No media files yet", "尚未找到媒體檔案") else uiText(language, "No matches", "沒有相符項目"),
                        if (state.query.isBlank()) {
                            if (activePlaylist != null) uiText(language, "This playlist is empty. Add songs with the ⋮ button.", "此播放清單是空的。使用 ⋮ 按鈕加入歌曲。")
                            else uiText(language, "No supported media was found under:\n${state.targetPath}", "在以下位置找不到支援的媒體：\n${state.targetPath}")
                        } else uiText(language, "Try a different search.", "請嘗試其他搜尋字詞。"),
                        uiText(language, "Scan again", "重新掃描"),
                        onRefresh,
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        itemsIndexed(
                            state.files,
                            key = { _, item -> item.id },
                            contentType = { _, item -> item.kind },
                        ) { index, item ->
                            MediaFileRow(
                                file = item,
                                isCurrent = item.path == currentPath,
                                index = index,
                                itemCount = state.files.size,
                                dragEnabled = state.query.isBlank() && (activePlaylist != null || state.sortMode == SortMode.CUSTOM),
                                onMove = onMoveItem,
                                onLoadThumbnail = onLoadThumbnail,
                                rowSize = preferences.libraryRowSize,
                                showThumbnails = preferences.showThumbnails,
                                showFileDetails = preferences.showFileDetails,
                                onPlay = { onPlay(item) },
                                onMore = { songListFile = item },
                                moreDescription = uiText(language, "Song list", "歌曲清單"),
                            )
                            HorizontalDivider(
                                Modifier.padding(start = if (preferences.showThumbnails) preferences.libraryRowSize.thumbnailWidth + 28.dp else 14.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (createPlaylistOpen) CreatePlaylistDialog(
        language = language,
        name = playlistName,
        onName = { playlistName = it.take(60) },
        seedPath = createSeedPath,
        keyword = seedKeyword,
        onKeyword = { seedKeyword = it },
        onDismiss = { createPlaylistOpen = false; playlistName = ""; seedKeyword = ""; createSeedPath = null },
        onCreate = {
            onCreatePlaylistAndSeed(playlistName, createSeedPath, null)
            createPlaylistOpen = false; playlistName = ""; seedKeyword = ""; createSeedPath = null
        },
        onCreateWithKeyword = {
            onCreatePlaylistAndSeed(playlistName, createSeedPath, seedKeyword.ifBlank { null })
            createPlaylistOpen = false; playlistName = ""; seedKeyword = ""; createSeedPath = null
        },
    )
    songListFile?.let { file ->
        AlertDialog(
            onDismissRequest = { songListFile = null },
            title = { Text(uiText(language, "Song list", "歌曲清單")) },
            text = {
                Column {
                    if (activePlaylist != null && file.path in activePlaylist.paths) {
                        TextButton(onClick = { onRemoveFromPlaylist(file.path); songListFile = null }) {
                            Text(uiText(language, "Remove from ${activePlaylist.name}", "從「${activePlaylist.name}」移除"))
                        }
                    }
                    preferences.playlists.forEach { playlist ->
                        val added = file.path in playlist.paths
                        TextButton(
                            enabled = !added,
                            onClick = { onAddToPlaylist(playlist.id, file.path); songListFile = null },
                        ) { Text(if (added) "✓  ${playlist.name}" else playlist.name) }
                    }
                    TextButton(onClick = { songListFile = null; createSeedPath = file.path; createPlaylistOpen = true }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null)
                        Text("  ${uiText(language, "Create playlist", "建立播放清單")}")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { songListFile = null }) { Text(uiText(language, "Done", "完成")) } },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    language: com.local.listentomusic.data.AppLanguage,
    name: String,
    onName: (String) -> Unit,
    seedPath: String?,
    keyword: String,
    onKeyword: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onCreateWithKeyword: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText(language, "Create playlist", "建立播放清單")) },
        text = {
            Column {
                OutlinedTextField(
                    name, onName, singleLine = true,
                    label = { Text(uiText(language, "Playlist name", "播放清單名稱")) },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    keyword, onKeyword, singleLine = true,
                    label = { Text(uiText(language, "Add all songs containing (optional)", "加入含此關鍵字的所有歌曲（選填）")) },
                    placeholder = { Text(uiText(language, "e.g. example", "例如：example")) },
                )
                if (seedPath != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        uiText(language, "Also adds the song you opened.", "同時加入你開啟的歌曲。"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = if (keyword.isBlank()) onCreate else onCreateWithKeyword,
            ) { Text(if (keyword.isBlank()) uiText(language, "Create", "建立") else uiText(language, "Create + add matches", "建立並加入")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } },
    )
}

@Composable
private fun MediaFileRow(
    file: MediaFile,
    isCurrent: Boolean,
    index: Int,
    itemCount: Int,
    dragEnabled: Boolean,
    onMove: (Int, Int) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    rowSize: LibraryRowSize,
    showThumbnails: Boolean,
    showFileDetails: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    moreDescription: String,
) {
    var accumulatedDrag by remember(index, file.path) { mutableFloatStateOf(0f) }
    val thumbnail by produceState<Bitmap?>(null, file.path, "${file.sizeBytes}:${file.modifiedMs}:$showThumbnails") {
        value = if (showThumbnails) onLoadThumbnail(file) else null
    }
    val dragModifier = if (dragEnabled) Modifier.pointerInput(index, itemCount) {
        val threshold = 54.dp.toPx()
        detectDragGesturesAfterLongPress(
            onDragStart = { accumulatedDrag = 0f },
            onDragEnd = { accumulatedDrag = 0f },
            onDragCancel = { accumulatedDrag = 0f },
            onDrag = { change, amount ->
                change.consume()
                accumulatedDrag += amount.y
                if (abs(accumulatedDrag) >= threshold) {
                    val target = if (accumulatedDrag > 0) index + 1 else index - 1
                    if (target in 0 until itemCount) onMove(index, target)
                    accumulatedDrag = 0f
                }
            },
        )
    } else Modifier
    Row(
        Modifier.fillMaxWidth()
            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onPlay).then(dragModifier)
            .padding(horizontal = 14.dp, vertical = rowSize.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) {
            Box(Modifier.width(3.dp).height(rowSize.accentHeight).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.secondary))
            Spacer(Modifier.width(9.dp))
        }
        if (showThumbnails) {
            MediaThumbnail(file, thumbnail, rowSize)
            Spacer(Modifier.width(rowSize.textSpacing))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                file.name.substringBeforeLast('.', file.name),
                style = when (rowSize) {
                    LibraryRowSize.SMALL -> MaterialTheme.typography.bodyLarge
                    LibraryRowSize.MEDIUM -> MaterialTheme.typography.titleMedium
                    LibraryRowSize.LARGE -> MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showFileDetails) {
                Spacer(Modifier.height(if (rowSize == LibraryRowSize.SMALL) 2.dp else 4.dp))
                Text(
                    buildString {
                        append(file.name.substringAfterLast('.', "media").uppercase())
                        append("  •  ${formatBytes(file.sizeBytes)}")
                        if (file.durationMs > 0) append("  •  ${formatDuration(file.durationMs)}")
                    },
                    style = if (rowSize == LibraryRowSize.LARGE) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (dragEnabled) Icon(Icons.Rounded.DragHandle, "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.MoreVert, moreDescription) }
    }
}

// Hoisted so the brush is not reallocated for every row during scrolling.
private val thumbnailBrush @androidx.compose.runtime.Composable get() =
    Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primaryContainer))

@Composable
private fun MediaThumbnail(file: MediaFile, bitmap: Bitmap?, rowSize: LibraryRowSize) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier.size(rowSize.thumbnailWidth, rowSize.thumbnailHeight).clip(shape).background(thumbnailBrush),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // Keep the play glyph subtle so the thumbnail remains visible.
            if (file.kind == MediaKind.VIDEO) {
                val badge = Modifier.align(Alignment.Center).clip(RoundedCornerShape(50))
                Box(badge.background(Color(0xFF0A0C0B).copy(alpha = 0.26f))) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        null,
                        Modifier.size(rowSize.playIconSize.times(1.25f)).padding(2.dp),
                        Color.White.copy(alpha = 0.82f),
                    )
                }
            }
        } else {
            Icon(
                if (file.kind == MediaKind.VIDEO) Icons.Rounded.PlayArrow else Icons.Rounded.MusicNote,
                null,
                Modifier.size(rowSize.placeholderIconSize),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageState(title: String, body: String, buttonText: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}

internal fun formatDuration(milliseconds: Long): String {
    if (milliseconds <= 0L) return "--:--"
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private val LibraryRowSize.thumbnailWidth get() = when (this) { LibraryRowSize.SMALL -> 84.dp; LibraryRowSize.MEDIUM -> 104.dp; LibraryRowSize.LARGE -> 136.dp }
private val LibraryRowSize.thumbnailHeight get() = when (this) { LibraryRowSize.SMALL -> 56.dp; LibraryRowSize.MEDIUM -> 62.dp; LibraryRowSize.LARGE -> 84.dp }
private val LibraryRowSize.verticalPadding get() = when (this) { LibraryRowSize.SMALL -> 3.dp; LibraryRowSize.MEDIUM -> 6.dp; LibraryRowSize.LARGE -> 8.dp }
private val LibraryRowSize.textSpacing get() = when (this) { LibraryRowSize.SMALL -> 10.dp; LibraryRowSize.MEDIUM -> 12.dp; LibraryRowSize.LARGE -> 14.dp }
private val LibraryRowSize.accentHeight get() = when (this) { LibraryRowSize.SMALL -> 34.dp; LibraryRowSize.MEDIUM -> 46.dp; LibraryRowSize.LARGE -> 58.dp }
private val LibraryRowSize.playIconSize get() = when (this) { LibraryRowSize.SMALL -> 14.dp; LibraryRowSize.MEDIUM -> 17.dp; LibraryRowSize.LARGE -> 20.dp }
private val LibraryRowSize.placeholderIconSize get() = when (this) { LibraryRowSize.SMALL -> 22.dp; LibraryRowSize.MEDIUM -> 27.dp; LibraryRowSize.LARGE -> 32.dp }

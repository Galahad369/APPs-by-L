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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.LibraryStatus
import com.local.listentomusic.LibraryUiState
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.UserPreferences
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
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onOpenSettings: () -> Unit,
    onPlay: (MediaFile) -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val markShape = RoundedCornerShape(9.dp)
                        Box(
                            modifier = Modifier.size(36.dp).clip(markShape)
                                .background(Color(0xFFF3F5F0))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, markShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFF0A0C0B),
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column {
                            Text("Greater Art", fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = "${state.files.size} files  •  offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Scan again")
                    }
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                        ) {
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (mode == state.sortMode) "✓  ${mode.label}" else mode.label)
                                    },
                                    onClick = {
                                        sortMenuOpen = false
                                        onSortChange(mode)
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Filter library") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            when (state.status) {
                LibraryStatus.NEEDS_PERMISSION -> MessageState(
                    title = "Allow local file access",
                    body = "Android will open settings. Enable ‘Allow access to manage all files’, then return here.",
                    buttonText = "Open settings",
                    onClick = onGrantStorageAccess,
                )
                LibraryStatus.SCANNING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                LibraryStatus.FOLDER_MISSING -> MessageState(
                    title = "Download folder unavailable",
                    body = "The app could not find or open:\n${state.targetPath}",
                    buttonText = "Scan again",
                    onClick = onRefresh,
                )
                LibraryStatus.CANNOT_READ -> MessageState(
                    title = "Folder access was lost",
                    body = "Grant file access again, then return to the app.",
                    buttonText = "Open settings",
                    onClick = onGrantStorageAccess,
                )
                LibraryStatus.READY -> if (state.files.isEmpty()) {
                    MessageState(
                        title = if (state.query.isBlank()) "No media files yet" else "No matches",
                        body = if (state.query.isBlank()) {
                            "No supported media was found anywhere under:\n${state.targetPath}"
                        } else {
                            "Try a different search."
                        },
                        buttonText = "Scan again",
                        onClick = onRefresh,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        itemsIndexed(state.files, key = { _, item -> item.id }) { index, item ->
                            MediaFileRow(
                                file = item,
                                isCurrent = item.path == currentPath,
                                index = index,
                                dragEnabled = state.sortMode == SortMode.CUSTOM && state.query.isBlank(),
                                itemCount = state.files.size,
                                onMove = onMoveItem,
                                onLoadThumbnail = onLoadThumbnail,
                                rowSize = preferences.libraryRowSize,
                                showThumbnails = preferences.showThumbnails,
                                showFileDetails = preferences.showFileDetails,
                                showTypeBadge = preferences.showTypeBadge,
                                onPlay = { onPlay(item) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (preferences.showThumbnails) {
                                        preferences.libraryRowSize.thumbnailWidth + 28.dp
                                    } else 14.dp
                                ),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }
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
    showTypeBadge: Boolean,
    onPlay: () -> Unit,
) {
    var accumulatedDrag by remember(index, file.path) { mutableFloatStateOf(0f) }
    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = file.path,
        key2 = "${file.sizeBytes}:${file.modifiedMs}:$showThumbnails",
    ) {
        value = if (showThumbnails) onLoadThumbnail(file) else null
    }
    val dragModifier = if (dragEnabled) {
        Modifier.pointerInput(index, itemCount) {
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
        }
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onPlay)
            .then(dragModifier)
            .padding(horizontal = 14.dp, vertical = rowSize.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) {
            Box(
                modifier = Modifier.width(3.dp).height(rowSize.accentHeight).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(Modifier.width(9.dp))
        }
        if (showThumbnails) {
            MediaThumbnail(
                file = file,
                bitmap = thumbnail,
                rowSize = rowSize,
                showTypeBadge = showTypeBadge,
            )
            Spacer(Modifier.width(rowSize.textSpacing))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = file.name.substringBeforeLast('.', file.name),
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
                    text = "${file.name.substringAfterLast('.', "media").uppercase()}  •  ${formatBytes(file.sizeBytes)}",
                    style = if (rowSize == LibraryRowSize.LARGE) {
                        MaterialTheme.typography.bodyMedium
                    } else MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (dragEnabled) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = "Long press and drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MediaThumbnail(
    file: MediaFile,
    bitmap: Bitmap?,
    rowSize: LibraryRowSize,
    showTypeBadge: Boolean,
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(width = rowSize.thumbnailWidth, height = rowSize.thumbnailHeight)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (file.kind == MediaKind.VIDEO) {
                Box(
                    modifier = Modifier.size(rowSize.playBadgeSize).clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(rowSize.playIconSize),
                    )
                }
            }
        } else {
            Icon(
                imageVector = if (file.kind == MediaKind.VIDEO) Icons.Rounded.PlayArrow else Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(rowSize.placeholderIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showTypeBadge) {
            Text(
                text = file.name.substringAfterLast('.', "").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun MessageState(
    title: String,
    body: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}

internal fun formatDuration(milliseconds: Long): String {
    if (milliseconds <= 0) return "--:--"
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private val LibraryRowSize.thumbnailWidth get() = when (this) {
    LibraryRowSize.SMALL -> 72.dp
    LibraryRowSize.MEDIUM -> 100.dp
    LibraryRowSize.LARGE -> 132.dp
}
private val LibraryRowSize.thumbnailHeight get() = when (this) {
    LibraryRowSize.SMALL -> 42.dp
    LibraryRowSize.MEDIUM -> 58.dp
    LibraryRowSize.LARGE -> 78.dp
}
private val LibraryRowSize.verticalPadding get() = when (this) {
    LibraryRowSize.SMALL -> 5.dp
    LibraryRowSize.MEDIUM -> 8.dp
    LibraryRowSize.LARGE -> 10.dp
}
private val LibraryRowSize.textSpacing get() = when (this) {
    LibraryRowSize.SMALL -> 10.dp
    LibraryRowSize.MEDIUM -> 12.dp
    LibraryRowSize.LARGE -> 14.dp
}
private val LibraryRowSize.accentHeight get() = when (this) {
    LibraryRowSize.SMALL -> 34.dp
    LibraryRowSize.MEDIUM -> 46.dp
    LibraryRowSize.LARGE -> 58.dp
}
private val LibraryRowSize.playBadgeSize get() = when (this) {
    LibraryRowSize.SMALL -> 20.dp
    LibraryRowSize.MEDIUM -> 25.dp
    LibraryRowSize.LARGE -> 30.dp
}
private val LibraryRowSize.playIconSize get() = when (this) {
    LibraryRowSize.SMALL -> 14.dp
    LibraryRowSize.MEDIUM -> 17.dp
    LibraryRowSize.LARGE -> 20.dp
}
private val LibraryRowSize.placeholderIconSize get() = when (this) {
    LibraryRowSize.SMALL -> 22.dp
    LibraryRowSize.MEDIUM -> 27.dp
    LibraryRowSize.LARGE -> 32.dp
}

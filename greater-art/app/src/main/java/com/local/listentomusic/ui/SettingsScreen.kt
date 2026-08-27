package com.local.listentomusic.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.local.listentomusic.BuildConfig
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.LocalPlaylist
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences

private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f)
private val seekOffsets = listOf(1000L, 2000L, 3000L, 5000L, 10000L, 20000L, 30000L, 60000L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    playback: PlaybackUiState,
    onBack: () -> Unit,
    onRowSize: (LibraryRowSize) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onShowThumbnails: (Boolean) -> Unit,
    onShowFileDetails: (Boolean) -> Unit,
    onPreloadThumbnails: (Boolean) -> Unit,
    onResumePlayback: (Boolean) -> Unit,
    onAutoPictureInPicture: (Boolean) -> Unit,
    onFloatingWindowMode: (FloatingWindowMode) -> Unit,
    onAppLanguage: (AppLanguage) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistAndSeed: (String, String?, String?) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeatMode: (Int) -> Unit,
    onClearThumbnailCache: () -> Unit,
    onRescan: () -> Unit,
    onReset: () -> Unit,
    onSeekOffset: (Long) -> Unit,
) {
    val language = preferences.appLanguage
    var cacheCleared by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    var editPlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var deletePlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var playlistName by remember { mutableStateOf("") }
    val repeatModes = listOf(
        Player.REPEAT_MODE_ONE to uiText(language, "One", "單曲"),
        Player.REPEAT_MODE_ALL to uiText(language, "All", "全部"),
        Player.REPEAT_MODE_OFF to uiText(language, "Off", "關閉"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiText(language, "Settings", "設定"), fontWeight = FontWeight.ExtraBold)
                        Text("Greater Art ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, uiText(language, "Back", "返回")) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                SectionTitle(uiText(language, "Language & appearance", "語言與外觀"))
                ChoiceSetting(
                    uiText(language, "Language", "語言"),
                    uiText(language, "English is the default. Changes apply immediately.", "變更會立即套用。預設語言為英文。"),
                    AppLanguage.entries,
                    preferences.appLanguage,
                    { if (it == AppLanguage.ENGLISH) "English" else "繁體中文" },
                    onAppLanguage,
                )
                ChoiceSetting(
                    uiText(language, "Theme", "主題"),
                    uiText(language, "Follow Android or keep one appearance.", "跟隨 Android，或固定使用淺色／深色外觀。"),
                    ThemeMode.entries,
                    preferences.themeMode,
                    {
                        when (it) {
                            ThemeMode.SYSTEM -> uiText(language, "System", "系統")
                            ThemeMode.LIGHT -> uiText(language, "Light", "淺色")
                            ThemeMode.DARK -> uiText(language, "Dark", "深色")
                        }
                    },
                    onThemeMode,
                )
                ChoiceSetting(
                    uiText(language, "Library row size", "音樂庫列大小"),
                    uiText(language, "Changes the full row and thumbnail. Small is the default.", "調整完整列與縮圖大小。預設為小。"),
                    LibraryRowSize.entries,
                    preferences.libraryRowSize,
                    {
                        when (it) {
                            LibraryRowSize.SMALL -> uiText(language, "Small", "小")
                            LibraryRowSize.MEDIUM -> uiText(language, "Medium", "中")
                            LibraryRowSize.LARGE -> uiText(language, "Large", "大")
                        }
                    },
                    onRowSize,
                )
                SwitchSetting(uiText(language, "Show thumbnails", "顯示縮圖"), uiText(language, "Turn off previews for the densest list.", "關閉預覽以顯示最緊密的清單。"), preferences.showThumbnails, onShowThumbnails)
                SwitchSetting(uiText(language, "Show file details", "顯示檔案詳情"), uiText(language, "Display format and file size below the title.", "在標題下顯示格式與檔案大小。"), preferences.showFileDetails, onShowFileDetails)

                SectionTitle(uiText(language, "Playback", "播放"))
                ChoiceSetting(uiText(language, "Playback speed", "播放速度"), uiText(language, "Applied immediately and remembered locally.", "立即套用並儲存在本機。"), speeds, playback.speed, { "${it}×" }, onSpeed)
                ChoiceSetting(
                    uiText(language, "Repeat", "循環"),
                    uiText(language, "Repeat one remains the default after reset.", "重設後仍以單曲循環為預設。"),
                    repeatModes,
                    repeatModes.first { it.first == playback.repeatMode },
                    { it.second },
                    { onRepeatMode(it.first) },
                )
                ChoiceSetting(
                    uiText(language, "Jump back / forward", "快退 / 快進"),
                    uiText(language, "How far the skip buttons move playback.", "快退快進按鈕一次移動的時間。"),
                    seekOffsets,
                    preferences.seekOffsetMs,
                    { if (it >= 60_000L) "1m" else "${it / 1000}s" },
                    onSeekOffset,
                )
                SwitchSetting(uiText(language, "Resume last position", "接續上次位置"), uiText(language, "Continue the last file where you stopped.", "從上次停止的位置繼續播放。"), preferences.resumePlayback, onResumePlayback)
                SwitchSetting(uiText(language, "Automatic floating video", "自動浮動影片"), uiText(language, "Enter picture-in-picture when leaving a playing video.", "離開正在播放的影片時進入子母畫面。"), preferences.autoPictureInPicture, onAutoPictureInPicture)
                ChoiceSetting(
                    uiText(language, "Floating window shape", "浮動視窗形狀"),
                    uiText(language, "Compact is the smallest default. Android still controls final size and pinch resizing.", "「精簡」為最小預設。最終大小與縮放仍由 Android 控制。"),
                    FloatingWindowMode.entries,
                    preferences.floatingWindowMode,
                    { if (it == FloatingWindowMode.COMPACT) uiText(language, "Compact", "精簡")
                      else if (it == FloatingWindowMode.FOLLOW_VIDEO) uiText(language, "Follow video", "跟隨影片")
                      else uiText(language, "Mini window", "迷你視窗") },
                    onFloatingWindowMode,
                )

                SectionTitle(uiText(language, "Song lists", "歌曲清單"))
                Text(
                    uiText(language, "Create local playlists, then add songs with the ⋮ button in the library.", "建立本機播放清單，然後使用音樂庫中的 ⋮ 按鈕加入歌曲。"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preferences.playlists.forEach { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(uiText(language, "${playlist.paths.size} songs", "${playlist.paths.size} 首歌曲")) },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { onPlayPlaylist(playlist.id) },
                                    enabled = playlist.paths.isNotEmpty(),
                                ) { Icon(Icons.Rounded.PlayArrow, uiText(language, "Play", "播放")) }
                                IconButton(onClick = { playlistName = playlist.name; editPlaylist = playlist }) { Icon(Icons.Rounded.Edit, uiText(language, "Rename", "重新命名")) }
                                IconButton(onClick = { deletePlaylist = playlist }) { Icon(Icons.Rounded.Delete, uiText(language, "Delete", "刪除")) }
                            }
                        },
                    )
                }
                OutlinedButton(onClick = { playlistName = ""; createOpen = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  ${uiText(language, "Create playlist", "建立播放清單")}")
                }

                SectionTitle(uiText(language, "Library & cache", "音樂庫與快取"))
                SwitchSetting(uiText(language, "Preload thumbnails", "預先載入縮圖"), uiText(language, "Warm the first library page for faster scrolling.", "預先載入第一頁，讓捲動更快速。"), preferences.preloadThumbnails, onPreloadThumbnails)
                ActionCard(Icons.Rounded.Cached, uiText(language, "Scan Download again", "重新掃描 Download"), uiText(language, "Refresh the recursive local media index.", "重新整理遞迴本機媒體索引。"), uiText(language, "Rescan", "重新掃描")) { onRescan(); onBack() }
                ActionCard(
                    Icons.Rounded.DeleteSweep,
                    uiText(language, "Thumbnail cache", "縮圖快取"),
                    if (cacheCleared) uiText(language, "Cache cleared. Previews will be recreated when needed.", "快取已清除，預覽會在需要時重新建立。") else uiText(language, "Remove generated previews without touching media files.", "移除產生的預覽，不會動到媒體檔案。"),
                    uiText(language, "Clear cache", "清除快取"),
                ) { onClearThumbnailCache(); cacheCleared = true }

                SectionTitle(uiText(language, "Privacy", "私隱"))
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Rounded.PrivacyTip, null)
                        Column {
                            Text(uiText(language, "Offline by design", "離線設計"), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(uiText(language, "No Internet permission, ads, analytics, account, telemetry, or cloud library. Everything stays on this device.", "沒有網絡權限、廣告、分析、帳戶、遙測或雲端音樂庫。所有資料都留在本機。"))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onReset(); cacheCleared = false }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Icon(Icons.Rounded.RestartAlt, null)
                    Text("  ${uiText(language, "Reset app settings", "重設應用程式設定")}")
                }
                Text(uiText(language, "Your playlists, library order, and media files are not changed.", "播放清單、音樂庫排序與媒體檔案不會被更改。"), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (createOpen) NameDialog(language, null, playlistName, { playlistName = it.take(60) }, { createOpen = false }, { onCreatePlaylistAndSeed(playlistName, null, null); createOpen = false })
    editPlaylist?.let { playlist ->
        NameDialog(language, playlist, playlistName, { playlistName = it.take(60) }, { editPlaylist = null }, { onRenamePlaylist(playlist.id, playlistName); editPlaylist = null })
    }
    deletePlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletePlaylist = null },
            title = { Text(uiText(language, "Delete playlist?", "刪除播放清單？")) },
            text = { Text(uiText(language, "${playlist.name} will be removed. Media files stay untouched.", "將移除「${playlist.name}」，媒體檔案不會被刪除。")) },
            confirmButton = { TextButton(onClick = { onDeletePlaylist(playlist.id); deletePlaylist = null }) { Text(uiText(language, "Delete", "刪除")) } },
            dismissButton = { TextButton(onClick = { deletePlaylist = null }) { Text(uiText(language, "Cancel", "取消")) } },
        )
    }
}

@Composable
private fun NameDialog(language: AppLanguage, playlist: LocalPlaylist?, name: String, onName: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (playlist == null) uiText(language, "Create playlist", "建立播放清單") else uiText(language, "Rename playlist", "重新命名播放清單")) },
        text = { OutlinedTextField(name, onName, singleLine = true, label = { Text(uiText(language, "Playlist name", "播放清單名稱")) }) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = onConfirm) { Text(uiText(language, "Save", "儲存")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(language, "Cancel", "取消")) } },
    )
}

@Composable private fun SectionTitle(text: String) = Text(text.uppercase(), Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)

@Composable
private fun SwitchSetting(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(description) }, trailingContent = { Switch(checked, onChecked, enabled = enabled) })
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun <T> ChoiceSetting(title: String, description: String, values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value -> FilterChip(value == selected, { onSelect(value) }, { Text(label(value)) }) }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, button: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick, Modifier.padding(top = 10.dp)) { Text(button) }
            }
        }
    }
}

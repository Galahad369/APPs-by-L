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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences

private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f)
private val repeatModes = listOf(
    Player.REPEAT_MODE_ONE to "One",
    Player.REPEAT_MODE_ALL to "All",
    Player.REPEAT_MODE_OFF to "Off",
)

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
    onShowTypeBadge: (Boolean) -> Unit,
    onPreloadThumbnails: (Boolean) -> Unit,
    onResumePlayback: (Boolean) -> Unit,
    onAutoPictureInPicture: (Boolean) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeatMode: (Int) -> Unit,
    onClearThumbnailCache: () -> Unit,
    onRescan: () -> Unit,
    onReset: () -> Unit,
) {
    var cacheCleared by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Greater Art ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                SectionTitle("Appearance")
                ChoiceSetting(
                    title = "Theme",
                    description = "Follow Android or keep one appearance.",
                    values = ThemeMode.entries,
                    selected = preferences.themeMode,
                    label = ThemeMode::label,
                    onSelect = onThemeMode,
                )
                ChoiceSetting(
                    title = "Library row size",
                    description = "Changes the complete row and its thumbnail. Small is the default.",
                    values = LibraryRowSize.entries,
                    selected = preferences.libraryRowSize,
                    label = LibraryRowSize::label,
                    onSelect = onRowSize,
                )
                SwitchSetting(
                    "Show thumbnails",
                    "Turn off previews for the densest possible list.",
                    preferences.showThumbnails,
                    onShowThumbnails,
                )
                SwitchSetting(
                    "Show file details",
                    "Display format and file size below the title.",
                    preferences.showFileDetails,
                    onShowFileDetails,
                )
                SwitchSetting(
                    "Show format badge",
                    "Display MP4, FLAC, MP3, and other labels on thumbnails.",
                    preferences.showTypeBadge,
                    onShowTypeBadge,
                    enabled = preferences.showThumbnails,
                )

                SectionTitle("Playback")
                ChoiceSetting(
                    title = "Playback speed",
                    description = "Applied immediately and remembered locally.",
                    values = speeds,
                    selected = playback.speed,
                    label = { "${it}×" },
                    onSelect = onSpeed,
                )
                ChoiceSetting(
                    title = "Repeat",
                    description = "Repeat one remains the default after a reset.",
                    values = repeatModes,
                    selected = repeatModes.first { it.first == playback.repeatMode },
                    label = { it.second },
                    onSelect = { onRepeatMode(it.first) },
                )
                SwitchSetting(
                    "Resume last position",
                    "Continue the last file where you stopped.",
                    preferences.resumePlayback,
                    onResumePlayback,
                )
                SwitchSetting(
                    "Automatic floating video",
                    "Enter Android picture-in-picture when leaving a playing video.",
                    preferences.autoPictureInPicture,
                    onAutoPictureInPicture,
                )

                SectionTitle("Library & cache")
                SwitchSetting(
                    "Preload thumbnails",
                    "Warm the first library page in the background for faster scrolling.",
                    preferences.preloadThumbnails,
                    onPreloadThumbnails,
                )
                ActionCard(
                    icon = Icons.Rounded.Cached,
                    title = "Scan Download again",
                    body = "Refresh the recursive local media index.",
                    button = "Rescan",
                    onClick = {
                        onRescan()
                        onBack()
                    },
                )
                ActionCard(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Thumbnail cache",
                    body = if (cacheCleared) {
                        "Cache cleared. Previews will be recreated when needed."
                    } else {
                        "Remove locally generated previews without touching media files."
                    },
                    button = "Clear cache",
                    onClick = {
                        onClearThumbnailCache()
                        cacheCleared = true
                    },
                )

                SectionTitle("Privacy")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Rounded.PrivacyTip, contentDescription = null)
                        Column {
                            Text("Offline by design", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No Internet permission, ads, analytics, account, telemetry, or cloud library. Settings and playback history stay on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onReset()
                        cacheCleared = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Text("  Reset app settings")
                }
                Text(
                    "Your library order and media files are not changed.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
        },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun <T> ChoiceSetting(
    title: String,
    description: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { value ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) },
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onClick, modifier = Modifier.padding(top = 10.dp)) { Text(button) }
            }
        }
    }
}

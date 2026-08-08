package com.local.listentomusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.local.listentomusic.PlaybackUiState

@Composable
fun MiniPlayer(
    playback: PlaybackUiState,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF181C1B),
                contentColor = Color(0xFFF3F5F0),
            ),
        ) {
            Column(modifier = Modifier.clickable(onClick = onOpen)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF28302D)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = Color(0xFF8BE9D3),
                        )
                    }
                    Text(
                        text = playback.title.substringBeforeLast('.', playback.title),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFF3F5F0),
                    )
                    IconButton(onClick = onPrevious, enabled = playback.hasPrevious) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color(0xFFF3F5F0),
                        )
                    }
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pause" else "Play",
                            tint = Color(0xFFF3F5F0),
                        )
                    }
                    IconButton(onClick = onNext, enabled = playback.hasNext) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color(0xFFF3F5F0),
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = {
                        if (playback.durationMs > 0) {
                            (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
                        } else 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF8BE9D3),
                    trackColor = Color(0xFF414745),
                )
            }
        }
    }
}

package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.ui.uiText

@Composable
fun MiniPlayer(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    language: AppLanguage,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else 0f
    Surface(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        color = Color.Transparent,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(9.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF171B1A),
                contentColor = Color(0xFFF4F6F2),
            ),
        ) {
            Column(modifier = Modifier.clickable(onClick = onOpen)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.padding(start = 5.dp).size(44.dp).clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF29302E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (artwork != null) {
                            Image(
                                bitmap = artwork.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                                tint = Color(0xFF8BE9D3),
                            )
                        }
                    }
                    Text(
                        text = playback.title.substringBeforeLast('.', playback.title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    )
                    IconButton(onClick = onPrevious, enabled = playback.hasPrevious, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, uiText(language, "Previous", "上一首"), modifier = Modifier.size(21.dp))
                    }
                    IconButton(onClick = onTogglePlay, modifier = Modifier.size(42.dp)) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            uiText(language, if (playback.isPlaying) "Pause" else "Play", if (playback.isPlaying) "暫停" else "播放"),
                            modifier = Modifier.size(25.dp),
                            tint = Color(0xFF8BE9D3),
                        )
                    }
                    IconButton(onClick = onNext, enabled = playback.hasNext, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.SkipNext, uiText(language, "Next", "下一首"), modifier = Modifier.size(21.dp))
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF8BE9D3),
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
            }
        }
    }
}

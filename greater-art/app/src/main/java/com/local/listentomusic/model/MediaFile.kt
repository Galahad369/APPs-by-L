package com.local.listentomusic.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

enum class MediaKind { AUDIO, VIDEO }

data class MediaFile(
    val path: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val modifiedMs: Long,
    val kind: MediaKind,
    val sourcePath: String = path,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long? = null,
    val artist: String = "",
    val album: String = "",
    val searchExtras: String = "",
    val coverUri: String = "",
) {
    val id: String get() = path

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(path)
        .setUri(Uri.fromFile(File(sourcePath)))
        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clipStartMs)
            .apply { clipEndMs?.let { setEndPositionMs(it) } }.build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(artist.takeIf { it.isNotBlank() })
                .setAlbumTitle(album.takeIf { it.isNotBlank() })
                .setMediaType(
                    if (kind == MediaKind.VIDEO) {
                        MediaMetadata.MEDIA_TYPE_VIDEO
                    } else {
                        MediaMetadata.MEDIA_TYPE_MUSIC
                    }
                )
                .build()
        )
        .build()
}

enum class SortMode(val label: String) {
    CUSTOM("Custom order"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
}

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
) {
    val id: String get() = path

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(path)
        .setUri(Uri.fromFile(File(path)))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
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

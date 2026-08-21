@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.local.localkit.core

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import java.io.File

data class MediaInfo(val durationMs: Long, val width: Int, val height: Int, val hasVideo: Boolean)

object MediaOperations {
    fun inspect(context: Context, uri: Uri): MediaInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            MediaInfo(
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            )
        } finally { retriever.release() }
    }

    fun export(
        context: Context,
        input: Uri,
        output: Uri,
        startMs: Long,
        endMs: Long,
        audioOnly: Boolean,
        mute: Boolean,
        targetHeight: Int?,
        onDone: (Result<Unit>) -> Unit
    ): Transformer {
        val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs.coerceAtLeast(0)).apply {
            if (endMs > startMs) setEndPositionMs(endMs)
        }.build()
        val mediaItem = MediaItem.Builder().setUri(input).setClippingConfiguration(clipping).build()
        val videoEffects: List<Effect> = targetHeight?.takeIf { it > 0 }?.let { listOf(Presentation.createForHeight(it)) }.orEmpty()
        val edited = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(audioOnly)
            .setRemoveAudio(mute && !audioOnly)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
        val temp = File(context.cacheDir, "export-${System.nanoTime()}.${if (audioOnly) "m4a" else "mp4"}")
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                val result = runCatching {
                    context.contentResolver.openOutputStream(output, "w")?.use { destination -> temp.inputStream().use { it.copyTo(destination) } } ?: error("Unable to open output")
                    temp.delete()
                    Unit
                }
                onDone(result)
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                temp.delete()
                onDone(Result.failure(exportException))
            }
        }
        val transformer = Transformer.Builder(context).addListener(listener).apply {
            setAudioMimeType(MimeTypes.AUDIO_AAC)
            if (!audioOnly) setVideoMimeType(MimeTypes.VIDEO_H264)
        }.build()
        transformer.start(edited, temp.absolutePath)
        return transformer
    }
}

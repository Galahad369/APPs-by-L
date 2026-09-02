package com.local.listentomusic.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max

/** Decodes real PCM peaks off the UI thread and keeps only a tiny local cache. */
class WaveformRepository(context: Context) {
    private val directory = File(context.cacheDir, "waveforms").apply { mkdirs() }

    suspend fun load(path: String, size: Long, modified: Long): FloatArray? = withContext(Dispatchers.Default) {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("$path|$size|$modified|$BINS".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val cached = File(directory, "$key.bin")
        read(cached)?.let { return@withContext it }
        val decoded = decode(path) ?: return@withContext null
        runCatching { write(cached, decoded) }
        decoded
    }

    private suspend fun decode(path: String): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
            extractor.selectTrack(track)
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            val peaks = FloatArray(BINS)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val input = codec.getInputBuffer(index) ?: continue
                        val count = extractor.readSampleData(input, 0)
                        if (count < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, count, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = codec.outputFormat
                        pcmEncoding = if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) out.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        else AudioFormat.ENCODING_PCM_16BIT
                    }
                    else -> if (index >= 0) {
                        codec.getOutputBuffer(index)?.let { buffer ->
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            var peak = 0f
                            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                val floats = buffer.slice().order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                                while (floats.hasRemaining()) peak = max(peak, abs(floats.get()).coerceAtMost(1f))
                            } else {
                                val shorts = buffer.slice().order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                while (shorts.hasRemaining()) peak = max(peak, abs(shorts.get().toInt()) / 32768f)
                            }
                            val bin = ((info.presentationTimeUs.toDouble() / durationUs) * BINS).toInt().coerceIn(0, BINS - 1)
                            peaks[bin] = max(peaks[bin], peak)
                        }
                        codec.releaseOutputBuffer(index, false)
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                }
            }
            val maxPeak = peaks.maxOrNull()?.coerceAtLeast(0.001f) ?: return null
            FloatArray(BINS) { (peaks[it] / maxPeak).coerceIn(0.04f, 1f) }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun read(file: File): FloatArray? = runCatching {
        if (!file.isFile) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            val count = input.readInt()
            if (count != BINS) return null
            FloatArray(count) { input.readFloat() }
        }
    }.getOrNull()

    private fun write(file: File, values: FloatArray) {
        DataOutputStream(file.outputStream().buffered()).use { output ->
            output.writeInt(values.size)
            values.forEach(output::writeFloat)
        }
    }

    private companion object {
        const val BINS = 96
        const val TIMEOUT_US = 10_000L
    }
}

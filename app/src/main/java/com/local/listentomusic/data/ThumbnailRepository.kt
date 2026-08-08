package com.local.listentomusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.scale
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Local-only thumbnail pipeline:
 * 1. memory LRU, 2. persistent disk cache, 3. Android system thumbnail API,
 * 4. MediaMetadataRetriever fallback. No network image loader is involved.
 */
class ThumbnailRepository(context: Context) {
    private val cacheDirectory = File(context.cacheDir, "media_thumbnails").apply { mkdirs() }
    private val keyLocks = ConcurrentHashMap<String, Mutex>()
    private val preloadWorkers = Semaphore(2)
    private val memoryCache = object : LruCache<String, Bitmap>(memoryBudgetKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = max(1, value.byteCount / 1024)
    }

    init {
        pruneDiskCache()
    }

    suspend fun load(file: MediaFile): Bitmap? = withContext(Dispatchers.IO) {
        val key = cacheKey(file)
        memoryCache.get(key)?.let { return@withContext it }

        val mutex = keyLocks.computeIfAbsent(key) { Mutex() }
        try {
            mutex.withLock {
                memoryCache.get(key)?.let { return@withLock it }
                readDisk(key)?.let {
                    memoryCache.put(key, it)
                    return@withLock it
                }

                val generated = generate(file) ?: return@withLock null
                memoryCache.put(key, generated)
                writeDisk(key, generated)
                generated
            }
        } finally {
            keyLocks.remove(key, mutex)
        }
    }

    suspend fun preload(files: List<MediaFile>) = supervisorScope {
        files
            .sortedByDescending { it.kind == MediaKind.VIDEO }
            .map { file ->
                async(Dispatchers.IO) {
                    preloadWorkers.withPermit { load(file) }
                }
            }
            .awaitAll()
        Unit
    }

    private fun generate(file: MediaFile): Bitmap? = when (file.kind) {
        MediaKind.VIDEO -> createVideoThumbnail(file)
        MediaKind.AUDIO -> createEmbeddedArtwork(file)
    }

    private fun createVideoThumbnail(media: MediaFile): Bitmap? {
        val source = File(media.path)
        val systemThumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                ThumbnailUtils.createVideoThumbnail(source, Size(VIDEO_WIDTH, VIDEO_HEIGHT), null)
            }.getOrNull()
        } else {
            null
        }
        if (systemThumbnail != null) return centerCrop(systemThumbnail, VIDEO_WIDTH, VIDEO_HEIGHT)

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(media.path)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            centerCrop(frame, VIDEO_WIDTH, VIDEO_HEIGHT)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun createEmbeddedArtwork(media: MediaFile): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(media.path)
            val bytes = retriever.embeddedPicture ?: return null
            decodeSampled(bytes, ARTWORK_SIZE, ARTWORK_SIZE)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeSampled(bytes: ByteArray, width: Int, height: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > width * 2 || bounds.outHeight / sample > height * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        if (source.width == width && source.height == height) return source
        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val scaledWidth = max(width, (source.width * scale).toInt())
        val scaledHeight = max(height, (source.height * scale).toInt())
        val scaled = source.scale(scaledWidth, scaledHeight)
        val left = ((scaledWidth - width) / 2).coerceAtLeast(0)
        val top = ((scaledHeight - height) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, left, top, width, height)
        if (scaled !== source && !source.isRecycled) source.recycle()
        if (cropped !== scaled && !scaled.isRecycled) scaled.recycle()
        return cropped
    }

    private fun readDisk(key: String): Bitmap? {
        val cached = File(cacheDirectory, "$key.webp")
        if (!cached.isFile) return null
        return BitmapFactory.decodeFile(cached.absolutePath)?.also {
            cached.setLastModified(System.currentTimeMillis())
        } ?: run {
            cached.delete()
            null
        }
    }

    private fun writeDisk(key: String, bitmap: Bitmap) {
        val destination = File(cacheDirectory, "$key.webp")
        val temporary = File(cacheDirectory, "$key.tmp")
        runCatching {
            FileOutputStream(temporary).use { output ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.PNG
                }
                check(bitmap.compress(format, 100, output))
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
        }.onFailure { temporary.delete() }
    }

    private fun cacheKey(file: MediaFile): String {
        val fingerprint = "${file.path}|${file.sizeBytes}|${file.modifiedMs}"
        return MessageDigest.getInstance("SHA-256")
            .digest(fingerprint.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun pruneDiskCache() {
        runCatching {
            val files = cacheDirectory.listFiles()?.filter { it.isFile }.orEmpty()
                .sortedByDescending { it.lastModified() }
            var retainedBytes = 0L
            files.forEachIndexed { index, file ->
                retainedBytes += file.length()
                if (index >= MAX_DISK_FILES || retainedBytes > MAX_DISK_BYTES) file.delete()
            }
        }
    }

    private fun memoryBudgetKb(): Int =
        (Runtime.getRuntime().maxMemory() / 12L / 1024L).coerceIn(8_192L, 65_536L).toInt()

    private companion object {
        const val VIDEO_WIDTH = 640
        const val VIDEO_HEIGHT = 360
        const val ARTWORK_SIZE = 512
        const val MAX_DISK_FILES = 600
        const val MAX_DISK_BYTES = 256L * 1024L * 1024L
    }
}

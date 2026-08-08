package com.local.listentomusic.data

import android.os.Environment
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScanner {
    private val videoExtensions = setOf(
        "mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi",
    )
    private val audioExtensions = setOf(
        "mp3", "aac", "m4a", "flac", "wav", "alac", "aif", "aiff", "opus", "ogg",
        "ape", "dsf", "dff", "amr", "ac3", "eac3", "mka",
    )
    private val supportedExtensions = videoExtensions + audioExtensions

    @Suppress("DEPRECATION")
    fun targetFolder(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun isInsideTarget(file: File): Boolean = runCatching {
        val root = targetFolder().canonicalFile.toPath()
        file.canonicalFile.toPath().startsWith(root)
    }.getOrDefault(false)

    /**
     * Recursively scans Download so media inside any subfolder is discovered without
     * hardcoding or exposing a particular source-app folder name.
     *
     * A Play-Store build should replace direct File access with the Storage Access Framework,
     * persist a tree URI using takePersistableUriPermission(), and scan DocumentFile children.
     */
    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val folder = targetFolder()
        if (!folder.exists()) return@withContext ScanResult.FolderMissing(folder.absolutePath)
        if (!folder.isDirectory || !folder.canRead()) {
            return@withContext ScanResult.PermissionMissing(folder.absolutePath)
        }

        val files = folder.walkTopDown()
            .onFail { _, _ -> /* Ignore unreadable children and keep the rest of the library. */ }
            .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            // Keep the launch scan fast: do not open or decode every file here.
            // Thumbnails and expensive metadata are loaded from a bounded background cache.
            .map { file ->
                MediaFile(
                    path = file.absolutePath,
                    name = file.name,
                    durationMs = 0L,
                    sizeBytes = file.length(),
                    modifiedMs = file.lastModified(),
                    kind = if (file.extension.lowercase() in videoExtensions) {
                        MediaKind.VIDEO
                    } else {
                        MediaKind.AUDIO
                    },
                )
            }
            .toList()

        ScanResult.Success(files)
    }

}

sealed interface ScanResult {
    data class Success(val files: List<MediaFile>) : ScanResult
    data class FolderMissing(val path: String) : ScanResult
    data class PermissionMissing(val path: String) : ScanResult
}

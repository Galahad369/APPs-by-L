package com.local.localkit.core

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class LocalFileInfo(val uri: Uri, val name: String, val size: Long, val mimeType: String?, val relativePath: String)
data class FolderReport(val files: List<LocalFileInfo>, val folders: Int, val totalBytes: Long)

object FileOperations {
    fun describe(resolver: ContentResolver, uri: Uri): LocalFileInfo {
        var name = uri.lastPathSegment ?: "file"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { size = cursor.getLong(it) }
            }
        }
        return LocalFileInfo(uri, name, size, resolver.getType(uri), name)
    }

    fun scanTree(context: Context, treeUri: Uri, maxFiles: Int = 20_000): FolderReport {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return FolderReport(emptyList(), 0, 0)
        val files = mutableListOf<LocalFileInfo>()
        var folders = 0
        fun walk(node: DocumentFile, path: String) {
            if (files.size >= maxFiles) return
            if (node.isDirectory) {
                folders++
                node.listFiles().forEach { child -> walk(child, if (path.isBlank()) child.name.orEmpty() else "$path/${child.name.orEmpty()}") }
            } else if (node.isFile) {
                files += LocalFileInfo(node.uri, node.name ?: "file", node.length(), node.type, path)
            }
        }
        walk(root, "")
        return FolderReport(files, (folders - 1).coerceAtLeast(0), files.sumOf { it.size.coerceAtLeast(0) })
    }

    fun sha256(resolver: ContentResolver, uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        resolver.openInputStream(uri)?.buffered()?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        } ?: error("Unable to open file")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun findDuplicates(resolver: ContentResolver, report: FolderReport): List<List<LocalFileInfo>> = report.files
        .filter { it.size >= 0 }
        .groupBy { it.size }
        .filterValues { it.size > 1 }
        .values
        .flatMap { sameSize -> sameSize.groupBy { sha256(resolver, it.uri) }.values.filter { it.size > 1 } }
        .sortedByDescending { group -> group.first().size * (group.size - 1) }

    fun createZip(resolver: ContentResolver, sources: List<Uri>, output: Uri) {
        resolver.openOutputStream(output, "w")?.let { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                val used = mutableSetOf<String>()
                sources.forEachIndexed { index, uri ->
                    val info = describe(resolver, uri)
                    var name = sanitizeEntryName(info.name)
                    if (!used.add(name)) {
                        val dot = name.lastIndexOf('.')
                        name = if (dot > 0) "${name.substring(0, dot)}-${index + 1}${name.substring(dot)}" else "$name-${index + 1}"
                        used += name
                    }
                    zip.putNextEntry(ZipEntry(name))
                    resolver.openInputStream(uri)?.use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("Unable to create output")
    }

    fun extractZip(context: Context, zipUri: Uri, destinationTree: Uri): Int {
        val resolver = context.contentResolver
        val root = DocumentFile.fromTreeUri(context, destinationTree) ?: error("Unable to open destination")
        var extracted = 0
        resolver.openInputStream(zipUri)?.let { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val safe = validateZipPath(entry.name)
                    if (safe.isBlank()) { zip.closeEntry(); continue }
                    val parts = safe.split('/').filter { it.isNotBlank() }
                    var parent = root
                    parts.dropLast(1).forEach { segment ->
                        parent = parent.findFile(segment)?.takeIf { it.isDirectory } ?: parent.createDirectory(segment) ?: error("Cannot create folder")
                    }
                    if (entry.isDirectory) {
                        parts.lastOrNull()?.let { if (parent.findFile(it) == null) parent.createDirectory(it) }
                    } else {
                        val name = parts.last()
                        parent.findFile(name)?.delete()
                        val file = parent.createFile("application/octet-stream", name) ?: error("Cannot create $name")
                        resolver.openOutputStream(file.uri, "w")?.use { output -> zip.copyTo(output) } ?: error("Cannot write $name")
                        extracted++
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Unable to open archive")
        return extracted
    }

    fun readableBytes(bytes: Long): String {
        if (bytes < 0) return "Unknown size"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
        return "%.1f %s".format(value, units[unit])
    }

    private fun sanitizeEntryName(value: String): String = value.replace('\\', '_').replace('/', '_').ifBlank { "file" }

    internal fun validateZipPath(value: String): String {
        val normalized = value.replace('\\', '/')
        require(!normalized.startsWith('/')) { "Absolute ZIP paths are blocked" }
        require(!Regex("^[A-Za-z]:").containsMatchIn(normalized)) { "Drive paths are blocked" }
        require(normalized.split('/').none { it == ".." }) { "Parent traversal is blocked" }
        return normalized.split('/').filter { it.isNotBlank() && it != "." }.joinToString("/")
    }
}


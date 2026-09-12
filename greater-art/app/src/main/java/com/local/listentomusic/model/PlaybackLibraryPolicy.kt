package com.local.listentomusic.model

internal fun browsingQueue(selected: MediaFile, ordered: List<MediaFile>, library: List<MediaFile>): List<MediaFile> =
    ordered.takeIf { files -> files.any { it.path == selected.path } }
        ?: (listOf(selected) + library.filter { it.sourcePath != selected.sourcePath })

internal fun waveformWarmupPaths(queue: List<MediaFile>, library: List<MediaFile>, currentPath: String?, limit: Int = 8): List<String> {
    val files = (queue + library).distinctBy { it.path }
    val start = files.indexOfFirst { it.path == currentPath }.coerceAtLeast(0)
    return (files.drop(start) + files.take(start)).filter { it.kind == MediaKind.AUDIO }
        .distinctBy { it.sourcePath }.take(limit.coerceAtLeast(0)).map { it.path }
}

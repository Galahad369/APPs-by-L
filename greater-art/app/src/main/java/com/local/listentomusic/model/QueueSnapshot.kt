package com.local.listentomusic.model

/** Preserve session order and every entry, including duplicates and not-yet-scanned files. */
internal fun <T> reconcileSessionQueue(ids: List<String>, known: Map<String, T>, fallback: (Int) -> T): List<T> =
    ids.mapIndexed { index, id -> known[id] ?: fallback(index) }

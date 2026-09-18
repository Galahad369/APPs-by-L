package com.local.listentomusic.ui

/** Order presentation only; never change persisted enum names or the user's selection. */
internal fun <T> defaultFirst(values: List<T>, default: T): List<T> =
    values.filter { it == default } + values.filterNot { it == default }

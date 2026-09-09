package com.local.listentomusic.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

/** MediaStore signals changes, while the file scanner remains authoritative for unusual formats. */
class LibraryObserver(private val context: Context, private val onChanged: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = Runnable(onChanged)
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { handler.removeCallbacks(refresh); handler.postDelayed(refresh, 1200) }
    }
    private var registered = false
    fun start() {
        if (registered) return
        registered = runCatching { context.contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer); true }.getOrDefault(false)
    }
    fun stop() {
        handler.removeCallbacks(refresh)
        if (registered) runCatching { context.contentResolver.unregisterContentObserver(observer) }
        registered = false
    }
}

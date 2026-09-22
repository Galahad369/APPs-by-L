package com.local.listentomusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** Shared by the three player presentations; only the final borrower releases it. */
internal object PlaybackConnection {
    private val pool = SharedPlaybackResource<ListenableFuture<MediaController>>(MediaController::releaseFuture)

    @MainThread
    fun acquire(context: Context): SharedPlaybackResource.Lease<ListenableFuture<MediaController>> = pool.acquire {
        val app = context.applicationContext
        MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
    }
}

/** Main-thread lease ownership, independent of Android so lifetime rules are testable. */
internal class SharedPlaybackResource<T>(private val release: (T) -> Unit) {
    private var current: T? = null
    private var borrowers = 0

    fun acquire(create: () -> T): Lease<T> {
        val resource = current ?: create().also { current = it }
        borrowers++
        return Lease(resource) {
            borrowers--
            if (borrowers == 0) {
                current = null
                release(resource)
            }
        }
    }

    class Lease<T>(val value: T, private val onClose: () -> Unit) : AutoCloseable {
        var closed = false
            private set
        override fun close() {
            if (closed) return
            closed = true
            onClose()
        }
    }
}

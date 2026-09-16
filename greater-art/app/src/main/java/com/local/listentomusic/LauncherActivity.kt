package com.local.listentomusic

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Exported launcher entry point with no command surface.
 *
 * The real MainActivity is non-exported so only this app can send its internal
 * mini-window command extras. External launchers receive this trampoline only.
 */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

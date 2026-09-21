package com.local.listentomusic.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Lets Android's share sheet sit above the system player overlay. The overlay
 * remains hidden until this chooser returns, without restarting playback.
 */
class ShareProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val chooser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CHOOSER, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CHOOSER)
        }
        if (chooser == null) {
            finish()
            return
        }
        runCatching { startActivityForResult(chooser, REQUEST_SHARE) }.onFailure {
            Toast.makeText(this, "Could not open sharing", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Deprecated("The platform chooser result API is sufficient for this transient Activity.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SHARE) finish()
    }

    override fun onDestroy() {
        if (isFinishing) sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName))
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CHOOSER = "share_chooser"
        const val ACTION_FINISHED = "com.local.listentomusic.SHARE_FINISHED"
        private const val REQUEST_SHARE = 1
    }
}

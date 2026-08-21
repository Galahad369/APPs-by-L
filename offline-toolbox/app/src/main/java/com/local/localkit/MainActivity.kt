package com.local.localkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.local.localkit.ui.LocalKitApp
import com.local.localkit.ui.theme.LocalKitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalKitTheme {
                LocalKitApp()
            }
        }
    }
}


package com.local.listentomusic.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Explicit, local-only diagnostics. It reports state supplied by the app and never
 * introspects private Compose internals or sends data anywhere.
 */
@Composable
internal fun DeveloperDiagnostics(
    report: String,
    regions: List<String>,
    warning: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var showRegions by remember { mutableStateOf(false) }
    val accent = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

    Box(modifier = Modifier.fillMaxSize()) {
        if (showRegions) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                regions.forEach { region ->
                    Text(
                        text = region,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Surface(
            onClick = { open = true },
            modifier = modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            shape = RoundedCornerShape(7.dp),
            color = Color.Black.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.9f)),
        ) {
            Text(
                if (warning) "DEV!" else "DEV",
                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    if (open) {
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.statusBarsPadding().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("SYSTEM INSPECTOR", style = MaterialTheme.typography.titleLarge)
                            Text("LOCAL ONLY · LIVE STATE", color = accent, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { open = false }) { Text("CLOSE") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SHOW REGION IDS", fontWeight = FontWeight.SemiBold)
                            Text("Labels the major visible UI zones.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = showRegions, onCheckedChange = { showRegions = it })
                    }
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        LazyColumn(Modifier.fillMaxSize().padding(14.dp)) {
                            item {
                                SelectionContainer {
                                    Text(report, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            item {
                                Text(
                                    "\nACTIVE REGIONS",
                                    color = accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            items(regions) { region ->
                                Text("• $region", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val payload = buildString {
                                appendLine(report)
                                appendLine("ACTIVE REGIONS")
                                regions.forEach { appendLine("- $it") }
                            }
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Greater Art bug report", payload))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("COPY BUG REPORT") }
                }
            }
        }
    }
}

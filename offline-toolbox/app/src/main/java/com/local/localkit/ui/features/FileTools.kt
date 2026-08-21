package com.local.localkit.ui.features

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.local.localkit.core.FileOperations
import com.local.localkit.core.FolderReport
import com.local.localkit.core.LocalFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FileBrowserScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<LocalFileInfo?>(null) }
    var report by remember { mutableStateOf<FolderReport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selected = FileOperations.describe(context.contentResolver, it); report = null }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            persistTree(context, it); selected = null; busy = true
            scope.launch { runCatching { withContext(Dispatchers.IO) { FileOperations.scanTree(context, it) } }.onSuccess { report = it }.onFailure { error = it.message }; busy = false }
        }
    }

    ToolPage("Files, with your permission", "Android's system picker grants access only to what you choose.") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { filePicker.launch(arrayOf("*/*")) }, Modifier.weight(1f)) { Text("Choose file") }
            OutlinedButton(onClick = { folderPicker.launch(null) }, Modifier.weight(1f)) { Text("Choose folder") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { ErrorCard(it) }
        selected?.let {
            ResultCard(it.name)
            InfoLine("Type", it.mimeType ?: "Unknown")
            InfoLine("Size", FileOperations.readableBytes(it.size))
            Text(it.uri.toString(), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
        report?.let { FolderSummary(it) }
    }
}

@Composable
fun ArchiveScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var archive by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val saveZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { output ->
        if (output != null && sources.isNotEmpty()) {
            busy = true; scope.launch {
                status = runCatching { withContext(Dispatchers.IO) { FileOperations.createZip(context.contentResolver, sources, output) }; "Created ZIP with ${sources.size} files" }.getOrElse { "Could not create ZIP: ${it.message}" }
                busy = false
            }
        }
    }
    val pickSources = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { sources = it; status = null }
    val pickArchive = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { archive = it; status = null }
    val pickDestination = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        val input = archive
        if (tree != null && input != null) {
            persistTree(context, tree); busy = true; scope.launch {
                status = runCatching { withContext(Dispatchers.IO) { FileOperations.extractZip(context, input, tree) } }.fold({ "Extracted $it files safely" }, { "Could not extract ZIP: ${it.message}" })
                busy = false
            }
        }
    }

    ToolPage("ZIP without surprises", "Extraction blocks absolute paths and parent-folder traversal.") {
        Text("CREATE", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { pickSources.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text(if (sources.isEmpty()) "Select files" else "${sources.size} files selected") }
        Button(onClick = { saveZip.launch("LocalKit-archive.zip") }, enabled = sources.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose where to save ZIP") }
        HorizontalDivider()
        Text("EXTRACT", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { pickArchive.launch(arrayOf("application/zip", "application/x-zip-compressed")) }, Modifier.fillMaxWidth()) { Text(if (archive == null) "Select ZIP" else "ZIP selected") }
        Button(onClick = { pickDestination.launch(null) }, enabled = archive != null && !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose extraction folder") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        status?.let { ResultCard(it) }
    }
}

@Composable
fun StorageAnalyzerScreen() {
    FolderAnalysisPage(
        title = "Evidence, not a fake cleaner",
        subtitle = "Choose a folder. LocalKit reports totals and largest files; it never deletes automatically."
    ) { report ->
        FolderSummary(report)
        Text("Largest files", style = MaterialTheme.typography.titleMedium)
        report.files.sortedByDescending { it.size }.take(30).forEach { FileInfoRow(it) }
    }
}

@Composable
fun DuplicateFinderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<List<LocalFileInfo>>?>(null) }
    var busy by remember { mutableStateOf(false) }

    FolderAnalysisPage(
        title = "Compare contents, not names",
        subtitle = "Files are grouped by size, then SHA-256. Nothing is deleted."
    ) { report ->
        Button(onClick = {
            busy = true
            scope.launch { groups = withContext(Dispatchers.IO) { FileOperations.findDuplicates(context.contentResolver, report) }; busy = false }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Hash possible duplicates") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        groups?.let { found ->
            ResultCard(if (found.isEmpty()) "No exact duplicates found" else "${found.size} duplicate groups")
            found.take(30).forEachIndexed { index, group ->
                Text("Group ${index + 1} · ${FileOperations.readableBytes(group.first().size)} each", style = MaterialTheme.typography.titleSmall)
                group.forEach { Text("• ${it.relativePath}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun HashVerifierScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<LocalFileInfo?>(null) }
    var hash by remember { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            info = FileOperations.describe(context.contentResolver, it); hash = ""; busy = true
            scope.launch { hash = runCatching { withContext(Dispatchers.IO) { FileOperations.sha256(context.contentResolver, it) } }.getOrElse { "Error: ${it.message}" }; busy = false }
        }
    }
    val normalizedExpected = expected.filterNot(Char::isWhitespace).lowercase()
    val matches = normalizedExpected.length == 64 && normalizedExpected == hash

    ToolPage("SHA-256 checksum", "Select a file, then optionally paste the publisher's expected hash.") {
        Button(onClick = { picker.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("Choose file") }
        info?.let { Text(it.name, style = MaterialTheme.typography.titleMedium) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (hash.isNotBlank()) {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(hash, Modifier.padding(14.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(expected, { expected = it }, Modifier.fillMaxWidth(), label = { Text("Expected SHA-256 (optional)") }, minLines = 2)
            if (normalizedExpected.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (matches) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, tint = if (matches) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                    Text(if (matches) "Exact match" else "Does not match", color = if (matches) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FolderAnalysisPage(title: String, subtitle: String, content: @Composable ColumnScope.(FolderReport) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<FolderReport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            persistTree(context, it); busy = true; error = null
            scope.launch { runCatching { withContext(Dispatchers.IO) { FileOperations.scanTree(context, it) } }.onSuccess { report = it }.onFailure { error = it.message }; busy = false }
        }
    }
    ToolPage(title, subtitle) {
        Button(onClick = { picker.launch(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose folder") }
        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Reading file metadata locally…") }
        error?.let { ErrorCard(it) }
        report?.let { content(it) }
    }
}

@Composable
private fun FolderSummary(report: FolderReport) {
    ResultCard("${report.files.size} files · ${FileOperations.readableBytes(report.totalBytes)}")
    InfoLine("Subfolders", report.folders.toString())
}

@Composable
private fun FileInfoRow(file: LocalFileInfo) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(file.relativePath, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(FileOperations.readableBytes(file.size), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun persistTree(context: android.content.Context, uri: Uri) {
    runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
}

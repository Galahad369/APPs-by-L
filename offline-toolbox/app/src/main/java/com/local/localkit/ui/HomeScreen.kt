package com.local.localkit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.local.localkit.model.*

@Composable
fun HomeScreen(vm: ToolboxViewModel, onOpenTool: (ToolId) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf<ToolSection?>(null) }
    val favorites by vm.favorites.collectAsState()
    val recents by vm.recents.collectAsState()
    val visibleTools = remember(query, section) { vm.filtered(query, section) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 42.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("LOCALKIT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Useful things.\nNothing watching.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("25 tools · offline · zero ads", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("What do you want to do?") },
                trailingIcon = if (query.isNotEmpty()) {{ IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } }} else null,
                shape = MaterialTheme.shapes.large
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction("Pick file", Icons.Outlined.FolderOpen, Modifier.weight(1f)) { onOpenTool(ToolId.FILE_BROWSER) }
                PrimaryAction("Scan", Icons.Outlined.CenterFocusStrong, Modifier.weight(1f)) { onOpenTool(ToolId.QR_SCANNER) }
                PrimaryAction("Paste", Icons.Outlined.ContentPaste, Modifier.weight(1f)) { onOpenTool(ToolId.TEXT_WORKBENCH) }
            }
        }

        if (recents.isNotEmpty() && query.isBlank() && section == null) {
            item { SectionLabel("Recent") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recents) { id ->
                        val tool = ToolRegistry.byId(id)
                        AssistChip(onClick = { onOpenTool(id) }, label = { Text(tool.title) }, leadingIcon = { Icon(iconFor(id), null, Modifier.size(18.dp)) })
                    }
                }
            }
        }

        item { SectionLabel("Browse") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = section == null, onClick = { section = null }, label = { Text("All") }) }
                items(ToolSection.entries) { item ->
                    FilterChip(selected = section == item, onClick = { section = if (section == item) null else item }, label = { Text(item.title) })
                }
            }
        }

        items(visibleTools, key = { it.id }) { tool ->
            ToolRow(tool, tool.id in favorites, onOpenTool)
        }

        if (visibleTools.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null, Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No matching tool", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(62.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ToolRow(tool: Tool, favorite: Boolean, onOpenTool: (ToolId) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpenTool(tool.id) },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(iconFor(tool.id), null, Modifier.padding(12.dp).size(25.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (favorite) {
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Outlined.Star, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(tool.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

package com.local.localkit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.local.localkit.model.ToolId
import com.local.localkit.model.ToolRegistry
import com.local.localkit.ui.features.ToolContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalKitApp(vm: ToolboxViewModel = viewModel()) {
    var activeTool by rememberSaveable { mutableStateOf<ToolId?>(null) }
    val favorites by vm.favorites.collectAsState()

    BackHandler(enabled = activeTool != null) { activeTool = null }

    val currentTool = activeTool
    if (currentTool == null) {
        HomeScreen(
            vm = vm,
            onOpenTool = { id ->
                vm.open(id)
                activeTool = id
            }
        )
    } else {
        val tool = ToolRegistry.byId(currentTool)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(tool.title, style = MaterialTheme.typography.titleLarge)
                            Text(tool.section.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { activeTool = null }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.toggleFavorite(tool.id) }) {
                            Icon(
                                if (tool.id in favorites) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                ToolContent(currentTool)
            }
        }
    }
}

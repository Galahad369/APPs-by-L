package com.local.localkit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.localkit.data.UserPreferences
import com.local.localkit.model.Tool
import com.local.localkit.model.ToolId
import com.local.localkit.model.ToolRegistry
import com.local.localkit.model.ToolSection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ToolboxViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = UserPreferences(application)

    val favorites = preferences.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val recents = preferences.recents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun filtered(query: String, section: ToolSection?): List<Tool> {
        val normalized = query.trim().lowercase()
        return ToolRegistry.tools.filter { tool ->
            (section == null || tool.section == section) &&
                (normalized.isBlank() || listOf(tool.title, tool.description, tool.section.title)
                    .plus(tool.keywords)
                    .any { normalized in it.lowercase() })
        }
    }

    fun open(id: ToolId) = viewModelScope.launch { preferences.recordRecent(id) }
    fun toggleFavorite(id: ToolId) = viewModelScope.launch { preferences.toggleFavorite(id) }
}


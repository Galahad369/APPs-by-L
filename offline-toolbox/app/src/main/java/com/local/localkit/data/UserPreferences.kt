package com.local.localkit.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.local.localkit.model.ToolId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("local_kit_preferences")

class UserPreferences(private val context: Context) {
    private val favoritesKey = stringPreferencesKey("favorites")
    private val recentsKey = stringPreferencesKey("recents")

    val favorites: Flow<Set<ToolId>> = context.dataStore.data.map { prefs ->
        decode(prefs[favoritesKey]).toSet()
    }

    val recents: Flow<List<ToolId>> = context.dataStore.data.map { prefs ->
        decode(prefs[recentsKey])
    }

    suspend fun toggleFavorite(id: ToolId) {
        context.dataStore.edit { prefs ->
            val next = decode(prefs[favoritesKey]).toMutableSet()
            if (!next.add(id)) next.remove(id)
            prefs[favoritesKey] = next.joinToString(",") { it.name }
        }
    }

    suspend fun recordRecent(id: ToolId) {
        context.dataStore.edit { prefs ->
            val next = decode(prefs[recentsKey]).filterNot { it == id }.toMutableList()
            next.add(0, id)
            prefs[recentsKey] = next.take(8).joinToString(",") { it.name }
        }
    }

    private fun decode(raw: String?): List<ToolId> = raw.orEmpty()
        .split(',')
        .mapNotNull { value -> runCatching { ToolId.valueOf(value) }.getOrNull() }
}


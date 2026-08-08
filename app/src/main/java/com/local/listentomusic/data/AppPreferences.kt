package com.local.listentomusic.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import com.local.listentomusic.model.SortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "listen_to_music_preferences")

data class UserPreferences(
    val sortMode: SortMode = SortMode.NAME_ASC,
    val customOrder: List<String> = emptyList(),
    val lastPath: String? = null,
    val lastPositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_ONE,
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val sortMode = stringPreferencesKey("sort_mode")
        val customOrder = stringPreferencesKey("custom_order")
        val lastPath = stringPreferencesKey("last_path")
        val lastPosition = longPreferencesKey("last_position_ms")
        val speed = floatPreferencesKey("playback_speed")
        val repeatMode = longPreferencesKey("repeat_mode")
    }

    val values: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            sortMode = runCatching {
                SortMode.valueOf(prefs[Keys.sortMode] ?: SortMode.NAME_ASC.name)
            }.getOrDefault(SortMode.NAME_ASC),
            customOrder = decodeOrder(prefs[Keys.customOrder].orEmpty()),
            lastPath = prefs[Keys.lastPath],
            lastPositionMs = prefs[Keys.lastPosition] ?: 0L,
            playbackSpeed = prefs[Keys.speed] ?: 1f,
            repeatMode = (prefs[Keys.repeatMode] ?: Player.REPEAT_MODE_ONE.toLong()).toInt(),
        )
    }

    suspend fun current(): UserPreferences = values.first()

    suspend fun setSortMode(mode: SortMode) {
        context.dataStore.edit { it[Keys.sortMode] = mode.name }
    }

    suspend fun setCustomOrder(paths: List<String>) {
        context.dataStore.edit {
            it[Keys.customOrder] = paths.joinToString("\n") { path ->
                Base64.encodeToString(path.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            }
            it[Keys.sortMode] = SortMode.CUSTOM.name
        }
    }

    suspend fun savePlayback(path: String?, positionMs: Long, speed: Float, repeatMode: Int) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.lastPath) else prefs[Keys.lastPath] = path
            prefs[Keys.lastPosition] = positionMs.coerceAtLeast(0L)
            prefs[Keys.speed] = speed
            prefs[Keys.repeatMode] = repeatMode.toLong()
        }
    }

    private fun decodeOrder(encoded: String): List<String> = encoded
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull {
            runCatching {
                Base64.decode(it, Base64.NO_WRAP).toString(Charsets.UTF_8)
            }.getOrNull()
        }
        .toList()
}

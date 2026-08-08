package com.local.listentomusic.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    val libraryRowSize: LibraryRowSize = LibraryRowSize.SMALL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showThumbnails: Boolean = true,
    val showFileDetails: Boolean = true,
    val showTypeBadge: Boolean = true,
    val preloadThumbnails: Boolean = true,
    val resumePlayback: Boolean = true,
    val autoPictureInPicture: Boolean = true,
)

enum class LibraryRowSize(val label: String) { SMALL("Small"), MEDIUM("Medium"), LARGE("Large") }
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

class AppPreferences(private val context: Context) {
    private object Keys {
        val sortMode = stringPreferencesKey("sort_mode")
        val customOrder = stringPreferencesKey("custom_order")
        val lastPath = stringPreferencesKey("last_path")
        val lastPosition = longPreferencesKey("last_position_ms")
        val speed = floatPreferencesKey("playback_speed")
        val repeatMode = longPreferencesKey("repeat_mode")
        val libraryRowSize = stringPreferencesKey("library_row_size")
        val themeMode = stringPreferencesKey("theme_mode")
        val showThumbnails = booleanPreferencesKey("show_thumbnails")
        val showFileDetails = booleanPreferencesKey("show_file_details")
        val showTypeBadge = booleanPreferencesKey("show_type_badge")
        val preloadThumbnails = booleanPreferencesKey("preload_thumbnails")
        val resumePlayback = booleanPreferencesKey("resume_playback")
        val autoPictureInPicture = booleanPreferencesKey("auto_picture_in_picture")
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
            libraryRowSize = enumValueOrDefault(
                prefs[Keys.libraryRowSize],
                LibraryRowSize.SMALL,
            ),
            themeMode = enumValueOrDefault(prefs[Keys.themeMode], ThemeMode.SYSTEM),
            showThumbnails = prefs[Keys.showThumbnails] ?: true,
            showFileDetails = prefs[Keys.showFileDetails] ?: true,
            showTypeBadge = prefs[Keys.showTypeBadge] ?: true,
            preloadThumbnails = prefs[Keys.preloadThumbnails] ?: true,
            resumePlayback = prefs[Keys.resumePlayback] ?: true,
            autoPictureInPicture = prefs[Keys.autoPictureInPicture] ?: true,
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

    suspend fun setLibraryRowSize(value: LibraryRowSize) = edit { it[Keys.libraryRowSize] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.themeMode] = value.name }
    suspend fun setShowThumbnails(value: Boolean) = edit { it[Keys.showThumbnails] = value }
    suspend fun setShowFileDetails(value: Boolean) = edit { it[Keys.showFileDetails] = value }
    suspend fun setShowTypeBadge(value: Boolean) = edit { it[Keys.showTypeBadge] = value }
    suspend fun setPreloadThumbnails(value: Boolean) = edit { it[Keys.preloadThumbnails] = value }
    suspend fun setResumePlayback(value: Boolean) = edit { it[Keys.resumePlayback] = value }
    suspend fun setAutoPictureInPicture(value: Boolean) = edit { it[Keys.autoPictureInPicture] = value }

    suspend fun resetAppSettings() {
        context.dataStore.edit {
            it.remove(Keys.libraryRowSize)
            it.remove(Keys.themeMode)
            it.remove(Keys.showThumbnails)
            it.remove(Keys.showFileDetails)
            it.remove(Keys.showTypeBadge)
            it.remove(Keys.preloadThumbnails)
            it.remove(Keys.resumePlayback)
            it.remove(Keys.autoPictureInPicture)
            it[Keys.speed] = 1f
            it[Keys.repeatMode] = Player.REPEAT_MODE_ONE.toLong()
        }
    }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
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

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value ?: fallback.name) }.getOrDefault(fallback)
}

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
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "listen_to_music_preferences")

data class UserPreferences(
    val sortMode: SortMode = SortMode.NAME_ASC,
    val customOrder: List<String> = emptyList(),
    val lastPath: String? = null,
    val lastPositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_ONE,
    val libraryRowSize: LibraryRowSize = LibraryRowSize.SMALL,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val showThumbnails: Boolean = true,
    val showFileDetails: Boolean = false,
    val preloadThumbnails: Boolean = true,
    val resumePlayback: Boolean = true,
    val autoPictureInPicture: Boolean = true,
    val floatingWindowMode: FloatingWindowMode = FloatingWindowMode.FOLLOW_VIDEO,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val backgroundMode: AppBackgroundMode = AppBackgroundMode.DEFAULT,
    val customBackgroundImageUri: String? = null,
    val customBackgroundVideoUri: String? = null,
    val backgroundDim: Float = 0.55f,
    val playlists: List<LocalPlaylist> = emptyList(),
    val activePlaylistId: String? = null,
    val seekOffsetMs: Long = 5_000L,
)

enum class LibraryRowSize(val label: String) { SMALL("Small"), MEDIUM("Medium"), LARGE("Large") }
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class FloatingWindowMode { COMPACT, FOLLOW_VIDEO, MINI_WINDOW }
enum class AppLanguage { ENGLISH, TRADITIONAL_CHINESE }
enum class AppBackgroundMode { DEFAULT, CUSTOM_IMAGE, CUSTOM_VIDEO, CURRENT_VIDEO }

data class LocalPlaylist(
    val id: String,
    val name: String,
    val paths: List<String>,
)

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
        val preloadThumbnails = booleanPreferencesKey("preload_thumbnails")
        val resumePlayback = booleanPreferencesKey("resume_playback")
        val autoPictureInPicture = booleanPreferencesKey("auto_picture_in_picture")
        val floatingWindowMode = stringPreferencesKey("floating_window_mode")
        // Distinguishes a deliberate Compact choice from the pre-1.6.1 Compact default.
        val floatingWindowDefaultV2 = booleanPreferencesKey("floating_window_default_v2")
        val appLanguage = stringPreferencesKey("app_language")
        val backgroundMode = stringPreferencesKey("background_mode")
        val customBackgroundImageUri = stringPreferencesKey("custom_background_image_uri")
        val customBackgroundVideoUri = stringPreferencesKey("custom_background_video_uri")
        val backgroundDim = floatPreferencesKey("background_dim")
        val playlists = stringPreferencesKey("playlists")
        val activePlaylistId = stringPreferencesKey("active_playlist_id")
        val seekOffsetMs = longPreferencesKey("seek_offset_ms")
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
            themeMode = enumValueOrDefault(prefs[Keys.themeMode], ThemeMode.DARK),
            showThumbnails = prefs[Keys.showThumbnails] ?: true,
            showFileDetails = prefs[Keys.showFileDetails] ?: false,
            preloadThumbnails = prefs[Keys.preloadThumbnails] ?: true,
            resumePlayback = prefs[Keys.resumePlayback] ?: true,
            autoPictureInPicture = prefs[Keys.autoPictureInPicture] ?: true,
            floatingWindowMode = enumValueOrDefault(
                prefs[Keys.floatingWindowMode],
                FloatingWindowMode.FOLLOW_VIDEO,
            ).let { savedMode ->
                // Existing installs inherited Compact without choosing it. Migrate that
                // old default once; future explicit Compact selections remain respected.
                if (prefs[Keys.floatingWindowDefaultV2] != true &&
                    savedMode == FloatingWindowMode.COMPACT
                ) FloatingWindowMode.FOLLOW_VIDEO else savedMode
            },
            appLanguage = enumValueOrDefault(prefs[Keys.appLanguage], AppLanguage.ENGLISH),
            backgroundMode = enumValueOrDefault(
                prefs[Keys.backgroundMode],
                AppBackgroundMode.DEFAULT,
            ),
            customBackgroundImageUri = prefs[Keys.customBackgroundImageUri],
            customBackgroundVideoUri = prefs[Keys.customBackgroundVideoUri],
            backgroundDim = (prefs[Keys.backgroundDim] ?: 0.55f).coerceIn(0.25f, 0.85f),
            playlists = decodePlaylists(prefs[Keys.playlists].orEmpty()),
            activePlaylistId = prefs[Keys.activePlaylistId],
            seekOffsetMs = prefs[Keys.seekOffsetMs] ?: 5_000L,
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
    suspend fun setPreloadThumbnails(value: Boolean) = edit { it[Keys.preloadThumbnails] = value }
    suspend fun setResumePlayback(value: Boolean) = edit { it[Keys.resumePlayback] = value }
    suspend fun setAutoPictureInPicture(value: Boolean) = edit { it[Keys.autoPictureInPicture] = value }
    suspend fun setFloatingWindowMode(value: FloatingWindowMode) = edit {
        it[Keys.floatingWindowMode] = value.name
        it[Keys.floatingWindowDefaultV2] = true
    }
    suspend fun setAppLanguage(value: AppLanguage) = edit { it[Keys.appLanguage] = value.name }
    suspend fun setBackgroundMode(value: AppBackgroundMode) =
        edit { it[Keys.backgroundMode] = value.name }
    suspend fun setCustomBackgroundImageUri(value: String?) = edit { prefs ->
        if (value == null) prefs.remove(Keys.customBackgroundImageUri)
        else prefs[Keys.customBackgroundImageUri] = value
    }
    suspend fun setCustomBackgroundVideoUri(value: String?) = edit { prefs ->
        if (value == null) prefs.remove(Keys.customBackgroundVideoUri)
        else prefs[Keys.customBackgroundVideoUri] = value
    }
    suspend fun setBackgroundDim(value: Float) =
        edit { it[Keys.backgroundDim] = value.coerceIn(0.25f, 0.85f) }
    suspend fun setSeekOffsetMs(value: Long) = edit { it[Keys.seekOffsetMs] = value }

    suspend fun setActivePlaylist(id: String?) = edit { prefs ->
        if (id == null) prefs.remove(Keys.activePlaylistId) else prefs[Keys.activePlaylistId] = id
    }

    suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        updatePlaylists { current -> current + LocalPlaylist(id, name.trim(), emptyList()) }
        return id
    }

    suspend fun renamePlaylist(id: String, name: String) = updatePlaylists { playlists ->
        playlists.map { if (it.id == id) it.copy(name = name.trim()) else it }
    }

    suspend fun deletePlaylist(id: String) {
        context.dataStore.edit { prefs ->
            val updated = decodePlaylists(prefs[Keys.playlists].orEmpty()).filterNot { it.id == id }
            prefs[Keys.playlists] = encodePlaylists(updated)
            if (prefs[Keys.activePlaylistId] == id) prefs.remove(Keys.activePlaylistId)
        }
    }

    suspend fun addToPlaylist(id: String, path: String) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id != id || path in playlist.paths) playlist
            else playlist.copy(paths = playlist.paths + path)
        }
    }

    suspend fun addAllToPlaylist(id: String, paths: List<String>) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id != id || paths.isEmpty()) playlist
            else playlist.copy(paths = (playlist.paths + paths).distinct())
        }
    }

    suspend fun removeFromPlaylist(id: String, path: String) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id == id) playlist.copy(paths = playlist.paths.filterNot { it == path }) else playlist
        }
    }

    suspend fun movePlaylistItem(id: String, fromIndex: Int, toIndex: Int) = updatePlaylists { playlists ->
        playlists.map { playlist ->
            if (playlist.id != id || fromIndex !in playlist.paths.indices || toIndex !in playlist.paths.indices) {
                playlist
            } else {
                playlist.copy(paths = playlist.paths.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                })
            }
        }
    }

    suspend fun resetAppSettings() {
        context.dataStore.edit {
            it.remove(Keys.libraryRowSize)
            it.remove(Keys.themeMode)
            it.remove(Keys.showThumbnails)
            it.remove(Keys.showFileDetails)
            it.remove(Keys.preloadThumbnails)
            it.remove(Keys.resumePlayback)
            it.remove(Keys.autoPictureInPicture)
            it.remove(Keys.floatingWindowMode)
            it.remove(Keys.floatingWindowDefaultV2)
            it.remove(Keys.appLanguage)
            it.remove(Keys.backgroundMode)
            it.remove(Keys.customBackgroundImageUri)
            it.remove(Keys.customBackgroundVideoUri)
            it.remove(Keys.backgroundDim)
            it.remove(Keys.seekOffsetMs)
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

    private suspend fun updatePlaylists(transform: (List<LocalPlaylist>) -> List<LocalPlaylist>) {
        context.dataStore.edit { prefs ->
            val current = decodePlaylists(prefs[Keys.playlists].orEmpty())
            prefs[Keys.playlists] = encodePlaylists(transform(current))
        }
    }

    private fun encodePlaylists(playlists: List<LocalPlaylist>): String = playlists.joinToString("\n") { playlist ->
        listOf(
            encode(playlist.id),
            encode(playlist.name),
            playlist.paths.joinToString(",", transform = ::encode),
        ).joinToString("|")
    }

    private fun decodePlaylists(encoded: String): List<LocalPlaylist> = encoded.lineSequence().mapNotNull { line ->
        val parts = line.split('|', limit = 3)
        if (parts.size != 3) return@mapNotNull null
        val id = decode(parts[0]) ?: return@mapNotNull null
        val name = decode(parts[1])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val paths = if (parts[2].isBlank()) emptyList() else parts[2].split(',').mapNotNull(::decode)
        LocalPlaylist(id, name, paths.distinct())
    }.toList()

    private fun encode(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)

    private fun decode(value: String): String? = runCatching {
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE).toString(Charsets.UTF_8)
    }.getOrNull()

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value ?: fallback.name) }.getOrDefault(fallback)
}

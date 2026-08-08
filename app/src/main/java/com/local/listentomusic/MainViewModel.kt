package com.local.listentomusic

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.data.ScanResult
import com.local.listentomusic.data.ThumbnailRepository
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class LibraryStatus { NEEDS_PERMISSION, SCANNING, READY, FOLDER_MISSING, CANNOT_READ }

data class LibraryUiState(
    val status: LibraryStatus = LibraryStatus.SCANNING,
    val files: List<MediaFile> = emptyList(),
    val sortMode: SortMode = SortMode.NAME_ASC,
    val query: String = "",
    val targetPath: String = MediaScanner.targetFolder().absolutePath,
)

data class PlaybackUiState(
    val connected: Boolean = false,
    val currentPath: String? = null,
    val title: String = "Nothing playing",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_ONE,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val videoAspectRatio: Float = 16f / 9f,
    val errorMessage: String? = null,
) {
    val hasMedia: Boolean get() = currentPath != null
    val isVideo: Boolean
        get() = currentPath?.substringAfterLast('.', "")?.lowercase(Locale.ROOT) in VIDEO_EXTENSIONS

    private companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi")
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val thumbnailRepository = ThumbnailRepository(application)
    private var userPreferences = UserPreferences()
    private var scannedFiles: List<MediaFile> = emptyList()
    private var orderedFiles: List<MediaFile> = emptyList()

    private val _library = MutableStateFlow(LibraryUiState())
    val library: StateFlow<LibraryUiState> = _library.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackUiState())
    val playback: StateFlow<PlaybackUiState> = _playback.asStateFlow()

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _settings = MutableStateFlow(UserPreferences())
    val settings: StateFlow<UserPreferences> = _settings.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var tickerJob: Job? = null
    private var thumbnailPreloadJob: Job? = null
    private var pendingPlay: MediaFile? = null
    private var lastPlaybackError: String? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publishPlayback(player)

        override fun onPlayerError(error: PlaybackException) {
            lastPlaybackError = "This file could not be decoded on this device. Trying the next item."
            publishPlayback(_controller.value ?: return)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) lastPlaybackError = null
        }
    }

    init {
        viewModelScope.launch {
            preferences.values.collect {
                userPreferences = it
                _settings.value = it
                applySortingAndFilter()
            }
        }
        connectController()
        rescan()
    }

    fun rescan() {
        if (!hasStorageAccess()) {
            _library.value = _library.value.copy(status = LibraryStatus.NEEDS_PERMISSION)
            return
        }
        viewModelScope.launch {
            _library.value = _library.value.copy(status = LibraryStatus.SCANNING)
            when (val result = MediaScanner.scan()) {
                is ScanResult.Success -> {
                    scannedFiles = result.files
                    applySortingAndFilter()
                    _library.value = _library.value.copy(status = LibraryStatus.READY)
                    thumbnailPreloadJob?.cancel()
                    thumbnailPreloadJob = if (userPreferences.preloadThumbnails) viewModelScope.launch {
                        // A recursive Download scan can find hundreds of files. Warm the first
                        // library page eagerly; every other preview still loads on demand.
                        thumbnailRepository.preload(orderedFiles.take(MAX_PRELOAD_ITEMS))
                    } else null
                }
                is ScanResult.FolderMissing -> {
                    scannedFiles = emptyList()
                    applySortingAndFilter()
                    _library.value = _library.value.copy(
                        status = LibraryStatus.FOLDER_MISSING,
                        targetPath = result.path,
                    )
                }
                is ScanResult.PermissionMissing -> {
                    scannedFiles = emptyList()
                    applySortingAndFilter()
                    _library.value = _library.value.copy(
                        status = LibraryStatus.CANNOT_READ,
                        targetPath = result.path,
                    )
                }
            }
        }
    }

    fun setQuery(query: String) {
        _library.value = _library.value.copy(query = query)
        applySortingAndFilter()
    }

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch {
            if (mode == SortMode.CUSTOM && userPreferences.customOrder.isEmpty()) {
                preferences.setCustomOrder(orderedFiles.map { it.path })
            } else {
                preferences.setSortMode(mode)
            }
        }
    }

    fun moveCustomItem(fromIndex: Int, toIndex: Int) {
        if (_library.value.query.isNotBlank() || userPreferences.sortMode != SortMode.CUSTOM) return
        if (fromIndex !in orderedFiles.indices || toIndex !in orderedFiles.indices || fromIndex == toIndex) return
        val moved = orderedFiles.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        orderedFiles = moved
        _library.value = _library.value.copy(files = moved)
        viewModelScope.launch { preferences.setCustomOrder(moved.map { it.path }) }
    }

    fun play(file: MediaFile) {
        thumbnailPreloadJob?.cancel()
        val player = _controller.value
        if (player == null) {
            // A tap can arrive while the MediaSession connection is still starting.
            // Keep it instead of silently dropping the user's request.
            pendingPlay = file
            return
        }
        playNow(player, file)
    }

    private fun playNow(player: Player, file: MediaFile) {
        lastPlaybackError = null
        val queue = orderedFiles.ifEmpty { listOf(file) }
        val index = queue.indexOfFirst { it.path == file.path }.coerceAtLeast(0)
        val resumeAt = if (userPreferences.resumePlayback && file.path == userPreferences.lastPath) {
            userPreferences.lastPositionMs
        } else 0L
        player.setMediaItems(queue.map(MediaFile::toMediaItem), index, resumeAt)
        player.playWhenReady = true
        player.prepare()
    }

    suspend fun loadThumbnail(file: MediaFile): Bitmap? = thumbnailRepository.load(file)

    fun togglePlayPause() {
        _controller.value?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) {
        _controller.value?.seekTo(positionMs.coerceAtLeast(0L))
        _controller.value?.let(::publishPlayback)
    }

    fun next() {
        _controller.value?.seekToNextMediaItem()
    }

    fun previous() {
        _controller.value?.let {
            if (it.currentPosition > 4_000) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun setSpeed(speed: Float) {
        _controller.value?.playbackParameters = PlaybackParameters(speed)
        _controller.value?.let(::publishPlayback)
    }

    fun setRepeatMode(mode: Int) {
        _controller.value?.let {
            it.repeatMode = mode
            publishPlayback(it)
        }
    }

    fun setLibraryRowSize(value: LibraryRowSize) = updatePreference { preferences.setLibraryRowSize(value) }
    fun setThemeMode(value: ThemeMode) = updatePreference { preferences.setThemeMode(value) }
    fun setShowThumbnails(value: Boolean) = updatePreference { preferences.setShowThumbnails(value) }
    fun setShowFileDetails(value: Boolean) = updatePreference { preferences.setShowFileDetails(value) }
    fun setShowTypeBadge(value: Boolean) = updatePreference { preferences.setShowTypeBadge(value) }
    fun setResumePlayback(value: Boolean) = updatePreference { preferences.setResumePlayback(value) }
    fun setAutoPictureInPicture(value: Boolean) = updatePreference { preferences.setAutoPictureInPicture(value) }

    fun setPreloadThumbnails(value: Boolean) {
        updatePreference { preferences.setPreloadThumbnails(value) }
        if (value) {
            thumbnailPreloadJob?.cancel()
            thumbnailPreloadJob = viewModelScope.launch {
                thumbnailRepository.preload(orderedFiles.take(MAX_PRELOAD_ITEMS))
            }
        } else {
            thumbnailPreloadJob?.cancel()
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch { thumbnailRepository.clear() }
    }

    fun resetAppSettings() {
        viewModelScope.launch {
            preferences.resetAppSettings()
            _controller.value?.let {
                it.playbackParameters = PlaybackParameters(1f)
                it.repeatMode = Player.REPEAT_MODE_ONE
                publishPlayback(it)
            }
        }
    }

    fun cycleRepeatMode() {
        _controller.value?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            publishPlayback(it)
        }
    }

    private fun connectController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { mediaController ->
                _controller.value = mediaController
                mediaController.addListener(playerListener)
                publishPlayback(mediaController)
                startTicker()
                pendingPlay?.let { requested ->
                    pendingPlay = null
                    playNow(mediaController, requested)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _controller.value?.let(::publishPlayback)
                delay(500)
            }
        }
    }

    private fun publishPlayback(player: Player) {
        val duration = player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: 0L
        val videoSize = player.videoSize
        val videoAspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
            videoSize.width.toFloat() / videoSize.height.toFloat()
        } else {
            16f / 9f
        }
        _playback.value = PlaybackUiState(
            connected = true,
            currentPath = player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() },
            title = player.mediaMetadata.title?.toString()
                ?: player.currentMediaItem?.mediaId?.substringAfterLast('/')
                ?: "Nothing playing",
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = player.playbackParameters.speed,
            repeatMode = player.repeatMode,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem() || player.currentPosition > 0,
            videoAspectRatio = videoAspectRatio,
            errorMessage = lastPlaybackError,
        )
    }

    private fun applySortingAndFilter() {
        orderedFiles = when (userPreferences.sortMode) {
            SortMode.CUSTOM -> {
                val rank = userPreferences.customOrder.withIndex().associate { it.value to it.index }
                scannedFiles.sortedWith(
                    compareBy<MediaFile> { rank[it.path] ?: Int.MAX_VALUE }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )
            }
            SortMode.NAME_ASC -> scannedFiles.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortMode.NAME_DESC -> scannedFiles.sortedWith(compareByDescending<MediaFile> { it.name.lowercase() })
        }
        val query = _library.value.query.trim()
        val shown = if (query.isBlank()) orderedFiles else orderedFiles.filter {
            it.name.contains(query, ignoreCase = true)
        }
        _library.value = _library.value.copy(
            files = shown,
            sortMode = userPreferences.sortMode,
        )
    }

    private fun hasStorageAccess(): Boolean {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updatePreference(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        thumbnailPreloadJob?.cancel()
        _controller.value?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        _controller.value = null
        super.onCleared()
    }

    private companion object {
        const val MAX_PRELOAD_ITEMS = 120
    }
}

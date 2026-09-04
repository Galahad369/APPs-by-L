package com.local.listentomusic

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.C
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
import com.local.listentomusic.data.WaveformRepository
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.LocalLyrics
import com.local.listentomusic.model.SortMode
import com.local.listentomusic.model.loadLocalLyrics
import com.local.listentomusic.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

private const val MAX_PRELOAD_ITEMS = 300

internal enum class CycleMode { OFF, ONE, ALL, RANDOM }

internal fun resolveCycleMode(repeatMode: Int, random: Boolean): CycleMode = when {
    random -> CycleMode.RANDOM
    repeatMode == Player.REPEAT_MODE_ONE -> CycleMode.ONE
    repeatMode == Player.REPEAT_MODE_ALL -> CycleMode.ALL
    else -> CycleMode.OFF
}

internal fun nextCycleMode(current: CycleMode): CycleMode = when (current) {
    CycleMode.OFF -> CycleMode.ONE
    CycleMode.ONE -> CycleMode.ALL
    CycleMode.ALL -> CycleMode.RANDOM
    CycleMode.RANDOM -> CycleMode.OFF
}

// Sleep timer: minute targets. -1L = "stop at the end of the current track".
val sleepTimerOptions = listOf(5L, 10L, 15L, 30L, 60L, -1L)

data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    val endOfTrack: Boolean = false,
)

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
    val shuffleEnabled: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val videoAspectRatio: Float = 16f / 9f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoFrameRendered: Boolean = false,
    val errorMessage: String? = null,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
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
    private val waveformRepository = WaveformRepository(application)
    val thumbnailStats = thumbnailRepository.stats
    val waveformDiagnostics = waveformRepository.diagnostics
    private var userPreferences = UserPreferences()
    private var scannedFiles: List<MediaFile> = emptyList()
    private var orderedFiles: List<MediaFile> = emptyList()

    private val _library = MutableStateFlow(LibraryUiState())
    val library: StateFlow<LibraryUiState> = _library.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaFile>>(emptyList())
    val queue: StateFlow<List<MediaFile>> = _queue.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackUiState())
    val playback: StateFlow<PlaybackUiState> = _playback.asStateFlow()

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()
    private var sleepTimerJob: Job? = null
    private var sleepTimerEndsAt: Long = 0L
    // Single-job wakeup. The exact remainingMs is computed at fire time,
    // so we never have to chase a moving target. End-of-track uses a flag, not a clock.

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _settings = MutableStateFlow(UserPreferences())
    val settings: StateFlow<UserPreferences> = _settings.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var tickerJob: Job? = null
    private var thumbnailWarmupJob: Job? = null
    private var thumbnailAheadJob: Job? = null
    private var scanJob: Job? = null
    private var durationProbeJob: Job? = null
    private var durationProbePath: String? = null
    private val probedDurations = mutableMapOf<String, Long>()
    private var pendingPlay: MediaFile? = null
    private var lastPlaybackError: String? = null
    private var videoFrameRendered = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publishPlayback(player)

        override fun onPlayerError(error: PlaybackException) {
            lastPlaybackError = "This file could not be decoded on this device. Trying the next item."
            publishPlayback(_controller.value ?: return)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) lastPlaybackError = null
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            videoFrameRendered = false
            mediaItem?.mediaId?.let { path ->
                val file = scannedFiles.firstOrNull { it.path == path }
                if (file?.kind == com.local.listentomusic.model.MediaKind.AUDIO) {
                    viewModelScope.launch { loadWaveform(path) }
                }
            }
            // Sleep timer in "end of track" mode fires when the next item lands.
            val timer = _sleepTimer.value
            if (timer.active && timer.endOfTrack) cancelSleepTimer()
        }

        override fun onRenderedFirstFrame() {
            videoFrameRendered = true
            _controller.value?.let(::publishPlayback)
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
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _library.value = _library.value.copy(status = LibraryStatus.SCANNING)
            when (val result = MediaScanner.scan()) {
                is ScanResult.Success -> {
                    scannedFiles = result.files
                    applySortingAndFilter()
                    _library.value = _library.value.copy(status = LibraryStatus.READY)
                    thumbnailWarmupJob?.cancel()
                    thumbnailAheadJob?.cancel()
                    thumbnailWarmupJob = if (userPreferences.preloadThumbnails) viewModelScope.launch {
                        // Warm a generous initial window while decoding remains bounded.
                        // Visible rows still bypass the preload throttle.
                        warmThumbnailsInStages(orderedFiles.take(MAX_PRELOAD_ITEMS))
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
        if (_library.value.query.isNotBlank()) return
        if (fromIndex !in orderedFiles.indices || toIndex !in orderedFiles.indices || fromIndex == toIndex) return
        userPreferences.activePlaylistId?.let { playlistId ->
            viewModelScope.launch { preferences.movePlaylistItem(playlistId, fromIndex, toIndex) }
            return
        }
        if (userPreferences.sortMode != SortMode.CUSTOM) return
        val moved = orderedFiles.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        orderedFiles = moved
        _library.value = _library.value.copy(files = moved)
        viewModelScope.launch { preferences.setCustomOrder(moved.map { it.path }) }
    }

    fun play(file: MediaFile) {
        thumbnailWarmupJob?.cancel()
        thumbnailAheadJob?.cancel()
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
    suspend fun loadWaveform(path: String): FloatArray? {
        val file = scannedFiles.firstOrNull { it.path == path }
        return waveformRepository.load(path, file?.sizeBytes ?: 0L, file?.modifiedMs ?: 0L)
    }

    fun playQueueItem(file: MediaFile) {
        val player = _controller.value
        val index = player?.let { current ->
            (0 until current.mediaItemCount).firstOrNull { current.getMediaItemAt(it).mediaId == file.path }
        }
        if (player != null && index != null) {
            player.seekToDefaultPosition(index)
            player.play()
        } else {
            play(file)
        }
    }

    suspend fun loadCurrentArtwork(path: String?): Bitmap? {
        val file = scannedFiles.firstOrNull { it.path == path } ?: return null
        return thumbnailRepository.load(file)
    }

    suspend fun loadLyrics(path: String?): LocalLyrics? = withContext(Dispatchers.IO) {
        loadLocalLyrics(path)
    }

    fun togglePlayPause() {
        _controller.value?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekBy(deltaMs: Long) {
        _controller.value?.let { seekTo(it.currentPosition + deltaMs) }
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

    fun setPlaybackCycle(mode: Int, random: Boolean) {
        _controller.value?.let {
            it.shuffleModeEnabled = random
            it.repeatMode = if (random) Player.REPEAT_MODE_ALL else mode
            publishPlayback(it)
        }
    }

    fun setLibraryRowSize(value: LibraryRowSize) = updatePreference { preferences.setLibraryRowSize(value) }
    fun setThemeMode(value: ThemeMode) = updatePreference { preferences.setThemeMode(value) }
    fun setShowThumbnails(value: Boolean) = updatePreference { preferences.setShowThumbnails(value) }
    fun setShowFileDetails(value: Boolean) = updatePreference { preferences.setShowFileDetails(value) }
    fun setResumePlayback(value: Boolean) = updatePreference { preferences.setResumePlayback(value) }
    fun setAutoPictureInPicture(value: Boolean) = updatePreference { preferences.setAutoPictureInPicture(value) }
    fun setFloatingWindowMode(value: FloatingWindowMode) =
        updatePreference { preferences.setFloatingWindowMode(value) }
    fun setAppLanguage(value: AppLanguage) = updatePreference { preferences.setAppLanguage(value) }
    fun setBackgroundMode(value: AppBackgroundMode) =
        updatePreference { preferences.setBackgroundMode(value) }
    fun setCustomBackgroundImage(uri: String?) = updatePreference {
        preferences.setCustomBackgroundImageUri(uri)
        if (uri != null) preferences.setBackgroundMode(AppBackgroundMode.CUSTOM_IMAGE)
    }
    fun setCustomBackgroundVideo(uri: String?) = updatePreference {
        preferences.setCustomBackgroundVideoUri(uri)
        if (uri != null) preferences.setBackgroundMode(AppBackgroundMode.CUSTOM_VIDEO)
    }
    fun setBackgroundDim(value: Float) = updatePreference { preferences.setBackgroundDim(value) }
    fun setSeekOffset(value: Long) = updatePreference { preferences.setSeekOffsetMs(value) }
    fun setAppFont(value: com.local.listentomusic.data.AppFont) =
        updatePreference { preferences.setAppFont(value) }
    fun setDeveloperMode(value: Boolean) = updatePreference { preferences.setDeveloperMode(value) }
    fun setEditableQueue(value: Boolean) = updatePreference { preferences.setEditableQueue(value) }
    fun setSilianRail(value: Boolean) = updatePreference { preferences.setSilianRail(value) }
    fun setActivePlaylist(id: String?) = updatePreference { preferences.setActivePlaylist(id) }

    fun importM3u(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val raw = runCatching { resolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() } }.getOrNull() ?: return@launch
            val byPath = scannedFiles.associateBy { File(it.path).canonicalPath }
            val byName = scannedFiles.groupBy { File(it.path).name.lowercase(Locale.ROOT) }
            val paths = raw.asSequence().map(String::trim).filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val decoded = Uri.decode(line.removePrefix("file://"))
                    runCatching { File(decoded).canonicalPath }.getOrNull()?.let(byPath::get)?.path
                        ?: byName[File(decoded).name.lowercase(Locale.ROOT)]?.singleOrNull()?.path
                }.distinct().toList()
            if (paths.isEmpty()) return@launch
            val displayName = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0).substringBeforeLast('.') else null
                }
            }.getOrNull().orEmpty().ifBlank { "Imported playlist" }
            val id = preferences.createPlaylistWithPaths(displayName.take(60), paths)
            preferences.setActivePlaylist(id)
        }
    }

    fun exportActiveM3u(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = buildString {
                appendLine("#EXTM3U")
                orderedFiles.forEach { appendLine(it.path) }
            }
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?.bufferedWriter()?.use { it.write(lines) }
            }
        }
    }
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = preferences.createPlaylist(name)
            preferences.setActivePlaylist(id)
        }
    }
    fun addAllToPlaylist(id: String, paths: List<String>) {
        if (paths.isEmpty()) return
        updatePreference { preferences.addAllToPlaylist(id, paths) }
    }
    fun createPlaylistAndSeed(name: String, seedPath: String?, keyword: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = preferences.createPlaylist(name)
            val toAdd = buildList {
                seedPath?.let { add(it) }
                keyword?.takeIf { it.isNotBlank() }?.let { kw ->
                    val upper = kw.uppercase(Locale.ROOT)
                    orderedFiles.filter { it.path.uppercase(Locale.ROOT).contains(upper) || it.name.uppercase(Locale.ROOT).contains(upper) }
                        .forEach { add(it.path) }
                }
            }.distinct()
            if (toAdd.isNotEmpty()) preferences.addAllToPlaylist(id, toAdd)
            preferences.setActivePlaylist(id)
        }
    }
    fun playPlaylist(id: String) {
        val player = _controller.value ?: return
        val playlist = userPreferences.playlists.firstOrNull { it.id == id } ?: return
        val byPath = orderedFiles.associateBy { it.path }
        val queue = playlist.paths.mapNotNull(byPath::get)
        if (queue.isEmpty()) return
        lastPlaybackError = null
        player.setMediaItems(queue.map(MediaFile::toMediaItem), 0, 0L)
        player.playWhenReady = true
        player.prepare()
    }
    fun renamePlaylist(id: String, name: String) {
        if (name.isBlank()) return
        updatePreference { preferences.renamePlaylist(id, name) }
    }
    fun deletePlaylist(id: String) = updatePreference { preferences.deletePlaylist(id) }
    fun addToPlaylist(id: String, path: String) = updatePreference { preferences.addToPlaylist(id, path) }
    fun removeFromActivePlaylist(path: String) {
        val id = userPreferences.activePlaylistId ?: return
        updatePreference { preferences.removeFromPlaylist(id, path) }
    }

    fun setPreloadThumbnails(value: Boolean) {
        updatePreference { preferences.setPreloadThumbnails(value) }
        if (value) {
            thumbnailWarmupJob?.cancel()
            thumbnailWarmupJob = viewModelScope.launch {
                warmThumbnailsInStages(orderedFiles.take(MAX_PRELOAD_ITEMS))
            }
        } else {
            thumbnailWarmupJob?.cancel()
            thumbnailAheadJob?.cancel()
        }
    }

    // Pre-warm later windows without cancelling the initial 300-item warmup.
    fun preloadThumbnailsStartingAt(startIndex: Int, count: Int) {
        if (!userPreferences.preloadThumbnails) return
        val window = orderedFiles.drop(startIndex).take(count)
        if (window.isEmpty()) return
        thumbnailAheadJob?.cancel()
        thumbnailAheadJob = viewModelScope.launch {
            thumbnailRepository.preload(window)
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            thumbnailRepository.clear()
            waveformRepository.clear()
        }
    }

    fun resetAppSettings() {
        viewModelScope.launch {
            preferences.resetAppSettings()
            _controller.value?.let {
                it.playbackParameters = PlaybackParameters(1f)
                it.repeatMode = Player.REPEAT_MODE_ONE
                it.shuffleModeEnabled = false
                publishPlayback(it)
            }
        }
    }

    fun cycleRepeatMode() {
        _controller.value?.let { player ->
            val current = resolveCycleMode(player.repeatMode, player.shuffleModeEnabled)
            val next = nextCycleMode(current)
            applyCycleMode(player, next)
            publishPlayback(player)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = _controller.value ?: return
        if (fromIndex !in _queue.value.indices || toIndex !in _queue.value.indices) return
        player.moveMediaItem(fromIndex, toIndex)
        _queue.value = _queue.value.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    fun removeQueueItem(index: Int) {
        val player = _controller.value ?: return
        if (index !in _queue.value.indices || player.mediaItemCount <= 1) return
        player.removeMediaItem(index)
        _queue.value = _queue.value.toMutableList().apply { removeAt(index) }
    }

    private suspend fun warmThumbnailsInStages(files: List<MediaFile>) {
        // Make the first viewport ready quickly, then yield between disk-heavy chunks.
        thumbnailRepository.preload(files.take(24))
        files.drop(24).chunked(24).forEach { chunk ->
            delay(45)
            thumbnailRepository.preload(chunk)
        }
    }

    // One control owns the full cycle: Off → One → All → Random → Off.
    private fun applyCycleMode(player: Player, mode: CycleMode) {
        when (mode) {
            CycleMode.OFF -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            CycleMode.ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            CycleMode.ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            CycleMode.RANDOM -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }
        }
    }

    // -1L minute target = pause at the end of the current track.
    fun setSleepTimer(minutes: Long) {
        sleepTimerJob?.cancel()
        if (minutes == 0L) {
            cancelSleepTimer()
            return
        }
        if (minutes == -1L) {
            _sleepTimer.value = SleepTimerState(active = true, endOfTrack = true)
            return
        }
        sleepTimerEndsAt = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimer.value = SleepTimerState(active = true, remainingMs = minutes * 60_000L)
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = (sleepTimerEndsAt - System.currentTimeMillis()).coerceAtLeast(0L)
                _sleepTimer.value = _sleepTimer.value.copy(remainingMs = remaining)
                if (remaining == 0L) break
                delay(1_000L)
            }
            _controller.value?.pause()
            _sleepTimer.value = SleepTimerState()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimer.value = SleepTimerState()
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
            // Four updates per second keep the timeline visually smooth without
            // driving Compose at video-frame rate.
            while (true) {
                _controller.value?.let(::publishPlayback)
                delay(250L)
            }
        }
    }

    private fun publishPlayback(player: Player) {
        val path = player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() }
        val timelineDuration = if (
            !player.currentTimeline.isEmpty &&
            player.currentMediaItemIndex in 0 until player.currentTimeline.windowCount
        ) {
            player.currentTimeline.getWindow(
                player.currentMediaItemIndex,
                androidx.media3.common.Timeline.Window(),
            ).durationMs
        } else {
            C.TIME_UNSET
        }
        val duration = resolveDurationMs(
            player.duration,
            player.contentDuration,
            timelineDuration,
            path?.let(probedDurations::get) ?: C.TIME_UNSET,
        )
        if (duration == 0L && path != null) probeDuration(path)
        val videoSize = player.videoSize
        val videoAspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
            videoSize.width.toFloat() / videoSize.height.toFloat()
        } else {
            16f / 9f
        }
        // Match the 250ms UI cadence so identical events do not cause extra emissions.
        val quantizedPosition = (player.currentPosition / 250L) * 250L
        val next = PlaybackUiState(
            connected = true,
            currentPath = path,
            title = player.mediaMetadata.title?.toString()
                ?: player.currentMediaItem?.mediaId?.substringAfterLast('/')
                ?: "Nothing playing",
            isPlaying = player.isPlaying,
            positionMs = quantizedPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = player.playbackParameters.speed,
            repeatMode = player.repeatMode,
            shuffleEnabled = player.shuffleModeEnabled,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem() || player.currentPosition > 0,
            videoAspectRatio = videoAspectRatio,
            videoWidth = videoSize.width,
            videoHeight = videoSize.height,
            videoFrameRendered = videoFrameRendered,
            errorMessage = lastPlaybackError,
        )
        // Skip identical emits. Every StateFlow update triggers a
        // recomposition storm across every screen that reads `playback`.
        if (next == _playback.value) return
        _playback.value = next
    }

    private fun applySortingAndFilter() {
        val sortedLibrary = when (userPreferences.sortMode) {
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
        orderedFiles = userPreferences.activePlaylistId
            ?.let { id -> userPreferences.playlists.firstOrNull { it.id == id } }
            ?.let { playlist ->
                val byPath = scannedFiles.associateBy(MediaFile::path)
                playlist.paths.mapNotNull(byPath::get)
            }
            ?: sortedLibrary
        _queue.value = orderedFiles
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

    private fun probeDuration(path: String) {
        if (durationProbePath == path || probedDurations.containsKey(path)) return
        durationProbeJob?.cancel()
        durationProbePath = path
        durationProbeJob = viewModelScope.launch {
            val duration = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                        retriever.setDataSource(path)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }
            }
            if (duration != null && duration > 0L) probedDurations[path] = duration
            durationProbePath = null
            _controller.value?.let(::publishPlayback)
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        thumbnailWarmupJob?.cancel()
        thumbnailAheadJob?.cancel()
        scanJob?.cancel()
        durationProbeJob?.cancel()
        sleepTimerJob?.cancel()
        _controller.value?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        _controller.value = null
        super.onCleared()
    }

}

internal fun resolveDurationMs(vararg candidates: Long): Long =
    candidates.firstOrNull { it > 0L && it != C.TIME_UNSET } ?: 0L

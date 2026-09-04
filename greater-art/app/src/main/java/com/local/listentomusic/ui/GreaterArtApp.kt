package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.Settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.components.MiniPlayer
import com.local.listentomusic.ui.components.AppBackground
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.model.LocalLyrics
import com.local.listentomusic.ui.theme.GreaterArtTheme

private enum class Screen { LIBRARY, NOW_PLAYING, SETTINGS }

@Composable
fun GreaterArtApp(
    viewModel: MainViewModel,
    openPlayerRequest: Int,
    onOpenPlayerRequestConsumed: (Int) -> Unit,
    isPictureInPicture: Boolean,
    onPlayerScreenChanged: (Boolean) -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onGrantStorageAccess: () -> Unit,
) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val controller by viewModel.controller.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val previousUri = settings.customBackgroundImageUri
        val grantPersisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        viewModel.setCustomBackgroundImage(uri.toString())
        if (grantPersisted && previousUri != null && previousUri != uri.toString()) runCatching {
            context.contentResolver.releasePersistableUriPermission(
                previousUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    val videoBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val previousUri = settings.customBackgroundVideoUri
        val grantPersisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        viewModel.setCustomBackgroundVideo(uri.toString())
        if (grantPersisted && previousUri != null && previousUri != uri.toString()) runCatching {
            context.contentResolver.releasePersistableUriPermission(
                previousUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    val artwork by produceState<Bitmap?>(initialValue = null, key1 = playback.currentPath) {
        value = viewModel.loadCurrentArtwork(playback.currentPath)
    }
    val m3uImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importM3u)
    }
    val m3uExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri -> uri?.let(viewModel::exportActiveM3u) }
    val lyrics by produceState<LocalLyrics?>(initialValue = null, key1 = playback.currentPath) {
        value = viewModel.loadLyrics(playback.currentPath)
    }
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }

    LaunchedEffect(openPlayerRequest, playback.hasMedia) {
        if (openPlayerRequest > 0 && playback.hasMedia) {
            screen = Screen.NOW_PLAYING
            onOpenPlayerRequestConsumed(openPlayerRequest)
        }
    }

    LaunchedEffect(screen) {
        onPlayerScreenChanged(screen == Screen.NOW_PLAYING)
    }

    BackHandler(enabled = screen != Screen.LIBRARY) { screen = Screen.LIBRARY }

    val appName = if (settings.silianRail) "PIERCE&PIERCE" else "Greater Art"
    GreaterArtTheme(
        themeMode = settings.themeMode,
        appFont = settings.appFont,
        silianRail = settings.silianRail,
    ) {
        val lightPalette = MaterialTheme.colorScheme.background.luminance() > 0.5f
        Box(modifier = Modifier.fillMaxSize()) {
            if (screen == Screen.NOW_PLAYING && playback.isVideo) {
                // Full-screen video completely occludes the wallpaper. Avoid spending
                // a decoder and animation frames on pixels the user cannot see.
                Box(Modifier.fillMaxSize().background(Color.Black))
            } else {
                AppBackground(preferences = settings, playback = playback, controller = controller)
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                // A light palette needs an opaque-enough base over black/custom media.
                // Dark mode keeps the liquid-metal wallpaper fully visible.
                color = if (lightPalette) {
                    MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                } else Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val spring = spring<IntOffset>(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy,
                    )
                    // A restrained horizontal push keeps screen changes spatially clear.
                    if (targetState > initialState) {
                        slideInHorizontally(spring, initialOffsetX = { it }) + fadeIn() togetherWith
                            slideOutHorizontally(spring, targetOffsetX = { -it / 3 }) + fadeOut()
                    } else {
                        slideInHorizontally(spring, initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                            slideOutHorizontally(spring, targetOffsetX = { it }) + fadeOut()
                    }
                },
                label = "screen",
            ) { scr ->
                when (scr) {
                Screen.LIBRARY -> Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        if (playback.hasMedia) {
                            MiniPlayer(
                                playback = playback,
                                artwork = artwork,
                                language = settings.appLanguage,
                                onOpen = { screen = Screen.NOW_PLAYING },
                                onTogglePlay = viewModel::togglePlayPause,
                                onPrevious = viewModel::previous,
                                onNext = viewModel::next,
                            )
                        }
                    },
                ) { padding ->
                    LibraryScreen(
                        appName = appName,
                        state = library,
                        preferences = settings,
                        currentPath = playback.currentPath,
                        contentPadding = padding,
                        onGrantStorageAccess = onGrantStorageAccess,
                        onRefresh = viewModel::rescan,
                        onQueryChange = viewModel::setQuery,
                        onSortChange = viewModel::setSortMode,
                        onMoveItem = viewModel::moveCustomItem,
                        onSelectPlaylist = viewModel::setActivePlaylist,
                        onCreatePlaylist = viewModel::createPlaylist,
                        onCreatePlaylistAndSeed = viewModel::createPlaylistAndSeed,
                        onPlayPlaylist = viewModel::playPlaylist,
                        onAddToPlaylist = viewModel::addToPlaylist,
                        onRemoveFromPlaylist = viewModel::removeFromActivePlaylist,
                        onDeletePlaylist = viewModel::deletePlaylist,
                        onLoadThumbnail = viewModel::loadThumbnail,
                        onPreloadAhead = viewModel::preloadThumbnailsStartingAt,
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onPlay = {
                            viewModel.play(it)
                            screen = Screen.NOW_PLAYING
                        },
                    )
                }
                Screen.NOW_PLAYING -> NowPlayingScreen(
                        playback = playback,
                        artwork = artwork,
                        queue = queue,
                        lyrics = lyrics,
                        showFileDetails = settings.showFileDetails,
                        editableQueue = settings.editableQueue,
                        language = settings.appLanguage,
                        controller = controller,
                        contentPadding = PaddingValues(0.dp),
                        isPictureInPicture = isPictureInPicture,
                        onVideoBoundsChanged = onVideoBoundsChanged,
                        onPictureInPicture = onEnterPictureInPicture,
                        onBack = { screen = Screen.LIBRARY },
                        onTogglePlay = viewModel::togglePlayPause,
                        onPrevious = viewModel::previous,
                        onNext = viewModel::next,
                        onSeek = viewModel::seekTo,
                        onSpeed = viewModel::setSpeed,
                        onRepeat = viewModel::cycleRepeatMode,
                        onSleepTimer = viewModel::setSleepTimer,
                        sleepTimer = sleepTimer,
                        seekOffsetMs = settings.seekOffsetMs,
                        onSeekBy = viewModel::seekBy,
                        onPlayQueueItem = viewModel::playQueueItem,
                        onLoadThumbnail = viewModel::loadThumbnail,
                        onLoadWaveform = viewModel::loadWaveform,
                        onMoveQueueItem = viewModel::moveQueueItem,
                        onRemoveQueueItem = viewModel::removeQueueItem,
                    )
                Screen.SETTINGS -> SettingsScreen(
                    appName = appName,
                    preferences = settings,
                    playback = playback,
                    onBack = { screen = Screen.LIBRARY },
                    onRowSize = viewModel::setLibraryRowSize,
                    onThemeMode = viewModel::setThemeMode,
                    onShowThumbnails = viewModel::setShowThumbnails,
                    onShowFileDetails = viewModel::setShowFileDetails,
                    onPreloadThumbnails = viewModel::setPreloadThumbnails,
                    onResumePlayback = viewModel::setResumePlayback,
                    onAutoPictureInPicture = viewModel::setAutoPictureInPicture,
                    onFloatingWindowMode = viewModel::setFloatingWindowMode,
                    onAppLanguage = viewModel::setAppLanguage,
                    onAppFont = viewModel::setAppFont,
                    onDeveloperMode = viewModel::setDeveloperMode,
                    onEditableQueue = viewModel::setEditableQueue,
                    onImportM3u = { m3uImporter.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain")) },
                    onExportM3u = { m3uExporter.launch("Greater-Art-playlist.m3u8") },
                    onBackgroundMode = { mode ->
                        when {
                            mode == AppBackgroundMode.CUSTOM_IMAGE &&
                                (settings.customBackgroundImageUri == null ||
                                    settings.backgroundMode == AppBackgroundMode.CUSTOM_IMAGE) ->
                                imageBackgroundPicker.launch(arrayOf("image/*"))
                            mode == AppBackgroundMode.CUSTOM_VIDEO &&
                                (settings.customBackgroundVideoUri == null ||
                                    settings.backgroundMode == AppBackgroundMode.CUSTOM_VIDEO) ->
                                videoBackgroundPicker.launch(arrayOf("video/mp4"))
                            else -> viewModel.setBackgroundMode(mode)
                        }
                    },
                    onChooseBackgroundImage = {
                        imageBackgroundPicker.launch(arrayOf("image/*"))
                    },
                    onChooseBackgroundVideo = {
                        videoBackgroundPicker.launch(arrayOf("video/mp4"))
                    },
                    onClearBackgroundImage = { viewModel.setCustomBackgroundImage(null) },
                    onClearBackgroundVideo = { viewModel.setCustomBackgroundVideo(null) },
                    onBackgroundDim = viewModel::setBackgroundDim,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onCreatePlaylistAndSeed = viewModel::createPlaylistAndSeed,
                    onPlayPlaylist = viewModel::playPlaylist,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onSpeed = viewModel::setSpeed,
                    onPlaybackCycle = viewModel::setPlaybackCycle,
                    onClearThumbnailCache = viewModel::clearThumbnailCache,
                    onRescan = viewModel::rescan,
                    onReset = viewModel::resetAppSettings,
                    onSeekOffset = viewModel::setSeekOffset,
                )
            }
            }
            if (settings.developerMode) {
                // Diagnostics are deliberately collected only while the inspector is
                // enabled. Thumbnail warmup changes these counters hundreds of times;
                // collecting them at the app root caused avoidable full-screen churn.
                val thumbnailStats by viewModel.thumbnailStats.collectAsStateWithLifecycle()
                val waveformDiagnostics by viewModel.waveformDiagnostics.collectAsStateWithLifecycle()
                val regions = when (screen) {
                    Screen.LIBRARY -> listOf("LIBRARY_TOP_BAR", "SEARCH_AND_SORT", "MEDIA_LIST", "MINI_PLAYER")
                    Screen.NOW_PLAYING -> listOf("MEDIA_STAGE", "TRACK_TITLE", "PLAYBACK_CONTROLS", "TIMELINE", "QUEUE", "LYRICS")
                    Screen.SETTINGS -> listOf("SETTINGS_TOP_BAR", "APPEARANCE", "PLAYBACK", "SONG_LISTS", "CACHE", "PRIVACY")
                }
                val warning = playback.errorMessage != null ||
                    (playback.isVideo && playback.isPlaying && playback.positionMs > 1_000L && !playback.videoFrameRendered) ||
                    waveformDiagnostics.error != null
                DeveloperDiagnostics(
                    report = buildString {
                        appendLine("version=${com.local.listentomusic.BuildConfig.VERSION_NAME}")
                        appendLine("screen=${screen.name}")
                        appendLine("media=${playback.currentPath ?: "none"}")
                        appendLine("playing=${playback.isPlaying} video=${playback.isVideo}")
                        appendLine("position=${playback.positionMs} duration=${playback.durationMs}")
                        appendLine("playerState=${controller?.playbackState ?: -1} buffered=${controller?.bufferedPosition ?: 0L}")
                        appendLine("video=${playback.videoWidth}x${playback.videoHeight} firstFrame=${playback.videoFrameRendered}")
                        appendLine("queue=${queue.size} library=${library.files.size}")
                        appendLine("repeat=${playback.repeatMode} random=${playback.shuffleEnabled}")
                        appendLine("floating=${settings.floatingWindowMode} auto=${settings.autoPictureInPicture}")
                        appendLine("background=${settings.backgroundMode} theme=${settings.themeMode}")
                        appendLine("thumbs=memory:${thumbnailStats.memoryHits} disk:${thumbnailStats.diskHits} made:${thumbnailStats.generated} failed:${thumbnailStats.failed} active:${thumbnailStats.inFlight}")
                        appendLine("waveform=${waveformDiagnostics.status} file=${waveformDiagnostics.fileName ?: "none"}")
                        appendLine("waveformError=${waveformDiagnostics.error ?: "none"}")
                        val storageGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                            Environment.isExternalStorageManager()
                        appendLine("storage=$storageGranted overlay=${Settings.canDrawOverlays(context)}")
                        appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} api=${Build.VERSION.SDK_INT}")
                        appendLine("warning=$warning")
                    },
                    regions = regions,
                    warning = warning,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        }
    }
}

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.remember
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
import com.local.listentomusic.ui.components.AppBackground
import com.local.listentomusic.ui.components.MiniPlayer
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.FloatingWindowMode
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
    onMiniWindowSourceBoundsChanged: (Rect) -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onGrantStorageAccess: () -> Unit,
) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val controller by viewModel.controller.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playHistory by viewModel.playHistory.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { viewModel.backupSettings(it) } }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.restoreSettings(it) } }
    var restoreConfirm by rememberSaveable { mutableStateOf(false) }
    var showDuplicates by rememberSaveable { mutableStateOf(false) }
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
    val artworkSource = remember(queue, library.files, playback.currentPath) {
        queue.firstOrNull { it.path == playback.currentPath } ?: library.files.firstOrNull { it.path == playback.currentPath }
    }
    // On session restore the path can arrive before the local index. Retry when its
    // media record arrives instead of keeping the initial null artwork indefinitely.
    val artwork by produceState<Bitmap?>(initialValue = null, key1 = playback.currentPath,
        key2 = settings.localOverrides[playback.currentPath], key3 = artworkSource) {
        value = null
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
    val libraryPager = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val navigationScope = rememberCoroutineScope()
    LaunchedEffect(libraryPager.currentPage) { if (libraryPager.currentPage == 1) viewModel.requestGraph() }
    var playerOpen by rememberSaveable { mutableStateOf(openPlayerRequest > 0) }
    var editDisplay by remember { mutableStateOf<com.local.listentomusic.model.MediaFile?>(null) }
    var createRule by remember { mutableStateOf(false) }
    var sheetState by remember { mutableStateOf(androidx.compose.animation.core.MutableTransitionState(openPlayerRequest > 0)) }
    val playerPresentationRequested = playerOpen || openPlayerRequest > 0
    sheetState.targetState = playerPresentationRequested
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) { viewModel.startLibraryObservation() }
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) { viewModel.stopLibraryObservation() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); viewModel.stopLibraryObservation() }
    }
    var jokeDismissed by rememberSaveable { mutableStateOf(false) }
    val inspector = remember { UiInspectorState() }

    LaunchedEffect(openPlayerRequest) {
        if (openPlayerRequest > 0) {
            // MainActivity already refreshes the live MediaSession on resume. Do not
            // make the Mini Window return wait on duplicate queue/session work before
            // declaring Now Playing visible.
            playerOpen = true
            sheetState = androidx.compose.animation.core.MutableTransitionState(true)
            onOpenPlayerRequestConsumed(openPlayerRequest)
        }
    }

    LaunchedEffect(playerPresentationRequested) {
        onPlayerScreenChanged(playerPresentationRequested)
    }

    BackHandler(enabled = screen != Screen.LIBRARY || playerOpen || libraryPager.currentPage == 1) {
        if (playerOpen) playerOpen = false
        else if (screen != Screen.LIBRARY) screen = Screen.LIBRARY
        else navigationScope.launch { libraryPager.animateScrollToPage(0) }
    }

    val appName = if (settings.silianRail) "PIERCE&PIERCE" else "Greater Art"
    GreaterArtTheme(
        themeMode = settings.themeMode,
        appFont = settings.appFont,
        silianRail = settings.silianRail,
    ) {
        val lightPalette = MaterialTheme.colorScheme.background.luminance() > 0.5f
        androidx.compose.runtime.SideEffect {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.setPresentation(
                playerPresentationRequested || sheetState.currentState || sheetState.targetState, isPictureInPicture,
            )
        }
        LaunchedEffect(lightPalette) {
            context.findActivity()?.let { activity ->
                androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = lightPalette
                    isAppearanceLightNavigationBars = lightPalette
                }
            }
        }
        UiInspectorHost(enabled = settings.developerMode, state = inspector) {
        Box(modifier = Modifier.fillMaxSize().inspectElement("APP_VIEWPORT", "Greater Art root viewport")) {
            // Stable ownership across navigation: prepare once, pause when covered,
            // and resume immediately when Library or Settings reveals the wallpaper.
            AppBackground(preferences = settings, currentPath = playback.currentPath, isVideo = playback.isVideo, controller = controller,
                visible = !(sheetState.currentState || sheetState.targetState))
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
                    if (playback.isVideo) {
                        // A shared decoder cannot display two outgoing/incoming
                        // surfaces at once. Switch ownership without an overlap.
                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    } else {
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
                                controller = controller,
                                videoPreviewActive = playback.isVideo &&
                                    !(sheetState.currentState || sheetState.targetState),
                                language = settings.appLanguage,
                                onPreviewBoundsChanged = onMiniWindowSourceBoundsChanged,
                                onOpen = { playerOpen = true },
                                onTogglePlay = viewModel::togglePlayPause,
                                onPrevious = viewModel::previous,
                                onNext = viewModel::next,
                            )
                        }
                    },
                ) { padding ->
                    HorizontalPager(state = libraryPager, modifier = Modifier.fillMaxSize(), key = { if (it == 1) "NODES" else "LIBRARY" }) { page ->
                    if (page == 1) {
                        val graph by viewModel.graph.collectAsStateWithLifecycle()
                        val loading by viewModel.graphLoading.collectAsStateWithLifecycle()
                        val error by viewModel.graphError.collectAsStateWithLifecycle()
                        NodesScreen(graph, loading, error, playback.currentPath, padding,
                            { navigationScope.launch { libraryPager.animateScrollToPage(0) } }, viewModel::requestGraph,
                            { viewModel.playGraphNode(it); playerOpen = true }, settings.graphOptions, viewModel::setGraphOptions, settings.appLanguage)
                    } else {
                    LibraryScreen(
                        appName = appName,
                        state = library,
                        preferences = settings,
                        playHistory = playHistory,
                        currentPath = playback.currentPath,
                        // The outer Scaffold's bottom inset is the mini-player height.
                        // Applying it to the whole Library created a permanent dead band.
                        // The list now draws behind the player and keeps only a scroll-end inset.
                        contentPadding = PaddingValues(0.dp),
                        onGrantStorageAccess = onGrantStorageAccess,
                        onRefresh = viewModel::rescan,
                        onPlayHistoryEnabled = viewModel::setPlayHistoryEnabled,
                        onClearPlayHistory = viewModel::clearPlayHistory,
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
                        onDeleteFile = viewModel::deleteMediaFile,
                        onShareMedia = { file ->
                            AndroidShare.media(context, file, uiText(settings.appLanguage, "Share media file", "分享媒體檔案")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this file", "無法分享此檔案"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onShareCurrentList = {
                            navigationScope.launch {
                                val label = when (settings.activePlaylistId) {
                                    com.local.listentomusic.data.FAVOURITES_PLAYLIST_ID -> uiText(settings.appLanguage, "Favorites", "我的最愛")
                                    else -> settings.playlists.firstOrNull { it.id == settings.activePlaylistId }?.name
                                        ?: uiText(settings.appLanguage, "Current Library list", "目前音樂庫清單")
                                }
                                AndroidShare.list(context, label, library.files, uiText(settings.appLanguage, "Share current Library list", "分享目前音樂庫清單")).onFailure {
                                    android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this list", "無法分享此清單"), android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onShareSelectedFiles = { files ->
                            AndroidShare.mediaFiles(context, files, uiText(settings.appLanguage, "Share files", "分享檔案")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share these files", "無法分享這些檔案"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onToggleFavourite = viewModel::toggleFavourite,
                        onLoadThumbnail = viewModel::loadThumbnail,
                        onPreloadAhead = viewModel::preloadThumbnailsStartingAt,
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onOpenNodes = { navigationScope.launch { libraryPager.animateScrollToPage(1) } },
                        onEditDisplay = { editDisplay = it },
                        onCreateRule = { createRule = true },
                        onAddSelected = viewModel::addAllToPlaylist,
                        onCreateSelected = viewModel::createSelectionPlaylist,
                        onPlay = {
                            viewModel.play(it)
                        },
                    )
                    }
                    }
                }
                Screen.NOW_PLAYING -> Unit // Legacy saved enum; playback now lives in the overlay.
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
                                        onBackgroundScaleMode = viewModel::setBackgroundScaleMode,
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
                    onSharePlaylist = { playlist ->
                        navigationScope.launch {
                            AndroidShare.list(context, playlist.name, viewModel.filesForPlaylist(playlist.id), uiText(settings.appLanguage, "Share playlist", "分享播放清單")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this list", "無法分享此清單"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onSpeed = viewModel::setSpeed,
                    onPlaybackCycle = viewModel::setPlaybackCycle,
                    onClearThumbnailCache = viewModel::clearThumbnailCache,
                    onRescan = viewModel::rescan,
                    onReset = viewModel::resetAppSettings,
                    onSeekOffset = viewModel::setSeekOffset,
                    onJokeAdsEnabled = viewModel::setJokeAdsEnabled,
                    onShowSleepControl = viewModel::setShowSleepControl,
                    onShowAbRepeat = viewModel::setShowAbRepeat,
                    onExtendedSearch = viewModel::setExtendedSearch,
                    onFolderExcluded = viewModel::setFolderExcluded,
                    onReplayGainEnabled = viewModel::setReplayGainEnabled,
                    onBlackDiscMode = viewModel::setBlackDiscMode,
                    onPlayHistoryEnabled = viewModel::setPlayHistoryEnabled,
                    onBackup = { backupPicker.launch("Greater-Art-settings.json") },
                    onRestore = { restoreConfirm = true },
                    onDuplicates = { showDuplicates = true },
                    onEqualizer = {
                        val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                            .putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, controller?.audioSessionId ?: 0)
                            .putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            .putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                        runCatching { context.startActivity(intent) }.onFailure {
                            android.widget.Toast.makeText(context, "No system equalizer is installed on this device.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
            }
            PlayerOverlay(
                sheetState,
                isPictureInPicture,
                { playerOpen = false },
                instantReveal = openPlayerRequest > 0,
            ) {
                NowPlayingScreen(playback, artwork, queue, lyrics, settings.showFileDetails, settings.editableQueue,
                    settings.blackDiscMode,
                    settings.appLanguage, controller, PaddingValues(0.dp), isPictureInPicture,
                    onVideoBoundsChanged, onEnterPictureInPicture,
                    { screen = Screen.LIBRARY; playerOpen = false; navigationScope.launch { libraryPager.scrollToPage(0) } }, { playerOpen = false },
                    viewModel::togglePlayPause, viewModel::previous, viewModel::next, viewModel::seekTo,
                    viewModel::setSpeed, viewModel::cycleRepeatMode, viewModel::setSleepTimer, sleepTimer,
                    settings.seekOffsetMs, viewModel::seekBy, viewModel::playQueueItem, viewModel::loadThumbnail,
                    viewModel::loadWaveform, viewModel::moveQueueItem, viewModel::removeQueueItem,
                    viewModel::beginTemporaryDoubleSpeed, viewModel::endTemporaryDoubleSpeed,
                    playback.currentPath in settings.favouritePaths, viewModel::toggleFavourite,
                    {
                        val current = queue.firstOrNull { it.path == playback.currentPath }
                            ?: library.files.firstOrNull { it.path == playback.currentPath }
                        if (current != null) {
                            AndroidShare.media(context, current, uiText(settings.appLanguage, "Share media file", "分享媒體檔案")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this file", "無法分享此檔案"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    {
                        navigationScope.launch {
                            AndroidShare.list(context, uiText(settings.appLanguage, "Current queue", "目前播放佇列"), queue,
                                uiText(settings.appLanguage, "Share current queue", "分享目前播放佇列")).onFailure {
                                android.widget.Toast.makeText(context, uiText(settings.appLanguage, "Could not share this list", "無法分享此清單"), android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    })
            }
            val undoMessage by viewModel.undoMessage.collectAsStateWithLifecycle()
            undoMessage?.let { message ->
                androidx.compose.material3.Snackbar(modifier = Modifier.align(Alignment.BottomCenter), action = {
                    androidx.compose.material3.TextButton(onClick = viewModel::undoLastEdit) { androidx.compose.material3.Text(uiText(settings.appLanguage, "Undo", "復原")) }
                }) { androidx.compose.material3.Text(message) }
            }
            if (settings.developerMode) {
                // Diagnostics are deliberately collected only while the inspector is
                // enabled. Thumbnail warmup changes these counters hundreds of times;
                // collecting them at the app root caused avoidable full-screen churn.
                val thumbnailStats by viewModel.thumbnailStats.collectAsStateWithLifecycle()
                val waveformDiagnostics by viewModel.waveformDiagnostics.collectAsStateWithLifecycle()
                val engineReport by com.local.listentomusic.playback.PlaybackDiagnostics.report.collectAsStateWithLifecycle()
                val indexStatus by viewModel.indexStatus.collectAsStateWithLifecycle()
                val viewport = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
                val density = androidx.compose.ui.platform.LocalDensity.current
                val regions = inspector.regions.values.sortedBy { it.order }.map { region ->
                    "${region.label} · ${region.bounds.width.toInt()}×${region.bounds.height.toInt()}px" +
                        region.detail.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                }
                val surface by com.local.listentomusic.ui.components.VideoSurfaceOwner.state.collectAsStateWithLifecycle()
                val warnings = surface.warnings(
                    com.local.listentomusic.ui.components.VideoSurfaceOwner.expectedOwner,
                    playback.isVideo && playback.isPlaying && controller?.playbackState == androidx.media3.common.Player.STATE_READY &&
                        (playerOpen || screen == Screen.LIBRARY), android.os.SystemClock.elapsedRealtime(),
                    playback.videoFrameRendered,
                ) + listOfNotNull(if (playback.errorMessage != null) "PLAYBACK_ERROR" else null,
                    if (waveformDiagnostics.error != null) "WAVEFORM_ERROR" else null)
                val warning = warnings.isNotEmpty()
                DeveloperDiagnostics(
                    report = buildString {
                        appendLine("version=${com.local.listentomusic.BuildConfig.VERSION_NAME}")
                        appendLine("screen=${if (playerOpen) Screen.NOW_PLAYING.name else if (screen == Screen.LIBRARY && libraryPager.currentPage == 1) "NODES" else screen.name} playerOverlay=$playerOpen")
                        appendLine("media=${playback.currentPath?.let { com.local.listentomusic.model.sourceMediaPath(it).substringAfterLast('.') } ?: "none"} (paths omitted)")
                        appendLine("playing=${playback.isPlaying} video=${playback.isVideo}")
                        appendLine("position=${playback.positionMs} duration=${playback.durationMs}")
                        appendLine("playerState=${controller?.playbackState ?: -1} buffered=${controller?.bufferedPosition ?: 0L}")
                        appendLine("video=${playback.videoWidth}x${playback.videoHeight} controllerMediaFirstFrame=${playback.videoFrameRendered}")
                        appendLine(com.local.listentomusic.ui.components.VideoSurfaceOwner.describe())
                        appendLine("firstFrameAttribution=renderer timestamp after transfer; not a screen-capture proof")
                        appendLine("queue=${queue.size} library=${library.files.size}")
                        appendLine("repeat=${playback.repeatMode} random=${playback.shuffleEnabled}")
                        appendLine("floating=${settings.floatingWindowMode} auto=${settings.autoPictureInPicture}")
                        appendLine("background=${settings.backgroundMode} theme=${settings.themeMode}")
                        appendLine("thumbs=memory:${thumbnailStats.memoryHits} disk:${thumbnailStats.diskHits} made:${thumbnailStats.generated} failed:${thumbnailStats.failed} noCoverOrUnsupported:${thumbnailStats.missingArtwork} active:${thumbnailStats.inFlight}")
                        appendLine("waveform=${waveformDiagnostics.status}")
                        appendLine("waveformError=${waveformDiagnostics.error ?: "none"}")
                        val storageGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                            Environment.isExternalStorageManager()
                        appendLine("storage=$storageGranted overlay=${Settings.canDrawOverlays(context)}")
                        appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} api=${Build.VERSION.SDK_INT}")
                        appendLine("warnings=$warnings")
                        appendLine("\nAUDIO ENGINE")
                        appendLine(engineReport)
                        appendLine("\nVIEWPORT / PERFORMANCE")
                        appendLine("viewportPx=${viewport.width}x${viewport.height} density=${density.density} fontScale=${density.fontScale}")
                        appendLine("heapUsedMiB=${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_048_576} heapLimitMiB=${Runtime.getRuntime().maxMemory() / 1_048_576}")
                        appendLine("lastTapToFirstFrameMs=${com.local.listentomusic.playback.PlaybackDiagnostics.firstFrameDelayMs ?: "not reported"} (last tap; independent of current surface)")
                        appendLine("$indexStatus")
                        appendLine("abControls=${settings.showAbRepeat} extendedSearch=${settings.extendedSearch} thumbPreload=${settings.preloadThumbnails}")
                        appendLine("displayOverrides=${settings.localOverrides.size} rulePlaylists=${settings.playlists.count { it.rule != null }}")
                        appendLine("No logs, file paths or listening history are uploaded.")
                    },
                    regions = regions,
                    warning = warning,
                    inspector = inspector,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            if (settings.jokeAdsEnabled && !jokeDismissed && !isPictureInPicture) {
                FakeAdInterstitial(
                    onSkip = { jokeDismissed = true },
                    onOpenLink = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                            "https://www.youtube.com/watch?v=dQw4w9WgXcQ".toUri())) }
                    },
                )
            }
            if (restoreConfirm) androidx.compose.material3.AlertDialog(
                onDismissRequest = { restoreConfirm = false },
                title = { androidx.compose.material3.Text(uiText(settings.appLanguage, "Restore settings and playlists?", "還原設定與播放清單？")) },
                text = { androidx.compose.material3.Text(uiText(settings.appLanguage, "The selected backup replaces portable settings and playlists. Media files remain unchanged.", "所選備份將取代可攜式設定與播放清單，不會更改媒體檔案。")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { restoreConfirm = false; restorePicker.launch(arrayOf("application/json", "text/plain")) }) { androidx.compose.material3.Text(uiText(settings.appLanguage, "Choose backup", "選擇備份")) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { restoreConfirm = false }) { androidx.compose.material3.Text(uiText(settings.appLanguage, "Cancel", "取消")) } },
            )
            if (showDuplicates) DuplicateDialog(library.files, playback.currentPath,
                onDismiss = { showDuplicates = false }, onChanged = viewModel::rescan)
            editDisplay?.let { file -> DisplayOverrideDialog(file, settings.localOverrides[file.path], settings.appLanguage,
                viewModel::loadThumbnail, { title, cover -> viewModel.setLocalOverride(file.path, title, cover) }, { editDisplay = null }) }
            if (createRule) RulePlaylistDialog(settings.appLanguage, viewModel::createRulePlaylist, { createRule = false })
        }
        }
        }
    }
}

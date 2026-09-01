package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.components.MiniPlayer
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.ui.theme.GreaterArtTheme

private enum class Screen { LIBRARY, NOW_PLAYING, SETTINGS }

@Composable
fun GreaterArtApp(
    viewModel: MainViewModel,
    isPictureInPicture: Boolean,
    onPlayerScreenChanged: (Boolean) -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onGrantStorageAccess: () -> Unit,
) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val controller by viewModel.controller.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val miniWindow by viewModel.miniWindowVisible.collectAsStateWithLifecycle()
    val artwork by produceState<Bitmap?>(initialValue = null, key1 = playback.currentPath) {
        value = viewModel.loadCurrentArtwork(playback.currentPath)
    }
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }

    LaunchedEffect(screen) {
        onPlayerScreenChanged(screen == Screen.NOW_PLAYING)
    }

    BackHandler(enabled = screen != Screen.LIBRARY) { screen = Screen.LIBRARY }

    GreaterArtTheme(themeMode = settings.themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val spring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                    // ponytail: iOS-style horizontal push; forward pushes in from right, back from left.
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
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onPlay = {
                            viewModel.play(it)
                            if (settings.floatingWindowMode == com.local.listentomusic.data.FloatingWindowMode.MINI_WINDOW) {
                                viewModel.setMiniWindowVisible(true)
                            }
                            screen = Screen.NOW_PLAYING
                        },
                    )
                }
                Screen.NOW_PLAYING -> NowPlayingScreen(
                        playback = playback,
                        artwork = artwork,
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
                        seekOffsetMs = settings.seekOffsetMs,
                        onSeekBy = viewModel::seekBy,
                    )
                Screen.SETTINGS -> SettingsScreen(
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
                    onCreatePlaylist = viewModel::createPlaylist,
                    onCreatePlaylistAndSeed = viewModel::createPlaylistAndSeed,
                    onPlayPlaylist = viewModel::playPlaylist,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onSpeed = viewModel::setSpeed,
                    onRepeatMode = viewModel::setRepeatMode,
                    onClearThumbnailCache = viewModel::clearThumbnailCache,
                    onRescan = viewModel::rescan,
                    onReset = viewModel::resetAppSettings,
                    onSeekOffset = viewModel::setSeekOffset,
                )
            }
            }
            MiniWindowEffect(
                enabled = settings.floatingWindowMode == com.local.listentomusic.data.FloatingWindowMode.MINI_WINDOW
                    && playback.hasMedia && !isPictureInPicture && screen != Screen.LIBRARY,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun MiniWindowEffect(enabled: Boolean, viewModel: MainViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { /* re-checked on next settings toggle */ }
    LaunchedEffect(enabled) {
        if (!enabled) {
            context.stopService(Intent(context, MiniWindowOverlayService::class.java))
            return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            launcher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
            )
            return@LaunchedEffect
        }
        context.startForegroundService(Intent(context, MiniWindowOverlayService::class.java))
    }
}

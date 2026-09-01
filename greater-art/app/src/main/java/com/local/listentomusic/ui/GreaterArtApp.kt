package com.local.listentomusic.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.components.MiniPlayer
import com.local.listentomusic.ui.components.AppBackground
import com.local.listentomusic.data.AppBackgroundMode
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
    val context = LocalContext.current
    val imageBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        viewModel.setCustomBackgroundImage(uri.toString())
    }
    val videoBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        viewModel.setCustomBackgroundVideo(uri.toString())
    }
    val artwork by produceState<Bitmap?>(initialValue = null, key1 = playback.currentPath) {
        value = viewModel.loadCurrentArtwork(playback.currentPath)
    }
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }

    LaunchedEffect(screen) {
        onPlayerScreenChanged(screen == Screen.NOW_PLAYING)
    }

    BackHandler(enabled = screen != Screen.LIBRARY) { screen = Screen.LIBRARY }

    GreaterArtTheme(themeMode = settings.themeMode) {
        val lightPalette = MaterialTheme.colorScheme.background.luminance() > 0.5f
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackground(preferences = settings, playback = playback)
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
                    val spring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
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
                    onBackgroundMode = { mode ->
                        when {
                            mode == AppBackgroundMode.CUSTOM_IMAGE && settings.customBackgroundImageUri == null ->
                                imageBackgroundPicker.launch(arrayOf("image/*"))
                            mode == AppBackgroundMode.CUSTOM_VIDEO && settings.customBackgroundVideoUri == null ->
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
                    onRepeatMode = viewModel::setRepeatMode,
                    onClearThumbnailCache = viewModel::clearThumbnailCache,
                    onRescan = viewModel::rescan,
                    onReset = viewModel::resetAppSettings,
                    onSeekOffset = viewModel::setSeekOffset,
                )
            }
            }
        }
        }
    }
}

package com.local.listentomusic.ui

import android.graphics.Rect
import android.graphics.Bitmap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.components.MiniPlayer
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
            when (screen) {
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
                        onAddToPlaylist = viewModel::addToPlaylist,
                        onRemoveFromPlaylist = viewModel::removeFromActivePlaylist,
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
                    )
                Screen.SETTINGS -> SettingsScreen(
                    preferences = settings,
                    playback = playback,
                    onBack = { screen = Screen.LIBRARY },
                    onRowSize = viewModel::setLibraryRowSize,
                    onThemeMode = viewModel::setThemeMode,
                    onShowThumbnails = viewModel::setShowThumbnails,
                    onShowFileDetails = viewModel::setShowFileDetails,
                    onShowTypeBadge = viewModel::setShowTypeBadge,
                    onPreloadThumbnails = viewModel::setPreloadThumbnails,
                    onResumePlayback = viewModel::setResumePlayback,
                    onAutoPictureInPicture = viewModel::setAutoPictureInPicture,
                    onFloatingWindowMode = viewModel::setFloatingWindowMode,
                    onAppLanguage = viewModel::setAppLanguage,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onSpeed = viewModel::setSpeed,
                    onRepeatMode = viewModel::setRepeatMode,
                    onClearThumbnailCache = viewModel::clearThumbnailCache,
                    onRescan = viewModel::rescan,
                    onReset = viewModel::resetAppSettings,
                )
            }
        }
    }
}

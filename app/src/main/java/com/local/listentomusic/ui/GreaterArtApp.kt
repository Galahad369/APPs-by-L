package com.local.listentomusic.ui

import android.graphics.Rect

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.ui.components.MiniPlayer
import com.local.listentomusic.ui.theme.GreaterArtTheme

private enum class Screen { LIBRARY, NOW_PLAYING }

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
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }

    LaunchedEffect(screen) {
        onPlayerScreenChanged(screen == Screen.NOW_PLAYING)
    }

    BackHandler(enabled = screen == Screen.NOW_PLAYING) { screen = Screen.LIBRARY }

    GreaterArtTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.LIBRARY -> Scaffold(
                    bottomBar = {
                        if (playback.hasMedia) {
                            MiniPlayer(
                                playback = playback,
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
                        currentPath = playback.currentPath,
                        contentPadding = padding,
                        onGrantStorageAccess = onGrantStorageAccess,
                        onRefresh = viewModel::rescan,
                        onQueryChange = viewModel::setQuery,
                        onSortChange = viewModel::setSortMode,
                        onMoveItem = viewModel::moveCustomItem,
                        onLoadThumbnail = viewModel::loadThumbnail,
                        onPlay = {
                            viewModel.play(it)
                            screen = Screen.NOW_PLAYING
                        },
                    )
                }
                Screen.NOW_PLAYING -> NowPlayingScreen(
                        playback = playback,
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
            }
        }
    }
}

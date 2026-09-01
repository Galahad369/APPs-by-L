# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video.
It scans supported media anywhere under Android's `Download` folder and contains no
ads, analytics, accounts, telemetry, or network access.

## Features

- Recursive local scan of `Download` with cached, preloaded thumbnails.
- Media3 playback for audio and video through one `MediaSessionService`.
- Library search, custom drag order, name sorting, and local playlists.
- Repeat-one default, gapless-friendly queues, speed controls, seek, next and previous.
- Full video playback, rotation, fullscreen mode and Android picture-in-picture.
- Three floating modes:
  - `COMPACT` — Android-controlled compact picture-in-picture.
  - `FOLLOW_VIDEO` — default; picture-in-picture following the video's aspect ratio.
  - `MINI_WINDOW` — 124×40dp audio or 108×61dp video system overlay.
- Four app backgrounds:
  - Animated black liquid metal (default).
  - User-selected image (`image/*`, including PNG/JPEG/WebP supported by Android).
  - User-selected muted looping MP4.
  - A muted, synchronized copy of the currently playing video.
- English and Traditional Chinese interface options.
- Dark theme by default; system and light themes remain selectable.

Custom backgrounds use Android's document picker and persist only the selected file's
read access. Background videos have their audio track disabled and pause when the app
is not visible. Missing or unsupported background files fall back to liquid metal.

## Privacy

The manifest intentionally contains **no `INTERNET` permission**. It also removes
Media3's transitive network-state permission. Media files, preferences, playlists,
thumbnail cache and background selections stay on the device.

This personal sideload build uses `MANAGE_EXTERNAL_STORAGE` to scan all supported
media recursively under `Download`. A Play Store build should replace this with a
persisted Storage Access Framework folder selection.

## Build

Requirements: JDK 21 and Android SDK/API 37.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar testDebugUnitTest lintDebug assembleDebug
```

Current release artifact:

```text
releases/GreaterArt-v1.6.2-debug.apk
SHA-256: 1725489EC9779ECE7A40CCD0DB8B0F589474A88ED9B0766B1FEB9A1917E43090
```

Versioned APKs are never overwritten. Builds remain signed by the pinned personal
debug keystore so a newer APK can update the existing installation.

## Architecture

```text
app/src/main/java/com/local/listentomusic/
├── MainActivity.kt                   Activity lifecycle, PiP and overlay fallback
├── MainViewModel.kt                  Library, playback state and preference actions
├── data/
│   ├── AppPreferences.kt             DataStore settings and playlists
│   ├── MediaScanner.kt               Recursive Download scan
│   └── ThumbnailRepository.kt        Memory/disk thumbnail cache
├── playback/
│   ├── PlaybackService.kt            Media3 MediaSessionService
│   └── MiniWindowOverlayService.kt   Tiny WindowManager overlay
└── ui/
    ├── GreaterArtApp.kt              Navigation, pickers and app background layer
    ├── LibraryScreen.kt
    ├── NowPlayingScreen.kt
    ├── SettingsScreen.kt
    └── components/
        ├── AppBackground.kt           Image and muted-video backgrounds
        ├── LiquidMetalSurface.kt      Non-measuring animated metal decoration
        └── MiniPlayer.kt              Bounded library bottom player
```

## Failure Prevention Notes

- Runtime receivers on target SDK 37 must be registered with an explicit exported
  state. The fallback receiver uses `ContextCompat.RECEIVER_NOT_EXPORTED`.
- Overlay service start/stop belongs to Activity lifecycle callbacks. Do not move it
  back into a screen-keyed Compose `LaunchedEffect`; that previously left dead state
  after reopening the app.
- Decorative children inside size-sensitive Compose containers must not participate
  in measurement. `LiquidMetalSurface` uses `BoxScope.matchParentSize()`, not
  `fillMaxSize()`. The latter expanded `Scaffold.bottomBar` over the entire library.
- Transparent `Surface`/`Scaffold` containers declare `contentColor`; otherwise a
  dark wallpaper can inherit a light-theme black foreground.
- The mini-window red-X path stops playback before destroying its controller, then
  removes both services and the Activity task.
- Every overlay view and controller future is nullable, guarded and released. A
  rejected optional close target must not kill an otherwise working mini player.
- Keep the application ID and signing certificate unchanged. Current certificate
  SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.

See [HANDOFF.md](HANDOFF.md) for the continuation checklist and copy-paste Hermes prompt.

## Current Verification Boundary

Unit tests, Android lint, compilation, manifest permissions and APK signature are
checked locally. This computer currently has no connected Android device or emulator,
so installation, OEM overlay behavior and real codec playback still require a phone
smoke test.

# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video.
It scans supported media anywhere under Android's `Download` folder and contains no
ads, analytics, accounts, telemetry, or network access.

## Features

- Recursive local scan of `Download` with a staged 300-item thumbnail warmup, bounded
  two-worker cache pipeline, embedded artwork and same-name/folder-cover fallback.
- Media3 playback for audio and video through one `MediaSessionService`.
- Library search, custom drag order, name sorting, local playlists and M3U/M3U8 import/export.
- Repeat-one default, gapless-friendly queues, speed controls, seek, next and previous.
- One-line playback controls: speed, Off/One/All/Random cycle and sleep timer.
- Compact one-line playback controls and an unlabelled scrollable queue with
  thumbnails and current-track highlight.
- Synchronized local lyrics from a matching `.lrc`, with embedded MP3/FLAC/Opus lyrics fallback.
- Noise-gated, percentile-scaled PCM waveform, generated once off the UI thread and
  cached locally without exaggerating silence or one loud spike.
- Optional queue editor for moving/removing upcoming items; disabled by default.
- Bluetooth output removal pauses playback before audio can spill to the phone speaker.
- Full video playback, rotation, fullscreen mode and Android picture-in-picture.
- Three floating modes:
  - `COMPACT` — Android-controlled compact picture-in-picture.
  - `FOLLOW_VIDEO` — picture-in-picture following the video's aspect ratio.
  - `MINI_WINDOW` — default; 124×40dp audio or 108×61dp video system overlay.
- Four app backgrounds:
  - Animated black liquid metal (default).
  - User-selected image (`image/*`, including PNG/JPEG/WebP supported by Android).
  - User-selected muted looping MP4.
  - A muted, synchronized copy of the currently playing video.
- English and Traditional Chinese interface options.
- Dark/black liquid metal by default; light mode uses white liquid metal.
- System text style by default plus platform families and six bundled open-licensed fonts.
- Optional full-screen developer inspector with live player, first-frame, cache,
  waveform, permission and UI-region facts; disabled by default and fully local.
- Optional reversible `Silian Rail` mode uses bundled EB Garamond small caps and the
  in-app `PIERCE&PIERCE` identity. It is deliberately last in the font choices.
- Restrained technical-grid liquid metal and a flat waveform/stylus launcher mark.
- The visible app keeps the display awake while still honoring the hardware lock key.

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
releases/GreaterArt-v1.7.8-debug.apk
SHA-256: e04a8c09dba381e7cd7d3e7f0c341037c2e30bd5a9a8343b2f57dc09f169515a
```

Versioned APKs are never overwritten. Builds remain signed by the pinned personal
debug keystore so a newer APK can update the existing installation.

## Architecture

```text
app/src/main/java/com/local/listentomusic/
├── MainActivity.kt                   Activity lifecycle, PiP and overlay routing
├── MainViewModel.kt                  Library, playback state and preference actions
├── data/
│   ├── AppPreferences.kt             DataStore settings and playlists
│   ├── MediaScanner.kt               Recursive Download scan
│   ├── ThumbnailRepository.kt        Memory/disk thumbnail and cover-art cache
│   └── WaveformRepository.kt         Background PCM peak decoder/cache
├── playback/
│   ├── PlaybackService.kt            Media3 MediaSessionService
│   └── MiniWindowOverlayService.kt   Tiny WindowManager overlay
└── ui/
    ├── GreaterArtApp.kt              Navigation, pickers and app background layer
    ├── LibraryScreen.kt
    ├── NowPlayingScreen.kt
    ├── SettingsScreen.kt
    ├── DeveloperDiagnostics.kt       Copyable local debug facts
    └── components/
        ├── AppBackground.kt           Image and muted-video backgrounds
        ├── LiquidMetalSurface.kt      Non-measuring animated metal decoration
        └── MiniPlayer.kt              Bounded library bottom player
```

## Failure Prevention Notes

- Do not rewrite the saved floating mode when a temporary overlay start fails. Earlier
  fallback code silently changed Mini to Compact and made the failure permanent.
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
- The red-X hit test reads both attached overlay bounds from Android; never rebuild
  its position from raw display metrics, which drift around gesture navigation bars.
- The red-X collision is circular. Rectangle intersection accepted invisible corner
  pixels outside the visible ring and made the real quit zone look offset.
- Android overlay coordinates are already constrained around Samsung system bars;
  adding the navigation inset twice placed the red X too high. Keep the target at the
  overlay frame's bottom margin and compare actual attached rectangles.
- Android 12 automatic PiP must be disabled whenever Mini window is selected. If it
  remains armed, system PiP wins before `onUserLeaveHint` can launch the tiny overlay.
- Thumbnail warmup and visible-list lookahead use separate jobs. Reusing one job made
  every scroll event cancel the previous preload before it could finish. Prefetch
  preserves visible queue order so video-heavy sorting cannot starve nearby audio art.
- Startup scan requests are deduplicated, and the 300-thumbnail pass is staged after
  the visible 24 rows to prevent first-launch layout/I/O shaking.
- Real waveforms decode on a worker and keep only normalized peaks; playback never
  waits for waveform generation. Audio transitions warm the cache even before the
  waveform UI requests it; WAV receives a direct PCM parser before codec fallback.
- Never create a second decoder for the currently playing video background. On phones
  with one hardware video decoder, that caused sound with a black foreground surface.
  Reuse the MediaController and detach old PlayerViews when media/screen changes.
- The player owns one repeat cycle (`Off → One → All → Random`). Random is Media3
  shuffle state inside that cycle, not a second button or a queue rebuild.
- Mini-window launch intents carry an explicit open-player request. Activity intent
  handling owns this durable pending request and consumes it only after media is ready;
  the overlay never guesses Compose navigation state.
- Developer cache counters are collected only while Developer Mode is enabled. Root
  collection previously recomposed the whole app several times for every thumbnail.
- Waveform transition warmup and the visible waveform share one decoder mutex and
  recheck the cache, preventing duplicate full-file PCM jobs.
- The first thumbnail batch waits briefly for the initial frame, then warms in staged
  chunks. Visible rows bypass the warmup gate, while background work uses two workers.
- Local lyrics accept UTF-8, UTF-16 BOM and Big5 text, use matching `song.lrc` or
  `song.mp3.lrc` files, and never make a network request.
- Every overlay view and controller future is nullable, guarded and released. A
  rejected optional close target must not kill an otherwise working mini player.
- Keep the application ID and signing certificate unchanged. Current certificate
  SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`.

See [HANDOFF.md](HANDOFF.md) for the continuation checklist and copy-paste Hermes prompt.
See [RESEARCH_IDEAS.md](RESEARCH_IDEAS.md) for privacy-compatible ideas compared
against maintained open-source Android players.

## Current Verification Boundary

Unit tests, Android lint, compilation, manifest permissions and APK signature are
checked locally. This computer currently has no connected Android device or emulator,
so installation, OEM overlay behavior and real codec playback still require a phone
smoke test.

Bundled font licenses are packaged under `app/src/main/assets/font_licenses/`.

`v1.7.2` is retained only as known-crashed forensic history. It is not a supported
baseline or release recommendation. No device logcat was captured for that build, so
its exact crash signature is intentionally not guessed.

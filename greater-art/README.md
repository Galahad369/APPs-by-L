# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video.
It scans supported media anywhere under Android's `Download` folder and contains no
ads, analytics, accounts, telemetry, or network access.

## Features

- v1.9.3 Now Playing layout: artwork/video above the scrollable playback queue, with
  the timeline and Repeat / Previous / Play-Pause / Next / Speed fixed at the bottom.
- Home returns to Library without stopping playback. Floating player and fullscreen
  remain available at the top. Controls retain Greater Art's selected theme.
- Now Playing opens as a large dismissible sheet over the current page. The arrow
  collapses secondary controls; A–B practice is optional and off by default, with
  visible A/B timeline markers when enabled. Double-tap seeking has directional feedback.
- Local title/cover overrides, natural name sorting, multi-selection, rule playlists
  and eight-second undo for playlist/queue removals. Original media is not rewritten.
- Optional artist/album/lyrics search builds a local metadata cache off the UI thread.
- CUE sheets expose separate tracks without splitting audio files. Local enhanced LRC
  and common clock-time TTML support word highlighting; SRT provides line timing.
- Optional parallel mixer: main track plus nine audio layers, with pause, mute, level
  and removal for added layers. Video layers contribute sound only. Buffers and mixer
  levels are bounded; actual simultaneous decoder capacity depends on the phone.
- Current-video wallpaper follows playback across Library and Settings using the
  main decoder. It is the new/reset default; audio falls back to liquid metal.
- Japanese, German, French and Cantonese options with offline core-interface translations.
  Some older utility and diagnostic text retains English fallback.

- Recursive local scan of `Download` with a staged 300-item thumbnail warmup, bounded
  two-worker cache pipeline, embedded artwork and same-name/folder-cover fallback.
- Media3 playback through a `MediaLibraryService` with background media controls.
- Library search, custom drag order, name sorting, local playlists and M3U/M3U8 import/export.
- Repeat-one default, gapless-friendly queues, speed controls, seek, next and previous.
- Collapsible secondary controls: speed, Off/One/All/Random and Mix. A–B practice
  and the sleep timer are optional and hidden by default.
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
  - `MINI_WINDOW` — default; 126×42dp audio or 110×63dp video system overlay.
- Four app backgrounds:
  - Animated black liquid metal.
  - User-selected image (`image/*`, including PNG/JPEG/WebP supported by Android).
  - User-selected muted looping MP4.
  - Current video's shared surface (new/reset default), without duplicate audio.
- English (default), Traditional Chinese, Japanese, German, French and Cantonese.
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

Local debug builds use the existing pinned sideload keystore when it is present, so
installed updates keep the same identity. Clean CI runners use a disposable debug key
for verification only; CI artifacts are not release APKs. Release signing never falls
back, and no keystore or signing secret is stored in this repository.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar testDebugUnitTest lintDebug assembleDebug
```

Current release artifact:

```text
releases/GreaterArt-v1.9.1-debug.apk
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
│   ├── PlaybackService.kt            Media3 service, mixer and shared audio focus
│   ├── ParallelPlayback.kt           Temporary mixer commands and UI state
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

For 1.9.3: 37 unit tests passed; lint reports 0 errors and 13 warnings; the debug
build and APK signature verification passed. The application ID/signing certificate
are unchanged and the packaged manifest has no INTERNET permission. No Android device
was connected for this review build.

APK: `releases/GreaterArt-v1.9.3-debug.apk`

SHA-256: `77a428e8ecabf7d4b6ded4d8558d12fbefb8facc4142e4719ac34d8bbcc647f9`.

The 1.9 series includes regression checks for mixer headroom, languages, natural
sorting, playlist rules, CUE/M3U boundaries and local lyric parsing. Build checks do
not establish phone performance: overlay dragging, Bluetooth/calls, ten concurrent
decoders, large-font layout and scrolling still require a real device.

## New local library tools

Open a song's overflow menu to edit its display title/cover, enter multi-selection,
or create a rule playlist. Selection survives changing the search query. Add selected
files to an existing ordinary playlist or create one. Rule playlists combine folder,
extension and title conditions; they do not use listening history.

Enable extended search in Settings to include artist, album and local lyrics. Cached
metadata is invalidated when the source or sidecar changes. MediaStore notifications
trigger a debounced refresh while the app is visible; Download remains authoritative
and is still traversed. Use Refresh for sidecars Android has not indexed.

Place a same-name `.lrc`, `.ttml` or `.srt` beside the media. Word highlighting needs
word timestamps in the file; ordinary LRC/SRT cannot invent them. TTML support covers
common clock-time exports, not every TTML dialect. CUE references must resolve to
audio already allowed by the library's Download scan. M3U exports retain CUE start/end
directives; another player must support those directives to honor the boundaries.

Title/cover overrides stay local and do not alter original files. Portable settings
backups do not export cover access grants or local overrides. Resetting settings does
not erase library overrides. Choose the original title/cover in its edit dialog to
remove an override.

## Parallel playback and background use

Start a track, open Now Playing, and tap **Mix**. Add up to nine extra files from the
current list. Added videos play sound only. Adjust a layer's slider, mute/pause it,
or remove it. **Remove all layers** leaves the main track playing. Mixing lowers each
voice for headroom; return to one voice for the original single-track level.

Playback continues when changing pages or locking the screen through foreground media
sessions. Calls, external audio-focus loss and Bluetooth removal pause every voice.
The mini-window red X shuts down all layers. Android Force stop and manufacturer
battery restrictions can still override background operation.

The current-video wallpaper shares the prepared main decoder across Library and Settings;
Now Playing takes its video surface back. Custom background MP4 remains muted and pauses
while the app is hidden. No second decoder is created for current-video wallpaper.

Unit tests, Android lint, compilation, manifest permissions and APK signature are
checked locally. This computer currently has no connected Android device or emulator,
so installation, OEM overlay behavior and real codec playback still require a phone
smoke test.

Bundled font licenses are packaged under `app/src/main/assets/font_licenses/`.

`v1.7.2` is retained only as known-crashed forensic history. It is not a supported
baseline or release recommendation. No device logcat was captured for that build, so
its exact crash signature is intentionally not guessed.

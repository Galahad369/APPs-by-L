# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video. It scans supported media under Android's `Download` folder and is intentionally local-first: **no Internet permission, ads, accounts, analytics, telemetry, or cloud playback dependency**.

## Current release

- Version: **1.12.2**
- Version code: **83**
- Application ID: `com.local.listentomusic`
- APK: `releases/GreaterArt-1.12.2.apk`
- APK SHA-256: `09a15cadecb06bfbf3c562fd163db8b8aea76a2fabe296ee42a09806ce8fd0f5`
- Signing certificate SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

Versioned APKs are never overwritten.

## Core experience

### Library and playback

- Recursive local media discovery under `Download`.
- Media3 playback through a `MediaLibraryService` with background/system media controls.
- Library search, sorting, playlists, custom ordering, M3U/M3U8 import/export, and local metadata overrides.
- Tapping a Library row starts playback in place; tapping the live Library mini-player opens Now Playing.
- Repeat Off / One / All / Random, playback speed, seek, next/previous, optional A–B practice, and optional sleep controls.
- Local Favorites and queue-only search without rebuilding playback order.
- Optional local play history is off by default and remains device-local.
- Bluetooth output removal pauses playback before audio can spill to the phone speaker.

### Video and floating playback

- Full local video playback, rotation, fullscreen, and Android picture-in-picture.
- Hold an actively playing foreground video for 700 ms for temporary 2× playback; release/cancel restores the exact prior speed.
- Three floating modes:
  - `COMPACT` — Android-controlled compact PiP.
  - `FOLLOW_VIDEO` — PiP following video aspect ratio.
  - `MINI_WINDOW` — tiny draggable system overlay.
- Shared video-surface ownership coordinates Library, Now Playing, fullscreen/PiP, and the mini-window without intentionally lowering source resolution or frame rate.

### Local text, waveform, and library tools

- Matching local LRC plus embedded lyric fallback.
- CUE virtual tracks and local SRT/TTML support where applicable.
- Cached decoded waveform peaks with playback coloring and a precise playhead; the waveform does not fabricate equalizer motion.
- Filename-similarity **Nodes** graph with pan, pinch, drag, find, and tap-to-play.
- Natural sorting, multi-selection, rule playlists, duplicate tools, and local title/cover overrides.

### Sharing

- Share an original media file through Android's system Sharesheet.
- Explicit multi-file sharing without loading the files into RAM.
- Portable M3U8 sharing for the current Library, playlist, or queue.
- Temporary share access is URI-based; raw private filesystem paths are not the sharing API.

### Appearance

- Dark/light liquid-metal backgrounds.
- User-selected image or muted looping video background.
- Current-video background mode.
- Multiple text styles and bundled open-licensed fonts.
- English, Traditional Chinese, Japanese, German, French, and Cantonese interface options.

### Developer mode

Developer Mode is optional and local. It exposes playback state, cache/waveform facts, surface ownership/generation diagnostics, and tagged UI regions for troubleshooting. Diagnostics are evidence about application events; they are not a substitute for observing pixels on a real device.

## Privacy

The packaged manifest intentionally contains **no `INTERNET` permission** and removes Media3's transitive network-state permission.

Media files, preferences, playlists, Favorites, optional history, thumbnails, waveform cache, and selected backgrounds remain on-device unless the user explicitly invokes an Android share/export action.

This personal sideload build uses `MANAGE_EXTERNAL_STORAGE` to scan media under `Download`. A Play Store distribution would need a different storage strategy such as a persisted Storage Access Framework folder grant.

## Build

Requirements:

- JDK 21
- Android SDK / API 37

From `greater-art/`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar testDebugUnitTest lintDebug assembleDebug
```

The repository does not contain the personal release keystore.

## Architecture

```text
app/src/main/java/com/local/listentomusic/
├── MainActivity.kt
├── MainViewModel.kt
├── data/
│   ├── AppPreferences.kt
│   ├── MediaScanner.kt
│   ├── MetadataIndex.kt
│   ├── ThumbnailRepository.kt
│   └── WaveformRepository.kt
├── graph/
│   ├── FilenameSimilarity.kt
│   ├── GraphBuilder.kt
│   └── GraphLayout.kt
├── model/
├── playback/
│   ├── PlaybackService.kt
│   ├── ParallelPlayback.kt
│   └── MiniWindowOverlayService.kt
└── ui/
    ├── GreaterArtApp.kt
    ├── LibraryScreen.kt
    ├── NowPlayingScreen.kt
    ├── SettingsScreen.kt
    └── components/
```

## Important engineering invariants

- Playback/audio and the visible primary video have priority over decorative effects.
- Do not cap source video resolution, bitrate, or frame rate to hide rendering problems.
- A stale video view must never clear a newer owner's active output.
- Same-owner/same-view/same-player reconciliation must remain a no-op.
- Hidden/completely covered UI should not continue expensive decorative rendering.
- Do not rebuild playback queues merely because search/filter presentation changes.
- Keep the application ID and signing certificate stable for install/update continuity.
- No network, account, analytics, or telemetry dependency should be introduced casually.

## Verification boundary

The 1.12.2 build passed:

- `testDebugUnitTest lintDebug assembleDebug --offline --no-daemon --max-workers=2`
- 99 unit tests, 0 failures/errors
- lint: 0 errors, 18 warnings, 1 hint
- APK signature verification
- 16 KiB zip alignment verification
- packaged manifest check: no `INTERNET` permission

No Android device/emulator was connected for that build verification. Real-device checks remain required for visible surface output, overlay geometry, Sharesheet receivers, Bluetooth behavior, hold-to-2× cancellation, and device-specific performance.

## Current technical notes

- [HANDOFF.md](HANDOFF.md) — concise continuation state and non-negotiable invariants.
- [docs/NODES.md](docs/NODES.md) — filename graph behavior and limits.
- [docs/SURFACE_DEBUG_1.12.2.md](docs/SURFACE_DEBUG_1.12.2.md) — current surface-warning diagnosis.
- [docs/LOCAL-FUTURES-1.12.2.md](docs/LOCAL-FUTURES-1.12.2.md) — architecture-only future possibilities.

Public demonstrations:

- [User demonstration](../greater-art-user-demo.html)
- [Technical demonstration](../greater-art-technical-demo.html)

Historical drafts and superseded release briefs are intentionally left to Git history rather than kept in the active documentation tree.

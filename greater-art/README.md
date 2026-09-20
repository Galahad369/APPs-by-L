# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video. It scans supported media under Android's `Download` folder and is intentionally local-first: **no Internet permission, ads, accounts, analytics, telemetry, or cloud playback dependency**.

## Current release

- Version: **1.12.3**
- Version code: **84**
- Application ID: `com.local.listentomusic`
- APK: `releases/GreaterArt-1.12.3.apk`
- APK SHA-256: `ac20c6535245dbe2440020f6e61026f155a7d56744e9488115b2e5186b9b55cb`
- Signing certificate SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`
- v1.12.3 fixes Favorites for filenames whose persisted path uses URL-safe Base64,
  including affected CJK paths. The reader accepts both legacy standard Base64 and
  the current URL-safe format. Favorites can now be played and shared as a real list.
  Now Playing also places the title, Favorite and current-file Share actions in one
  compact row, with Queue/Search/queue export grouped directly above the queue.
- v1.12.2 keeps the playback journey library-first: tapping a Library row starts playback in
  place, while tapping the live Library mini-player deliberately opens Now Playing.
  The list now draws behind that player instead of reserving a permanent blank band;
  only its scroll-end inset keeps the final row reachable.
- Hold an actively playing video for 700 ms for temporary 2× playback. Releasing
  restores the exact prior speed without changing the saved preference. Queue search,
  local Favorites, original-file sharing, multi-file sharing and portable M3U8 list
  sharing are available without adding network access.
- Video surface ownership now keeps the same PlayerView through media transitions and
  cross-checks Media3's current-media first-frame signal. This removes the unnecessary
  surface recreation behind misleading `READY_VIDEO_NO_FRAME` warnings; see
  [the 1.12.2 surface note](docs/SURFACE_DEBUG_1.12.2.md).
- The mini-window preserves the established visible `103×56dp` widescreen footprint,
  switches to `56×56dp` for square art/video, reaches screen edges without a hidden
  gutter, shows embedded audio artwork, and applies a strong transparent red tint
  only while it overlaps the real close target. The target is another three physical
  pixels higher than 1.11.7.
- Library search now uses a focused liquid-metal treatment and a clear action; the
  top bar uses denser controls. Initial loading uses the real app mark. Now Playing
  keeps side-only double-tap seeking with one restrained YouTube-style cue, adds an
  optional pause-aware black-disc presentation for audio, and leaves the center
  double-tap zone inert.
- Optional play history is disabled by default. When enabled, successful playback
  transitions are stored locally (up to 500 entries), never included in settings
  backups, and can be permanently burned from the Library. Resetting app settings
  also clears it. Developer inspection can cycle through overlapping tagged elements.
- Two standalone phone films document the exact journey: the
  [user film](../greater-art-user-demo.html) and the
  [technical journey](../greater-art-technical-demo.html).

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

Current release artifact:

```text
releases/GreaterArt-1.12.3.apk
```

Versioned APKs are never overwritten. Builds remain signed by the pinned personal
debug keystore so a newer APK can update the existing installation.

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

For 1.12.3 (code 84): 102 unit tests passed; lint reports 0 errors, 18 warnings and 1 hint; the debug
build and APK signature verification passed. The application ID/signing certificate
are unchanged and the packaged manifest has no INTERNET permission. No Android device
was connected for this review build.

APK: `releases/GreaterArt-1.12.3.apk` (26,074,638 bytes)

SHA-256: `ac20c6535245dbe2440020f6e61026f155a7d56744e9488115b2e5186b9b55cb`.

Real-device checks remain required for visible surface output, overlay geometry, Sharesheet receivers, Bluetooth behavior, hold-to-2× cancellation, and device-specific performance.

## Current technical notes

- [HANDOFF.md](HANDOFF.md) — concise continuation state and non-negotiable invariants.
- [docs/NODES.md](docs/NODES.md) — filename graph behavior and limits.
- [docs/SURFACE_DEBUG_1.12.2.md](docs/SURFACE_DEBUG_1.12.2.md) — current surface-warning diagnosis.
- [docs/LOCAL-FUTURES-1.12.2.md](docs/LOCAL-FUTURES-1.12.2.md) — architecture-only future possibilities.

Public demonstrations:

- [User demonstration](../greater-art-user-demo.html)
- [Technical demonstration](../greater-art-technical-demo.html)

Historical drafts and superseded release briefs are intentionally left to Git history rather than kept in the active documentation tree.

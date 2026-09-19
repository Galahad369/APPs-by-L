# Greater Art — Offline Android Media Player

Greater Art is a native Kotlin/Jetpack Compose player for local music and video.
It scans supported media anywhere under Android's `Download` folder and contains no
ads, analytics, accounts, telemetry, or network access.

## Features

- v1.11.6 makes primary video ownership follow the visible player rather than view
  callback order. Developer Mode separates media/active-surface first-frame states,
  generations and actionable warnings. It also restores ambient-artwork retries on
  session restore, moves Nodes right, and raises/brightens the floating close target.
  Samsung rendering verification remains pending; see the
  [surface investigation](docs/SURFACE_DEBUG_1.11.6.md) for evidence and limits.

- v1.11.4 fixes a Library video-preview lifecycle detach, restores the preview on
  resume, and removes background video quality restrictions. Wallpaper follows
  pause/speed/seek events without rapid resync seeks. Source quality and frame rate
  remain uncapped; actual codec capability and smoothness depend on the device.
- Now Playing uses restrained colors from cached artwork, tighter title spacing and
  the existing bottom transport/queue layout. Nodes is now on the right. Settings put
  default choices first; graph and newer controls support all six bundled languages.
  The close target is more opaque and three physical pixels higher; mini-window size,
  edge access and its remembered position are unchanged.

- v1.10.4 adds Fit, Stretch, and Cut to screen size background scaling choices. The
  latter remains the default.
- v1.10.3 removes the last ratio-generated edge gap from the floating video surface
  without restoring hidden padding: the fixed window stays `103×56dp` and the video crops
  edge-to-edge. Nodes now shows the selected app background. Current-video wallpaper uses
  a separate muted low-resolution decoder with aspect-ratio-preserving `FIT`, so it neither
  stretches nor steals Media3's primary surface from the Library live-video preview. The
  Library position seeds the external mini-window only once; later launches restore the
  user's last dragged position.
- v1.10.2 restores the older mini-window's effective `103×56dp` footprint and removes
  the double-applied bottom system inset so it can reach the physical bottom edge. The
  Library bottom player uses the same preview dimensions, displays live video when safe,
  and supplies the floating window's spawn position. Now Playing follows the pull-down
  handle with a real dismiss animation; its timeline/transport area sits lower and tighter.
  Library uses the app mark and gives the primary All songs control clearer hierarchy over
  the outlined Nodes action. Nodes adds restrained organic drift and much stronger visual
  weighting for highly connected files. Double-tap seek is back to one basic YouTube-style
  icon/readout, with the middle zone still inert.
- v1.10.1 makes Developer mode readable regardless of theme and identifies individual
  controls instead of only broad screen regions. It adds guarded three-confirmation
  source-file deletion, reduces the mini overlay by another three physical pixels,
  replaces the cold-loading artwork with a properly inset mark, limits double-tap seeking
  to the outer side zones with one restrained cue, and makes Nodes place its strongest
  weighted hub in the center with visibly shorter strong links. A finite wand animation
  reveals the graph hub-first without leaving continuous physics running.
- v1.10 makes the mini overlay exactly three physical pixels smaller and gives default
  small Library thumbnails the same physical footprint. It restores Newest/Oldest sort,
  adds one-tap filter clearing, preserves the selected Library order in whole-library
  playback queues, bounds the double-tap seek animation, and expands Nodes with local
  link-distance, repulsion, elasticity, and connected-node spring controls. Developer
  mode can now identify tagged UI regions and copy exact px/dp geometry for bug reports.
- v1.9.19 keeps the verified lightweight playback motion and removes a later experimental
  animation rewrite that duplicated seeking, fabricated a fixed beat and broke row-size
  choices. The five intended effects remain: staggered waveform reveal, subtle waveform
  breathing, real-envelope cover motion, scrub feedback and finite double-tap seek ripples.
- v1.9.18 refines **Nodes**, an optional filename-similarity graph. Library remains the
  default: swipe left (or tap Nodes) to enter. On Nodes, swipe right on the header or
  tap Library to return. The canvas itself supports pan, pinch zoom, drag and tap-to-play.
  Find / play a node provides a searchable text alternative to small visual targets.
  Nodes includes physical media files across the scanned library, not just a filtered playlist.
- Nodes has a focused vault-style surface, a double-ring/play marker for the current
  track and node sizes weighted by strong filename connections. Controls persist link
  threshold, node size, edge visibility, labels, isolated nodes and connection weighting.
- Playback motion uses a single waveform canvas with a short staggered reveal and
  subtle decorative breathing. Cover scale follows the cached amplitude envelope,
  not invented BPM; paused/hidden playback stops decorative motion. Scrubbing shows
  a lifted timestamp/cached cover without decoding on every drag. Only actual A/B
  markers attract the seek position, never fabricated keyframe/10% markers.
- Double-tap seek feedback uses a basic side icon/readout and accumulates repeated
  seeks. Playback speed uses a compact gauge with an animated needle.
- Graph connections use raw filenames only: Unicode words/CJK runs, shared characters,
  bigrams/trigrams and a small prefix boost. No tags, listening history or manual links.
  Similarity and finite force-layout computation run off the UI thread. Edges and
  settled coordinates are cached locally; only the visible graph draws gentle drift.
  See [graph implementation notes](docs/NODES.md) for the exact formula and large-library limits.
- Mini-window return rebuilds Now Playing from the active media session without waiting
  for a file scan. Audio and video overlays share the same tap/drag behavior and use a
  111×64dp base reduced by exactly three physical pixels on each axis.
  Waveforms use stable physical-file cache keys and a rolling eight-source audio warmup,
  including startup before anything is playing. Slow/stalled waveform decoding times out
  independently of playback. Coverless/unsupported audio is distinguished from thumbnail
  extraction failures in the local developer panel; missing artwork is not a memory failure.

- v1.9.4 Now Playing layout: artwork/video above the scrollable playback queue, with
  the timeline and Repeat / Previous / Play-Pause / Next / Speed fixed at the bottom.
- The stable top bar exposes Floating player / Home / Fullscreen / Close. Home returns
  to Library; Close dismisses the Now Playing sheet without stopping playback.
  Controls retain Greater Art's selected theme.
- Now Playing opens as a large dismissible sheet over the current page.
  A–B practice is optional and off by default, with
  visible A/B timeline markers when enabled. Double-tap seeking has directional feedback.
- Local title/cover overrides, natural name sorting, multi-selection, rule playlists
  and eight-second undo for playlist/queue removals. Original media is not rewritten.
- Optional artist/album/lyrics search builds a local metadata cache off the UI thread.
- CUE sheets expose separate tracks without splitting audio files. Local enhanced LRC
  and common clock-time TTML support word highlighting; SRT provides line timing.
- Parallel mixer infrastructure is retained internally; its unfinished UI was removed
  in 1.9.11 and is not presented as an available control.
- Current-video wallpaper follows playback across Library and Settings using the
  main decoder. It is the new/reset default; audio falls back to liquid metal.
- Japanese, German, French and Cantonese options with offline core-interface translations.
  Some older utility and diagnostic text retains English fallback.

- Recursive local scan of `Download` with a staged 300-item thumbnail warmup, bounded
  two-worker cache pipeline, embedded artwork and same-name/folder-cover fallback.
- Media3 playback through a `MediaLibraryService` with background media controls.
- Library search, custom drag order, name sorting, local playlists and M3U/M3U8 import/export.
- Repeat-one default, gapless-friendly queues, speed controls, seek, next and previous.
- Speed and Off/One/All/Random controls. A–B practice and the sleep timer are optional
  and hidden by default; enabled options are shown inline.
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
  - `MINI_WINDOW` — default; 111×64dp audio/video system overlay.
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
releases/GreaterArt-1.11.6.apk
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

For 1.11.6 (code 80): 96 unit tests passed; lint reports 0 errors, 17 warnings and 1 hint; the debug
build and APK signature verification passed. The application ID/signing certificate
are unchanged and the packaged manifest has no INTERNET permission. No Android device
was connected for this review build.

APK: `releases/GreaterArt-1.11.6.apk` (26,585,380 bytes)

SHA-256: `b62b1bb624d9aae150511c2f2b0eba9384f123fc18653008584748b240f1061b`.

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

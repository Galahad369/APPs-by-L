# HANDOFF — Greater Art Android Media Player

**Project:** `greater-art/` in the repository checkout  
**Current version:** `1.9.12` (code 63), Mix and options collapser removed, clean secondary controls  
**Latest APK:** `releases/GreaterArt-v1.9.12-debug.apk`
**Application ID:** `com.local.listentomusic`
**Signing certificate SHA-256:** `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

## Current State

### September 9 — 1.9.4 reference-aligned Now Playing (newest)

- Now Playing uses a stable four-action header in the requested order: floating
  player, Home, fullscreen and Close. Home always returns to Library; Close dismisses
  the player sheet and reveals whichever app page was underneath it.
- Portrait video controls no longer compete with the status bar or cover the frame.
  The header sits above the media; immersive landscape/fullscreen keeps an overlay
  header inside the correctly inset video surface.
- Audio artwork is larger, the queue owns the flexible middle area, and timeline plus
  Repeat/Random, Previous, Play/Pause, Next and Speed stay fixed at the bottom.
  A-B/Mix/Sleep options default collapsed so the queue is usable immediately.
- Verification: 37 unit tests passed; lint completed with 0 errors / 13 warnings;
  assembleDebug and signing verification passed. Package ID and pinned certificate
  remain unchanged; the packaged manifest contains no INTERNET permission.
- APK SHA-256: `86cf787103c14050c9be8ce02374953e381743ca412270f3e4d5e713d4a9770c`.
- No Android device or emulator was connected. Insets, rotation, PiP/mini-window
  handoff and large-font layout still need a real-device visual check.

### September 9 — clean-runner signing fix

- GitHub's Android CI and CodeQL builds failed before analysis because `debug` was
  unconditionally assigned the private sideload keystore at `~/.android/debug.keystore`.
  Clean runners correctly do not contain that local file.
- Debug builds now use the pinned sideload identity only when that exact file exists.
  A clean runner falls back to Android Gradle Plugin's disposable debug identity.
  Release builds remain pinned, no keystore is committed, and published/versioned APKs
  must still be built locally with the pinned certificate.
- Verified both paths: the normal local test/lint/build passed, and an isolated
  `user.home` with no keystore successfully generated a disposable debug key and built.
  The versioned 1.9.3 APK hash remains unchanged.

### September 9 — 1.9.3 corrected Now Playing layout (newest)

- The screenshot-style layout belongs to Now Playing, not Library. The mistaken local
  Library redesign was rolled back: Library retains its normal toolbar, filters,
  scrollable file list and mini-player, and tapping a row opens Now Playing.
- Now Playing keeps media at top, its scrollable playback queue in the remaining space,
  and timeline/transport at bottom. Repeat/Random is mirrored left of Previous;
  Speed is mirrored right of Next. Speed/repeat were removed from the expandable
  options strip so controls are not duplicated.
- Home replaces the ambiguous back arrow and returns to Library without stopping the
  player. Floating and fullscreen actions remain at the top. Audio and portrait video
  both use the same bottom-control ordering and the original selected theme.
- Status/navigation icon colors follow the selected app palette, including after
  fullscreen exits. Previously disposal restored the OS palette, which could conflict
  with an explicit in-app dark/light selection.
- The wrong-layout 1.9.2 APK remains a local intermediate and must not be published.
  It was not overwritten. Final 1.9.3 validation/hash is recorded after rebuilding.
- 1.9.3 validation: 37 unit tests passed; lint 0 errors / 13 warnings;
  assembleDebug and signature verification passed. Package ID and pinned certificate
  are unchanged; packaged permissions contain no INTERNET permission. Repository audit
  passed. No Android device was connected, so visual/PiP checks remain outstanding.
- 1.9.3 APK SHA-256: `77a428e8ecabf7d4b6ded4d8558d12fbefb8facc4142e4719ac34d8bbcc647f9`.
- Original APKs/signing identity are preserved. No GitHub upload was retried for this
  layout request; the previous explicit-publication approval remains outstanding.
- Phone-only checks: status/navigation insets, landscape and large fonts, Home scroll
  behavior, Library ↔ sheet ↔ PiP video handoff and actual screen readability.

### September 9 update: current continuation checkpoint

- The approved UI and local-library additions are in 1.9.1. Now Playing is an
  in-Activity sheet, not a separate Dialog window, preserving PiP/surface ownership.
  One transition owns scrim and sheet lifetimes: transferring video back before the
  slide finishes would produce blank/sound-only frames. Verify navigation on a phone.
- Secondary controls collapse behind an arrow. A–B is off by default and clears its
  active range when disabled. A/B markers, mixer thumbnails, directional seek feedback,
  smaller waveform bar counts and expanded local diagnostics are implemented.
- Added local display overrides, natural sort, optional metadata/lyrics search,
  multi-selection, rule playlists, removal undo, enhanced LRC/TTML/SRT and CUE tracks.
  CUE IDs are virtual; every file IO, artwork, duplicate check, restore and permission
  check must use sourcePath/sourceMediaPath, never treat the ID as a physical filename.
- Queue edits now read back MediaController's actual queue. Updating a UI snapshot
  after synchronous listener callbacks applied reorder twice; sorting the library
  must never silently replace an active playback queue.
- Position persistence had triggered full-library sorting. Only library-relevant
  preference changes now sort, off the main thread. Saved exclusions load before the
  first scan. Refresh preserves the existing list rather than flashing an empty state.
- Thumbnail decode concurrency is globally limited to two, including visible requests
  and preload. Fixed striped locks bound memory and prevent duplicate decode races.
  Playback taps yield warmup capacity then resume the 300-item warmup; the previous
  unconditional cancellation left covers cold. No phone startup benchmark is claimed.
- Waveform UI no longer normalizes already-normalized peaks a second time. That
  flattened bars toward full height. New tracks clear stale peaks; unavailable decoding
  is distinct from loading. Animation follows playback position, not fabricated beats.
- Metadata cache reuses unchanged file/sidecar tags. A lifecycle-bound MediaStore
  observer debounces refresh; this is not a complete incremental row-merge index.
  Unindexed sidecars still need Refresh. Search indexing is optional and off by default.
- Mini-window dimensions are 126×42dp audio / 110×63dp video. Defaults remain mini
  window, repeat One, current-video background, English, system font and dark theme.
- Original media files are unchanged by these additions. CUE tracks sharing a physical
  source are collapsed before duplicate hashing; playing sources are protected.
  Display overrides and persisted cover grants are not in portable JSON backups.
- Final verification: 37 unit tests passed, lint 0 errors / 13 warnings, and
  assembleDebug passed. APK metadata confirms version 1.9.1/code 54, the unchanged
  application ID, the pinned certificate above and no INTERNET permission.
  Public-repository audit passed for history, working files and APK containers.
- APK SHA-256: `70d08304d6f9a2dfcda54f5f6775ca0da659e04fad530c2a5777128ba7cc9cfc`.
  Artifact: `releases/GreaterArt-v1.9.1-debug.apk`. Created once, not overwritten.
- Publishing checkpoint: automated approval rejected the combined commit/push command
  before execution, including after the exact public remote was verified. Changes are
  local and uncommitted on `codex/greater-art-v1.9`; no PR was created. Obtain explicit
  approval to publish this source/docs/workflow/APK payload to the public repository.
  Keep intermediate `releases/GreaterArt-v1.9.0-debug.apk` out of staging.
- No connected Android device was available. Phone-only
  checks remain: background/overlay transitions, red-X targeting, ten voices, Bluetooth,
  large libraries/fonts, actual waveform shape and word-timing cadence.

Hermes continuation: read this section, README and the current diff before editing.
Preserve the package ID, pinned signing certificate and every named APK. Do not restore
the intermediate 1.9.0 implementation over these fixes. Use the next unused version for
any later APK. Keep any device reports separate from unit/lint/build evidence.

### September 8 update: read before older historical notes

- Hermes released 1.8.0 in `6fc92a2` and updated its release docs in `e6dcfe6`.
  Both commits and the released APK are preserved. New work uses `codex/greater-art-v1.9`.
- Previous / Play-Pause / Next and the timeline occupy the bottom of Now Playing.
  Audio waveform sits above the transport row; the queue uses remaining space above.
  Portrait video has no duplicate transport overlay; fullscreen keeps bottom overlays.
- Mix opens simultaneous playback: main track plus up to nine audio layers. Extra
  layers have pause, mute, level and removal. Added videos disable their video renderer.
  Layers are temporary and never saved as listening history.
- PlaybackService owns every layer and MediaSession. Extra buffers are limited to
  2 MiB each. One AudioFocusRequest coordinates all voices. External focus loss, noisy
  routes, Bluetooth removal and service destruction apply to the full mix. Decoder
  failures remove the failing layer. Ten is a capacity ceiling; actual codec capacity
  depends on the phone and formats, and is not yet device-tested.
- Mixer gains divide by total allocated voice count to retain headroom. Removing
  layers restores main gain. ReplayGain applies to the main track only.
- Red-X and stop-intent entry points synchronously stop every mixer voice before
  requesting service destruction. The intermediate 1.9.0 APK is retained locally,
  never overwritten; 1.9.1 adds this explicit shutdown step.
- Current-video wallpaper is the new/reset default; explicitly saved choices remain.
  It shares the main player's prepared decoder through VideoSurfaceOwner. Library and
  Settings show the playing video; Now Playing takes surface ownership. Video navigation
  avoids overlapping animations so views cannot compete for the same decoder output.
- Only a separately selected custom MP4 uses a muted decoder, with a 4 MiB buffer.
  It pauses when covered or the Activity is hidden. Hidden decorative animation stops.
- Japanese, German, French and Cantonese join English and Traditional Chinese. Core
  playback, navigation and settings translations are bundled offline. Cantonese uses
  colloquial labels with Traditional Chinese fallback. Some older diagnostics,
  interpolated messages and 1.8.0 utility dialogs retain English fallback. Native-speaker
  review remains outstanding; do not claim every source literal is translated.
- Fixed the missing Android Auto MEDIA_PLAY_FROM_SEARCH manifest declaration.
  Internal media-ID validation now runs on IO instead of the main/service looper.
- The eight features from the prior research batch landed in 1.8.0: tagged ReplayGain,
  exclusions, exact duplicate report, system EQ, backup/restore, widget, car browsing,
  and A–B repeat. The optional parody toggle remains default-off and unchanged here.
- Verification: 21 unit tests passed; lint passed with 0 errors and 11 warnings;
  assembleDebug passed. No phone is connected and no emulator is installed.
- Remaining phone checks: ten voices, calls/Bluetooth, background notifications,
  red-X closing all voices, lock screen, wallpaper transfers, large text and translations.

Older numbered sections below are historical. Where they conflict, this section and
the actual code describe the current implementation; no uncaptured crash is assumed.

- Recursive local scan of all supported media under `Download`.
- Cached thumbnails with a 300-item bounded two-worker warmup, prioritized viewport
  lookahead, cache telemetry, search, name/custom sorting and playlists.
- Media3 `MediaSessionService`, repeat-one default and notification controls.
- Video fullscreen/rotation/PiP plus a separate tiny overlay mode.
- Mini window is the new/reset default floating shape. Android automatic PiP is
  explicitly disarmed for this mode so it cannot steal the Home-button transition.
- Tiny overlay sizes: 124×40dp audio and 108×61dp video.
- Default dark theme and animated black liquid-metal surfaces.
- App backgrounds: default metal, custom image, muted MP4 and current video.
- Background selection uses `OpenDocument` plus persisted read permission; it adds no
  broad permission or network access.
- English and Traditional Chinese settings.
- Player screen has a compact Speed / Off-One-All-Random / Sleep row and an
  unlabelled thumbnail-backed queue in the remaining space. Queue numbering is never
  shown; format/duration/size appear only when Show file details is enabled.
- Tapping the system mini window returns directly to Now Playing.
- Matching sibling `.lrc` files display synchronized, seekable local lyrics.
- Embedded MP3/FLAC/Opus lyrics are a local fallback; untimed text remains static.
- Audio waveform peaks come from real decoded PCM and are cached without blocking play.
- Same-name images and `cover`/`folder`/`front`/`album`/`artwork` files fill artwork gaps.
- Optional queue editing, M3U/M3U8 import/export, an offline font catalog and a
  full-screen local system inspector are available in Settings.
- `Silian Rail` is a reversible final font option: bundled EB Garamond small caps and
  the in-app `PIERCE&PIERCE` identity. It now uses the same persisted picker value as
  every other font; reset explicitly disables it.
- Media3 1.11.0, DataStore 1.2.1, AndroidX Core 1.19.0 and Lifecycle 2.11.0.
- Bluetooth A2DP/BLE/SCO/hearing-aid removal pauses playback immediately.
- The foreground Activity keeps the display awake; the hardware power key still locks it.

## Bugs Fixed and Why They Happened

### 1. Android 13+ launch crash

`MainActivity` registered a runtime broadcast receiver without declaring whether it
was exported. With target SDK 37 this can throw `SecurityException` during launch.

**Fix:** register through `ContextCompat.registerReceiver(...,
RECEIVER_NOT_EXPORTED)` and guard `unregisterReceiver`.

**Rule:** every dynamically registered app-internal receiver needs an explicit
not-exported flag on modern Android.

### 2. Mini window failed to return or started on the wrong screen

A Compose `LaunchedEffect` tied overlay service lifetime to the current screen and a
dead `miniWindowVisible` state. After the service stopped itself, the effect key could
remain unchanged, so Compose did not restart it on the next background transition.

**Fix:** remove the dead Compose state/effect. `MainActivity.onUserLeaveHint` starts the
overlay, `onResume` removes stale overlays, and explicit floating-player actions route
through the same Activity method.

**Rule:** Activity/process lifecycle owns system overlays; navigation composables do not.

### 3. Bottom mini player covered the entire library

`LiquidMetalSurface` placed `Canvas(Modifier.fillMaxSize())` inside a `Box` used by
`Scaffold.bottomBar`. That decorative canvas participated in measurement and demanded
the maximum available height, so the bottom bar became effectively full-screen.

**Fix:** use `BoxScope.matchParentSize()` for the canvas and cap the inner mini-player
surface at 62dp. The canvas now follows content size without influencing it.

**Rule:** decorative Box children use `matchParentSize`; `fillMaxSize` is for layout
content that is supposed to affect measurement.

### 4. Overlay instability and leaks

Older service code used `root!!`, a full-screen transparent close target, unreleased
controller futures and ambiguous touch handling.

**Fix:** nullable views, bounded position, tiny optional close target, owned drag/click
gesture, `performClick`, detached video surface and `MediaController.releaseFuture`.

**Rule:** an optional overlay failure must degrade gracefully, not stop playback.

### 5. Audio seek thumb stuck at the end

The audio slider used millisecond-sized `Float` ranges. Long durations lose enough
precision to produce unstable visual positioning.

**Fix:** the waveform slider operates in a normalized `0f..1f` range and converts to
milliseconds only when seeking.

### 6. Black text on the black/custom background

Transparent `Surface` and `Scaffold` containers relied on inferred content colors.
For transparent colors, that inference can be unspecified or inherited from the
Activity. Light mode also placed black foreground colors directly over black metal.

**Fix:** transparent containers declare `onBackground`/`onSurface` explicitly,
liquid-metal content provides a stable foreground color, and light mode places a
94%-opaque light base over media backgrounds.

**Rule:** transparent layout containers must declare content contrast; never assume
`contentColorFor(Color.Transparent)` will match the pixels behind them.

### 7. A selected custom background could not be replaced reliably

The active Image/MP4 choice only re-selected its mode, which made tapping it look
dead. Replacement decoding could also leave the previous bitmap displayed.

**Fix:** tapping an active Image/MP4 choice reopens the document picker, the button
changes to “Change”, the old persisted URI grant is released after replacement, and
the old bitmap is cleared before decoding the new one.

### 8. Red-X drop could race playback shutdown

The overlay called `stopSelf()` before sending `controller.stop()`. Service destruction
could release the controller first, leaving playback or the task alive.

**Fix:** stop and clear media first, stop `PlaybackService`, remove the foreground
notification and overlay, then open the stop intent. `MainActivity` stops both services
and calls `finishAndRemoveTask()`.

### 9. Red-X target was visibly misplaced and unreliable

The target used a guessed fixed Y offset while collision used reconstructed display
coordinates. Gesture navigation, rotation, cutouts and OEM insets made those two
coordinate systems disagree.

**Fix:** position the bottom-centered target inside the overlay's already-inset frame,
recompute after configuration changes, keep it measured while hidden, and detect
collision from the actual attached view rectangles returned by Android.

**Rule:** overlay hit testing must compare real window coordinates; do not derive one
window's rectangle from `displayMetrics`.

### 10. Thumbnail preload repeatedly cancelled itself

Library scrolling called lookahead for nearly every visible index. Warmup and
lookahead shared one coroutine job, so each call cancelled work started by the last.
Launching hundreds of preload coroutines also created avoidable scheduling pressure.

**Fix:** warm the first 300 entries once, request later windows only when crossing a
100-row boundary, separate warmup/lookahead jobs, and use two queue workers. Visible
thumbnail requests bypass the background preload throttle while sharing per-file locks.

**Rule:** scrolling may advance a bounded prefetch window; it must not restart the
same cache job for every row.

### 11. Random and repeat were competing controls

Separate Shuffle and Repeat buttons could represent contradictory modes, wasted a
whole row, and old code rebuilt the queue when toggling shuffle.

**Fix:** a single cycle is now `Off → One → All → Random → Off`. Random enables
Media3 shuffle with repeat-all and never rebuilds or restarts the current queue. The
three secondary controls share one dark, equal-width row.

### 12. Player screen wasted its lower half

The audio artwork could consume most of the viewport while video controls left a
large blank panel. Users had to return to the library to select another song.

**Fix:** cap audio artwork from live constraints and give the remaining height to a
lazy song list. The same list fills portrait video's control panel. Rows show cached
thumbnails, highlight the current file and play on a single tap.

### 13. Screen timed out during active use

The app did not express that a visible local player should stay awake.

**Fix:** `MainActivity` sets `FLAG_KEEP_SCREEN_ON`. This is scoped to the visible
Activity, requires no new permission and does not defeat the physical lock button.

### 14. Mini-window tap always returned to the library

The overlay started `MainActivity` without stating which destination was intended,
while the Compose screen state always initialized to Library.

**Fix:** the overlay sends a one-shot `EXTRA_OPEN_PLAYER` intent using
`CLEAR_TOP | SINGLE_TOP`. `MainActivity` consumes it in both `onCreate` and
`onNewIntent`, then a request counter moves Compose to Now Playing once media exists.

**Rule:** external/system entry points must communicate navigation intent explicitly;
do not make an overlay depend on private composable state.

### 15. Queue metadata ignored the details preference

The player queue always rendered index and extension, duplicating information and
showing `MP4` even when file details were disabled.

**Fix:** remove section/count/row numbering completely. Render extension, duration and
size only when the existing Show file details preference is enabled.

### 16. Bluetooth disconnect could expose playback through the phone speaker

Media3 noisy-route handling was enabled, but relying on one broadcast path leaves
room for OEM routing differences.

**Fix:** keep Media3 handling and also register an `AudioDeviceCallback`. Removing a
Bluetooth A2DP, BLE, SCO or hearing-aid output pauses an actively playing player. The
callback is unregistered before player release and requires no Bluetooth permission.

### 17. Local lyric files were ignored

There was no sibling-file resolver or timed-text parser.

**Fix:** match `song.lrc` and `song.mp3.lrc` case-insensitively beside the current
media, decode UTF-8/UTF-16 BOM with Big5 fallback, parse multiple timestamps and
offsets, and display a synchronized three-line panel. Tapping a line seeks locally.

### 18. Mini mode opened system Follow-video PiP

Android 12+ automatic PiP was enabled for every playing video. The OS entered its own
PiP before `onUserLeaveHint` could start `MiniWindowOverlayService`.

**Fix:** `updatePictureInPictureParams` sets `autoEnterEnabled(false)` whenever the
selected mode is Mini window. The explicit floating button uses the same routing rule.

**Rule:** never arm two competing background-window mechanisms for one lifecycle event.

### 19. Red X was offset on Samsung navigation layouts

The overlay frame was already inset by Android, then code added the navigation-bar
inset again. This double offset moved the target above its intended location.

**Fix:** anchor an invisible 92dp hit target 12dp from the overlay frame bottom, keep
the visible X at 56dp, and compare both attached views' real screen rectangles.

### 20. First load visibly shook and thumbnail warmup competed with UI

Both ViewModel initialization and Activity resume requested a full scan. Afterward,
300 previews were warmed as one uninterrupted storage task.

**Fix:** ignore duplicate active scans; warm the first 24 previews first, then process
24-item chunks with short yields. Visible requests still share per-file locks.

### 21. Large files could pressure the process

Time-prioritized buffering could exceed a predictable memory envelope on very high
bitrate local video.

**Fix:** cap ExoPlayer target buffering at 96 MiB, retain only five seconds behind the
playhead and preserve decoder fallback plus one bounded retry.

### 22. Video sometimes produced sound but no picture

The current-video wallpaper created a second ExoPlayer for the same file while the
real player also needed a video decoder. Some phones expose only one usable hardware
decoder, so audio continued while the foreground surface stayed black.

**Fix:** the wallpaper reuses the existing MediaController, old PlayerViews detach on
release, and the foreground video surface is keyed by media path so stale surfaces are
rebuilt on track changes. Developer mode reports video size and first-frame delivery.

**Rule:** one playing item gets one decoder pipeline. Multiple views may take turns
owning its surface; they must not create competing players for decoration.

### 23. Thumbnail preload looked random or incomplete

Preload reordered all videos before audio artwork and scroll lookahead used coarse
late windows. In a mixed library, visible audio rows could wait behind unrelated video
frame extraction.

**Fix:** preserve caller/viewport priority, prefetch a 96-item window around the live
scroll position, retain the staged 300-item warmup, and use three bounded workers with
shared per-file locks. Developer mode exposes memory/disk/generation/failure counters.

### 24. Waveform cache had no reliable warm path

Waveforms were requested only while their Composable existed. Navigation could cancel
the request, and codec failures were invisible.

**Fix:** audio media transitions warm the cache independently; direct WAV PCM parsing
handles common integer/float WAV files before MediaCodec fallback; status and decoder
errors appear in the local inspector. Clearing preview cache clears waveforms too.

### 25. Red-X collision disagreed with the visible target

The target was drawn as a circle but collision used its square WindowManager bounds.
Invisible square corners therefore counted as a drop, which felt several pixels off.

**Fix:** collision now measures the actual attached views and tests the mini-window
rectangle against the visible target circle using its real center and radius.

### 26. Developer mode did not help reproduce UI bugs

The old AlertDialog exposed a few static strings and no rendering/cache evidence.

**Fix:** a full-screen local inspector now reports player state, buffered position,
video frame delivery, thumbnail/cache activity, waveform status/error, permissions,
device/API and named screen regions. It can overlay region IDs and copy one bug report.
Nothing is transmitted.

### 27. Mini-window failures silently changed the user's setting

The overlay failure broadcast called `setFloatingWindowMode(COMPACT)`. One temporary
permission/OEM failure therefore became a permanent preference change, so later Home
presses stopped requesting Mini even after the device condition recovered.

**Fix:** remove the destructive fallback and leave the saved Mini choice untouched.
Missing permission is handled by the explicit floating action/settings flow.

**Rule:** a runtime fallback may degrade one attempt; it must not rewrite user intent.

### 28. Mini tap could lose its Now Playing destination

The intent request could arrive before the restored MediaController reported media.
Navigation depended on one transient Compose timing window.

**Fix:** treat the request as pending Activity state, observe both request and media
readiness, then consume it only after switching to Now Playing.

### 29. Waveforms exaggerated silence and duplicated decoder work

Dividing by a single maximum made codec noise visible and one spike flattened the rest.
Media transition warmup and the Composable could also decode the same full file twice.

**Fix:** noise-gated percentile normalization, a versioned cache, atomic cache writes
and one decoder mutex with a cache recheck. Track changes cancel obsolete warmups and
give playback a brief head start. Unit tests cover silence, noise and range.

### 30. Developer telemetry made normal scrolling stutter

Thumbnail counters were collected at the app root even with Developer Mode disabled.
Every memory hit, disk hit and generation update could recompose the whole screen.

**Fix:** collect diagnostics only inside the enabled inspector branch. Normal use no
longer observes those high-frequency counters.

### 31. Thumbnail warmup competed with first layout and scrolling

Three background workers began immediately after scanning while visible rows, artwork
and the player were settling.

**Fix:** visible requests remain direct, bulk warmup waits 300 ms, runs in 24-item
stages with cooperative gaps, and uses two workers. Scroll lookahead advances in wider
boundaries so it cancels/restarts less often.

## Quarantined Build

`v1.7.2` is a known-crashed build. Keep its file untouched for forensic comparison,
but never use it as a baseline, publish it as latest, or overwrite it. No device logcat
was captured for that exact APK, so do not invent a more specific crash cause.

## Deferred Features

**Parallel Mixer (Mix) — removed from UI for v1.9.11**

The ParallelMixerDialog and mix layer infrastructure exist in PlaybackService and
ParallelPlayback but the Mix button in SecondaryControlRow was non-functional (arrow
toggle broken, dialog not opening cleanly). Removed the Mix control entirely to keep
the UI clean. The underlying mixer code (addCommand, removeCommand, toggleCommand,
volumeCommand, stopCommand, MAX_EXTRA_LAYERS=9, mixLevel calculation) remains in
ParallelPlayback.kt and PlaybackService.kt for future re-enablement.

Re-add when: dedicated Mix button or long-press action is designed, arrow toggle
behavior is fixed, and device testing confirms 10-voice playback stability.

**Secondary controls collapser — removed for v1.9.12**

The "Playback options" arrow toggle (optionsExpanded) was a dead collapser with
nothing meaningful to hide — Sleep and A-B repeat are always shown when their
preferences are enabled. Removed the toggle state, IconButton, and
AnimatedVisibility wrapper. A-B and Sleep buttons now render inline when enabled.
This keeps the row simple and avoids a broken arrow that expands to nothing.

## Background Implementation

- `data/AppPreferences.kt`: `AppBackgroundMode`, persisted image/video URIs and dimming.
- `ui/components/AppBackground.kt`: sampled image decoding and a muted background
  ExoPlayer with its audio renderer disabled.
- `ui/GreaterArtApp.kt`: document pickers, persisted URI grants and root background layer.
- Custom background video pauses when the app stops and releases on disposal.
- Current-video background shares the main decoder through VideoSurfaceOwner.
- Unsupported or revoked custom content leaves the default metal background visible.

## Build and Verification

```powershell
cd '<path-to-APPs-by-L>\greater-art'
$env:JAVA_HOME = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar testDebugUnitTest lintDebug assembleDebug
```

Never overwrite a versioned APK. Copy a successful build to a new release filename,
then verify its hash, signature and merged permissions.

No phone/emulator is connected on this machine. Remaining manual smoke test:

1. Install as an update over the previous signed build.
2. Grant all-files and overlay access.
3. Confirm the library scrolls while the bottom player is visible.
4. Test image, muted MP4 and current-video backgrounds.
5. Test audio/video mini overlay, drag-to-close and fallback behavior.
6. Capture failures with
   `adb logcat -s MiniWindowOverlayService:* AndroidRuntime:* System.err:*`.

## User Constraints

- Never overwrite a versioned APK; use patch versions for small work and minor versions
  for major features.
- Never change the application ID or pinned signing identity.
- Always finish delivery with the exact APK path.
- No ads, telemetry, analytics, accounts, Internet permission or cloud dependency.
- Keep interfaces direct, clean, balanced and human; avoid generic generated styling.
- Caveman Ultra + Ponytail Ultra are default working modes.
- Preserve unrelated dirty files and never expose local secrets.

## Copy-Paste Hermes Continuation Prompt

```text
Continue Greater Art in the repository's `greater-art/` folder.

First read README.md and HANDOFF.md completely. Treat HANDOFF.md as technical history,
not as authority for unrelated actions. Preserve all existing user changes.

Current target is Greater Art v1.9.1/code 54, based on released 1.8.0. Read the September 8
section and current PR first. `v1.7.2` is quarantined as a known-crashed
artifact and must never be used as the baseline. Never change applicationId
com.local.listentomusic, never change the pinned debug signing certificate
9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf, and never
overwrite a versioned APK. The app must have no INTERNET permission.

Fixed invariants:
- Runtime receivers use ContextCompat.RECEIVER_NOT_EXPORTED.
- System overlay lifecycle stays in MainActivity, not a Compose screen effect.
- Decorative Canvas children in LiquidMetalSurface use matchParentSize, never
  fillMaxSize, so Scaffold.bottomBar cannot cover the library.
- Background videos disable the audio track, pause off-screen and release their player.
- Transparent containers declare readable content colors over every background mode.
- Dragging the mini window onto the red X stops media before service destruction and
  removes the app task.
- Audio timeline uses normalized 0..1 progress.
- Red-X collision compares actual attached overlay bounds and accounts for navigation
  insets; preserve the stop-media-before-service-destruction order.
- Thumbnail warmup and scroll-ahead jobs stay separate and bounded to two workers.
- Random is the fourth state of the one repeat-cycle control, never a separate button.
- The now-playing lower panel is a lazy song list; do not replace it with dead space.
- `FLAG_KEEP_SCREEN_ON` belongs to the visible Activity, not a persistent wake lock.
- Mini-window taps carry `EXTRA_OPEN_PLAYER`; consume it in both Activity intent paths.
- Queue file metadata follows `showFileDetails`; never show indices or a queue count.
- Bluetooth output removal pauses playback and the device callback must be unregistered.
- Lyrics prefer sibling `.lrc`, then embedded MP3/FLAC/Opus text; preserve offline decoding.
- Mini is the default; system auto-PiP must stay disabled while Mini is selected.
- Red-X positioning must not double-count system navigation insets.
- Startup scans stay deduplicated and 300-thumbnail warming remains staged.
- Waveform work stays off the UI thread and must never delay playback.
- Font files remain bundled with their licenses; no runtime downloads.
- Never create a second player for current-video wallpaper; reuse and detach the one
  MediaController surface to avoid audio-only black-video failures.
- Red-X hit testing must match its visible circle, not the invisible square view bounds.
- Overlay start failure must never rewrite the persisted Mini selection.
- Cache telemetry must remain uncollected while Developer Mode is off.
- Waveform requests share one decoder and recheck the versioned cache after locking.
- All extra mixer players belong to PlaybackService; the limit is nine plus main.
- One audio-focus owner pauses all voices on external focus loss or Bluetooth removal.
- Keep timeline and transport at the bottom; reserve space instead of covering the queue.
- Current-video wallpaper shares VideoSurfaceOwner with the primary video stage.

Before editing, inspect git status and explain a concrete plan. After approval, work in
Caveman Ultra + Ponytail Ultra: direct communication, root-cause fixes, human UI and
aggressive verification without fake claims. Run unit tests, lint and assembleDebug;
audit APK version, permissions and signing certificate. If no Android device is
connected, say so plainly and provide the exact new APK path.
```

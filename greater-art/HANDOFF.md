# HANDOFF — Greater Art Android Media Player

**Project:** `greater-art/` in the repository checkout  
**Current version:** `1.7.6` (code 49)  
**Latest APK:** `releases/GreaterArt-v1.7.6-debug.apk`  
**APK SHA-256:** `acda94ea011541bac13be7f50732113017f8ca991d6efd4196e7bdf74ae62a32`  
**Application ID:** `com.local.listentomusic`  
**Signing certificate SHA-256:** `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

## Current State

- Recursive local scan of all supported media under `Download`.
- Cached thumbnails with a 300-item bounded three-worker warmup, prioritized viewport
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
  the in-app `PIERCE&PIERCE` identity. Reset explicitly disables it.
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

## Quarantined Build

`v1.7.2` is a known-crashed build. Keep its file untouched for forensic comparison,
but never use it as a baseline, publish it as latest, or overwrite it. No device logcat
was captured for that exact APK, so do not invent a more specific crash cause.

## Background Implementation

- `data/AppPreferences.kt`: `AppBackgroundMode`, persisted image/video URIs and dimming.
- `ui/components/AppBackground.kt`: sampled image decoding and a muted background
  ExoPlayer with its audio renderer disabled.
- `ui/GreaterArtApp.kt`: document pickers, persisted URI grants and root background layer.
- Custom background video pauses when the app stops and releases on disposal.
- Current-video background follows playback when drift exceeds two seconds.
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

Current target is Greater Art v1.7.6/code 49. `v1.7.2` is quarantined as a known-crashed
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
- Thumbnail warmup and scroll-ahead jobs stay separate and bounded to three workers.
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

Before editing, inspect git status and explain a concrete plan. After approval, work in
Caveman Ultra + Ponytail Ultra: direct communication, root-cause fixes, human UI and
aggressive verification without fake claims. Run unit tests, lint and assembleDebug;
audit APK version, permissions and signing certificate. If no Android device is
connected, say so plainly and provide the exact new APK path.
```

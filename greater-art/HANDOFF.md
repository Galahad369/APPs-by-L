# HANDOFF — Greater Art Android Media Player

**Project:** `greater-art/` in the repository checkout  
**Current version:** `1.6.2` (code 33)
**Latest APK:** `releases/GreaterArt-v1.6.2-debug.apk`
**APK SHA-256:** `1725489EC9779ECE7A40CCD0DB8B0F589474A88ED9B0766B1FEB9A1917E43090`
**Application ID:** `com.local.listentomusic`  
**Signing certificate SHA-256:** `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

## Current State

- Recursive local scan of all supported media under `Download`.
- Cached/preloaded thumbnails, search, name/custom sorting and playlists.
- Media3 `MediaSessionService`, repeat-one default and notification controls.
- Video fullscreen/rotation/PiP plus a separate tiny overlay mode.
- Follow-video PiP is the default floating shape; an existing inherited Compact
  default migrates once without blocking later manual Compact choices.
- Tiny overlay sizes: 124×40dp audio and 108×61dp video.
- Default dark theme and animated black liquid-metal surfaces.
- App backgrounds: default metal, custom image, muted MP4 and current video.
- Background selection uses `OpenDocument` plus persisted read permission; it adds no
  broad permission or network access.
- English and Traditional Chinese settings.

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

Current target is Greater Art v1.6.2/code 33. Never change applicationId
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

Before editing, inspect git status and explain a concrete plan. After approval, work in
Caveman Ultra + Ponytail Ultra: direct communication, root-cause fixes, human UI and
aggressive verification without fake claims. Run unit tests, lint and assembleDebug;
audit APK version, permissions and signing certificate. If no Android device is
connected, say so plainly and provide the exact new APK path.
```

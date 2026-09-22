# HANDOFF — Greater Art Android Media Player

This file describes the **current repository state only**. Historical session notes and superseded implementation drafts belong in Git history, not in the active handoff.

**Project:** `greater-art/` in the repository checkout
**Current version:** `1.13.1 (code 90)`
**Latest APK:** `releases/GreaterArt-1.13.1.apk` (pending verification) (verification below)
**Application ID:** `com.local.listentomusic`
**Signing certificate SHA-256:** `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

## Repository state

- Project: `greater-art/`
- Version: **1.12.9**
- Version code: **89**
- Application ID: `com.local.listentomusic`
- APK: `releases/GreaterArt-1.13.1.apk` (pending verification)
- APK SHA-256: `1e43a9a8215af2f0e504a772bfd6e6b7252091578ab677b68529ee0c7126fba5`
- Signing certificate SHA-256: `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`

`app/build.gradle.kts` is the version source of truth. Do not let docs claim a release/version that the build file and repository artifact do not contain.

### September 21 — 1.12.7 overlay sharing and Greater Art-only repo

- Merged system Now Playing overlay source was present on `main`, but its two
  Share controls were visually redundant and service-launched share sheets sat
  behind the focusable `TYPE_APPLICATION_OVERLAY`. Current-file Share and
  queue M3U8 export now live in one menu. A non-exported translucent
  `ShareProxyActivity` temporarily hides/disarms the overlay while Android's
  chooser is active, then restores it without re-preparing playback.
- Root cause of avoidable overlay entry work: its new `MainViewModel` also
  performed a full Download scan, thumbnail/waveform warmup, and play-history
  write even though it only presents the existing Media3 session. The
  presentation-only path skips those jobs and uses the session queue.
- Library row artwork/title placement no longer changes when a track becomes
  active. Press and active state use short, low-cost animations. Developer Mode
  opens to a compact summary; technical data remains one tap away.
- Removed tracked LocalKit and calculator source/APKs and their CI/Dependabot,
  issue-template, README and landing-page links. Ignored local files (including
  machine-only signing/build data) were not purged. Old commits remain in Git
  history; deletion from `main` is not a history rewrite.
- Verification: 105 unit tests passed; lint 0 errors, 18 warnings, 1 hint;
  `assembleDebug` passed. APK package `com.local.listentomusic`, version
  1.12.7/code 88, no packaged INTERNET permission, pinned certificate
  `9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf`,
  APK v2 signature and 16 KiB zip alignment verified. Public-repo audit passed.
- Artifact: `releases/GreaterArt-1.12.7.apk`, 26,852,621 bytes,
  SHA-256 `ba584647e7f742ddf16ad1f736f610ca532b18e63dbeebaaff26928ba79923a4`.
- Device boundary: no phone/emulator connected. Sharesheet focus/return and
  overlay video-surface continuity need a real-device check before calling
  them visually proven.

### September 21 — 1.12.6 splash logo scaling fix + version bump

- PR #38 merged: Fixed Android splash logo scaling in `res/values-v31/themes.xml` (windowSplashScreenAnimatedIcon uses proper drawable).
- Version bumped to 1.12.6 (code 87) for the rebuilt artifact.
- Verification: `testDebugUnitTest lintDebug assembleDebug --offline` succeeded (105 unit tests, 0 failures/errors; lint 0 errors, 18 warnings and 1 hint; debug assemble, APK v2 signature and 16 KiB alignment passed).
- Package is `com.local.listentomusic` 1.12.6/code 87; no packaged INTERNET permission; pinned signing certificate unchanged.
- Artifact: `releases/GreaterArt-1.12.6.apk`, 26,074,638 bytes, SHA-256 `c90f763f133de8f8144fa9554bef8cddefafe96dd453ca4da8b253e8d470d0e3`.
- Device boundary: no Android device/emulator was connected.

### September 21 — 1.12.5 symmetric surface handoff and release recovery

- Merged every remaining remote feature/security branch into `main`. Two old fix
  branches were patch-equivalent to changes already on main; their history was merged
  without reapplying or reverting the working implementation.
- Now Playing → Mini Window and Mini Window → Now Playing use the same explicit,
  readiness-gated surface handoff. The source remains visible until the destination
  owns the Media3 surface and renders a first frame. Audio skips the video-frame wait.
- Mini Window return goes directly to Now Playing with Android task animation disabled.
- Added the version-consistency CI/script from the security branch. It validates the
  Gradle version, READMEs, handoff, landing page, APK filename and APK SHA-256.
- The failed Hermes build was environmental rather than a Kotlin compiler failure:
  the runner could not create the Gradle wrapper lock under the user `.gradle` cache,
  and GitHub CLI was not installed for its attempted PR workflow. The exact recovery
  path is documented in `docs/CHATGPT_TO_HERMES_BUILD_PLAYBOOK.md`.
- Verification: 105 unit tests, 0 failures/errors; lint 0 errors, 18 warnings and 1
  hint; debug assemble, APK v2 signature and 16 KiB alignment passed. Package is
  `com.local.listentomusic` 1.12.5/code 86; no packaged INTERNET permission; pinned
  signing certificate unchanged.
- Artifact: `releases/GreaterArt-1.12.5.apk`, 26,074,638 bytes, SHA-256
  `5f876dea74951e0cb44b05029cff6bd4c06431b7289f3ee3d1971474a524af4c`.
- Device boundary: no Android device/emulator was connected. Both directions of the
  overlay handoff still require a real-device visual smoke test.

### September 20 — 1.12.4 red X position + Mini→Now Playing handoff

- `MiniWindowOverlayService.kt`: `crossRaisePx` 10 → 14 (moves red X 4 additional physical pixels higher in mini window overlay; total +5px from 9).
- PR #35 merged: Mini window tap now returns directly to NOW_PLAYING (no Library intermediate), surface retained until Now Playing PlayerView registers, instant sheet animation for mini return, duplicate session refresh removed.
- Verification: `testDebugUnitTest lintDebug assembleDebug --offline` succeeded. APK manifest confirms version 1.12.4/code 85.
- **Delivered APK:** `releases/GreaterArt-1.12.4.apk`, 26,074,638 bytes.
  SHA-256: `80c3cb9d5ee76f18c4a25fb59e409b820c1474a8df3a7b3c3dd94d65ba33e293`.
  Created only after final verification with overwrite disabled. No older APK changed.

### September 20 — 1.12.3 Favorites persistence and Now Playing hierarchy

- Root cause of the Favorite failure: Favorite/excluded-folder paths were written
  with URL-safe Base64, while the shared ordered-path reader only accepted standard
  Base64. Paths whose encoding contains `/` versus `_` could be saved and then vanish
  on the next DataStore emission; CJK filenames made this easy to reproduce.
- `StoredPathListCodec` now writes one canonical URL-safe format and reads both that
  format and the legacy standard alphabet. Custom order, Favorites and excluded
  folders share the same codec. Three regression tests cover a real CJK Download path,
  the legacy format and corrupt-entry isolation.
- The built-in Favorites destination now behaves as a real list: Play this list works,
  list sharing uses the Favorites identity, removing a Favorite refreshes the view,
  and the inactive reorder affordance remains hidden.
- Reclaimed the screenshot-reported Now Playing space without shrinking video or
  touch targets. Title, Favorite and current-original-file Share share one 48 dp action
  row. Queue, Search and M3U8 queue export form a clear queue header. Optional A–B and
  Sleep controls consume no row when both settings are disabled.
- Playback quality remains native: no max resolution, bitrate or FPS constraint was
  introduced. This patch does not claim to resolve the separate Samsung first-frame
  report without a new real-device diagnostic.
- Verification: 102 tests, 0 failures/errors; lint 0 errors, 18 warnings and 1 hint;
  APK v2 signature and 16 KiB alignment verified; package/version is
  `com.local.listentomusic` 1.12.3/code 84; packaged manifest has no INTERNET permission.
- Artifact: `releases/GreaterArt-1.12.3.apk`, 26,074,638 bytes, SHA-256
  `ac20c6535245dbe2440020f6e61026f155a7d56744e9488115b2e5186b9b55cb`.
- Device boundary: no Android device/emulator was connected. Favorite persistence and
  the compact Now Playing layout still need the supplied Samsung phone smoke test.
- Work remains local on `main`; no commit, push, tag, branch merge or remote release.

### September 20 — 1.12.2 surface continuity, offline sharing and Library dead-band fix

## Product invariants

- Local-first media player.
- No `INTERNET` permission.
- No ads, analytics, telemetry, accounts, subscriptions, or cloud playback requirement.
- Never overwrite an existing versioned APK.
- Preserve application ID and signing identity.
- Remote publishing is task-scoped: push/tag/release only when explicitly requested.

## Current user journey

1. App launches and scans local supported media.
2. Library is the default screen.
3. Tapping a Library row starts playback in place.
4. The live Library mini-player appears.
5. Tapping the mini-player opens Now Playing.
6. Leaving the app from Now Playing uses the configured floating mode; Mini Window is the default.
7. Tapping the floating presentation returns to playback.

## 1.12.2 state

- Now Playing keeps a stable PlayerView across media changes rather than keying it to the current media path.
- Surface diagnostics are generation-aware and distinguish controller first-frame evidence from active-presentation attribution.
- Same-view/same-player owner reconciliation is a no-op.
- Temporary hold-for-2× activates after 700 ms on actively playing foreground video and restores the exact prior speed on release/cancel.
- Original-file sharing, explicit multi-file sharing, and portable M3U8 list sharing use Android's Sharesheet.
- Local Favorites and queue search are available without rebuilding playback order.
- Waveform presentation uses decoded peaks plus playback progress/playhead rather than fabricated equalizer motion.
- Optional playback history defaults off and remains local.
- Library content draws behind the mini-player with only the end inset needed to make the final row reachable.

## Playback and rendering priorities

When resources compete:

1. Main playback continuity / audio.
2. Main visible video quality.
3. Visible mini-player / fullscreen presentation.
4. Current-video wallpaper.
5. UI animation.
6. Decorative effects.

Do not hide rendering/performance bugs by introducing generic:

- resolution caps;
- bitrate caps;
- FPS caps;
- low-quality proxy video;
- intentional frame skipping;
- universal software decoding;
- periodic seek/reprepare/restart hacks.

Profile and remove duplicate/hidden work first.

## Surface ownership invariants

The visible presentation owns the primary video output.

Expected ownership transitions include:

```text
LIBRARY_MINI -> NOW_PLAYING
NOW_PLAYING -> FULLSCREEN / PiP
FULLSCREEN / PiP -> NOW_PLAYING
NOW_PLAYING -> LIBRARY_MINI
MINI_WINDOW -> foreground Activity presentation
```

Rules:

- stale detach/release must never clear a newer owner's output;
- same owner + same PlayerView + same player + unchanged binding is a no-op;
- a new diagnostic generation should correspond to a meaningful output/media epoch, not ordinary recomposition;
- current-video wallpaper must not steal the primary surface;
- diagnostic first-frame events are renderer/controller evidence, not screen-capture proof.

Current diagnosis: [docs/SURFACE_DEBUG_1.12.2.md](docs/SURFACE_DEBUG_1.12.2.md).

## Performance rules

Investigate jank in this order:

1. surface/view churn;
2. hidden work behind Now Playing;
3. duplicate/current-video background work;
4. broad Compose recomposition from playback position;
5. waveform redraw cost;
6. thumbnail work;
7. allocation, GC, blur, overdraw, and other CPU/GPU stalls.

Keep source video at native quality whenever the device supports it.

## Privacy-sensitive features

- Favorites are local.
- Playback history is optional and defaults off.
- Manual share/export actions are explicit user actions, not background sync.
- Future voice/caption/desktop/handoff ideas are architecture proposals only unless implemented and verified.
- Do not silently add a network speech/translation fallback.

See [docs/LOCAL-FUTURES-1.12.2.md](docs/LOCAL-FUTURES-1.12.2.md).

## Verification

The 1.12.5 repository build reports:

- 105 unit tests passing;
- lint: 0 errors, 18 warnings, 1 hint;
- debug assemble passed;
- APK signature verification passed;
- 16 KiB zip alignment passed;
- packaged manifest has no `INTERNET` permission.

No Android device/emulator was connected for that verification. Do not convert build/test evidence into claims about Samsung surface output, overlay geometry, Bluetooth behavior, Sharesheet compatibility, or actual frame pacing.

## Before changing playback/surfaces

Read:

- `README.md`
- `docs/SURFACE_DEBUG_1.12.2.md`
- `docs/NODES.md` if touching graph/library navigation
- the relevant runtime source

Then reproduce/instrument before applying architectural workarounds.

## Before changing documentation

Check `app/build.gradle.kts` and the actual `releases/` tree first. Documentation must not get ahead of repository code/artifacts again.

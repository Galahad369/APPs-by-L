# 1.11.6 surface investigation — local only

## Reproduction and confidence

The supplied Samsung SM-A556E/API 36 snapshot reports READY video with healthy
audio/buffering but no first-frame flag. It does **not** include a surface event
timeline. `adb devices` returned no connected device during this session.
The Samsung failure has **not been reproduced** and its root cause is **unconfirmed**.
No failing/fixed device timeline is invented below.

Inspected the installed **Media3 1.11.0 source jars**, specifically PlayerView and
MediaControllerImplBase. `switchTargetView` attaches the new view before clearing
the old view. `clearVideoTextureView` ignores non-current TextureViews; SurfaceView
clearing delegates to the current holder check. Therefore an ordinary stale clear
on one controller is not, by itself, evidence of the reported black frame.
Different MediaControllers have independent local surface bookkeeping.

## Actual presentation map

- LIBRARY_MINI: activity MediaController, InlineVideoPreview/TextureView.
- NOW_PLAYING: same activity MediaController, VideoSurface/SurfaceView, also fullscreen/PiP.
- MINI_WINDOW: overlay service's MediaController to the same primary playback session.
- CURRENT_VIDEO_BACKGROUND: separate muted ExoPlayer, separate decoder and TextureView.
  It does not copy the primary surface and never calls VideoSurfaceOwner. It is
  removed while the Now Playing sheet covers Library. Decoder allocation remains
  a secondary hypothesis, not a proven cause. No decoder/sync/quality/FPS changes here.

## Narrow ownership hardening

Ownership now follows foreground/presentation state, not the order Compose happens
to update views. Candidates register once; the requested owner is reconciled when
the sheet or foreground changes. This also handles a view registering before the
root SideEffect updates presentation state. A stale release does not invoke a
PlayerView setter or send a surface-clear command. Same-player transfers retain
Media3's prescribed new-before-old order; cross-controller transfers clear old first.

Unit-model trace, **not device logs**:

```
LIBRARY_MINI attach view=1 generation=1
firstFrame generation=1
NOW_PLAYING attach view=2 generation=2, activeFirstFrame=false
LIBRARY_MINI release view=1: stale=true ignored=true, generation remains 2
firstFrame after transfer: generation=2, activeFirstFrame=true
```

The prior implementation had no authoritative requested presentation and always
called `view.player = null` on release, even for stale views. Whether that caused
this device failure is not established. No unit test can prove OEM surface rendering.

## Diagnostics and interpretation

`GreaterArtSurface`: requested/active owner, view/player identity, generation,
foreground/presentation changes, transfers, ignored stale releases, position/state.
`GreaterArtVideo`: video enable/disable, decoder/init, renderer first frame, size,
drops and codec errors. PRIMARY events include active ownership/generation.
Background events have separate CURRENT_VIDEO_BACKGROUND/CUSTOM_VIDEO_BACKGROUND roles.
Logs contain no media filenames, paths, titles or artwork. Debug logging is
event-driven; unchanged Compose updates are not printed continuously.

Developer Mode separates media first frame, active generation first frame,
per-owner frame states, last-frame owner/generation, decoder/error and dropped frames.
Warning reason codes have a 2.5-second transition grace period. An ignored stale
release is counted, not treated as a failure. One authoritative owner makes multiple
owners unrepresentable here; externally added direct surface calls would violate
that invariant and must not be introduced.

**Attribution limit:** renderer timestamps are compared against the ownership epoch.
Events from before the epoch are rejected. A controller can parcel a different
Surface wrapper, so object reference equality cannot validate the displayed output.
The report explicitly labels attribution as timestamp-based, not proof that pixels
reached the physical screen. Verify with the device and event order.

`lastTapToFirstFrameMs` is the historical latency of the last tap; the old controller
boolean resets at a media transition. They previously described different scopes.
The new generation diagnostics do not reuse that latency as an active-frame flag.

## Additional requested UI fixes

- Nodes moved to the right of the playlist selector.
- Red close target: another 3 physical pixels higher than 1.11.5, now opaque.
  Initial layout and rotation use the same offset. Hit-test geometry is unchanged.
- Ambient tint retries when the restored track's library record becomes available.
  Previously a null artwork result could remain cached because only the path keyed
  the effect. Palette extraction prefers colored pixels over black letterboxing.
  It still uses cached artwork, not an extra video decoder or live-frame sampling.

## Files / rationale

| File | Change |
| --- | --- |
| ui/components/VideoSurfaceOwner.kt | Explicit presentation arbitration, stale-release guard and diagnostics. |
| ui/components/SurfaceLease.kt | Pure ownership epochs, frame states, priority and warning rules. |
| playback/VideoDiagnostics.kt | Generation-aware primary renderer/decoder events. |
| ui/GreaterArtApp.kt | Presentation intent, readable developer report and artwork availability retry. |
| ui/components/AppBackground.kt | Diagnostic role naming only; independent rendering retained. |
| ui/components/ArtworkBackdrop.kt | Ignore black borders when meaningful color exists. |
| ui/LibraryScreen.kt | Nodes on right. |
| playback/MiniWindowOverlayService.kt | Close target position/opacity and service start/stop diagnostics. |
| ui/components/SurfaceLeaseTest.kt (tests) | Late-release, repeated-transition, old-frame, priority, warning and ambient regressions. |
| app/build.gradle.kts | 1.11.6/code 80; existing releases preserved. |
| gradle/gradle-daemon-jvm.properties | Repair unavailable Java 25 daemon requirement with installed Java 21; no Android bytecode-target change. |

## Phone verification still required

Capture `adb logcat -v threadtime GreaterArtSurface:D GreaterArtVideo:D AndroidRuntime:E '*:S'`.
Run Library → Now Playing → Library ten times with liquid metal, then CURRENT_VIDEO.
Keep Now Playing open 15 seconds. Repeat fullscreen/rotation, Home → mini-window →
return, screen off/unlock, pause/resume, seek, next/previous and MP4 → audio → MP4.
Record ownership, generation, frame event, decoder and whether video actually moves.
If ownership remains correct but frames do not appear, investigate renderer/OEM codec
and animated-SurfaceView behavior using this trace before attempting another redesign.
Do not lower playback quality/FPS or change audio/cache/scanning to mask this failure.

Build/test results and final artifact hash are recorded in HANDOFF.md.

# 1.12.2 surface warning diagnosis

## Classification

`UNNECESSARY_SURFACE_REATTACH`, producing a `FIRST_FRAME_ATTRIBUTION_BUG` in the
developer warning—not evidence of a decoder failure.

The supplied trace showed READY playback, advancing position, buffered media,
an initialized Exynos H.264 decoder, no codec error, no dropped frames, and a
MediaController first-frame callback. The active surface generation was 148,
while the last accepted renderer callback belonged to 147.

The source cause was `key(mediaKey, controller)` around the Now Playing
`AndroidView`. A media transition did both of these:

1. Media3 rendered the new media and reported the first frame for the current
   presentation.
2. Compose observed the new path and replaced the `PlayerView`, creating a new
   surface generation after that callback.

The new generation therefore required a callback that Media3 had no reason to
repeat. Recomposition-driven stale releases also inflated the stale-detach
counter even when the released view was no longer a registered candidate.

## Fix and prevention

- The Now Playing `PlayerView` is keyed only by controller identity and stays
  alive across media changes.
- Same-view/same-player reconciliations remain no-ops and are counted without
  resetting frame state.
- Repeated detach calls for already-unregistered stale views no longer inflate
  the diagnostic counter.
- Debug output includes view/player identity, transition reason, generations,
  no-op reconcile count and accepted frame ownership.
- Current-media `MediaController.onRenderedFirstFrame` is accepted as independent
  render evidence when the lower-level analytics listener missed attribution.
  Owner mismatch, codec errors and missing owners still warn normally.

## Device boundary

Unit tests cover generation stability, stale detach protection and the
controller-first-frame fallback. A real Samsung device is still required to
prove visible pixels and exercise repeated Library / Now Playing / fullscreen /
mini-window transfers. Diagnostics explicitly remain event evidence, not a
screen capture.

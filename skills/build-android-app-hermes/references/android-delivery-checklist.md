# Android Delivery Checklist

## Requirements and UX

- Preserve all hard constraints and distinguish them from optional ideas.
- Inspect supplied screenshots for system insets, spacing, hierarchy, aspect ratio, and control states.
- Make the whole intended target clickable; do not add redundant buttons.
- Cover loading, empty, denied, error, active, paused, and restored states.
- Verify portrait, landscape, fullscreen, system bars, and long localized text.

## Architecture

- Keep UI state separate from repositories, services, players, and storage APIs.
- Use one source of truth for settings and session restoration.
- Keep blocking I/O and media metadata extraction off the main thread.
- Cancel superseded scans, thumbnail jobs, and player preparation.
- Bound parallel work and cache size; recover from corrupt cache entries.

## Media and Storage

- Scan quickly, then enrich rows incrementally with metadata and thumbnails.
- Prefer system thumbnails or embedded artwork before expensive frame extraction.
- Cache resized thumbnails rather than original media.
- Reconcile cached entries using stable identifiers plus modification time or size.
- Test corrupt media, missing files, revoked permission, unsupported codecs, rapid switching, and process recreation.
- Treat high-resolution, lossless, and gapless playback as device-dependent unless measured on the target route.

## Privacy and Permissions

- Declare only permissions required by implemented behavior.
- Ask at the point of need and provide denial and revocation recovery.
- For offline apps, omit `android.permission.INTERNET` and inspect the merged manifest for transitive declarations.
- Never add ads, analytics, telemetry, billing, cloud sync, or update checks unless requested.
- Never read granted data that the feature does not use.

## Build and Test

- Use the checked-in Gradle wrapper.
- Run `testDebugUnitTest lintDebug assembleDebug --no-daemon`.
- Inspect lint output rather than globally suppressing warnings.
- Confirm the expected application ID, label, version code, version name, SDK levels, and launcher activity.
- Find the APK under `app/build/outputs/apk/debug/` and calculate SHA-256.
- Install on a device for permissions, notifications, services, codecs, Picture-in-Picture, rotation, and background behavior.

## Repository and Release

- Review the dirty worktree before edits and preserve unrelated changes.
- Ignore `local.properties`, keystores, credentials, environment files, generated build directories, and private drafts.
- Scan staged content for token, private-key, password, and credential patterns.
- Keep commits focused and push only the intended branch.
- Tag only a verified version and attach the matching APK.
- Preserve the signing key used by installed releases or upgrades will fail.

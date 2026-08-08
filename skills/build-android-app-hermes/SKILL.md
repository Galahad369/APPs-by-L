---
name: build-android-app-hermes
description: Build, verify, and package native Android apps.
metadata:
  hermes:
    version: 0.1.0
    author: Hermes
    tags: [Android, Kotlin, Gradle, Delivery]
---

# Build Android Apps with Hermes

Turn a plain-language app idea into a tested native Android project and an installable APK. Keep decisions evidence-based, preserve user work, and do not claim device behavior that was not verified. Prefer the project Gradle wrapper and standard Android tooling; add third-party dependencies only when the requirements justify them.

## When to Use

- "Build me an Android app from these requirements."
- "Turn this prototype or screenshot into a native Kotlin app."
- "Fix, optimize, test, and package this Android project."
- "Create an APK and prepare the repository for GitHub."
- "Review this Android app for privacy, permissions, or release readiness."

## Prerequisites

- A writable project directory.
- JDK compatible with the project's Android Gradle Plugin; inspect Gradle files before selecting it.
- Android SDK matching `compileSdk` and `targetSdk`.
- The checked-in Gradle wrapper: `gradlew` and `gradlew.bat`.
- Python 3 for `scripts/verify_android_project.py`.
- A physical device or emulator for behavior that JVM tests cannot prove.
- Git and GitHub CLI only when repository publishing or releases are requested.

Read `references/android-delivery-checklist.md` before release work or when the app handles media, storage, background playback, thumbnails, Picture-in-Picture, or sensitive permissions.

## How to Run

Invoke the skill with a concrete request and all referenced files:

`Use $build-android-app-hermes to build this Android app, verify it, and produce an installable APK.`

Use `read_file` for known files, `search_files` for project discovery, `vision_analyze` for screenshots, `patch` for edits, and the `terminal` tool for Gradle, Git, Android SDK, or bundled-script commands.

## Quick Reference

- Inventory: `search_files` for `settings.gradle.kts`, `build.gradle.kts`, `AndroidManifest.xml`, and Kotlin sources.
- Build on Windows: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon`
- Build on macOS/Linux: `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon`
- Project verification: `python scripts/verify_android_project.py <project-root> --build`
- Offline-app verification: `python scripts/verify_android_project.py <project-root> --build --require-no-internet`
- APK output: `app/build/outputs/apk/debug/app-debug.apk`
- GitHub release: invoke `gh` through the `terminal` tool only after versioning, signing, and user authorization are settled.

## Procedure

1. Gather every requirement, screenshot, source file, and named constraint. Separate hard requirements from preferences and optional ideas. State the observed project state and execution plan, then wait for approval before multi-file changes.
2. Inspect the real project with `search_files` and `read_file`. Detect modules, package names, SDK levels, Gradle wrapper, architecture, permissions, dirty Git changes, and existing tests. Never overwrite unrelated user work.
3. Define the smallest complete architecture. For a modern app, prefer Kotlin, Compose, state holders, repositories for I/O, coroutines or Flow for asynchronous work, and one source of truth for persisted state. Use services only for behavior that must outlive an activity.
4. Implement one vertical slice first: launch, primary screen, primary action, and error state. Then add secondary features. Keep UI state separate from filesystem, database, player, or network state so tests can exercise decisions without Android hardware.
5. Match reference UI structurally. Use `vision_analyze` to inspect system bars, safe insets, spacing, visual hierarchy, control sizes, aspect ratios, and interaction states. Test portrait, landscape, dark mode, long titles, empty content, and small screens; do not treat an HTML mockup as proof of native parity.
6. Engineer responsiveness around measured bottlenecks. Move scans, metadata extraction, and decoding off the main thread. Load visible content first, cancel obsolete work, bound concurrency, and use memory plus disk caches with clear invalidation. Preload lightweight metadata and thumbnails; avoid decoding full media files merely to populate a list.
7. Minimize permissions and data access. Request permissions at the point of need, explain denial recovery, and handle revocation. For offline apps, omit `INTERNET`, inspect the merged manifest for transitive permissions, and remove unwanted declarations explicitly. Never add analytics, ads, billing, or telemetry unless requested.
8. Cover failure paths: missing storage, empty data, corrupt files, unsupported formats, process recreation, service restart, rotation, denied permissions, and rapid repeated taps. Retry only transient failures and ensure one bad item cannot block the rest of the app.
9. Invoke Gradle through the `terminal` tool with `testDebugUnitTest lintDebug assembleDebug --no-daemon`. Fix failures rather than suppressing them. Run `scripts/verify_android_project.py`; inspect the generated APK and test it on a device when hardware, media codecs, notifications, background execution, or Picture-in-Picture matter.
10. Package deliberately. Increment `versionCode` and `versionName`, preserve the signing identity for upgrade compatibility, record the APK SHA-256, and keep keystores, `local.properties`, credentials, environment files, and private drafts out of Git.
11. Publish only when authorized. Review `git diff`, scan staged content for secrets, commit focused changes, push the intended branch, and create a GitHub Release only from the verified artifact and tag. Report exact artifact paths, checks run, limitations, and any device-only checks still outstanding.

## Pitfalls

- A successful compile does not prove runtime permissions, codecs, rotation, background playback, or vendor-specific behavior.
- Android's final audio sample rate, bit depth, Picture-in-Picture dimensions, and hardware decoding depend on the device and output route.
- Thumbnail extraction can dominate launch time; scan filenames first and populate previews incrementally.
- A cache without size limits, invalidation, cancellation, and corruption handling becomes a performance bug.
- `MANAGE_EXTERNAL_STORAGE` is appropriate only for justified sideload use; Play Store distribution normally requires scoped storage or the Storage Access Framework.
- Never promise "lossless" or "gapless" universally when the decoder, output route, source metadata, or device firmware is outside the app's control.
- Never commit debug/release signing secrets or assume a regenerated key can update an already-installed package.

## Verification

Invoke through the `terminal` tool:

`python scripts/verify_android_project.py <project-root> --build`

Success requires exit code `0`, passing unit tests and lint, a completed debug build, and at least one discovered APK.

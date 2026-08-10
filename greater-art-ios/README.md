# Greater Art for iPhone

Greater Art is a native SwiftUI audio and video player for media that already belongs to you. This iOS edition is intentionally offline: it has no ads, analytics, account system, telemetry, update checker, web API, or network code.

## What is included

- SwiftUI library, audio player, video player, settings, and compact mini-player
- multi-file or folder import through Apple's Files picker
- private on-device media library that remains available after relaunch
- embedded audio artwork and representative video-frame extraction
- two-level memory/disk artwork cache plus optional first-page preheating
- whole-row playback, search, Name A–Z, Name Z–A, and persistent custom order
- local playlists with add, remove, rename, delete, and drag-to-reorder
- background audio, lock-screen/Control Center commands, previous/next, and seeking
- playback speed from 0.25× through 3×
- repeat one by default, plus repeat all and repeat off
- restored last item and position
- safe-area inline video, custom controls, full-screen landscape rotation, and Picture in Picture
- English and Traditional Chinese UI
- system, light, and dark themes
- small, medium, and large library rows; compact is the default
- Apple privacy manifest declaring no tracking or collected data

## Why import instead of scanning Downloads?

iOS apps are sandboxed and cannot silently crawl the entire Downloads directory. Press **Import**, choose files or a folder in Files, and Greater Art copies supported media into its private local library. The original files are not edited. Folder selection is recursive so nested albums can be imported in one operation.

## Supported formats

Greater Art delegates decoding to AVFoundation so playback remains hardware-accelerated and uses Apple's native media pipeline. The importer recognizes MP4, MOV, M4V, MP3, M4A, AAC, WAV, AIFF, CAF, FLAC, Opus, and Ogg, then rejects files that the current device reports as unplayable.

Codec support depends on the iPhone and iOS release. APE and DSD/DSF/DFF are not advertised in version 1.0 because Apple does not provide dependable native decoding for them; supporting those formats properly would require a separately audited decoder and licensing review.

## Build on a Mac

Requirements:

- macOS with Xcode 16 or newer
- an iPhone running iOS 16 or newer, or an iOS Simulator
- an Apple Account for personal-device testing
- Apple Developer Program membership for TestFlight distribution

Steps:

1. Clone this repository on the Mac.
2. Open `greater-art-ios/GreaterArt.xcodeproj` in Xcode.
3. Select the **GreaterArt** target, then **Signing & Capabilities**.
4. Choose your development team. If Xcode says the bundle identifier is unavailable, replace `com.galahad.greaterart` with a unique reverse-domain identifier belonging to you.
5. Select an iPhone or Simulator and press **Run**.

The committed Xcode project is ready to open. `project.yml` is also included as an optional XcodeGen source of truth; XcodeGen is not required for a normal build.

Command-line verification on macOS:

```bash
xcodebuild build-for-testing \
  -project GreaterArt.xcodeproj \
  -scheme GreaterArt \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO
```

## Send it to a friend with TestFlight

1. Enroll in the Apple Developer Program.
2. Create a new **Greater Art** app record in App Store Connect using the same bundle identifier as Xcode.
3. In Xcode, select **Any iOS Device (arm64)** and choose **Product → Archive**.
4. In Organizer, choose **Distribute App → App Store Connect → Upload**.
5. Open the app's **TestFlight** page in App Store Connect.
6. Add the build to an external testing group and submit it for TestFlight beta review.
7. Invite your friend by email or send the approved public TestFlight link.

Never commit an Apple distribution certificate, private key, provisioning profile, App Store Connect API key, or exported signed `.ipa`. The repository ignores the common signing-file formats.

## Privacy and storage

- Media is copied to `Application Support/GreaterArt/Media` inside the app sandbox.
- Library metadata and playlists are stored as a local JSON snapshot.
- Preferences and the last playback position use `UserDefaults`.
- Generated artwork is kept under the app's cache directory and can be cleared in Settings.
- Removing a track deletes Greater Art's private copy, not the original selected through Files.
- No network framework or endpoint is used by the app.

## Testing notes

The included tests cover the progress calculation that prevents an audio slider from falsely sticking at the end, duration formatting, alphabetical sorting, and persistent custom ordering. GitHub Actions compiles both the app and unit-test bundle on a macOS runner without signing.


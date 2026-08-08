# Greater Art

Greater Art is a private, fully offline Android player for local video and audio. It contains no ads, analytics, accounts, update checks, cloud features, or network access. The packaged app requests neither `INTERNET` nor `ACCESS_NETWORK_STATE`.

## Install the ready-built app

The project folder includes `GreaterArt-v1.4.0-debug.apk`. Copy it to the phone, open it, and allow installation from the Files app if Android asks. It is signed with this computer's Android debug key for personal sideloading.

## First launch

1. Open **Greater Art** and tap **Open settings**.
2. Enable **Allow access to manage all files**.
3. Keep media anywhere inside `/storage/emulated/0/Download/`, including subfolders.
4. Return to the app.
5. Tap refresh after adding or removing files.

The app recursively scans Download. It does not depend on, display, or hardcode a specific source subfolder.

## Main features

- Opens the library quickly without decoding every file during the initial scan
- Uses Android's system video-thumbnail generator first, then a frame-extraction fallback
- Reads embedded audio artwork when available
- Preloads the first library batch into a memory LRU and persistent, automatically pruned disk cache
- Stops background thumbnail work immediately when playback is requested
- Starts playback by tapping anywhere on a media row
- Includes custom drag ordering and Name A-Z / Name Z-A sorting
- Includes a complete local Settings screen with system/light/dark themes
- Offers Small, Medium, and Large library rows; Small is the compact default
- Can hide thumbnails, file details, or thumbnail format badges independently
- Lets the user control thumbnail preloading, resume behavior, automatic Picture-in-Picture, speed, and repeat mode
- Includes safe local rescan, thumbnail-cache clearing, and settings reset actions
- Includes previous, play/pause, next, seek, repeat, and speeds from 0.25x to 3x
- Uses repeat-one by default
- Continues audio through a Media3 session with notification and lock-screen controls
- Keeps portrait video below the system status bar, with compact tap-to-hide controls, rotation, fullscreen, and Picture-in-Picture
- Remembers the last file, position, speed, repeat mode, sorting, and custom order locally
- Recovers from a transient playback failure once, then skips a corrupt or unsupported item safely

## Floating video window

Tap the floating-window button in the video controls, or press Home while a video is playing. Android owns the final Picture-in-Picture size and minimum dimensions. On supported devices the window is resizable by pinching or dragging its edges, and this app supplies the video's real aspect ratio for a compact result.

Force-stopping the app from Android Settings terminates every Android service and floating window by design. Ordinary Home/app switching keeps eligible video in Picture-in-Picture and keeps audio available through the media notification.

## Formats and audio quality

The scanner recognizes:

- Audio: MP3, AAC, M4A, FLAC, WAV, ALAC, AIFF/AIF, Opus, Ogg, APE, DSF, DFF, AMR, AC3, EAC3, and MKA
- Video: MP4, MOV, M4V, MKV, WebM, 3GP, TS, MPEG/MPG, FLV, and AVI

Media3 directly supports common containers and formats including MP3, AAC, M4A, FLAC, WAV, Ogg/Opus, MP4, Matroska, and WebM. Other recognized files are attempted, but decoding ultimately depends on the phone. Media3 does not contain built-in APE or DSD decoders, so APE, DSF, and DFF playback cannot be guaranteed on every device.

Files are decoded locally without transcoding, normalization, or silence removal. Decoder fallback is enabled, and PCM playback is used for the most consistent gapless behavior across phones. The final sample rate and bit depth depend on the Android audio route, phone firmware, and connected DAC; software cannot force 24-bit/192 kHz output on hardware that does not expose it.

For the cleanest high-resolution route, use 1x speed and a DAC or audio output that advertises the source sample rate. Changing playback speed necessarily activates audio processing.

## Build it

1. Install the latest stable [Android Studio](https://developer.android.com/studio).
2. Choose **Open** and select this `App` folder.
3. Let Android Studio install Android SDK 37 and JDK 17 when prompted.
4. Connect a phone with Developer options and USB debugging enabled.
5. Select the phone and click the green **Run** button.

To generate another APK, use **Build -> Build APK(s)**. Android Studio writes it to `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub builds and releases

The repository includes an Android CI workflow that runs unit tests, lint, and a debug APK build on pushes, pull requests, and manual dispatches. Versioned installable APKs are also attached to GitHub Releases for straightforward sideloading.

Signing keys, local properties, credentials, environment files, and private writing are excluded by `.gitignore`. Never commit a release keystore; losing the signing key also prevents future Android upgrades from being installed over the same package.

## Storage and privacy

This personal sideload build uses `MANAGE_EXTERNAL_STORAGE` to scan Download and its subfolders. It is not intended for Google Play. A Play Store version should use the Storage Access Framework folder picker instead; scanner comments describe that alternative.

The thumbnail cache lives inside Android's private app cache. It contains only resized previews, may be cleared by Android, and never modifies original media. Greater Art never edits or deletes media files.

## Animated walkthrough

Open `presentation/index.html` in any modern browser. It is a standalone offline presentation with no CDN, font download, or external asset requests.

## Fuck "Subscribe to Remove Ads"

- This app is free and offline. It contains no ads, analytics, or subscriptions.
- It does not require an account or network access. It does not track you or your media. It does not send any data to any server.
- It does not ask for money.
- It does not nag you to pay for features.
- It does not have a "Subscribe to Remove Ads" button.

* Just like the old days, you can just copy your media to your phone and play it. No strings attached.
* Just like Clippy, not invasive, not annoying, not tracking you, not asking for money, not nagging you to pay for features. Just trying to help.
* Fuck Dystopian "Terms and Conditions", no one read that shit, and no one should have to. Just copy your media to your phone and play it. No strings attached.

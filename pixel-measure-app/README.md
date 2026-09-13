# Pixel Measure

Offline Android developer ruler for measuring exact physical pixels or density-independent pixels.

- Tap once for point A, tap again for point B, then tap again to restart.
- Measures X/Y, edge offsets, width, height, and straight-line distance.
- Loads a screenshot through Android's system picker and maps taps back to source-image pixels.
- Adapts its controls to portrait and landscape layouts.
- Requests no network, storage, camera, or tracking permission.

Current local artifact: `releases/PixelMeasure-v1.0.1-debug.apk`.

Build with `gradlew assembleDebug`. The raw debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

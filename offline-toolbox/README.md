# LocalKit

**Your files. Your device. Your tools.**

LocalKit is a native Android utility workbench containing 25 focused tools in six organized sections. It has no ads, analytics, account, subscription, telemetry, or `INTERNET` permission.

## Organization

- **Scan:** QR/barcodes, QR generation, document capture, bundled offline OCR, magnifier/color sampling
- **Documents:** PDF reader, page organizer, images-to-PDF
- **Media:** image compression/conversion, EXIF cleanup, video and audio exports
- **Files:** scoped browser, safe ZIP, storage report, duplicate detection, SHA-256 verification
- **Calculate:** scientific calculator, unit converter, date math
- **Pocket tools:** private text workbench, secret generator, flashlight/SOS, compass/level

The home screen provides search, recent tools and favorites. Its main actions—**Pick file**, **Scan** and **Paste**—route to the relevant focused tool instead of mixing every control together.

## Privacy design

- The manifest intentionally omits `android.permission.INTERNET`.
- Camera access is requested only when a camera tool opens.
- Files and folders are selected through Android's system picker; the app does not request all-files access.
- Clipboard text is read only after the user taps **Paste**.
- Generated passwords are never written to preferences or history.
- Originals are never changed automatically; exports use a destination chosen by the user.
- Duplicate and storage tools report evidence but never delete anything.

See [PRIVACY.md](PRIVACY.md) for the concise privacy statement.

## Build

Requirements:

- Android Studio with JDK 17 or newer
- Android SDK platform 37
- Network access during the first build to download Gradle dependencies; the built app itself cannot use the network

From PowerShell:

```powershell
.\build-personal-debug.ps1
```

Or open this folder in Android Studio and run the `app` configuration. The project uses Gradle Kotlin DSL.

## Important compatibility notes

- QR and Latin/Chinese OCR models are bundled, which increases APK size but avoids first-use downloads.
- PDF organizer compatibility mode rasterizes pages during export. This preserves visible content but does not preserve searchable text, forms, links, or vector structure.
- Video and audio import/export support depends on codecs provided by the phone. Video exports H.264/AAC MP4; audio exports AAC/M4A.
- Compass and level accuracy depends on device sensors and calibration.
- The sound meter, fake RAM cleaner, battery booster, antivirus, VPN, call recorder, wallet and background clipboard monitoring are deliberately excluded.

## Research

The product-discovery and scope rationale is in [research/utility-app-landscape.md](research/utility-app-landscape.md).


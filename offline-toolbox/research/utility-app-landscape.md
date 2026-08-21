# Offline Toolbox: product-discovery brief

Date: 2026-08-11  
Platform assumption: Android first  
Working title only: **Offline Toolbox**

## The opportunity

Build one coherent, offline-first utility app for jobs that should take seconds but are often wrapped in advertising, trial funnels, accounts, or misleading prompts. It should not imitate twenty-five unrelated apps. It should feel like one local workbench built around a single flow:

> **Pick, scan, or paste something → transform it locally → preview the result → save or share it.**

The complaint is not imaginary. Google Play explicitly prohibits unexpected disruptive ads, false close buttons, ads that resemble operating-system warnings, and manipulative subscriptions. It also says subscriptions must provide sustained recurring value rather than disguise a one-time benefit. The FTC identifies hidden subscriptions, disguised ads, misdirection, and visually unequal choices as dark patterns.

The product answer is intentionally stricter than the policy minimum:

- no advertising SDK;
- no analytics or tracking SDK;
- no account;
- no `INTERNET` permission in the core app;
- no subscription;
- permissions requested only when the selected tool needs them;
- processing and history stored locally, with a visible **Clear local history** action.

## Ranking method

Each candidate was judged qualitatively on five factors: frequency of need, annoyance in the existing category, offline feasibility, implementation risk, and how well it shares an input/output pipeline with the rest of the suite. The priority is a product decision, not a claim that every app in the category is abusive.

Legend: **MVP** = first public build, **Next** = useful follow-up, **Later** = specialized or riskier.

## Top 25 basic utility functions

| # | Utility | What the user actually needs | Typical category friction | Offline / permission reality | Priority |
|---:|---|---|---|---|---|
| 1 | QR and barcode scanner | Point, read, copy, safely open | Full-screen ads after a scan; misleading buttons | Fully on-device; Camera only while scanning | MVP |
| 2 | QR generator | Turn text, URL, Wi-Fi or contact into a code | Watermarks, export paywalls | Fully local; no permission | MVP |
| 3 | PDF reader | Open, search, zoom, remember page | Fake-update-style ads; reader subscription prompts | Android has native PDF rendering; file chosen by user | MVP |
| 4 | PDF organizer | Merge, split, reorder, rotate, delete pages | One-time operations placed behind recurring plans | Local; selected files only | MVP |
| 5 | Image-to-PDF | Select photos, crop, reorder, export | Watermarks and page-count limits | Local; photo picker | MVP |
| 6 | Document scanner | Auto-edge detection, perspective fix, clean page | Account/cloud pressure; export paywalls | Camera; can process locally | MVP |
| 7 | Offline OCR | Copy searchable text from image or scan | Credit quotas and cloud upload | Bundled on-device model; Camera/files as used | MVP |
| 8 | Image compressor/resizer | Hit a target size or dimensions | Repeated ads between every image | Local; selected images only | MVP |
| 9 | Image format converter | PNG/JPEG/WebP conversion and transparency choices | Fake download buttons; batch limits | Local; selected images only | MVP |
| 10 | Metadata cleaner | Inspect/remove GPS, device and EXIF data | Privacy tool that itself tracks users | Local; selected images only | MVP |
| 11 | Video trim/compress/rotate | Make a clip smaller or shorter | Export ads, watermark, forced trial | Local hardware codecs; selected video | Next |
| 12 | Audio trim/convert | Cut a recording/ringtone; change common formats | Export limits and subscription screens | Local codecs; selected audio | Next |
| 13 | File opener/browser | Find, rename, copy, move selected files | Broad storage permissions and promotional clutter | Use Android system picker; no broad permission | MVP |
| 14 | ZIP archive/extract | Pack or unpack ordinary archives | Ads before extraction; password-feature paywalls | Fully local; selected files/folder | MVP |
| 15 | Storage analyzer | See what is large and where space went | Misleading “boost/clean” claims | User-selected folder scope; transparent results | Next |
| 16 | Duplicate finder | Hash and compare likely duplicate files | One-tap deletion anxiety; broad access | User-selected folder; confirmation before deletion | Next |
| 17 | Hash/checksum verifier | SHA-256 a file and compare a supplied hash | Simple feature buried in specialist apps | Fully local; selected file | MVP |
| 18 | Calculator suite | Basic, scientific, percent, tip | Ads covering keys; subscription calculators | Fully local; no permission | MVP |
| 19 | Unit converter | Length, mass, temperature, data, cooking | Ad-heavy single-purpose converters | Fully local; no permission | MVP |
| 20 | Date/time calculator | Difference, add/subtract, age, countdown | Separate apps per tiny calculation | Fully local; no permission | MVP |
| 21 | Text workbench | Word count, case, sort, dedupe, find/replace | Web tools send pasted text to servers | Fully local; clipboard only on explicit paste | MVP |
| 22 | Password/passphrase generator | Generate strong random secrets | Account upsells and clipboard ambiguity | Fully local; secure RNG; do not retain output | Next |
| 23 | Flashlight and SOS pattern | Reliable torch with optional pattern | Launch ads delay an urgent utility | Camera/torch access only; no storage | Next |
| 24 | Compass and spirit level | Direction and surface angle | Skin/theme purchases and ads | Device sensors; no dangerous permission | Next |
| 25 | Magnifier and color picker | Zoom small text; sample color and contrast | Separate ad-heavy apps for each camera action | Camera only while open | Next |

### Strong substitutes, but not first 25

- Sound meter: useful, but phone microphones are not calibrated instruments.
- Metronome/tuner: coherent and offline, but serves a narrower audience.
- Ruler/protractor: screen calibration and camera perspective make accuracy claims tricky.
- Contact export: useful, but contacts are sensitive and broaden the trust surface.
- Notes/checklists: saturated category and weak connection to the transform workflow.

## The product should have six rooms, not 25 icons

### 1. Scan

QR/barcodes, document scan, OCR, magnifier, and color picker share one camera pipeline. A segmented mode selector changes the operation without repeatedly reopening the camera.

### 2. Documents

PDF reader, merge/split/reorder/rotate, image-to-PDF, and scanned documents share a page-strip editor. The preview is the product: users should always see the exact output before writing a file.

### 3. Media

Image resize/convert/metadata cleanup and later video/audio edits share a non-destructive edit queue and one export sheet.

### 4. Files

Browse user-selected locations, archive/extract, inspect size, detect duplicates, and calculate hashes. “Cleaner” must mean **show evidence and let the user decide**, never pretend to boost RAM or battery.

### 5. Calculate

Calculator, unit conversion, and date math share searchable inputs, favorites, copy, and local history.

### 6. Text & Pocket Tools

Text transformations plus later flashlight, level, compass, and secret generation. These are quick actions, not a junk drawer on the home screen.

## The thing that makes it impressive

The advantage is not merely “many tools.” It is **local workflow chaining**:

- scan paper → correct perspective → OCR → create searchable PDF → compress → share;
- select photos → strip location metadata → resize → ZIP → share;
- scan QR → preview decoded content → copy only, or open with an explicit warning for suspicious schemes;
- select a download → calculate SHA-256 → compare to a pasted checksum;
- choose a video → trim → estimate output size → export once.

The home screen should be a command surface:

1. a large **What do you want to do?** search field;
2. **Pick a file**, **Scan**, and **Paste text** as the three primary actions;
3. pinned tools and recent local workflows;
4. context-aware suggestions after an input is chosen.

This prevents the “Swiss-army app with 100 tiny icons” problem.

## Recommended MVP

Ship a tight first build with 15 functions:

1. QR/barcode scan and QR generation
2. PDF reading
3. PDF merge/split/reorder/rotate
4. image-to-PDF
5. document scanning
6. bundled offline OCR
7. image resize/compress/convert
8. EXIF inspect/remove
9. SAF-based file selection
10. ZIP create/extract
11. SHA-256 verifier
12. calculator
13. unit converter
14. date calculator
15. text workbench

Add video/audio transforms, duplicates/storage analysis, sensors, and the password generator after the shared pipelines are stable.

## Deliberate exclusions

These would damage trust or explode the scope:

- “RAM cleaner,” “battery booster,” or “phone cooler” claims;
- antivirus claims without a real signature/reputation service;
- VPN, live weather, currency rates, or anything requiring a backend;
- call recording because of legal and platform complications;
- crypto wallet, seed phrase storage, or full password vault;
- background clipboard monitoring;
- automatic deletion of files;
- all-files access when Android's user-controlled file picker is sufficient.

## Android feasibility and privacy architecture

- **Files:** Android's Storage Access Framework lets the user grant access to a chosen file or directory without broad system storage permission.
- **QR/barcode:** ML Kit offers a bundled model that is available immediately and scans on-device without network access. Restricting expected formats improves latency.
- **OCR:** ML Kit text recognition supports bundled models. Bundle the initial language models rather than downloading them at first use.
- **PDF:** Android's PDF APIs render pages locally. Advanced editing still needs careful compatibility testing and possibly a well-licensed library.
- **Video/audio:** Media3 Transformer supports local trim, crop, rotate, effects, and transcode with hardware acceleration where the device supports it.
- **Camera:** keep one CameraX session warm while switching Scan modes; release it when leaving Scan.
- **Jobs:** use WorkManager for long exports, show real progress, and never invent a completion percentage.
- **Temporary data:** write to an app cache staging area and use atomic final saves; offer a visible cache-size control.
- **Permissions:** Camera is just-in-time. The file picker is preferred over broad storage access. No contacts, location, microphone, or notification permission unless a future tool genuinely needs it.

## Business model that does not recreate the problem

Best fit for the stated mission:

- free and open source, optional donation outside the core workflow; or
- one honest lifetime purchase for the entire app, with a meaningful free version;
- never charge monthly for static, local utilities;
- no artificial daily limits, watermarks, countdown timers, or export-quality traps.

If ongoing development eventually requires revenue, paid major-version upgrades are more defensible than a subscription because the customer buys a concrete new release.

## Naming directions

Names should communicate calm ownership, not optimization hype:

- **PlainTools** — clearest promise
- **LocalKit** — emphasizes on-device work
- **OneBench** — emphasizes workflows rather than icon count
- **Pocket Works** — friendlier, less technical
- **ZeroKit** — signals zero ads/tracking, but is less descriptive

My strongest working pair is **LocalKit** as the product name and **Your files. Your device. Your tools.** as the line. Trademark and store-name checks are still required before committing.

## Sources and limitations

This is a product-discovery brief, not a prevalence study. It identifies recurring category patterns and official platform constraints; it does not claim that every app in a named category uses them.

- [FTC: Bringing Dark Patterns to Light](https://www.ftc.gov/system/files/ftc_gov/pdf/P214800%20Dark%20Patterns%20Report%209.14.2022%20-%20FINAL.pdf)
- [Google Play: Ads policy](https://support.google.com/googleplay/android-developer/answer/9857753)
- [Google Play: Deceptive Behavior](https://support.google.com/googleplay/android-developer/answer/17006354)
- [Google Play: Subscriptions policy](https://support.google.com/googleplay/android-developer/answer/9900533)
- [Android: Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- [Android: PDF APIs](https://developer.android.com/reference/android/graphics/pdf/package-summary)
- [Google ML Kit: on-device barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning)
- [Google ML Kit: model installation paths](https://developers.google.com/ml-kit/tips/installation-paths)
- [Android: Media3 Transformer](https://developer.android.com/media/media3/transformer)


# APPs by L

A public collection of experimental Android apps. Each app lives in its own top-level folder and is intended to be understandable, auditable, and usable without hidden service dependencies.

> **Vibe-coded disclosure:** these projects were built through iterative work with AI coding agents. Human direction, product decisions, testing feedback, and final acceptance remain part of the development process; substantial implementation and documentation were AI-assisted.

## Apps

### Greater Art

A local-first Android audio/video player built with Kotlin, Jetpack Compose, and Media3.

- Current repository version: **1.12.5 (code 86)**
- Documentation: [greater-art/README.md](greater-art/README.md)
- APK: [greater-art/releases/GreaterArt-1.12.5.apk](greater-art/releases/GreaterArt-1.12.5.apk)
- User demonstration: [greater-art-user-demo.html](greater-art-user-demo.html)
- Technical demonstration: [greater-art-technical-demo.html](greater-art-technical-demo.html)
- Source directory: [greater-art/](greater-art/)

Greater Art intentionally has no Internet permission, advertising, accounts, analytics, telemetry, or cloud playback dependency.

### Offline Toolbox / LocalKit

An offline-first utility workbench for local file, text, media, scan, conversion, and device tasks.

- Documentation: [offline-toolbox/README.md](offline-toolbox/README.md)
- Privacy notes: [offline-toolbox/PRIVACY.md](offline-toolbox/PRIVACY.md)
- Verification notes: [offline-toolbox/VERIFICATION.md](offline-toolbox/VERIFICATION.md)
- Source directory: [offline-toolbox/](offline-toolbox/)

### Useless Calculator

A harmless parody of hostile permission and subscription onboarding wrapped around a calculator.

- Documentation: [useless-calculator/README.md](useless-calculator/README.md)
- APK: [useless-calculator/UselessCalculator-v1.1.0-debug.apk](useless-calculator/UselessCalculator-v1.1.0-debug.apk)
- Source directory: [useless-calculator/](useless-calculator/)

## Repository structure

```text
APPs-by-L/
├── greater-art/
├── offline-toolbox/
├── useless-calculator/
├── skills/
├── scripts/
├── .github/
├── README.md
├── SECURITY.md
└── index.html
```

Historical implementation drafts, superseded promo pages, and duplicate session handoffs are intentionally kept out of the current tree. Git history remains the archive.

## Security and privacy

This public repository is intended to contain no production credentials, personal signing keys, local machine secrets, analytics identifiers, or private user data.

- Keystores, credentials, environment files, and machine-local Android configuration are excluded.
- `scripts/audit-public-repo.ps1` scans for high-confidence credential patterns, sensitive filenames, local user paths, and unsafe public commit metadata.
- GitHub Actions runs repository verification and security checks.
- See [SECURITY.md](SECURITY.md) for reporting guidance.

Run the local audit from the repository root:

```powershell
pwsh -NoProfile -File scripts/audit-public-repo.ps1
```

## Build verification

Each Android app is a standalone Gradle project with its own build instructions. Public APKs are experimental/personal sideload builds; review source, permissions, and verification notes before installation.

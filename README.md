# APPs by L

A public, expandable collection of experimental Android and iOS apps. Each app lives in its own top-level folder, with shared automation and transferable agent skills kept at the repository root.

> **Vibe-coded disclosure:** These apps were built through iterative conversations with AI coding agents. The human supplied the ideas, taste, constraints, testing feedback, and final decisions; AI produced substantial code, documentation, and automation. “Vibe-coded” is a description of the workflow, not a claim that the software is flawless.

## Apps

### `greater-art/`

Greater Art is an ad-free, offline local audio and video player. It requests no Internet permission and contains no analytics, accounts, subscriptions, telemetry, or cloud features.

- Documentation: [`greater-art/README.md`](greater-art/README.md)
- Ready APK: [`greater-art/GreaterArt-v1.5.0-debug.apk`](greater-art/GreaterArt-v1.5.0-debug.apk)

### `greater-art-ios/`

Greater Art for iPhone is a native SwiftUI edition of the offline media player. It imports local files through Apple's Files picker, caches artwork, supports background audio and Picture in Picture, and is designed for private TestFlight distribution.

- Documentation: [`greater-art-ios/README.md`](greater-art-ios/README.md)
- Xcode project: [`greater-art-ios/GreaterArt.xcodeproj`](greater-art-ios/GreaterArt.xcodeproj)

### `useless-calculator/`

Useless Calculator is harmless satire about hostile mobile-app onboarding. It presents absurd terms and unused permission prompts before blocking `=` behind a fake `$29.99/month` subscription screen.

Despite the joke, it includes:

- no Internet or notification permission
- no analytics, advertising, billing, or background service
- no reading, storing, or transmitting granted data
- no editable password, wallet-secret, seed-phrase, or credential field

- Documentation: [`useless-calculator/README.md`](useless-calculator/README.md)
- Ready APK: [`useless-calculator/UselessCalculator-v1.1.0-debug.apk`](useless-calculator/UselessCalculator-v1.1.0-debug.apk)

Each app folder is standalone: Android projects include their own Gradle wrapper, while iOS projects include an Xcode project. Future apps should follow the same one-folder-per-app structure.

## Security and privacy

This public repository is intentionally designed to contain no personal secrets, production credentials, signing keys, analytics identifiers, or machine-specific paths.

- Commit metadata uses a pseudonym and a GitHub noreply address.
- Keystores, credentials, local Android configuration, environment files, and private writing are ignored.
- `scripts/audit-public-repo.ps1` scans the working tree and reachable Git history for high-confidence credential patterns, sensitive filenames, local user paths, and public commit emails.
- GitHub Actions runs that audit on pushes, pull requests, and a weekly schedule.
- CodeQL analyzes Kotlin/Java code, dependency review checks pull requests, and Dependabot monitors Gradle and workflow dependencies.
- Workflow dependencies are pinned to immutable commit SHAs and run with minimum permissions.

The included APKs are personal debug builds. Never treat a public repository as a password vault, and never paste a live credential into an issue, source file, commit, or AI prompt. See [`SECURITY.md`](SECURITY.md) for private reporting guidance.

Run the local audit from the repository root:

```powershell
pwsh -NoProfile -File scripts/audit-public-repo.ps1
```

## Portable agent skills

The [`skills/`](skills/) directory contains two reusable skills:

- `build-android-app-hermes` builds, verifies, packages, and prepares native Android apps for delivery.
- `learn-reusable-skill` turns a completed workflow, conversation, file set, or URL collection into one transferable Hermes skill.

To add this repository to CC-Switch, open **Skills -> Repository Management -> Add Repository** and use:

- Owner: `Galahad369`
- Name: `APPs-by-L`
- Branch: `main`
- Subdirectory: `skills`

CC-Switch can then copy or link installed skills into supported Codex, Claude Code, Gemini, OpenCode, and Hermes skill directories.

## Build verification

GitHub Actions tests, lints, and builds the Android projects and performs an unsigned compile of the iOS app and its test bundle. Local commands are documented in each app folder.

The code and APKs are provided for experimentation and personal sideloading. Review the source, permissions, and build output before installing software from any public repository.

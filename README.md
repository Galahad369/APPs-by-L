# Offline Android satire lab

This private monorepo contains two deliberately opposite Android apps.

## `greater-art/`

Greater Art is the real app: a private, ad-free, offline local audio and video player. It requests no Internet permission and contains no analytics, accounts, subscriptions, telemetry, or cloud features.

See [`greater-art/README.md`](greater-art/README.md) for installation, supported formats, settings, and build instructions.

Ready APK: [`greater-art/GreaterArt-v1.4.0-debug.apk`](greater-art/GreaterArt-v1.4.0-debug.apk)

## `useless-calculator/`

Useless Calculator is satire about hostile mobile-app onboarding. It presents more than 300 absurd terms, randomly swaps the meaning of yellow action buttons, requests permissions it never uses, erases onboarding progress when permission is denied, locally bans pizza dissenters, and blocks the equals button behind a fake `$29.99/month` paywall.

Despite the joke, it is intentionally harmless:

- no Internet permission
- no analytics or advertising SDK
- no notification permission or background spam
- no real payment or billing integration
- no reading, storing, or transmitting granted data
- no editable password, wallet-secret, seed-phrase, or credential field

The wallet-password screen is a non-editable warning parody. Never enter real credentials into random apps.

Ready APK: [`useless-calculator/UselessCalculator-v1.1.0-debug.apk`](useless-calculator/UselessCalculator-v1.1.0-debug.apk)

Each folder is a standalone Android Studio project with its own Gradle wrapper.

## Portable Hermes skills

The [`skills/`](skills/) directory contains two reusable, CC-Switch-compatible skills:

- `build-android-app-hermes` builds, verifies, packages, and prepares native Android apps for delivery.
- `learn-reusable-skill` turns a completed workflow, conversation, file set, or URL collection into one transferable Hermes skill.

To add this private repository to CC-Switch, open **Skills -> Repository Management -> Add Repository** and use:

- Owner: `Galahad369`
- Name: `greater-art`
- Branch: `main`
- Subdirectory: `skills`

CC-Switch can then copy or link an installed skill into its supported Codex, Claude Code, Gemini, OpenCode, and Hermes skill directories. The skills contain no credentials or machine-specific paths.

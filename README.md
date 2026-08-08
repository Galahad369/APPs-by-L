# Offline Android satire lab

This private monorepo contains two deliberately opposite Android apps.

## `greater-art/`

Greater Art is the real app: a private, ad-free, offline local audio and video player. It requests no Internet permission and contains no analytics, accounts, subscriptions, telemetry, or cloud features.

See [`greater-art/README.md`](greater-art/README.md) for installation, supported formats, settings, and build instructions.

Ready APK: [`greater-art/GreaterArt-v1.4.0-debug.apk`](greater-art/GreaterArt-v1.4.0-debug.apk)

## `useless-calculator/`

Useless Calculator is satire about hostile mobile-app onboarding. It presents absurd terms, requests permissions it never uses, erases onboarding progress when permission is denied, and blocks the equals button behind a fake `$29.99/month` paywall.

Despite the joke, it is intentionally harmless:

- no Internet permission
- no analytics or advertising SDK
- no notification permission or background spam
- no real payment or billing integration
- no reading, storing, or transmitting granted data
- no editable password, wallet-secret, seed-phrase, or credential field

The wallet-password screen is a non-editable warning parody. Never enter real credentials into random apps.

Ready APK: [`useless-calculator/UselessCalculator-v1.0.0-debug.apk`](useless-calculator/UselessCalculator-v1.0.0-debug.apk)

Each folder is a standalone Android Studio project with its own Gradle wrapper.

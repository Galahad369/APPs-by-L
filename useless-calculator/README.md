# Useless Calculator

An intentionally hostile, completely offline calculator parody.

## The joke

The user must scroll through more than 300 numbered terms, accept multiple acknowledgements, survive a long sequence of irrelevant Android permission dialogs, and acknowledge a non-editable wallet-password warning. Pressing `=` never calculates anything; it opens a fake `$29.99/month` subscription screen.

The yellow action button randomly changes meaning. Sometimes yellow continues; sometimes yellow denies, quits, and erases onboarding progress. Disagreeing with the mandatory pizza statement stores a single local ban flag and replaces the calculator with a permanent-ban overlay. Clearing app data or uninstalling resets the joke.

## Safety boundaries

- No `INTERNET` or notification permission
- No ads, analytics, billing library, or background service
- Permission results are used only to advance or terminate onboarding
- Granted contacts, media, location, microphone, camera, phone, calendar, sensor, and Bluetooth data are never queried
- No editable credential or wallet-secret field
- Denying a permission closes the task and leaves no saved onboarding progress
- No body-sensor permission
- The only persisted value is the local satire-ban boolean

This project is satire and should not be presented as a trustworthy calculator.

## Install

The ready personal-sideload build is `UselessCalculator-v1.1.0-debug.apk`. It is a separate package from Greater Art and cannot replace or modify the media player.

## Build

Open this `useless-calculator` folder in Android Studio or run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

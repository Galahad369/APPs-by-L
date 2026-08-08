# Useless Calculator

An intentionally hostile, completely offline calculator parody.

## The joke

The user must scroll through excessive terms, accept multiple acknowledgements, survive a long sequence of irrelevant Android permission dialogs, and acknowledge a non-editable wallet-password warning. Pressing `=` never calculates anything; it opens a fake `$29.99/month` subscription screen.

## Safety boundaries

- No `INTERNET` or notification permission
- No ads, analytics, billing library, or background service
- Permission results are used only to advance or terminate onboarding
- Granted contacts, media, location, microphone, camera, phone, calendar, sensor, and Bluetooth data are never queried
- No editable credential or wallet-secret field
- Denying a permission closes the task and leaves no saved onboarding progress

This project is satire and should not be presented as a trustworthy calculator.

## Install

The ready personal-sideload build is `UselessCalculator-v1.0.0-debug.apk`. It is a separate package from Greater Art and cannot replace or modify the media player.

## Build

Open this `useless-calculator` folder in Android Studio or run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

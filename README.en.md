# KioSync

[Versi Bahasa Indonesia](README.md)

KioSync is an Android kiosk launcher built with Kotlin and Jetpack Compose. This project is designed for devices provisioned as Device Owner, allowing the app to control the device shell, apply Lock Task policy, and expose only admin-selected apps to users.

## Features

- Kiosk home screen with a grid of allowed apps.
- Hidden admin access by tapping the status text 7 times.
- Admin PIN dialog before opening kiosk settings.
- Toggle for enabling or disabling kiosk mode.
- App allowlist management for Lock Task mode.
- Device Owner policy application through `DevicePolicyManager`.
- Persistent HOME activity alias while kiosk mode is active.
- Boot grace period before starting Lock Task mode.
- DataStore-backed persistence for kiosk state and selected packages.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- AndroidX Lifecycle
- Jetpack DataStore Preferences
- JUnit and kotlinx-coroutines-test

## Project Structure

```text
app/src/main/java/com/mascill/kiosync
|-- app                # Activity, root Compose wiring, and side-effect handling
|-- core
|   |-- data           # DataStore, repository, and launchable app discovery
|   |-- designsystem   # Compose theme, colors, and typography
|   |-- dpc            # DeviceAdminReceiver for provisioning
|   |-- kiosk          # Device Owner policy and Lock Task controllers
|   |-- model          # Shared models
|   |-- navigation     # External app launching and HOME navigation
|   `-- system         # Helpers for Android system APIs
|-- di                 # Manual dependency providers
`-- feature/kiosk      # Kiosk UI, state models, and ViewModel
```

## Requirements

- Android Studio with JDK 11 support.
- Android SDK 36.
- A physical Android device or emulator for kiosk testing.
- A fresh/factory-reset device when provisioning as Device Owner.

## Build

```bash
./gradlew assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run Tests

```bash
./gradlew testDebugUnitTest
```

Instrumentation tests can be run with:

```bash
./gradlew connectedDebugAndroidTest
```

## Install Debug APK

The app manifest currently uses `android:testOnly="true"`, so install the debug APK with `-t` when using ADB directly:

```bash
adb install -t app/build/outputs/apk/debug/app-debug.apk
```

## Device Owner Provisioning

KioSync requires Device Owner privileges to apply kiosk policies. Provisioning usually requires a fresh device with no accounts configured.

After installing the APK, set KioSync as Device Owner:

```bash
adb shell dpm set-device-owner com.mascill.kiosync/.core.dpc.KioSyncDeviceAdminReceiver
```

Then open the app:

```bash
adb shell monkey -p com.mascill.kiosync 1
```

When kiosk mode is enabled, KioSync applies the policy, enables the kiosk HOME alias, updates the Lock Task allowlist, hides system bars, and starts Lock Task mode when permitted by the system.

## Admin Flow

1. Open KioSync.
2. Tap the status text 7 times.
3. Enter the admin PIN.
4. Enable or disable kiosk mode.
5. Select the apps that should appear in the kiosk launcher.
6. Refresh the app list if newly installed apps do not appear yet.

The current development PIN is:

```text
123456
```

Do not use a hardcoded PIN in production builds.

## Important Notes

- Device Owner APIs only work after successful provisioning.
- Lock Task mode can only start when the app is allowlisted by `DevicePolicyManager`.
- KioSync always includes its own package in the Lock Task allowlist so the kiosk shell remains available.
- The app filters persisted packages against currently installed launchable apps to avoid stale entries.
- The boot grace period prevents kiosk startup from running too early before Android services are ready after boot.

## Useful ADB Commands

Check Device Owner status:

```bash
adb shell dumpsys device_policy
```

Remove KioSync as active admin for test-only/debug builds:

```bash
adb shell dpm remove-active-admin com.mascill.kiosync/.core.dpc.KioSyncDeviceAdminReceiver
```

Uninstall the app:

```bash
adb uninstall com.mascill.kiosync
```

## Production Checklist

- Replace the hardcoded admin PIN with a secure credential flow.
- Remove `android:testOnly="true"` for release builds.
- Review backup and data extraction rules.
- Enable minification and add ProGuard/R8 rules if needed.
- Validate Device Owner provisioning on the target device model.
- Test kiosk recovery after reboot, app updates, and missing allowlisted apps.

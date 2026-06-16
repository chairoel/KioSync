# KioSync

[English version](README.en.md)

KioSync adalah aplikasi Android kiosk launcher yang dibuat dengan Kotlin dan Jetpack Compose. Project ini ditujukan untuk perangkat yang diprovisioning sebagai Device Owner, sehingga aplikasi dapat mengontrol shell perangkat, menerapkan Lock Task policy, dan hanya menampilkan aplikasi yang dipilih oleh admin.

## Fitur

- Halaman kiosk home dengan grid aplikasi yang diizinkan.
- Akses admin tersembunyi dengan mengetuk teks status 7 kali.
- Dialog PIN admin sebelum membuka pengaturan kiosk.
- Toggle untuk mengaktifkan atau menonaktifkan kiosk mode.
- Pengelolaan app allowlist untuk Lock Task mode.
- Penerapan Device Owner policy melalui `DevicePolicyManager`.
- Persistent HOME activity alias saat kiosk mode aktif.
- Boot grace period sebelum memulai Lock Task mode.
- Penyimpanan status kiosk dan package terpilih menggunakan DataStore.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- AndroidX Lifecycle
- Jetpack DataStore Preferences
- JUnit and kotlinx-coroutines-test

## Struktur Project

```text
app/src/main/java/com/mascill/kiosync
├── app                # Activity, root Compose wiring, dan side-effect handling
├── core
│   ├── data           # DataStore, repository, dan discovery aplikasi launchable
│   ├── designsystem   # Compose theme, color, dan typography
│   ├── dpc            # DeviceAdminReceiver untuk provisioning
│   ├── kiosk          # Device Owner policy dan Lock Task controller
│   ├── model          # Model bersama
│   ├── navigation     # Launch aplikasi eksternal dan navigasi HOME
│   └── system         # Helper untuk API sistem Android
├── di                 # Provider dependency manual
└── feature/kiosk      # UI kiosk, model state, dan ViewModel
```

## Kebutuhan

- Android Studio dengan dukungan JDK 11.
- Android SDK 36.
- Perangkat Android fisik atau emulator untuk pengujian kiosk.
- Perangkat fresh/factory reset saat melakukan provisioning sebagai Device Owner.

## Build

```bash
./gradlew assembleDebug
```

Debug APK akan dibuat di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Menjalankan Test

```bash
./gradlew testDebugUnitTest
```

Instrumentation test dapat dijalankan dengan:

```bash
./gradlew connectedDebugAndroidTest
```

## Install Debug APK

Manifest aplikasi saat ini menggunakan `android:testOnly="true"`, jadi gunakan flag `-t` saat menginstall debug APK lewat ADB secara langsung:

```bash
adb install -t app/build/outputs/apk/debug/app-debug.apk
```

## Device Owner Provisioning

KioSync membutuhkan privilege Device Owner untuk menerapkan kiosk policy. Provisioning biasanya membutuhkan perangkat yang masih fresh dan belum memiliki akun yang dikonfigurasi.

Setelah APK terinstall, jadikan KioSync sebagai Device Owner:

```bash
adb shell dpm set-device-owner com.mascill.kiosync/.core.dpc.KioSyncDeviceAdminReceiver
```

Lalu buka aplikasi:

```bash
adb shell monkey -p com.mascill.kiosync 1
```

Saat kiosk mode diaktifkan, KioSync akan menerapkan policy, mengaktifkan kiosk HOME alias, memperbarui Lock Task allowlist, menyembunyikan system bars, dan memulai Lock Task mode jika sudah diizinkan oleh sistem.

## Alur Admin

1. Buka KioSync.
2. Ketuk teks status 7 kali.
3. Masukkan PIN admin.
4. Aktifkan atau nonaktifkan kiosk mode.
5. Pilih aplikasi yang boleh tampil di kiosk launcher.
6. Refresh daftar aplikasi jika aplikasi yang baru diinstall belum muncul.

PIN development saat ini:

```text
123456
```

Jangan gunakan PIN hardcoded untuk production build.

## Catatan Penting

- Device Owner API hanya bekerja setelah provisioning berhasil.
- Lock Task mode hanya dapat dimulai jika aplikasi sudah masuk allowlist dari `DevicePolicyManager`.
- KioSync selalu memasukkan package miliknya sendiri ke Lock Task allowlist agar kiosk shell tetap tersedia.
- Aplikasi memfilter package yang tersimpan terhadap daftar aplikasi launchable yang masih terinstall agar tidak memakai entry lama.
- Boot grace period mencegah startup kiosk berjalan terlalu cepat sebelum service Android selesai siap setelah boot.

## Perintah ADB Berguna

Cek status Device Owner:

```bash
adb shell dumpsys device_policy
```

Hapus KioSync sebagai active admin untuk build test-only/debug:

```bash
adb shell dpm remove-active-admin com.mascill.kiosync/.core.dpc.KioSyncDeviceAdminReceiver
```

Uninstall aplikasi:

```bash
adb uninstall com.mascill.kiosync
```

## Production Checklist

- Ganti PIN admin hardcoded dengan credential flow yang aman.
- Hapus `android:testOnly="true"` untuk release build.
- Review aturan backup dan data extraction.
- Aktifkan minification dan tambahkan rule ProGuard/R8 jika diperlukan.
- Validasi Device Owner provisioning pada model perangkat target.
- Test recovery kiosk setelah reboot, update aplikasi, dan kondisi aplikasi allowlist yang hilang.

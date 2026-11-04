<p align="center">
  <strong>LUCENT</strong><br>
  <em>A considered, Liquid Glass video player for Android.</em>
</p>

<p align="center">
  <a href="https://github.com/tejastajane/LUCENT-Android/releases/latest/download/lucent.apk"><strong>Download latest APK</strong></a>
  ·
  <a href="https://github.com/tejastajane/LUCENT-Android/actions/workflows/android.yml">Build workflow</a>
  ·
  <a href="https://github.com/tejastajane/LUCENT-Android/issues">Report an issue</a>
</p>

<p align="center">
  <a href="https://github.com/tejastajane/LUCENT-Android/actions/workflows/android.yml"><img alt="LUCENT APK build" src="https://github.com/tejastajane/LUCENT-Android/actions/workflows/android.yml/badge.svg?branch=main"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/platform-Android-3DDC84">
  <img alt="Language: Kotlin" src="https://img.shields.io/badge/language-Kotlin-7F52FF">
  <img alt="UI: Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4">
  <img alt="Playback: libVLC" src="https://img.shields.io/badge/playback-libVLC-FFFFFF">
</p>

LUCENT is a local-media player built with Kotlin and Jetpack Compose. It combines libVLC playback with a custom, neutral Liquid Glass interface designed around translucent surfaces, backdrop blur, and lens refraction.

> **Project status:** Active development. Once the persistent release-signing secrets are configured, every successful `main` build verifies the signing certificate and publishes the APK as the latest GitHub Release. Non-main and pull-request builds remain temporary testing artifacts. LUCENT is not a Play Store release.

## Download

**[Download lucent.apk](https://github.com/tejastajane/LUCENT-Android/releases/latest/download/lucent.apk)**

The URL above always resolves to the APK attached to the latest GitHub Release. After the persistent signing secrets are configured, successful builds from `main` publish this release automatically. The workflow keeps the signing key private in GitHub Actions secrets and verifies its certificate fingerprint before publishing. Branch and pull-request builds remain available as temporary Actions artifacts.

To install, download the APK on your Android device and open it. Android may ask you to allow installation from the browser or file manager you used.

## Features

### Media library
- Scans the device's video library through Android MediaStore.
- Three main sections: **Videos**, **Folders**, and **Settings**.
- Filter videos by All, Latest, Movies, TV Shows, Anime, and Favourites.
- Manually correct a video's category from its video options when automatic classification does not fit.
- Search video names and folder paths.
- Open individual videos through Android's system document picker or supported video shares from other apps.
- Display video thumbnails, duration, file size, and storage location.
- Sort episode and season filenames in natural numeric order.
- Pull down on the library pages to rescan indexed media.
- Resume the last video from its saved playback position.
- View file information, rename, share, and delete videos.

### Playback
- libVLC-based local video playback.
- Play, pause, seek, and move through the library playlist with previous/next controls.
- Remember playback progress and per-video audio/subtitle selections.
- Handle audio-focus changes when another app interrupts playback.
- Select embedded audio and subtitle tracks.
- Load an external subtitle file and adjust subtitle delay in 0.1-second steps.
- Change playback speed from 0.5× to 2×.
- Choose Original, 16:9, 4:3, 1:1, 21:9, and 2.35:1 display ratios.
- Save and revisit per-video playback bookmarks.
- Mark videos as favourites, browse them through the Favourites filter, share them, and inspect playback information.
- Use picture-in-picture on supported Android versions.

### Player controls and design
- Double-tap the left or right side to seek in 10-second increments; the on-screen feedback displays the cumulative seek amount.
- Toggle the progress-bar time between total duration and negative remaining time.
- Gesture-based brightness and system-volume adjustment, with an option to disable gestures.
- Optional hardware video acceleration.
- Lock and unlock playback controls.
- Expanding player panels use the same theme-aware Kyant0 Backdrop material as the app's glass dialogs.
- Dark and light library themes.
- Portrait and landscape player layouts.

## Screens and navigation

| Section | Purpose |
| --- | --- |
| Videos | Filtered and searchable local video library |
| Folders | Browse videos grouped by their storage folder |
| Settings | Theme, playback, gesture, media-management, and app information settings |

The player adapts its controls to portrait and landscape layouts. Some controls, including picture-in-picture, are shown only where the Android version or layout supports them.

## Compatibility

- **Minimum Android version:** Android 6.0 (API 23)
- **Target SDK:** 36
- **Application ID:** `com.tejas.liquidplayer`

The application ID is intentionally retained across the LUCENT rebrand so existing installations can be updated without changing the app's package identity.

## Technology

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Video playback | VideoLAN libVLC Android |
| Liquid Glass effects | Kyant0 Backdrop and Shapes |
| Device media library | Android MediaStore |
| File and subtitle picking | Android Storage Access Framework |
| Build and distribution | GitHub Actions, Gradle, GitHub Releases |

## Build from source

1. Open the repository in a recent Android Studio installation.
2. Install JDK 21 and Android SDK Platform 37.
3. Sync the Gradle project.
4. Build the production-flavour debug APK:

```bash
./gradlew --no-daemon :app:assembleProductionDebug
```

Run the focused local regression tests and Android lint checks:

```bash
./gradlew --no-daemon :app:testProductionDebugUnitTest :app:lintProductionDebug
```

The local APK is written to:

```text
app/build/outputs/apk/production/debug/app-production-debug.apk
```

## Automated APK builds

The APK workflow:
1. Uses the tracked Gradle Wrapper and JDK 21.
2. Builds a persistently signed production APK when the release keystore secrets are present; otherwise it builds a debug/testing APK.
3. Generates a SHA-256 checksum and uploads the APK artifact.
4. Publishes or updates the public latest release only for a main-branch build whose APK certificate matches the configured fingerprint.

The verification workflow separately runs focused unit tests and Android lint on pushes and pull requests. It does not claim to replace device testing, playback-corpus testing, or accessibility sessions.

**Stable release signing:** Configure these GitHub Actions repository secrets before expecting new main builds to update the public latest APK: `LUCENT_KEYSTORE_BASE64`, `LUCENT_KEYSTORE_PASSWORD`, `LUCENT_KEY_ALIAS`, `LUCENT_KEY_PASSWORD`, and `LUCENT_SIGNING_CERT_SHA256`. Keep one keystore safe and backed up outside the repository; never commit it. The SHA-256 fingerprint must be the certificate belonging to that key. CI rejects a release APK if its signer differs.

Historical testing APKs were signed with ephemeral debug certificates. Because those certificates differ, users of those historical builds may need a one-time uninstall/reinstall to move to the new stable signing identity; uninstalling removes app data. Future stable-signed releases can update in place when the same key is retained.

See the [APK workflow](https://github.com/tejastajane/LUCENT-Android/actions/workflows/android.yml) and [verification workflow](https://github.com/tejastajane/LUCENT-Android/actions/workflows/verify.yml) for build and quality-check history.

## Design direction

LUCENT uses a restrained neutral palette: black, white, and grey. It uses real backdrop-sampled Liquid Glass—not generic blue glassmorphism. The design contract and implementation constraints are documented in [DESIGN.md](DESIGN.md).

## Data backup

LUCENT keeps portable app settings separate from local playback history. Theme and playback settings can participate in Android backup, while playback history, media-provider URIs, bookmarks, and other device-local player state are excluded because those references may not remain usable after a restore or device transfer.

## Third-party notices and licensing

Third-party dependency information is recorded in [NOTICE.md](NOTICE.md). The repository does not yet include a project-level `LICENSE` file, so do not assume the app's source is available for reuse or redistribution under an open-source licence. A project licence should be chosen and added before inviting external contributions or authorising reuse.

## Feedback

Bug reports and feature requests are welcome through [GitHub Issues](https://github.com/tejastajane/LUCENT-Android/issues). Include your Android version, device model, steps to reproduce, and relevant logs where possible.

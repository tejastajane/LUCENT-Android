<p align="center">
  <strong>LUCENT</strong><br>
  <em>A considered, Liquid Glass video player for Android.</em>
</p>

<p align="center">
  <a href="https://github.com/tejastajane/LiquidPlayer-Android/releases/latest/download/lucent.apk"><strong>Download latest APK</strong></a>
  ·
  <a href="https://github.com/tejastajane/LiquidPlayer-Android/actions/workflows/android.yml">Build &amp; playback tests</a>
  ·
  <a href="https://github.com/tejastajane/LiquidPlayer-Android/issues">Report an issue</a>
</p>

<p align="center">
  <a href="https://github.com/tejastajane/LiquidPlayer-Android/actions/workflows/android.yml"><img alt="Android build and playback test" src="https://github.com/tejastajane/LiquidPlayer-Android/actions/workflows/android.yml/badge.svg?branch=main"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/platform-Android-3DDC84">
  <img alt="Language: Kotlin" src="https://img.shields.io/badge/language-Kotlin-7F52FF">
  <img alt="UI: Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4">
  <img alt="Playback: libVLC" src="https://img.shields.io/badge/playback-libVLC-FFFFFF">
</p>

LUCENT is a local-media player built with Kotlin and Jetpack Compose. It combines libVLC playback with a custom, neutral Liquid Glass interface designed around translucent surfaces, backdrop blur, and lens refraction.

> **Project status:** Active development. The APK linked above is an automatically built **debug/testing build**, not a Play Store release.

## Download

**[Download lucent.apk](https://github.com/tejastajane/LiquidPlayer-Android/releases/latest/download/lucent.apk)**

The link always points to the APK attached to the latest successful main-branch release. A release is published by GitHub Actions only after the build, Android UI smoke tests, and libVLC playback smoke test complete successfully.

To install, download the APK on your Android device and open it. Android may ask you to allow installation from the browser or file manager you used.

## Features

### Media library
- Scans the device's video library through Android MediaStore.
- Browse videos in dedicated **Videos**, **Folder**, **Browse**, and **More** sections.
- Filter the library by All, Movies, TV Shows, Anime, and Recent.
- Search video names and folder paths.
- Open individual videos through Android's system file picker.
- Display video thumbnails, duration, file size, and storage location.
- Resume the last video from its saved playback position.

### Playback
- libVLC-based local video playback.
- Play, pause, seek, and move through the library playlist with previous/next controls.
- Remember playback progress and per-video audio/subtitle selections.
- Select embedded audio and subtitle tracks.
- Load an external subtitle file and adjust subtitle delay in 0.1-second steps.
- Change playback speed from 0.5× to 2×.
- Choose Original, 16:9, 4:3, 1:1, 21:9, and 2.35:1 display ratios.
- Save and revisit per-video playback bookmarks.
- Mark videos as favourites, share them, and inspect file information.
- Use picture-in-picture on supported Android versions.

### Player controls and design
- Double-tap the left/right sides to seek 10 seconds.
- Gesture-based brightness and volume adjustment, with an option to disable gestures.
- Optional hardware video acceleration.
- Lock and unlock playback controls.
- A custom monochrome Liquid Glass design using the real Backdrop effect library: backdrop blur, lens refraction, highlights, and shadows.
- Dark and light library themes.

## Screens and navigation

| Section | Purpose |
| --- | --- |
| Videos | Filtered and searchable local video library |
| Folder | Browse videos grouped by their storage folder |
| Browse | Browse storage and open files through Android's document picker |
| More | Theme, playback, gesture, and app information settings |

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
| Liquid Glass effects | Kyant0 Backdrop |
| Device media library | Android MediaStore |
| File and subtitle picking | Storage Access Framework |
| Build and validation | GitHub Actions, Gradle, Android Emulator |

## Build from source

1. Open the repository in a recent Android Studio installation.
2. Install JDK 21 and Android SDK Platform 37.
3. Sync the Gradle project.
4. Build the production-flavour debug APK:

```bash
gradle --no-daemon :app:assembleProductionDebug
```

The local APK is written to:

```text
app/build/outputs/apk/production/debug/app-production-debug.apk
```

The CI workflow also builds an isolated CI-flavour APK and Android instrumentation test APK.

## Automated validation

The GitHub Actions workflow:
1. Builds the production APK and isolated CI test target.
2. Creates a short local video fixture for instrumentation tests.
3. Builds the Android instrumentation test APK.
4. Runs a real Settings-screen smoke test.
5. Opens a MediaStore video in the player UI.
6. Runs a libVLC playback smoke test and verifies playback time advances.
7. Publishes the latest `lucent.apk` release asset after all checks pass on `main`.

See [Build & playback tests](https://github.com/tejastajane/LiquidPlayer-Android/actions/workflows/android.yml) for run history and logs.

## Design direction

LUCENT uses a restrained monochrome palette: black, white, and neutral grey. The intended material is real backdrop-sampled Liquid Glass—not a blue glassmorphism effect. The design contract and implementation constraints are documented in [DESIGN.md](DESIGN.md).

## Third-party notices and licensing

Third-party dependency information is recorded in [NOTICE.md](NOTICE.md). The repository does not yet include a project-level `LICENSE` file, so do not assume the app's source is available for reuse or redistribution under an open-source licence. A project licence should be chosen and added before inviting external contributions or authorising reuse.

## Feedback

Bug reports and feature requests are welcome through [GitHub Issues](https://github.com/tejastajane/LiquidPlayer-Android/issues). Include your Android version, device model, steps to reproduce, and relevant logs where possible.

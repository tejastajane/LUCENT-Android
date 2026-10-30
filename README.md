# LUCENT for Android

A Kotlin + Jetpack Compose video player powered by libVLC and Kyant0's Backdrop Liquid Glass library.

## Design contract

- True black AMOLED surfaces.
- Apple-inspired clear Liquid Glass: neutral translucent surfaces, realistic blur/refraction, and white/grey reflections.
- **No blue tint, colored glass, blue glow, or blue borders.**
- White vector icons; the bottom navigation contains icons only: Videos, Folder, Browse, More.
- Four main sections: video library, folder view, internal-storage browser, and More/settings.
- Landscape player with floating controls, timeline, audio/subtitle selectors, aspect ratio/display controls, and a tools menu.
- Use the actual `io.github.kyant0:backdrop` effects from https://github.com/Kyant0/AndroidLiquidGlass; do not replace them with generic imitation glass.

## Architecture

- Kotlin / Jetpack Compose UI.
- Kyant0 Backdrop for Liquid Glass refraction/highlights and backdrop effects.
- libVLC Android for broad local video/audio/subtitle support.
- MediaStore for indexed video discovery and Storage Access Framework for browsing/picking files.
- GitHub Actions debug APK build.

## Build

Open this project in current Android Studio and sync Gradle. CI uses Java 21 and runs `gradle assembleDebug`.

## License notes

Backdrop is Apache-2.0. libVLC is LGPL-2.1; before distributing modified or bundled builds, review the applicable license and notices for all dependencies.

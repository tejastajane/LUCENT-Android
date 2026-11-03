# LUCENT design contract

LUCENT uses an Apple-inspired, neutral Liquid Glass visual language. The intended material is real backdrop-sampled glass, not generic blue glassmorphism.

## Palette and theme

- Dark canvas: pure black AMOLED (`#000000`).
- Glass fill: translucent neutral charcoal in dark theme and translucent white in light theme.
- Edges and highlights: restrained white/grey specular highlights.
- Dark-theme controls: white with inactive controls in neutral grey.
- Light-theme controls: neutral dark foregrounds; the selected bottom-navigation icon uses the app's blue accent.
- Do not add blue tint, blue borders, or blue glow to the glass material.

## Liquid Glass implementation

Use the Backdrop and Shapes APIs from Kyant0's AndroidLiquidGlass library, published as `io.github.kyant0:backdrop` and `io.github.kyant0:shapes`.

Glass surfaces sample the content behind them and use blur, lens refraction, highlights, and shadows. Chromatic aberration should remain disabled for UI glass. Player controls and panels should sample the actual video frame wherever their layout provides a meaningful backdrop.

Dialogs and expanding player panels share the same theme-aware Kyant0 material component. The video control buttons retain their separate, original GlassSurface treatment.

## Screens and navigation

1. **Videos:** searchable media library with All, Latest, Movies, TV Shows, and Anime filters.
2. **Folders:** media grouped by storage directory.
3. **Settings:** theme, hardware acceleration, playback gestures, media scan, direct media-management access, and app information.
4. **Player:** portrait and landscape layouts with transport controls, seek bar, audio/subtitle track selectors, external subtitle picker, aspect ratio, brightness, volume, playback speed, bookmarks, favourites, sharing, and information.

The player supports a duration/remaining-time toggle and cumulative feedback for repeated double-tap seeking.

## Validation

Keep the GitHub Actions workflow focused on building and uploading the production APK. Validate playback and visual changes on a physical Android device before merging user-facing experiments into `main`.

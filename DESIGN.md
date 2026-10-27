# Liquid Player design contract

The agreed visual direction is Apple-inspired neutral Liquid Glass, not generic blue glassmorphism.

## Non-negotiable palette

- Canvas: pure black AMOLED (`#000000`).
- Glass fill: extremely subtle neutral charcoal/white translucency.
- Edge/highlight: fine neutral white/grey specular highlights.
- Icons and controls: white, with inactive controls in neutral grey.
- No blue tint, blue borders, blue glow, coloured glass fill, or blue selection state.
- No labels in the bottom navigation. The four tabs are Videos, Folder, Browse, and More.

## Liquid Glass implementation

Use the real backdrop capture and effects API from the user's fork, https://github.com/tejastajane/AndroidLiquidGlass, published as `io.github.kyant0:backdrop`. Surfaces use backdrop sampling, blur, and restrained lens refraction. Chromatic aberration should remain disabled for UI glass. Controls must sample the actual content behind them, especially the video frame in playback. If the neutral effect is not visible on a pure black area, do not compensate with a blue hue; use a slightly stronger neutral surface/highlight instead.

## Screens

1. Videos: searchable media list with thumbnail, duration, size and local path.
2. Folder: media grouped by directory.
3. Browse: storage overview, SAF directory tree and direct file picking.
4. More: media scan, playback hardware acceleration, gestures and feature sections.
5. Landscape player: filename, floating transport controls, seeking, audio/subtitle track selectors, external subtitle picker, aspect ratio, brightness, volume, playback speed, and tools panel.

## Validation

Do not call this a working release until Android CI succeeds and playback is tested with local files on a physical device or emulator.

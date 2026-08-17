# Glyph MapGUI Integration Design

## Goal

Add a separate optional MapGUI integration artifact that lets players draw Glyphcraft strokes on a private 128×128 handheld map screen without changing the existing Paper 1.21.8/Java 21 plugin target.

## Compatibility boundary

The existing `paper` and `java-compiler` modules remain unchanged in platform target. Shared glyph model/capture/rasterization code remains Java 21-compatible. A new `mapgui-integration` module targets Java 25 and the current MapGUI API (`io.github.flog99:mapgui-api:1.0.0`) with `compileOnly` scope.

Deployment requires the matching MapGUI plugin jar on the server. The integration plugin descriptor declares MapGUI as a required server dependency loaded before the integration plugin; it must not enable without MapGUI.

## Drawing interaction

MapGUI’s supported input is repeated right-click callbacks with cursor coordinates, not a raw button-held packet. The screen treats repeated right-clicks as a drag-like stroke:

- first right-click begins a stroke;
- each repeated callback appends the current cursor position and interpolates between samples;
- the stroke stays continuous while the button is held — MapGUI repeats right-click about every 200ms, and the pen is **not** lifted by wall-clock gaps (a 300ms gap while held must not end the stroke);
- the pen is lifted only by an explicit event: left-click (opens the tool overlay), menu open, mode change, or an explicit pause;
- Q closes the screen while preserving the draft.

This gives the requested hold-and-drag behavior within the API’s actual input contract. It does not claim access to a client-side button state that MapGUI does not expose.

## Screen

`GlyphScreen extends Screen` owns per-player UI state and a shared/owned `GlyphCaptureSession`. A `Draw` node renders the full 128×128 raster derived from the vector draft. The overlay provides brush size, undo, clear, save, and close controls. The screen watches the draft/model so each accepted point redraws only the affected screen state.

Saving sends the vector draft to the existing glyph persistence seam. Closing preserves the last snapshot. Recognition remains outside the screen and is invoked by an explicit interpret/confirm action later; drawing itself never changes enchantment power.

## Error behavior

Invalid or oversized points are rejected before entering the draft. A failed save leaves the current in-memory session open and reports an error. Missing MapGUI prevents the integration plugin from loading rather than causing a runtime `NoClassDefFoundError`.

## Verification

- compile the integration module against `mapgui-api:1.0.0` and Java 25;
- test stroke interpolation and pause-based stroke boundaries without a server;
- test screen model updates and clear/undo/save actions with API-compatible test doubles where possible;
- inspect the descriptor’s required MapGUI dependency/load ordering;
- run existing Java 21 compiler/Paper tests unchanged;
- perform a live Paper/MapGUI smoke test separately because the current environment has no server/client session.

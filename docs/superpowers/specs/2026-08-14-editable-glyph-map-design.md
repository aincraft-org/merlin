# Editable Glyph Map Design

## Problem

Glyph drafts currently persist only as metadata on a paper canvas. Saving confirms that metadata changed, but the held item still looks like paper and does not display the drawn glyph as a Minecraft map.

## User Experience

`/glyph book` continues to create a blank editable paper canvas. The first successful save replaces that exact held canvas with one `FILLED_MAP` whose 128×128 image shows the current glyph. The map remains editable: holding it and running `/glyph` reloads the stored strokes, and later saves update the same map item rather than creating another map.

The saved map uses the glyph rasterizer’s intensity as grayscale ink on a dark background. Empty pixels are black; stronger glyph pixels are progressively lighter up to white. The map is locked from normal terrain updates so its glyph image remains stable.

## Architecture

`GlyphDraftStoreAdapter` owns glyph-item identity and serialization for both blank paper canvases and filled glyph maps. It recognizes only items carrying the plugin marker and stable glyph UUID. A conversion operation validates the held item and expected UUID, serializes the draft, creates a Bukkit map view when necessary, applies the glyph pixels, copies the glyph metadata onto the filled map, and returns the replacement item. Updating an existing glyph map keeps its existing map view and UUID.

A dedicated map renderer supplies the rasterized glyph image through Bukkit’s map rendering API. It owns no draft persistence and renders only the immutable pixel image supplied at save time. The map view removes prior renderers before installing this renderer, preventing duplicate overlays on repeat saves.

`GlyphMapGuiPlugin` performs the inventory mutation only after conversion succeeds. It verifies the same glyph item remains in the main hand, replaces that slot atomically, and reports success. Serialization overflow, invalid identity, missing map metadata, or map allocation failure leaves the original held item unchanged and reports failure.

## Data Flow

1. The editor snapshots the active `GlyphStrokeTracker`.
2. The store validates the held item and its expected glyph UUID.
3. The store encodes the complete editable draft within the existing size limit.
4. For paper, the store allocates a map view and creates one filled-map replacement. For an existing glyph map, it reuses its view.
5. The renderer receives `GlyphRasterizer.renderFull(draft)` pixels and installs them on the map view.
6. The replacement map receives the marker, stable UUID, and encoded draft metadata.
7. The plugin replaces the exact main-hand item only after all prior steps succeed.
8. A future `/glyph` call loads the draft from the held map through the same UUID-bound path.

## Correctness

- A successful first save converts exactly one paper canvas into exactly one filled map.
- The converted map preserves the original glyph UUID and complete editable draft.
- Repeat saves reuse the existing map view and do not duplicate items or renderers.
- Only plugin-marked paper or filled-map items are accepted as glyph canvases.
- A changed or invalid held item is never overwritten.
- Failed conversion leaves the original item unchanged.
- Every rendered coordinate maps deterministically to the corresponding 128×128 glyph pixel.
- The saved image remains stable across map viewing and server restarts.
- Existing paper glyph drafts remain readable and convert on their next save.

## Testing and Verification

Unit tests cover item recognition, paper-to-map conversion, UUID and draft preservation, repeat-map updates, invalid-item rejection, and serialization limits. Renderer tests verify empty, partial-intensity, and full-intensity pixel conversion at exact coordinates. Plugin-level tests isolate the main-hand replacement rule. Final verification builds the plugin, runs the Java/compiler and MapGUI integration suites, starts the Paper server, creates and saves a glyph map, and confirms that the held item is a filled map that can reopen its draft.
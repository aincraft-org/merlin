# Glyph Element Color Raster — Design Spec

> Status: approved for implementation
> Date: 2026-08-17

## Goal

Ink on the glyph canvas is one of four element colors. Overlap **computes** a mixed color. The CNN sees that computed RGB field (`3×64×64`), not a 1-bit mask and not a school token list.

## Palette

| Element | Name | Hex | RGB 0..1 (÷255) |
|---|---|---|---|
| `FIRE` | ember | `#FF4D00` | `1`, `77/255`, `0` |
| `FROST` | glacier | `#3DDCFF` | `61/255`, `220/255`, `1` |
| `ARCANE` | violet | `#B44AFF` | `180/255`, `74/255`, `1` |
| `PHYSICAL` | bone | `#E8E4D9` | `232/255`, `228/255`, `217/255` |

Default stroke element is `PHYSICAL` (today’s chalk).

## Blend

Canvas starts at RGB `(0,0,0)`. Each brush stamp of color `C` with coverage `a`:

```text
a = clamp(radius + 0.5 − distance, 0, 1)
out = (1 − a) · old + a · C
```

`distance` is pixel-center to stamp center. Java and Python must match this formula (same float32 ops, same loop order is not required if results match within `1e-5`).

## CNN

- Preprocessor version: `preprocessing-v2`
- Raster tensor: `[batch, 3, 64, 64]` float32, channels R,G,B in `0..1`
- Crop/pad/resample unchanged (ink = any channel `> 0`)
- `RasterEncoder` first conv: `Conv2d(3, 8, 3, padding=1)`
- Manifest `input_schema.raster.shape` is `[null, 3, 64, 64]`
- 1-channel bundles are incompatible (`BundleException`)

## Language

Color does **not** insert a school page. `fire` remains a classified glyph. Mix lives in pixels.

## Non-goals

- Retraining a release ONNX in this drop (architecture + tests first)
- Pen color picker UI (strokes default physical; tests and fixtures may set element)
- School-set grammar from hue

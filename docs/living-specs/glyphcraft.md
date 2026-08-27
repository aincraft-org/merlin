# Glyphcraft — Living Spec
> Status: active
> Last updated: 2026-08-25

## Intent

A glyph is one map: one drawing, one classified word, optional pips, a printed mana cost. A tome is those maps bound as pages. The whole book is the spell.

Shape classifies the word. Magical ink is how a `GlyphElement` is supplied onto strokes. When a word cares about ink, that element is the school; which words care is not settled.

Player-facing language: `docs/glyphcraft-language.md`.

## Boundaries

### In scope

- Map canvases, classify/save, pips, tomes, bind/tear/cast
- Four elements: physical, flame, frost, arcane (`GlyphElement` colors)
- Magical ink as a domain fill plus a Paper potion bottle tinted to the school
- Stroke-level element for raster color
- In-pen color chips for filled inks in inventory
- Mortar & pestle grinding: vanilla flowers become full ink bottles

### Out of scope / non-goals

- Scribe books (separate language, separate items)
- Mana pool (mana is printed, not spent)
- Classifier treating color as the class label

## Invariants

- A classified word has exactly one role.
- `GlyphStroke.element` is a `GlyphElement`. Default is physical.
- Placement and ink color do not decide the classifier class (shape still names the word).
- Magical ink is aligned to exactly one `GlyphElement`.
- The school word and ink name is `flame` (not `fire`). `fire` still loads as flame.
- No remaining fill of the selected color means no new stroke.
- One durability per new stroke, charged on the first accepted point. Interpolated points are free.
- Empty bottles stay in inventory; they do not vanish.
- Undo/clear do not refund durability.
- Stamp is not drawing and does not require ink.
- `merlin-api` has no Bukkit types. `MagicalInk` lives in api; `InkStore` lives in paper.

## Implementation guidance

- Two classes: `MagicalInk` (api) and `InkStore` (paper). Not four ink subclasses.
- Canvas is main hand. Color chips list filled inks from inventory (off-hand first, then storage). Armor is ignored.
- Charge spends the first remaining bottle of the **selected** color. Write failure aborts the stroke.
- A stroke keeps the element that paid for it even if selection changes mid-stroke.
- Rehydrate a draft by copying each stroke's element. Do not spend ink.
- Failed strokes are silent no-ops (no chat spam).
- Display names: Physical / Flame / Frost / Arcane Ink.
- The Paper item is a potion bottle tinted to the school. Hide potion effects on the tooltip. Unset consumable so it cannot be drunk. Old glass-bottle inks still read; write converts them.
- Chip order: physical, flame, frost, arcane. Missing fills are hidden.
- Grinding lives in paper beside `InkStore`: a pure `FlowerGrind` mapping (`Material -> GlyphElement`, a data table, not per-flower branches) plus a thin interact listener.
- The mortar & pestle is a marked bowl with PDC, same marker style as ink bottles. No durability. Crafted shapeless from bowl + stick.
- Grind gesture: mortar main hand + mapped flower off hand + right-click consumes the flower and yields one full bottle. Unmapped off-hand items are silent no-ops.
- Do not implement school-from-ink until the ink-agnostic word list is decided.
- Tests: MagicalInk and tracker payment without a live inventory. `InkStore` follows tome PDC style.
- Stroke capture densifies sparse poll samples with a quadratic midpoint spline on the live append path so turns cut inside the corner instead of kinking at each poll. Spline any gap >1px from recent poll history; a 1px prefix does not disable later rounding. 1px steps stay linear so the 256-point rollover still fires. Do not spline across `pause` / `endStroke`. Velocity width smoothing stays independent of the path curve. Interpolated in-between points are free.

## Current

- [x] Glyph maps, classify/save, pips, tome bind/cast
- [x] `GlyphElement` palette and per-stroke element on drafts
- [x] `MagicalInk` fill type in `merlin-api`
- [x] Capture session tags strokes with the paid element
- [x] Stroke tracker charges 1 durability per new stroke from an ink supplier
- [x] `InkStore` Paper bottle (PDC, damage bar, max stack 1)
- [x] Potion bottle tinted to the school; tooltip hides potion effects; not drinkable
- [x] `/glyph ink <element>` authoring hatch
- [x] Pen color chips from inventory fills; selected color is spent
- [x] Player-facing ink rules in `docs/glyphcraft-language.md`
- [x] School word `flame` (`fire` still loads)
- [x] Sparse canvas-poll samples spline so turns are not sharp poll vertices
- [x] Mortar & pestle item: marked bowl, shapeless bowl + stick recipe
- [x] Grind gesture: flower off hand + mortar main hand yields one full matching bottle

### Current notes

Ink is the drawing medium. School-from-ink for specific words is next, not this slice.

## Next

- [ ] Decide which words are ink-agnostic (still classify from shape only)
- [ ] When a word is not agnostic, ink on that stone is the school (no separate school page)

## Future

- [ ] Durability refund on undo
- [ ] Classifier trained on color as a class channel
- [ ] Retrain catalog/model ids from `fire` to `flame`

## Decisions log

| Date | Decision | Why |
|---|---|---|
| 2026-08-24 | Ink is the school on a stone that cares about ink | Shape stays the word; color supplies element |
| 2026-08-24 | Two classes: `MagicalInk` + `InkStore` | Substance in api, bottle in paper |
| 2026-08-24 | No remaining fill → no stroke; physical is a real fourth ink | Ink is required |
| 2026-08-24 | Spend vanilla durability, 1 per stroke | Bottle is a resource; interpolation is free |
| 2026-08-24 | Color chips from inventory; off-hand preferred for spend and default pick | Canvas occupies main hand |
| 2026-08-24 | Empty bottle remains; undo does not refund | Refill can exist later; spend is one-way |
| 2026-08-24 | Mixed-element drafts allowed at this layer | Selection can change between strokes |
| 2026-08-24 | Element names are physical, flame, frost, arcane | Not studio flavor names; school word is flame |
| 2026-08-24 | Quadratic midpoint spline between poll samples | Round poll kinks; interpolating cubics overshoot outside the turn |
| 2026-08-25 | Flowers grind to full bottles via a handheld mortar & pestle | Explicit user request promotes acquisition into scope; crafting leaves non-goals |
| 2026-08-25 | One flower → one full bottle; refill dropped | Refill would cost exactly what a fresh bottle costs |
| 2026-08-25 | Generous mapping: several flowers per element | Torchflower needs sniffer farming; strict gating starves flame early game |

## Open questions

- [ ] Which words are ink-agnostic (patients, charms, school pages, catalog flag)?

# Ritual Crafting — Living Spec
> Status: active
> Last updated: 2026-08-25

## Intent

Ritual Crafting turns frozen glyph maps and vanilla materials into magical crafting intermediates. A drawn glyph is the recipe; a ritual circle is the station; pips determine yield. This is not enchanting gear directly — it produces reagents that future gear-crafting systems can consume.

## Boundaries

### In scope

- A placeable **Ritual Anchor** block (a marked tile-entity block, e.g., a dropper) and surrounding **Pedestal** markers (same block type).
- A right-click activation using the existing Mortar & Pestle.
- One frozen glyph map + one vanilla material placed on the anchor as the core inputs.
- One matching magical ink bottle on a surrounding pedestal as fuel.
- One element-specific catalyst block (or item) per school in the surrounding layout.
- Output as a marked, non-vanilla-useful intermediate item (e.g., Aether Ingot, Sun Drop, Cinder Salt, Rime Crystal, Kinetic Coil).
- Static recipe table keyed by `(glyph word, material)` → output item.
- Pip-based yield scaling for known recipes.
- Sound + particle feedback on success and failure.

### Out of scope / non-goals

- Producing finished weapons, armor, wands, or scrolls (those are future gear-crafting domains).
- Consuming whole tomes or multi-page spells.
- Custom block models or tile entity rendering.
- Player skill trees, progression, or recipe unlocking.

## Invariants

- A ritual only consumes inputs and produces output when every required slot is satisfied.
- The glyph map is consumed; the material, ink bottle, and catalyst are consumed.
- Output quantity is determined by the glyph's pips for that recipe, not by randomness alone.
- The ritual anchor belongs to exactly one glyphcraft plugin; it does not overlap with the Scribe language.
- Intermediates are marked with PDC and are not usable as vanilla gear without a future crafting step.
- Failure is silent on missing inputs and audible/visible on activation with an invalid layout.

## Implementation guidance

- Keep the ritual logic in `merlin-paper` beside `InkStore` and the new `GrindListener`. No Bukkit types in `merlin-api`.
- Use `TileState` PDC markers for the anchor and pedestals; mark them on `BlockPlaceEvent` when placed by the special anchor/pedestal item. Do not introduce custom block entities for v1.
- Model the recipe table as pure data: `Map<RecipeKey, RecipeResult>` where `RecipeKey` is `(Label word, Material material)`.
- `RitualCircle` is a pure class that, given an anchor `Block`, inspects the anchor's inventory and the inventories of the surrounding pedestal blocks, returning a `RitualValidation`.
- `RitualListener` is the thin `PlayerInteractEvent` wrapper; it cancels the event when the player holds a Mortar & Pestle and activates the ritual.
- Output items are created through the same PDC item pattern as Mortar & Pestle and magical ink.
- Tests: pure layout validation and recipe lookup without a live server; listener tests mock `PlayerInteractEvent`.
- Pedestals in v1 are the same tile-entity block as the anchor (e.g., dropper) with a different PDC marker; items are placed inside their inventories.

## Current

- [x] Placeable Ritual Anchor item and PDC marker.
- [x] Pedestal detection around the anchor (radius 1, cardinals or full 3x3).
- [x] Static recipe table for at least four intermediates:
  - `damage` + iron ingot → Aether Ingot
  - `heal` + gold ingot → Sun Drop
  - `push` + copper ingot → Kinetic Coil
  - `flame` (school word) + redstone → Cinder Salt
  - `frost` (school word) + amethyst → Rime Crystal
- [x] Activation with Mortar & Pestle consumes inputs and drops output.
- [x] Pip-to-yield mapping (1 pip = 1 output, 5 pips = 3 outputs + small pure bonus chance).
- [x] Player-facing section in `docs/glyphcraft-language.md` under a new "Ritual Crafting" heading.

## Next

- [ ] Catalyst block reagents per school (flame: coal block, frost: snow block, arcane: lapis block, physical: stone).
- [ ] Geared crafting domain that consumes these intermediates.

## Future

- [ ] Custom block models for anchor and pedestals.
- [ ] Recipe unlocking / player discovery.
- [ ] Multi-school rituals combining two glyphs.

## Decisions log

| Date | Decision | Why |
|---|---|---|
| 2026-08-25 | Ritual Crafting produces intermediates, not finished gear | Keeps the domain focused; future gear-crafting can consume these reagents |
| 2026-08-25 | Core input is one frozen glyph + one vanilla material | Glyphs are already the recipe language; materials give the output base |
| 2026-08-25 | Use `DROPPER` for anchor and pedestal blocks | Gives a free inventory, no barrel rotation, and right-click can be cancelled for activation |
| 2026-08-25 | Separate `Ritual Anchor` and `Ritual Pedestal` craftable items | Avoids placement-mode confusion and makes the layout readable |

## Open questions

- [x] Use `DROPPER` as the tile-entity block for both anchor and pedestal.
- [x] Use two separate craftable items: `Ritual Anchor` and `Ritual Pedestal`.

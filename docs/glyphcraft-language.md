# Glyphcraft Tome Language

A glyph is one map: one drawing, one classified word, optional pips, a printed mana cost. A tome is those maps bound as pages. Flip to look. The whole book is the spell.

Scribe writes ordinary words onto text pages. Glyphcraft writes stones and binds them. Both can say “burn the thing I am looking at.” Only the tome is `flame` next to `damage ●●●●●`. Only the Scribe page is `summon sheep`.

## Stones

`/glyph book` gives a blank canvas. Hold it and run `/glyph` (or hold it and open the pen) to draw.

- Draw on the map. Color chips on the pen are the inks in your inventory with remaining fill. Click a chip to paint with that element. No remaining ink lays no stroke.
- Four elements: physical, flame, frost, arcane. Each new stroke spends 1 durability from a bottle of the selected color (off-hand first, then the bag). Undo does not refund.
- `/glyph ink <element>` gives a full bottle (`physical`, `flame`, `frost`, `arcane`).
- The pip clicker sits above the canvas: `◀ ●●●●● ▶`. Left is −1, right is +1, clamp `1..5`. Sneak+left jumps to 1; sneak+right jumps to 5.
- Left-click opens the tool row: Classify, Save, Undo, Clear, Close.
- Classify names the drawing. Save freezes that label, the current pips, and the printed mana onto the stone.
- A canvas with ink but no frozen label will not bind into a tome.

Pips belong to **that** stone. They are overlay chrome, not ink, and they are not classified. After classify, a school or patient stone is forced to 1 pip. Inventory cannot retune a frozen stone.

`/glyph stamp <label> [pips]` is an authoring hatch: it creates a frozen stone without drawing and does not require a bottle. Allowed labels are the grammatical words below. Reserved words will not stamp.

## Types

Every classified word has exactly one role. A combat tome is a bag of pages, typed — not shared pixels, not page order.

| Role | Words | Hunger | Pips |
|---|---|---|---|
| Effect | `damage`, `heal`, `push` | Head of a combat term. Exactly one per tome | Yes — magnitude `1..5` |
| School | `physical`, `flame`, `frost`, `arcane` | Wants that effect. At most one | No |
| Patient | `self`, `target` | Wants that effect. At most one | No |
| Charm | `sharpness` | Wants a sword, not a combat tome | Yes — enchantment rank `1..5` |

`shield` classifies as an effect. It has no cast in this drop and will not bind.

Page order is how you flip. It does not change meaning. At most three pages.

## Implicits

Applied only to what the pages left open:

| Omitted | Fills as |
|---|---|
| Patient | `target` |
| School | none (plain). `damage` still burns |
| Pips | `1` |

`heal` does **not** default to `self`. One patient implicit.

`damage` / `heal` magnitude equals the pips. `push` pips clamp to 3 (the shove cap). A target patient looks 32 blocks along the crosshair.

## Legal terms

| Pages | Casts as |
|---|---|
| `damage` | `burn target` 1 |
| `damage ●●●●` | `burn target` 4 |
| `flame` + `damage ●●●●●` | `burn target` 5 |
| `heal` + `self` | `mend self` 1 |
| `push ●●●●●` | `shove target` 3 |

A lone `flame` (or a lone `self`) is unfinished. It may sit in a tome while you build. It will not cast.

Illegal: two effects, two schools, two patients, a charm mixed into a combat tome, more than three pages, a reserved word.

## Reserved

These classify. They will not stamp, will not bind, and will not compile:

| Role | Words |
|---|---|
| Aim | `target-ray` |
| Trigger | `on-hit`, `on-hurt`, `on-use`, `periodic` |
| Condition | `if-health`, `if-undead`, `if-outdoors` |
| Scope | `area`, `repeat` |
| Limit | `cooldown`, `charges` |
| Patient (locked) | `attacker` |

`reject` is never a word.

## Commands

Permission `merlin.glyph.draw` opens the pen. Tome commands also accept `merlin.glyph.tome`.

| Command | Holds | Does |
|---|---|---|
| `/glyph book` | — | Blank canvas |
| `/glyph ink <element>` | — | Full magical ink bottle (`physical`, `flame`, `frost`, `arcane`) |
| `/glyph` | canvas | Opens the pen |
| `/glyph stamp <label> [pips]` | — | Frozen stone without drawing |
| `/glyph tome` | — | Empty tome |
| `/glyph bind` | map main, tome offhand | Bind the stone as a page. Sneak consumes the map; otherwise the loose stone stays |
| `/glyph tear` | tome | Tear the current page back into a stone |
| `/glyph cast` | tome main | Compile the pages and cast |
| `/glyph enchant` | sharpness map main, sword offhand | Bind vanilla Sharpness onto that sword |

### Grinding Ink

Craft a **Mortar & Pestle** from one bowl and one stick. Hold it in your main hand and a mapped flower in your off hand, then right-click air or a block. The flower is consumed and produces one full bottle of the matching ink. The mortar has no durability.

| Element | Flowers |
|---|---|
| Flame Ink | Torchflower, poppy |
| Frost Ink | Blue orchid, cornflower |
| Arcane Ink | Allium, wither rose, lilac |
| Physical Ink | Oxeye daisy, dandelion, azure bluet, sunflower |

Other flowers do nothing and are not consumed. Grinding creates a new full bottle; it does not refill an empty bottle. `/glyph ink <element>` remains the authoring shortcut.

### Ritual Crafting

Build a 3×3 ritual circle: one **Ritual Anchor** in the center and **Ritual Pedestals** around it. Place a frozen glyph map and a vanilla material in the anchor, and a matching ink bottle in a pedestal. Right-click the anchor with a Mortar & Pestle to create magical intermediates. Pips on the glyph determine output count.

| Glyph | Material | Output |
|---|---|---|
| `damage` | iron ingot | Aether Ingot |
| `heal` | gold ingot | Sun Drop |
| `push` | copper ingot | Kinetic Coil |
| `flame` | redstone | Cinder Salt |
| `frost` | amethyst shard | Rime Crystal |


Right-click a frozen map while a tome is in the off hand is the same bind as `/glyph bind`. Sneak still consumes.

A charm will not bind as a tome page. A reserved or unfinished-for-the-wrong-reason stone will not bind. A lone school or patient may.

Cast fails atomically on an unfinished or illegal tome: a diagnostic, no effect. Missing the looked-at target also does nothing.

## Sharpness

`sharpness` is not in the current classifier catalog. Stamp it (`/glyph stamp sharpness` through `/glyph stamp sharpness 5`), or classify it once that class ships.

`/glyph enchant` consumes the frozen sharpness map and writes vanilla Sharpness I–V onto the off-hand sword. The host must be a sword. Equal or greater Sharpness already on the blade fails and the map stays.

## Mana

Mana is printed on the stone, not spent.

| Token | Printed |
|---|---|
| Effect, pips *n* | `2 + n` (I = 3, V = 7) |
| School | `2` |
| Patient | `1` |
| Charm `sharpness`, pips *n* | `2 + 2n` (I = 4, V = 12) |

Tome lore shows the sum of its pages. There is no mana pool in this drop.

## Different from Scribe

Scribe pages are lines of words in a marked writable book. Glyphcraft pages are map items in a marked tome. They share the combat tape (`burn` / `mend` / `shove`) and they do not share identities, item types, or the rest of the lexicon. A Scribe book is never a tome.

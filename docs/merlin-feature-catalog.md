# Merlin Feature & Enchantment Catalog

> **Scope:** current player-visible behavior implemented by the Merlin Paper plugin, plus the registered enchantment catalog.
>
> **Source rule:** runtime source, registry definitions, and `config.yml` are authoritative. Living player documentation provides terminology. Design specs and companion mockups are historical or proposed unless the source is wired and active.

## At a glance

Merlin currently exposes:

- **81 registered altar definitions:** 6 vanilla/over-cap definitions and 75 custom definitions.
- A custom **Eterna/Quanta Enchanter's Altar Matrix** opened by right-clicking an enchanting table.
- Marked Scribe books containing a bounded natural-language spell DSL.
- Glyph maps, elemental ink, glyph tomes, classification, and spell casting.
- Ritual Anchors and Ritual Pedestals that turn frozen glyphs plus materials into marked intermediates.
- A modular custom-enchantment dispatcher covering combat, tools, armor, movement, fishing, buckets, projectiles, and passive equipment effects.
- ONNX glyph-classifier loading through the Paper plugin loader.

## Source hierarchy and status labels

| Label | Meaning |
|---|---|
| **Current** | Wired by `MerlinPlugin.onEnable()` or implemented in the active command/listener path. |
| **Next** | Listed by an active living spec as the next slice, but not part of the current runtime contract. |
| **Future** | Explicitly deferred or not implemented. |
| **Proposed** | Described by a design spec but not sufficient evidence of runtime support. |

Primary source files:

- [MerlinPlugin.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java) — plugin wiring.
- [EnchantmentRegistry.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java) — enchantment registrations and targets.
- [config.yml](../merlin-paper/src/main/resources/config.yml) — model, altar, offer, and disabled-enchantment defaults.
- [Glyphcraft living spec](living-specs/glyphcraft.md) and [Glyphcraft language guide](glyphcraft-language.md).
- [Ritual Crafting living spec](living-specs/ritual-crafting.md).
- [Scribe language guide](scribe-language.md).

---

# Enchantment catalog

## Registry rules

- Vanilla definitions use their `minecraft` keys and may be stored natively up to their vanilla maximum. Higher ranks are stored in the `merlin:overcap_enchantments` PDC container and handled by the over-cap dispatcher where a handler exists.
- Custom definitions use `merlin:<key>` keys, `vanillaMaxLevel = 0`, and are stored in `merlin:overcap_enchantments` even at rank I. Lore mirrors the stored custom rank.
- The **Max** column is the registered absolute maximum rank, not the rank guaranteed by a particular altar tier.
- The row target is the direct gear target. A plain `BOOK` is also a universal matrix target, so the altar can generate and persist custom enchanted books; applying a definition to concrete gear still uses the row target rules.
- Custom ranks are selected only when the target and Eterna requirement are eligible. They are weighted rolls, not a guarantee that every roll contains a different enchantment.

## Completeness audit

The tables use the exact keys from `EnchantmentRegistry.defaultRegistry()`: 6 `minecraft:` definitions and 75 `merlin:` definitions. The executable registry contract is [CustomEnchantmentRegistryTest.java](../merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/CustomEnchantmentRegistryTest.java); target eligibility is covered by [EnchantmentRegistryTest.java](../merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistryTest.java) and offer bounds by [QuantaRollEngineTest.java](../merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/QuantaRollEngineTest.java).

To re-audit the catalog after registry changes:

```text
./gradlew :merlin-paper:test --tests 'dev.mintychochip.merlin.paper.enchanting.CustomEnchantmentRegistryTest' --tests 'dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistryTest' --tests 'dev.mintychochip.merlin.paper.enchanting.QuantaRollEngineTest'
```

## Target shorthand

| Shorthand | Materials |
|---|---|
| `swords` | Wooden, stone, iron, golden, diamond, and netherite swords. |
| `weapons` | `swords` plus wooden, stone, iron, golden, diamond, and netherite axes. |
| `weapons/tools` | Swords, axes, pickaxes, shovels, and hoes. |
| `custom tools` | Pickaxes, axes, shovels, and hoes. |
| `pickaxes` | Wooden through netherite pickaxes. |
| `pickaxes + shovels` | Wooden through netherite pickaxes and shovels. |
| `durable items` | `custom tools` plus bows, crossbows, and tridents. |
| `all armor` | Leather, chainmail, iron, golden, diamond, and netherite armor. |
| `leather armor` | Leather helmet, chestplate, leggings, and boots. |
| `boots` | Leather through netherite boots. |
| `leggings` | Leather through netherite leggings. |
| `bows` / `crossbows` | The corresponding ranged weapon. |
| `fishing rods` | Fishing rods. |
| `elytra` | Elytra. |
| `saddle` | Saddles. |
| `buckets` | Empty and water buckets where listed. |

## Vanilla and over-cap definitions (6)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Sharpness (`minecraft:sharpness`) | `swords` | VII | Normal Sharpness behavior through V; the over-cap handler adds the registered high-rank damage delta. | Native enchantment through V; over-cap PDC and damage handler. |
| Smite (`minecraft:smite`) | `swords` | VII | Normal Smite behavior. | Native enchantment; no custom over-cap handler. |
| Fortune (`minecraft:fortune`) | `pickaxes` | V | Normal Fortune behavior through III; the over-cap handler supports high-rank drop scaling. | Native enchantment through III; over-cap PDC and block-drop handler. |
| Efficiency (`minecraft:efficiency`) | `pickaxes` | VII | Normal Efficiency behavior. | Native enchantment; no custom over-cap handler. |
| Protection (`minecraft:protection`) | Iron, diamond, and netherite armor | VI | Normal Protection behavior. | Native enchantment; no custom over-cap handler. |
| Unbreaking (`minecraft:unbreaking`) | `swords` | V | Normal Unbreaking behavior. | Native enchantment; no custom over-cap handler. |

## Core custom definitions (24)

These are the original Merlin custom entries. All are `merlin:` PDC-backed definitions.

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Sticky Grip (`merlin:sticky_grip`) | `weapons/tools` | I | Prevents the holder from dropping the enchanted item. | Player item-drop event. |
| Equilibrium (`merlin:equilibrium`) | `weapons` | V | Adds a random 1–3 damage bonus per rank and applies half of that bonus back to the attacker through a guarded damage call. | Entity hit; PDC rank. |
| Nether's Scourge (`merlin:nethers_scourge`) | `weapons` | VI | Adds a random 1–3 damage bonus per rank against native Nether hostile mobs. | Entity hit; PDC rank. |
| Cold Aspect (`merlin:cold_aspect`) | `weapons` | III | Extends the victim's freeze duration to at least 20 ticks per rank. | Entity hit. |
| Confusing Aspect (`merlin:confusing_aspect`) | `weapons` | III | Applies Nausea for 100 ticks per rank. | Entity hit. |
| Toxin Aspect (`merlin:toxin_aspect`) | `weapons` | III | Applies Poison for 80 ticks per rank. | Entity hit. |
| Knowledge (`merlin:knowledge`) | `weapons` | III | Adds a random 3–4 experience per rank on entity death. | Entity death. |
| Vorpal (`merlin:vorpal`) | `weapons` | III | Gives a 0.5% per-rank chance to produce a mob head. | Entity death. |
| Vampirism (`merlin:vampirism`) | `weapons` | V | Heals the attacking player on successful hits; the amount is rank-scaled by the handler. | Entity hit. |
| Flurry (`merlin:flurry`) | `swords` | III | On entity interaction, knocks back up to three times the rank in nearby living entities within four blocks. | Entity interaction. |
| Array (`merlin:array`) | `bows` | II | Spawns two additional projectiles per rank with a small random spread. | Bow shoot; projectile spawn. |
| Plunder (`merlin:plunder`) | `bows` | III | Adds one copy of each non-empty original entity drop per rank. | Entity death. |
| Wisdom (`merlin:wisdom`) | `bows` | III | Adds base death experience multiplied by the enchantment rank. | Entity death. |
| Drill (`merlin:drill`) | `pickaxes` | III | Vein-mines matching connected ores, up to four blocks per rank. | Block break with cascade guard. |
| Expertise (`merlin:expertise`) | `pickaxes` | III | Registered metadata-only enchantment; no effect handler is currently attached. | PDC rank only. |
| Quenching (`merlin:quenching`) | `leggings` | IV | Restores up to the rank when food level decreases, capped at full food. | Food-level change. |
| Colorama (`merlin:colorama`) | `leather armor` | I | Randomizes leather armor color when the item takes damage. | Item damage. |
| Leaping (`merlin:leaping`) | `saddle` | III | Adds 0.1 horse jump power per rank. | Horse jump. |
| Feather Hooves (`merlin:feather_hooves`) | `saddle` | I | Prevents horse fall damage. | Entity environmental damage. |
| Molten Touch (`merlin:molten_touch`) | `custom tools` | I | Smelts eligible block drops. | Block drop. |
| Prismatic (`merlin:prismatic`) | Shears | I | Produces additional shearing drops. | Shear event. |
| Overflowing (`merlin:overflowing`) | `buckets` | I | Refills an emptied water bucket. | Bucket empty. |
| Vacuum (`merlin:vacuum`) | `buckets` | I | Returns an empty bucket when a bucket is filled. | Bucket fill. |
| Heat Wave (`merlin:heat_wave`) | Flint and steel | I | Places fire around the interaction location. | Active interaction. |

## Tools and durability custom definitions (12)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Telepathy (`merlin:telepathy`) | `custom tools` | I | Routes mined drops into the player's inventory. | Block drop. |
| Timber (`merlin:timber`) | Axes | III | Breaks connected matching logs with cascade protection. | Block break. |
| Trench (`merlin:trench`) | `pickaxes + shovels` | III | Breaks an area around the original block with bounded cascades. | Block break. |
| Replanter (`merlin:replanter`) | Hoes | I | Replants a mature crop after harvest. | Block break. |
| Planter (`merlin:planter`) | Hoes | I | Plants seeds in a 3×3 area when interacting with tilled soil. | Active interaction. |
| Carrot Planter (`merlin:carrot_planter`) | Hoes | I | Plants carrots in a 3×3 area when interacting with tilled soil. | Active interaction. |
| Potato Planter (`merlin:potato_planter`) | Hoes | I | Plants potatoes in a 3×3 area when interacting with tilled soil. | Active interaction. |
| Experience (`merlin:experience`) | `pickaxes` | III | Adds experience from eligible ore breaks. | Block break. |
| Rebreather (`merlin:rebreather`) | `pickaxes` | III | Restores 20 air ticks per rank while mining underwater. | Block break. |
| Replenish (`merlin:replenish`) | `pickaxes` | III | Restores food while mining. | Block break. |
| Unbreakable (`merlin:unbreakable`) | `durable items` | I | Prevents item damage from reducing durability. | Item damage. |
| Reforged (`merlin:reforged`) | `durable items` | V | Reduces incoming item damage. | Item damage. |

## Armor and defense custom definitions (10)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Aegis (`merlin:aegis`) | `all armor` | III | Grants Speed after fall damage for 100 ticks per rank. | Environmental damage. |
| Angelic (`merlin:angelic`) | `all armor` | III | Heals the wearer after taking damage. | Armor defense. |
| Armored (`merlin:armored`) | `all armor` | III | Reduces damage from sword attackers by 10% per rank. | Armor defense. |
| Chunky (`merlin:chunky`) | `all armor` | III | Reduces incoming damage by 5% per rank. | Armor defense. |
| Dodge (`merlin:dodge`) | `all armor` | III | Has a 10% per-rank chance to cancel physical attacks; projectiles are not included. | Armor defense. |
| Heavy (`merlin:heavy`) | `all armor` | III | Reduces arrow damage by 10% per rank. | Armor defense. |
| Molten (`merlin:molten`) | `all armor` | III | Sets a living attacker on fire for 60 ticks per rank. | Armor defense. |
| Reflect (`merlin:reflect`) | `all armor` | III | Reflects 10% of initial damage per rank. | Armor defense. |
| Safeguard (`merlin:safeguard`) | `all armor` | III | Grants Resistance for 60 ticks per rank when defending. | Armor defense. |
| Tank (`merlin:tank`) | `all armor` | III | Reduces damage from axe attackers by 10% per rank. | Armor defense. |

## Combat custom definitions (7)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Bleed (`merlin:bleed`) | `weapons` | III | Applies Wither for 80 ticks per rank. | Entity hit. |
| Blind (`merlin:blind`) | `weapons` | III | Applies Blindness for 100 ticks per rank. | Entity hit. |
| Block (`merlin:block`) | `weapons` | III | Has an 8% per-rank chance to cancel the incoming hit. | Entity hit. |
| Berserk (`merlin:berserk`) | `weapons` | III | Grants Strength and applies Mining Fatigue for 60 ticks per rank. | Entity hit. |
| Critical (`merlin:critical`) | `weapons` | III | Adds 10% damage per rank for airborne critical hits. | Entity hit. |
| Double Strike (`merlin:double_strike`) | `weapons` | III | Deals an additional hit based on initial damage multiplied by rank. | Entity hit. |
| Thunderlord (`merlin:thunderlord`) | `weapons` | III | Calls lightning on every third consecutive hit. | Entity hit and bounded hit tracking. |

## Ranged custom definitions (3)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Archer (`merlin:archer`) | `bows` | III | Increases projectile velocity by 10% per rank. | Bow shoot. |
| Marksman (`merlin:marksman`) | `crossbows` | III | Increases projectile velocity by 8% per rank. | Bow shoot. |
| Sniper (`merlin:sniper`) | `bows` | III | On a projectile hit near the victim's eye height, applies extra damage based on projectile base damage multiplied by rank. | Projectile hit. The runtime behavior is not a strict headshot raycast or a generic “double damage” rule. |

## Fishing and water custom definitions (6)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Auto Reel (`merlin:auto_reel`) | `fishing rods` | III | Automatically pulls the hook on a bite. | Fishing bite. |
| Bait (`merlin:bait`) | `fishing rods` | III | Duplicates caught item drops once per rank. | Fishing catch. |
| Hook (`merlin:hook`) | `fishing rods` | III | Grants four experience per rank on a catch. | Fishing catch. |
| Snap (`merlin:snap`) | `fishing rods` | III | Teleports a caught entity to the player. | Fishing catch. |
| Fire Hook (`merlin:fire_hook`) | `fishing rods` | III | Sets a hook-contact victim on fire for 80 ticks per rank. | Hook contact. |
| Poisoned Hook (`merlin:poisoned_hook`) | `fishing rods` | III | Applies Poison for 80 ticks per rank on hook contact. | Hook contact. |

## Mobility and passive custom definitions (13)

| Name / key | Target | Max | Implemented behavior | Trigger / storage |
|---|---|---:|---|---|
| Lava Walker (`merlin:lava_walker`) | `boots` | III | Converts nearby lava into obsidian while moving. | Player movement. |
| Water Walker (`merlin:water_walker`) | `boots` | III | Creates temporary frosted ice while moving over water. | Player movement. |
| Plummet (`merlin:plummet`) | `all armor` | III | Damages nearby mobs when the wearer takes fall damage. | Environmental damage. |
| Jelly Legs (`merlin:jelly_legs`) | `all armor` | III | Cancels fall damage. | Environmental damage. |
| Gears (`merlin:gears`) | `all armor` | III | Applies Speed while equipped; amplifier scales with rank. | Passive equipment tick. |
| Springs (`merlin:springs`) | `all armor` | III | Applies Jump Boost while equipped. | Passive equipment tick. |
| Aquatic (`merlin:aquatic`) | `all armor` | III | Applies Water Breathing while equipped. | Passive equipment tick. |
| Glowing (`merlin:glowing`) | `all armor` | III | Applies Night Vision while equipped. | Passive equipment tick. |
| Implants (`merlin:implants`) | `all armor` | III | Periodically restores food and saturation. | Passive equipment tick. |
| Obsidianshield (`merlin:obsidianshield`) | `all armor` | III | Applies Fire Resistance while equipped. | Passive equipment tick. |
| Overload (`merlin:overload`) | `all armor` | III | Adds two maximum-health points per rank while equipped. | Passive equipment tick. |
| Wings (`merlin:wings`) | `elytra` | III | Allows flight while equipped. | Passive equipment tick. |
| Momentum (`merlin:momentum`) | `elytra` | III | Increases elytra firework-boost velocity by 50% per rank. | Firework boost. |

---

# Eterna and Quanta altar

## Current flow

1. Right-click a vanilla `ENCHANTING_TABLE`.
2. Merlin cancels the vanilla table interaction and scans the surrounding altar structure.
3. The **Enchanter's Altar Matrix** opens as a 54-slot inventory.
4. Place a target item or book, lapis, and optionally a catalyst.
5. Choose a generated Tier I, II, or III offer, or spend lapis to reroll.
6. Merlin validates the offer, applies the enchantments, deducts costs only after successful application, and rerolls the offers.
7. Closing the GUI returns or safely drops target, lapis, and catalyst inputs.

## Structure scan and block stats

The scanner checks a horizontal radius of 2 blocks and one block below or above the table. Configured blocks contribute only when the line-of-sight rules allow them; contributions are capped per block type.

| Block | Eterna | Quanta | Eterna cap | Quanta cap |
|---|---:|---:|---:|---:|
| Bookshelf | 1.0 | 0.00 | 15.0 | — |
| Chiseled bookshelf | 1.2 | 0.00 | 18.0 | — |
| Amethyst block | 1.5 | 0.05 | 30.0 | 0.25 |
| Budding amethyst | 2.0 | 0.08 | 35.0 | 0.35 |
| Crying obsidian | 2.5 | 0.15 | 45.0 | 0.60 |
| Sculk catalyst | 3.0 | 0.25 | 50.0 | 0.80 |
| Candle | 0.25 | -0.05 | 10.0 | -0.30 |

Eterna gates eligible enchantment ranks. Quanta controls the high-roll check and secondary-enchantment chance. The runtime high-roll chance is capped at 0.90 after multiplying total Quanta by the offer's configured multiplier.

## Offer tiers

| Tier | Required player level | XP-level cost | Lapis cost | Enchantments | Quanta multiplier |
|---|---:|---:|---:|---:|---:|
| I | 10 | 1 | 1 | 1 | 0.5 |
| II | 20 | 2 | 2 | 1–2 | 1.0 |
| III | 30 | 3 | 3 | 2–3 | 1.5 |

The reroll button costs one lapis. Valid catalysts are amethyst shards, echo shards, and glowstone dust; a catalyst is accepted but is not required by the current validator.

## GUI slots and transaction rules

| Slot | Purpose |
|---:|---|
| 2 | Eterna meter |
| 6 | Quanta meter |
| 20 | Target item or book |
| 22 | Lapis Lazuli |
| 24 | Optional catalyst |
| 38 / 40 / 42 | Tier I / II / III offer buttons |
| 44 | Reroll |

The target validator is registry-driven, so custom targets such as fishing rods, saddles, shears, buckets, flint and steel, and elytra can enter the same altar flow. Custom book offers use the same registry applicability rule as validation and PDC application.

Set `enchantments.disabled` to a list of custom keys to remove those definitions from offer and target queries. Definitions remain available for inspection, but disabled entries are skipped during application.

Relevant source: [altar package](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/).

---

# Scribe books and spell DSL

## Current player flow

`/scribe book` creates a marked writable Scribe book. Right-clicking the marked main-hand book opens the Paper multiline dialog. `/scribe begin` is the chat fallback. Save persists source; Save & Cast persists, compiles, and executes only a successful compilation. Invalid source is atomic: diagnostics are returned and no world effect or cooldown mutation occurs.

Permission: `merlin.scribe.book`.

## Commands

| Command | Current behavior |
|---|---|
| `/scribe` | Shows Scribe usage. |
| `/scribe book` | Gives a marked writable Scribe book. |
| `/scribe begin` | Starts the fallback chat editor for the marked main-hand book. |
| `/scribe save` | Saves the current chat draft. |
| `/scribe cast` | Compiles the marked book's saved source and casts it. |
| `/scribe cancel` | Cancels the fallback editing session. |

## Language actions

The closed DSL supports:

- `look ahead NUMBER` for target ray setup.
- `summon sheep|rocket|fangs`, optionally `at PLACE` and with one indented `riding` modifier.
- `burn self|target NUMBER`.
- `mend self|target NUMBER`.
- `shove self|target NUMBER`.
- `strike target` or `strike at PLACE`.
- `send skyward`.
- `vanish self|target NUMBER seconds`.
- `rest NUMBER seconds`.

The compiler enforces lowercase ASCII words, four-space indentation, at most one indentation level, source size limits, at most 16 non-blank lines, at most four effects, bounded numeric ranges, and a maximum diagnostic count. Accepted pages compile to the canonical `scribe-compiler/0.2` identity format. The runtime executes only a complete `CompileResult.Ok` and applies the configured target and cooldown rules.

Scribe books are distinct from glyph maps and glyph tomes; ordinary books retain vanilla behavior.

Relevant sources: [ScribeCommand.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/command/ScribeCommand.java), [ScribeBookStore.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/book/ScribeBookStore.java), [SpellRuntime.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/runtime/SpellRuntime.java), and [Scribe language guide](scribe-language.md).

---

# Glyphcraft maps and tomes

## Map authoring

`/glyph book` creates a marked blank canvas. `/glyph` opens the MapGUI pen for the main-hand canvas. Players draw, select a filled elemental ink chip, classify the shape, set pips, and save a frozen glyph map.

Current invariants:

- Four elements: `physical`, `flame`, `frost`, and `arcane`.
- Shape determines the classified word; color does not change the classifier label.
- A new stroke consumes one durability unit from the selected ink bottle; interpolated points are free.
- No remaining selected ink means no new stroke.
- Pips are clamped from 1 to 5; school and patient stones are forced to one pip after classification.
- A tome can contain at most three pages.
- Mana is printed on stones and tome lore; there is no mana pool in the current implementation.

## Glyph commands

| Command | Current behavior |
|---|---|
| `/glyph` | Opens the pen for a marked main-hand glyph canvas. |
| `/glyph book` | Gives a blank marked glyph canvas. |
| `/glyph ink <element> [count]` | Gives 1–64 full magical ink bottles; `fire` loads as the `flame` alias. |
| `/glyph stamp <label> [pips]` | Authoring hatch that creates a frozen glyph without drawing. |
| `/glyph tome` | Gives an empty marked tome. |
| `/glyph bind` | Binds a frozen map from main hand into the tome in the off hand. Sneaking consumes the loose map; normal binding leaves it. |
| `/glyph tear` | Tears the current tome page back into a stone. |
| `/glyph cast` | Compiles the tome pages and casts the resulting glyph spell. |
| `/glyph enchant` | Applies a frozen Sharpness glyph map to a sword in the off hand. |

Permissions:

- `merlin.glyph.draw` — drawing, saving, and base glyph authoring; default true.
- `merlin.glyph.tome` — tome operations; default true. Tome operations also accept the draw permission in the current command implementation.

## Glyph roles and current vocabulary

| Role | Current words | Notes |
|---|---|---|
| Effect | `damage`, `heal`, `push` | One effect per combat tome; pips select magnitude. |
| School | `physical`, `flame`, `frost`, `arcane` | At most one; no school means the plain effect. |
| Patient | `self`, `target` | At most one; omitted patient defaults to `target`. |
| Charm | `sharpness` | Separate charm path for `/glyph enchant`; it does not bind into a combat tome. |

Reserved words classify but cannot be stamped, bound, or compiled. `shield` currently classifies but has no cast implementation. Sharpness is available through the stamp hatch and `/glyph enchant`; it is not in the current classifier catalog.

Relevant sources: [GlyphCommand.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/mapgui/GlyphCommand.java), [GlyphDraftStoreAdapter.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/mapgui/GlyphDraftStoreAdapter.java), [GlyphTomeStore.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/tome/GlyphTomeStore.java), and [Glyphcraft language guide](glyphcraft-language.md).

---

# Magical ink and grinding

## Ink bottles

Ink is stored in marked potion bottles. Each bottle records its element, remaining fill, and maximum fill in plugin-owned PDC. The current elements are physical, flame, frost, and arcane. The bottle is not drinkable, has custom styling, and uses remaining fill like a durability bar.

`/glyph ink <element> [count]` remains the direct authoring shortcut. The `fire` input alias is normalized to `flame`.

## Mortar and Pestle

A Mortar & Pestle is a marked bowl crafted shapelessly from one bowl and one stick. Hold it in the main hand and a mapped flower in the off hand, then right-click air or a block. One flower is consumed and one full bottle of the matching ink is produced. Empty bottles are not refilled; unmapped flowers are not consumed.

| Ink element | Mapped flowers |
|---|---|
| Flame | Torchflower, poppy |
| Frost | Blue orchid, cornflower |
| Arcane | Allium, wither rose, lilac |
| Physical | Oxeye daisy, dandelion, azure bluet, sunflower |

Relevant sources: [InkStore.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/InkStore.java), [FlowerGrind.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/FlowerGrind.java), [GrindListener.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/GrindListener.java), and [MortarPestle.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ink/MortarPestle.java).

---

# Ritual Crafting

## Current setup and activation

- Craft a marked **Ritual Anchor** from one obsidian and one dropper.
- Craft a marked **Ritual Pedestal** from one cobblestone and one dropper.
- Place one anchor and one or more marked pedestals in the surrounding layout.
- Put one frozen glyph map and one matching vanilla material into the anchor inventory.
- Put a non-empty ink bottle matching the recipe school into a pedestal.
- Hold a Mortar & Pestle and right-click the marked anchor.
- On success, the glyph and material are consumed, one matching ink bottle is consumed, and a marked product is dropped. Yield is based on glyph pips and capped at three.

## Current recipe table

| Glyph | Material in anchor | Required school / catalyst block | Product name | Output material |
|---|---|---|---|---|
| `damage` | Iron ingot | Physical ink; current code does not require a catalyst block | Aether Ingot | Stone |
| `heal` | Gold ingot | Physical ink; current code does not require a catalyst block | Sun Drop | Gold ingot |
| `push` | Copper ingot | Physical ink; current code does not require a catalyst block | Kinetic Coil | Piston |
| `flame` | Redstone | Flame ink; current code does not require a catalyst block | Cinder Salt | Redstone |
| `frost` | Amethyst shard | Frost ink; current code does not require a catalyst block | Rime Crystal | Amethyst shard |

The product has a `ritual_product` PDC marker and a maximum stack size of one. Ritual products are intermediates, not finished weapons, armor, wands, or scrolls.

Relevant sources: [RitualAnchor.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualAnchor.java), [RitualPedestal.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualPedestal.java), [RitualCircle.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualCircle.java), [RitualRecipeTable.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualRecipeTable.java), and [RitualListener.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/ritual/RitualListener.java).

---

# Custom-enchantment runtime features

## Event dispatch

The registered `CustomEnchantmentListener` adapts high-priority Paper events into narrow trigger contracts. Active trigger coverage includes:

- Entity attacks, entity hits, environmental damage, and armor defense.
- Entity death and mutable dropped experience.
- Item dropping, food changes, horse jumps, and entity interaction.
- Bow shooting, projectile hits, and projectile tracking.
- Block breaking, block drops, block placement, buckets, and shearing.
- Fishing, hook contact, item damage, item consumption, and interactions.
- Player/entity movement, jumps, sneaking, sprinting, gliding, firework boosts, and experience gain.
- Quit/death cleanup and bounded cascade protection.

`CustomEnchantmentDispatcher` reads PDC ranks, resolves definitions through the registry, sorts handlers by priority/key, and dispatches only the trigger interface implemented by each handler. `CascadeGuard` and `CascadeScope` bound simulated damage and block-break cascades.

## Passive equipment effects

`PassiveEquipListener` and `PassiveEffectApplier` periodically inspect equipped items. The current passive set is Gears, Springs, Aquatic, Glowing, Implants, Obsidianshield, Overload, and Wings. Passive effects are refreshed by the scheduled equipment tick; implants use the same periodic path for food and saturation restoration.

A separate hook-contact tracker runs every five ticks for Fire Hook and Poisoned Hook contact effects. The passive/active listeners are registered in [MerlinPlugin.java](../merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java).

---

# Marked items and persistence

| Item or data | Marker / keys | Purpose |
|---|---|---|
| Scribe writable book | `scribe_book`, `scribe_book_id`, `scribe_source` | Identifies a Merlin Scribe book and stores its source. |
| Glyph canvas / frozen map | `glyph_item`, `glyph_item_id`, `glyph_draft_v1`, then `glyph_label`, `glyph_pips`, `glyph_mana` | Tracks drafts and committed glyph tokens. |
| Glyph tome | `glyph_tome`, `glyph_tome_id`, `glyph_tome_pages`, `glyph_tome_index`, draft slots | Stores up to three glyph pages and the current page. |
| Magical ink bottle | `magical_ink`, `ink_element`, `ink_remaining`, `ink_max` | Stores elemental ink identity and fill. |
| Ritual Anchor | `ritual_anchor` | Marks the special dropper item and placed tile. |
| Ritual Pedestal | `ritual_pedestal` | Marks the special pedestal dropper item and placed tile. |
| Ritual product | `ritual_product` | Marks an intermediate output item. |
| Mortar & Pestle | `mortar_pestle` | Marks the bowl tool used for grinding and ritual activation. |
| Custom enchantments | `merlin:overcap_enchantments` TAG_CONTAINER with namespaced INTEGER entries | Stores custom ranks and over-cap vanilla ranks; lore mirrors entries. |

The plugin does not use native Bukkit `Enchantment` objects for the custom `merlin:` definitions. Custom effects resolve through the registry and PDC adapter.

---

# Permissions, configuration, and limits

## Permissions

| Permission | Default | Grants |
|---|---|---|
| `merlin.scribe.book` | true | Create and use Scribe books. |
| `merlin.glyph.draw` | true | Draw and save glyph maps and, in the current command path, use tome actions. |
| `merlin.glyph.tome` | true | Use glyph tome operations. |

## Configuration

- `model.repository`, `model.version`, and `model.allow-unreleased` select the classifier bundle. The bundle must contain the expected manifest/model/checksum files; unreleased bundles are disabled by default.
- `altar.*` controls scan radii, block contributions, and Eterna/Quanta caps.
- `enchanting_offers.*` controls tier requirements, XP costs, lapis costs, enchantment counts, and Quanta multipliers.
- `enchantments.disabled` removes named `merlin:<key>` definitions from offer/application queries without removing them from registry inspection.

## Runtime safety limits

- Scribe source: 4,096 Unicode scalars and 16,384 UTF-8 bytes, at most 16 non-blank lines, four effects, and bounded numeric arguments.
- Glyph drafts and classifier work use bounded serialized data and a bounded classifier queue; classifier callbacks return to the main thread.
- Glyph tomes contain at most three pages.
- Altar offers are validated before costs are deducted, and GUI inputs are reclaimed on close, quit, or death.
- Custom damage, block-break, and hook cascades are bounded by their scope/guard contracts.

---

# Current boundaries and non-current proposals

The following distinctions prevent the catalog from overstating the companion pages or design specs:

- The source-backed altar is current, but the Eterna/Quanta document is still labeled an approved design draft. Runtime behavior in the source wins over prose examples.
- The source registry and handlers currently implement all 75 custom definitions. Older custom-enchantment specs describe only the original 24; the advanced-enchantment spec describes the later 75-entry roster.
- Runtime `Sniper` behavior is an eye-height projectile-hit rule with rank-scaled extra damage, not a strict headshot raycast or a universal double-damage promise.
- Ritual Crafting currently consumes matching ink and does not check a catalyst block. Catalyst blocks are a **Next** item in the living spec.
- The exploratory glyph-seal and permanent gear-enchanting model is **Proposed**, not a current seal item or a replacement for the active Glyphcraft/Scribe flows.
- Current Glyphcraft next/future items include deciding school-from-ink words, catalyst blocks, geared crafting that consumes ritual intermediates, custom block models, recipe unlocking, multi-school rituals, durability refunds, and classifier color channels.
- The companion HTML page is illustrative and is not the runtime contract; where it describes seals or an altar ritual that the source does not implement, this catalog follows the source.

---

# Further reading

- [Glyphcraft language guide](glyphcraft-language.md)
- [Scribe language guide](scribe-language.md)
- [Glyphcraft living spec](living-specs/glyphcraft.md)
- [Ritual Crafting living spec](living-specs/ritual-crafting.md)
- [Custom enchantment framework design](superpowers/specs/2026-08-28-custom-enchantment-framework-design.md)
- [Archived custom enchantments design](superpowers/specs/2026-08-28-custom-enchantments-design.md)
- [Advanced enchantments port design](superpowers/specs/2026-08-29-advanced-enchantments-port-design.md)
- [Eterna/Quanta enchanting design](superpowers/specs/2026-08-28-eterna-quanta-enchanting-design.md)

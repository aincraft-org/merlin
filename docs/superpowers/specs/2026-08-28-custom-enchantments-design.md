# Archived Custom Enchantments — Design Specification

**Date:** 2026-08-28  
**Status:** Approved for implementation  
**Source:** [Mayhem Multiverse Enchanter archive](https://web.archive.org/web/20150516232412/http://wiki.mayhem-multiverse.com/index.php/Enchanter)  
**Target:** `merlin-paper` enchanting subsystem

## 1. Goal and scope

Implement every entry in the source page's **Custom** enchantment table. The existing vanilla registry entries remain unchanged. The custom set contains 24 definitions; 23 have source-described mechanics and Expertise is metadata-only because the source gives it no description.

Custom enchantments use the existing `EnchantmentRegistry`, `OvercapItemAdapter`, `CustomEnchantmentDispatcher`, and `CustomEnchantmentListener`. No second item-storage format or independent event bus is introduced.

## 2. Registry and persistence

Register the following keys in the `merlin` namespace using lower snake case:

| Key | Name | Target | Maximum rank |
| --- | --- | --- | ---: |
| `sticky_grip` | Sticky Grip | weapons/tools | I |
| `equilibrium` | Equilibrium | weapons | V |
| `nethers_scourge` | Nether's Scourge | weapons | VI |
| `cold_aspect` | Cold Aspect | weapons | III |
| `confusing_aspect` | Confusing Aspect | weapons | III |
| `toxin_aspect` | Toxin Aspect | weapons | III |
| `knowledge` | Knowledge | weapons | III |
| `vorpal` | Vorpal | weapons | III |
| `vampirism` | Vampirism | weapons | V |
| `flurry` | Flurry | swords | III |
| `array` | Array | bows | II |
| `plunder` | Plunder | bows | III |
| `wisdom` | Wisdom | bows | III |
| `molten_touch` | Molten Touch | tools | I |
| `drill` | Drill | pickaxes | III |
| `expertise` | Expertise | pickaxes | III |
| `quenching` | Quenching | leggings | IV |
| `colorama` | Colorama | leather armor | I |
| `leaping` | Leaping | saddles | III |
| `feather_hooves` | Feather Hooves | saddles | I |
| `prismatic` | Prismatic | shears | I |
| `overflowing` | Overflowing | buckets | I |
| `vacuum` | Vacuum | buckets | I |
| `heat_wave` | Heat Wave | flint and steel | I |

Custom definitions set `vanillaMaxLevel = 0`, making every positive custom rank an over-cap level. `OvercapItemAdapter.applyEnchantments` explicitly recognizes a registered non-vanilla key and persists rank I as well as higher ranks in `merlin:overcap_enchantments`; it also adds the display line to lore. Existing vanilla behavior remains unchanged.

The current definition model has no job-level field. Existing Eterna eligibility fields are retained for offer selection, with source tier unlocks represented by the registered rank maxima and current altar offer system rather than inventing a separate job subsystem.

## 3. Handler architecture

Each active definition gets a focused `OvercapEffectHandler` implementing only its trigger contracts. Shared package-private helpers provide:

- per-rank random integer rolls;
- native Nether-mob and mob-head mappings;
- safe experience, drop, velocity, and held-item operations;
- recursion protection for Equilibrium's self-damage.

The dispatcher continues to resolve PDC enchantments, filter by trigger interface, and execute deterministic priority/key order. All event adapters continue to run inside `CascadeGuard`.

## 4. Event contracts and mechanics

The framework gains Paper-backed contracts for `PlayerDropItemEvent`, `FoodLevelChangeEvent`, `HorseJumpEvent`, and environmental damage on non-player living entities. Entity-kill dispatch receives mutable dropped experience and writes it back to `EntityDeathEvent`.

The mechanics are:

- **Sticky Grip:** cancel dropping an item bearing the enchantment.
- **Equilibrium:** on a hit, add a random 1–3 damage roll per rank and apply half of the resulting bonus to the attacker through a guarded Bukkit damage call.
- **Nether's Scourge:** add a random 1–3 damage roll per rank against native Nether hostile mobs only.
- **Cold Aspect:** set the target's freeze duration to at least 20 ticks per rank.
- **Confusing Aspect:** apply Nausea for 100 ticks per rank.
- **Toxin Aspect:** apply Poison for 80 ticks per rank.
- **Knowledge:** add a random 3–4 experience per rank to the death event.
- **Vorpal:** with a 0.5% per-rank chance, append the matching vanilla mob-head item.
- **Vampirism:** with a 10% per-rank chance, heal the attacker by one health point without exceeding maximum health.
- **Flurry:** on a right-clicked living target, apply outward knockback to nearby living mobs, up to three mobs per rank.
- **Array:** make ranks I/II fire 3/5 total arrows. Only the original bow event consumes ammunition; extra arrows are spawned directly. The source has no Rank III entry.
- **Plunder:** append one additional copy of each current mob drop per rank.
- **Wisdom:** add one extra copy of the original vanilla experience amount per rank.
- **Molten Touch:** in block-drop dispatch, replace furnace-smeltable ore and sand drops with their smelted output.
- **Drill:** safely break connected same-type ore blocks, with a maximum of `4 × rank` blocks total including the original block.
- **Expertise:** register metadata only; no effect is invented because the source description is blank.
- **Quenching:** when food level would decrease, restore up to one food point per rank, capped at 20.
- **Colorama:** randomize leather armor color when its durability is damaged.
- **Leaping:** increase horse jump power by 0.1 per rank, capped at the API maximum.
- **Feather Hooves:** cancel fall damage received by a saddled horse.
- **Prismatic:** randomize a sheep's wool color before shearing.
- **Overflowing:** restore a water bucket after it empties water.
- **Vacuum:** restore an empty bucket after it fills from water.
- **Heat Wave:** ignite valid blocks in a horizontal 3×3 area around the clicked block.

A per-rank roll means one independent bounded roll for each rank; values are summed. Effects never bypass `CascadeGuard`, event cancellation, or existing dispatcher ordering.

## 5. Error and lifecycle handling

Handlers are no-ops when their context is incompatible: wrong target type, non-living target, non-Nether mob, unsupported drop, absent world, or cancelled source event. Mutable event values are clamped to API-safe bounds. Item restoration uses the event's hand and does not create additional inventory stacks. Recursive damage and drill breaks are bounded by the existing cascade guard.

## 6. Verification

Add focused behavioral coverage for:

1. all 24 registry keys, target sets, and maximum ranks;
2. rank-I custom PDC persistence and lore;
3. each active handler's observable effect and incompatible-context no-op;
4. every new Paper event adapter and mutable entity-kill experience write-back;
5. recursion/cascade limits and existing dispatcher ordering.

Run the focused enchanting tests, then the full Gradle test suite and a Paper plugin smoke path if the development server can start in the current environment.

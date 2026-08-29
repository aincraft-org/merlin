# AdvancedEnchantments Port — Design Specification

**Date:** 2026-08-29  
**Status:** Proposed Design Draft  
**Target Subsystem:** `merlin-paper` (`dev.mintychochip.merlin.paper.enchanting.custom`)  
**Source Material:** AdvancedEnchantments default pack (250+ enchants), [ae.advancedplugins.net/enchantments/list-of-enchantments/default-enchants-200+](https://ae.advancedplugins.net/enchantments/list-of-enchantments/default-enchants-200+).

---

## 1. System Overview & Core Philosophy

This spec ports a curated set of **51 AdvancedEnchantments (AE)** enchants into Merlin's existing Custom Enchantment Framework, bringing the total custom enchantment roster to **75** (24 existing + 51 new). The selection prioritizes:

- **Utility, mobility, field tools, and exploration** over chaotic PvP spell-spam.
- **Existing trigger compatibility** where possible (first-wave enchants map to already-implemented trigger interfaces).
- **Clear separation** of enchants that require new subsystems (passive equip effects, projectile-hit tracking, trident support, hook-entity collision, elytra/fireworks hooks).
- **No duplication** with existing Merlin enchants, and no grief/OP mechanics (soul economies, guard spawning, inventory manipulation, player disarming).

The implementation is staged so that the first 39 enchants can be delivered immediately, while the remaining 12 depend on small infrastructure additions.

---

## 2. Enchant Roster

### 2.1 Existing Merlin enchants (24) — retained

These remain unchanged and are the baseline for the 75-enchant cap:

`sticky_grip`, `equilibrium`, `nethers_scourge`, `cold_aspect`, `confusing_aspect`, `toxin_aspect`, `knowledge`, `vorpal`, `vampirism`, `flurry`, `array`, `plunder`, `wisdom`, `drill`, `expertise`, `quenching`, `colorama`, `leaping`, `feather_hooves`, `molten_touch`, `prismatic`, `overflowing`, `vacuum`, `heat_wave`.

### 2.2 AE ports (51)

#### Tools / harvesting (10) — Phase 1
| Enchant | AE Description | Merlin Trigger | Notes |
|---|---|---|---|
| `Telepathy` | Mined drops go to inventory | `BlockDropTrigger` | Move `Item` entities to player inventory |
| `Timber` | Break a tree in one hit | `BlockBreakTrigger` + `CascadeScope` | Like `drill` but for logs; use `scope.breakBlockSafely` |
| `Trench` | Break a 3×3 area | `BlockBreakTrigger` + `CascadeScope` | Pickaxe/shovel 3×3 break |
| `Replanter` | Replants crops when harvested | `BlockBreakTrigger` | Re-plant same crop on mature break |
| `Planter` | 3×3 seed plant on shift-right-click | `ActiveInteractTrigger` | Right-click tilled soil |
| `Carrot Planter` | 3×3 carrot plant | `ActiveInteractTrigger` | Right-click tilled soil |
| `Potato Planter` | 3×3 potato plant | `ActiveInteractTrigger` | Right-click tilled soil |
| `Experience` | More XP from ores | `BlockBreakTrigger` | Add XP orb value or direct player XP |
| `Rebreather` | Regain air when mining underwater | `BlockBreakTrigger` | Restore air if player is underwater |
| `Replenish` | Restore food while mining | `BlockBreakTrigger` | Small food/saturation restore per ore break |

#### Tool durability (2) — Phase 1
| Enchant | AE Description | Merlin Trigger | Notes |
|---|---|---|---|
| `Unbreakable` | Tools never break | `ItemDamageTrigger` | Return `0` from `onItemDamage` |
| `Reforged` | Items take durability slower | `ItemDamageTrigger` | Reduce `originalDamageAmount` |

#### Armor / defense (10) — Phase 1
| Enchant | AE Description | Merlin Trigger | Notes |
|---|---|---|---|
| `Aegis` | Speed when taking fall damage | `EnvironmentalDamageTrigger` | Apply Speed on `FALL` damage |
| `Angelic` | Heal when damaged | `ArmorDefenseTrigger` | Heal a small amount per proc |
| `Armored` | Reduced sword damage | `ArmorDefenseTrigger` | Reduce `MutableDamage` if attacker holds sword |
| `Chunky` | Take less damage | `ArmorDefenseTrigger` | Flat or percentage reduction |
| `Dodge` | Chance to dodge physical attacks | `ArmorDefenseTrigger` | Cancel/negate physical damage |
| `Heavy` | Reduced bow damage | `ArmorDefenseTrigger` | Reduce if attacker projectile |
| `Molten` | Set attackers on fire | `ArmorDefenseTrigger` | Ignite attacker |
| `Reflect` | Reflect some damage | `ArmorDefenseTrigger` | Deal partial damage back to attacker |
| `Safeguard` | Resistance when defending | `ArmorDefenseTrigger` | Apply Resistance briefly |
| `Tank` | Reduced axe damage | `ArmorDefenseTrigger` | Reduce if attacker holds axe |

#### Melee (7) — Phase 1
| Enchant | AE Description | Merlin Trigger | Notes |
|---|---|---|---|
| `Bleed` | DoT on hit | `EntityHitTrigger` | Apply custom bleed ticks or `PotionEffect.WITHER` reskinned |
| `Blind` | Blindness proc | `EntityHitTrigger` | Apply `PotionEffect.BLINDNESS` |
| `Block` | Chance to negate/reflect a hit | `EntityHitTrigger` | Reduce or reflect `MutableDamage` |
| `Berserk` | Strength + mining fatigue | `EntityHitTrigger` | Apply `STRENGTH` to attacker, `MINING_FATIGUE` as self-debuff |
| `Critical` | More crit damage | `EntityHitTrigger` | Add bonus when `damage` is a critical hit |
| `Double Strike` | Strike twice | `EntityHitTrigger` | Proc second hit entity |
| `Thunderlord` | Lightning every 3rd consecutive hit | `EntityHitTrigger` | Track hit counter per target/level |

#### Ranged (3)
| Enchant | AE Description | Merlin Trigger | Phase | Notes |
|---|---|---|---|---|
| `Archer` | Bow damage up | `BowShootTrigger` | 1 | Increase projectile velocity/damage on shoot |
| `Marksman` | Crossbow damage up | `BowShootTrigger` | 1 | Same for crossbow projectiles |
| `Sniper` | Headshots double damage | `ProjectileHitTrigger` | 2 | Needs new projectile hit detection + headshot raycast |

#### Fishing / water (6)
| Enchant | AE Description | Merlin Trigger | Phase | Notes |
|---|---|---|---|---|
| `Auto Reel` | Auto-reel on bite | `PlayerFishTrigger` | 1 | Trigger on `State.BITE` |
| `Bait` | Double fishing drops | `PlayerFishTrigger` | 1 | Trigger on `State.CAUGHT_FISH/ENTITY` |
| `Hook` | More fishing XP | `PlayerFishTrigger` | 1 | Add XP on catch |
| `Snap` | Pull caught entity | `PlayerFishTrigger` | 1 | Pull `caught` entity toward player |
| `Fire Hook` | Flaming hook | *new hook-entity system* | 2 | Needs hook-collision/entity ignition |
| `Poisoned Hook` | Poison on hook contact | *new hook-entity system* | 2 | Needs hook-collision/entity poison |

#### Mobility & passive utility (13)
| Enchant | AE Description | Merlin Trigger | Phase | Notes |
|---|---|---|---|---|
| `Lava Walker` | Walk on lava | `PlayerMoveTrigger` | 1 | Place temporary obsidian/stone under feet |
| `Water Walker` | Walk on water | `PlayerMoveTrigger` | 1 | Place frosted ice under feet |
| `Plummet` | Damage nearby mobs when taking fall damage | `EnvironmentalDamageTrigger` | 1 | AOE damage on `FALL` cause |
| `Gears` | Passive speed | *passive equip system* | 2 | Apply Speed attribute/effect while equipped |
| `Springs` | Jump boost | *passive equip system* | 2 | Apply Jump Boost while equipped |
| `Aquatic` | Water breathing | *passive equip system* | 2 | Apply Water Breathing while equipped |
| `Glowing` | Night vision | *passive equip system* | 2 | Apply Night Vision while equipped |
| `Implants` | Passive food regen | *passive equip system* | 2 | Periodic food/saturation restore |
| `Obsidianshield` | Fire resistance | *passive equip system* | 2 | Apply Fire Resistance while equipped |
| `Overload` | Extra hearts | *passive equip system* | 2 | Increase max health attribute |
| `Wings` | Creative flight when worn | *passive equip system* | 2 | Allow flight while equipped |
| `Jelly Legs` | Negate fall damage | `EnvironmentalDamageTrigger` | 1 | Cancel `FALL` damage |
| `Momentum` | Elytra speed with fireworks | *elytra/fireworks hook* | 2 | Detect firework boost, apply extra velocity |

---

## 3. Total Count

- **24** existing Merlin enchants
- **51** AE ports
- **75** total custom enchants

Of the 51 ports:
- **39** are **Phase 1** (existing triggers)
- \*\*12\*\* are \*\*Phase 2\*\* (new infrastructure)

---

## 4. Architecture & Integration

### 4.1 Existing framework reuse

All ports plug into the existing stack:

```
Paper Event
    ▼
CustomEnchantmentListener
    ▼
CustomEnchantmentDispatcher
    ▼
EnchantmentRegistry.resolveTriggers(item, TriggerClass.class)
    ▼
Concrete OvercapEffectHandler implementing a Trigger interface
```

### 4.2 Adding a new enchant

For each port, the implementation is:

1. **Create a handler** in `merlin-paper/.../enchanting/custom/handler/` implementing the relevant `Trigger` interface and `OvercapEffectHandler`.
2. **Register the definition** in `EnchantmentRegistry.defaultRegistry(Set<NamespacedKey>)` with:
   - `key` (e.g. `merlin:telepathy`)
   - `displayName`
   - `absoluteMaxLevel` and `vanillaMaxLevel` (usually `0`)
   - `baseEternaRequired`, `eternaPerLevel`, `weight`
   - `targetMaterials`
   - `Optional.of(handler)`
3. **Wire the listener** if the trigger is new (only for Phase 2).
4. **Add a test** in `merlin-paper/.../enchanting/custom/handler/` or the relevant test class.

### 4.3 Disabled enchant support

All new enchants must be compatible with the recently added `enchantments.disabled` list in `config.yml`. The `EnchantmentRegistry` already filters disabled keys from offers/applications, and existing items with disabled enchants continue to function.

---

## 5. Phase 2: New Subsystems Required

### 5.1 Passive equip/unequip effect system
Needed for: `Gears`, `Springs`, `Aquatic`, `Glowing`, `Implants`, `Obsidianshield`, `Overload`, `Wings`.

**Design:**
- Track equipped armor/tool state in a `PlayerEquipTracker` keyed by `PlayerInventory` slots.
- On equip of an item with a passive enchant, apply the relevant `AttributeModifier` or `PotionEffect`.
- On unequip (including drop/death), remove the modifier/effect.
- Use `PlayerItemHeldEvent`, `InventoryClickEvent`, `PlayerDropItemEvent`, and `PlayerDeathEvent` for tracking.

### 5.2 `ProjectileHitTrigger`
Needed for: `Sniper`.

**Design:**
- Add `ProjectileHitTrigger` interface: `onProjectileHit(LivingEntity shooter, Entity projectile, Entity hitEntity, int level)`.
- Listener on `ProjectileHitEvent`.
- For `Sniper`, detect headshot by checking if the arrow's impact location is in the victim's head hitbox.

### 5.3 Trident support
Needed for (deferred, not in Phase 1–2 core): `Poseidon`, `Strife`, `Deadshot`, `Impact`, `Spark`, `Twinge`.

**Design:**
- Add `TridentThrowTrigger` and/or reuse `ProjectileHitTrigger` for tridents.
- Listener on `PlayerRiptideEvent` and trident throw events.

### 5.4 Hook-entity collision
Needed for: `Fire Hook`, `Poisoned Hook`.

**Design:**
- Track `FishHook` entities and listen for `PlayerFishEvent.State.FISHING` hook updates or a tick task checking collided entities.
- Trigger when the hook pierces or contacts a `LivingEntity`.

### 5.5 Elytra / fireworks boost hook
Needed for: `Momentum` (in the 51-roster). `Rocket Escape` and `Slingshot` use the same hooks but are deferred to the optional pool.

\*\*Design:\*\* Listen to `PlayerToggleGlideEvent` and `FireworkExplodeEvent`.
- `Momentum`: on firework boost while gliding, add extra velocity.
- `Rocket Escape` (optional): on low HP while gliding, auto-firework boost.
- `Slingshot` (optional): right-click while wearing elytra to trigger a firework launch (`ActiveInteractTrigger` + elytra check).

---

## 6. Implementation Phases

### Phase 1 — First-wave ports (39 enchants)

Add all Phase 1 enchants from §2.2 using existing triggers. Estimated 2–3 weeks of focused handler + test work.

Order of attack:
1. **Tools / harvesting** — highest value, low risk (`Telepathy`, `Timber`, `Trench`, `Replanter`, `Planter`, `Carrot Planter`, `Potato Planter`, `Experience`, `Rebreather`, `Replenish`).
2. **Tool durability** — trivial (`Unbreakable`, `Reforged`).
3. **Armor / defense** — reuse `ArmorDefenseTrigger` (`Aegis`, `Angelic`, `Armored`, `Chunky`, `Dodge`, `Heavy`, `Molten`, `Reflect`, `Safeguard`, `Tank`).
4. **Mobility first-wave** — `Lava Walker`, `Water Walker`, `Plummet`, `Jelly Legs`.
5. **Melee** — simple potion/damage procs (`Bleed`, `Blind`, `Block`, `Berserk`, `Critical`, `Double Strike`, `Thunderlord`).
6. **Fishing first-wave** — `Auto Reel`, `Bait`, `Hook`, `Snap`.
7. **Ranged first-wave** — `Archer`, `Marksman`.

### Phase 2 — Infrastructure + remaining ports (12 enchants)

1. Build passive equip system; add `Gears`, `Springs`, `Aquatic`, `Glowing`, `Implants`, `Obsidianshield`, `Overload`, `Wings`.
2. Build `ProjectileHitTrigger`; add `Sniper`.
3. Build hook-entity collision; add `Fire Hook`, `Poisoned Hook`.
4. Build elytra/fireworks hook; add `Momentum`.

(Note: `Rocket Escape` and `Slingshot` are not in the 51-roster; they remain in the optional pool.)

### Phase 3 — Deferred / optional pool

Tuned variants and mechanics requiring larger systems, to be reviewed later:

- **Tuned variants:** `Vein Miner` (tune of `drill`), `Smelting` (tune of `molten_touch`), `Ice Aspect` (tune of `cold_aspect`), `Confuse`/`Fuddle` (tune of `confusing_aspect`), `Poison`/`Virus`/`Poisoned` sword (tune of `toxin_aspect`), `Vampire`/`Lifesteal` (tune of `vampirism`), `Nether Slayer`/`Netherling` (tune of `nethers_scourge`), `Inquisitive` (tune of `wisdom`/`knowledge`).
- **Soul economy:** `Diploid`, `Multiplication`, `Soulbound`, `Soulgrind`, `Soulminer`, `Spiritmaster`, `Nulify`, `Rush`.
- **Guard / summon:** `Guardians`, `Spirits`, `Undead Ruse`, `Explosive Demise`, `Phoenix`.
- **Trident tree:** `Poseidon`, `Strife`, `Deadshot`, `Impact`, `Spark`, `Twinge`.
- **Grief / OP PvP:** `Disarm`, `Disarmor`, `Fuddle`, `Scare`, `Kill Aura`, `Decapitation`.

---

## 7. Balance & Configuration Guidance

- **Rarity distribution:** Phase 1 tool/utility enchants should skew `Simple`/`Unique`; combat/armor procs skew `Elite`/`Ultimate`; `Wings` and `Overload` should be `Legendary` or `Fabled`.
- **Max levels:** Keep low (1–3 for utility, 3–5 for combat) to avoid AE-style level inflation.
- **Eterna/Quanta tuning:** Follow the existing `baseEternaRequired`, `eternaPerLevel`, `weight` patterns in `EnchantmentRegistry.defaultRegistry`.
- **Disabled-by-default option:** All new enchants should be eligible for the `enchantments.disabled` list; none should be hard-coded enabled.

---

## 8. Testing Strategy

- **Handler unit tests:** Mock the relevant trigger inputs (`MutableDamage`, `Block`, `Player`, etc.) and assert expected output.
- **Dispatcher integration tests:** Ensure each trigger class resolves through `CustomEnchantmentDispatcher` in priority order.
- **Registry tests:** Verify each new enchant is present in `EnchantmentRegistry.defaultRegistry()` and can be disabled.
- **Cascade tests:** Any `BlockBreakTrigger` that breaks extra blocks must use `CascadeScope` and respect `MAX_CASCADE_DEPTH`.

---

## 9. Risks

1. **Passive equip system** is the largest new subsystem; it must correctly track armor swaps, disconnects, deaths, and inventory drags.
2. **Water/Lava Walker** can be grief-prone if they convert protected blocks; use `setType` only on non-protected blocks and avoid persistent block changes.
3. **Wings** trivializes exploration; consider gating it to endgame rarity or a separate elytra slot.
4. **`Timber`** and **`Trench`** must respect `CascadeScope` to avoid runaway cascades.
5. **Duplicate avoidance** must be verified at handler implementation time, not just name comparison.

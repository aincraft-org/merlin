# Enchantment Port — Armor & Defense Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the ten Phase-1 armor/defense enchants from the approved roster into Merlin’s existing damage dispatch pipeline.

**Architecture:** `ArmorDefenseTrigger` handlers mutate `MutableDamage`, apply short-lived effects, or issue bounded defensive procs. `Aegis` uses `EnvironmentalDamageTrigger`; all remaining enchants use `ArmorDefenseTrigger`. Shared attacker/material and reflected-target logic lives in a small package-private support class.

**Tech Stack:** Java 21, PaperMC 1.21.4, JUnit 5, Mockito, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-29-advanced-enchantments-port-design.md`

## Global Constraints

- Preserve the existing `CustomEnchantmentDispatcher` and `MutableDamage` contracts.
- `MutableDamage` is the only damage mutation container; use `multiply` or `setCancelled`.
- Do not recurse through a second custom dispatch path; `CascadeGuard` bounds nested Bukkit damage.
- All new definitions remain compatible with `enchantments.disabled`.
- Armor targets use the existing six material armor sets; no new item-storage model.
- Tests must cover positive behavior, wrong trigger/cause, level zero, and attacker-type boundaries.

---

### Task 1: Shared armor-defense support

**Files:**
- Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ArmorDefenseSupport.java`
- Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/handler/ArmorDefenseSupportTest.java`

**Interfaces:**
- Produces package-private helpers:
  - `static boolean attackerHolds(LivingEntity attacker, String materialSuffix)`
  - `static LivingEntity livingAttacker(Entity attacker)` resolving direct living attackers and projectile shooters
  - `static double reductionMultiplier(int level, double reductionPerLevel)`

- [ ] **Step 1: Write failing tests** covering sword/axe detection, projectile shooter resolution, and multiplier clamping.
- [ ] **Step 2: Run:** `./gradlew :merlin-paper:test --tests '*ArmorDefenseSupportTest'`; expected class-not-found.
- [ ] **Step 3: Implement null-safe helpers.** `attackerHolds` checks `getEquipment().getItemInMainHand()` and `Material.name().endsWith(suffix)`. `livingAttacker` returns a living entity directly or a projectile’s living shooter. `reductionMultiplier` returns `max(0, 1 - reductionPerLevel * level)`.
- [ ] **Step 4: Run the focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 2: `Aegis` — speed after fall damage

**Files:**
- Create: `.../handler/AegisHandler.java`
- Test: `.../handler/AegisHandlerTest.java`

**Interfaces:**
- Consumes: `EnvironmentalDamageTrigger`
- Produces: key `merlin:aegis`

- [ ] **Step 1: Test FALL applies `PotionEffectType.SPEED` for `100 * level` ticks at amplifier `level - 1`, while FIRE and level zero do nothing.**
- [ ] **Step 2: Run focused test; expected FAIL before class exists.**
- [ ] **Step 3: Implement null-safe handler using `player.addPotionEffect(new PotionEffect(...))`, clamping amplifier to 4.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 3: `Angelic` — heal when damaged

**Files:**
- Create: `.../handler/AngelicHandler.java`
- Test: `.../handler/AngelicHandlerTest.java`

**Interfaces:**
- Consumes: `ArmorDefenseTrigger`
- Produces: key `merlin:angelic`

- [ ] **Step 1: Test a damaged defender gains `level` health through `CustomEnchantmentSupport.healToMax`, and a full-health defender is unchanged.**
- [ ] **Step 2: Run focused test; expected FAIL.**
- [ ] **Step 3: Implement using `healToMax(defender, level)`.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 4: `Armored` and `Tank` — weapon-specific reductions

**Files:**
- Create: `.../handler/ArmoredHandler.java`
- Create: `.../handler/TankHandler.java`
- Test: `.../handler/WeaponDefenseHandlerTest.java`

**Interfaces:**
- Consumes: `ArmorDefenseTrigger`
- Produces: keys `merlin:armored` and `merlin:tank`

- [ ] **Step 1: Test sword damage is reduced by 10% per `Armored` level, axe damage by 10% per `Tank` level, wrong weapon types are unchanged, and reductions clamp at zero.**
- [ ] **Step 2: Run focused test; expected FAIL.**
- [ ] **Step 3: Implement with `ArmorDefenseSupport.attackerHolds(attacker, "_SWORD"/"_AXE")` and `damage.multiply(ArmorDefenseSupport.reductionMultiplier(level, 0.10))`.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 5: `Chunky` and `Heavy` — general and projectile reductions

**Files:**
- Create: `.../handler/ChunkyHandler.java`
- Create: `.../handler/HeavyHandler.java`
- Test: `.../handler/GeneralDefenseHandlerTest.java`

**Interfaces:**
- Consumes: `ArmorDefenseTrigger`
- Produces: keys `merlin:chunky` and `merlin:heavy`

- [ ] **Step 1: Test `Chunky` reduces all direct damage by 5% per level, `Heavy` reduces `AbstractArrow` damage by 10% per level, and non-arrow attackers are unchanged.**
- [ ] **Step 2: Run focused test; expected FAIL.**
- [ ] **Step 3: Implement null-safe multipliers; `Heavy` accepts `AbstractArrow` only.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 6: `Dodge` — chance to negate physical attacks

**Files:**
- Create: `.../handler/DodgeHandler.java`
- Test: `.../handler/DodgeHandlerTest.java`

**Interfaces:**
- Consumes: `ArmorDefenseTrigger`
- Produces: key `merlin:dodge`

- [ ] **Step 1: Test an injected `Random` roll below `10 * level` sets `MutableDamage.cancelled`, a miss leaves it unchanged, and projectile attacks are ignored.**
- [ ] **Step 2: Run focused test; expected FAIL.**
- [ ] **Step 3: Implement constructors with injectable `Random`; use `ArmorDefenseSupport.livingAttacker` and a 100-point roll.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 7: `Molten` and `Reflect` — defensive procs

**Files:**
- Create: `.../handler/MoltenHandler.java`
- Create: `.../handler/ReflectHandler.java`
- Test: `.../handler/ReactiveDefenseHandlerTest.java`

**Interfaces:**
- Consumes: `ArmorDefenseTrigger`
- Produces: keys `merlin:molten` and `merlin:reflect`

- [ ] **Step 1: Test `Molten` sets a living attacker on fire for at least `60 * level` ticks, and `Reflect` deals `10% * level` of initial damage to the living attacker/shooter.**
- [ ] **Step 2: Run focused test; expected FAIL.**
- [ ] **Step 3: Implement null-safe procs; skip cancelled/zero damage and non-living attackers.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 8: `Safeguard` — resistance while defending

**Files:**
- Create: `.../handler/SafeguardHandler.java`
- Test: `.../handler/SafeguardHandlerTest.java`

**Interfaces:**
- Consumes: `ArmorDefenseTrigger`
- Produces: key `merlin:safeguard`

- [ ] **Step 1: Test defensive damage applies Resistance for `60 * level` ticks at amplifier `level - 1`, with null/zero guards.**
- [ ] **Step 2: Run focused test; expected FAIL.**
- [ ] **Step 3: Implement with `PotionEffectType.RESISTANCE`, duration and amplifier clamps.**
- [ ] **Step 4: Run focused test; expected PASS.**
- [ ] **Step 5: Commit.**

---

### Task 9: Register armor definitions

**Files:**
- Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/EnchantmentRegistry.java`
- Modify: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/CustomEnchantmentRegistryTest.java`

- [ ] **Step 1: Add definitions and exact maximums:** `aegis` 3, `angelic` 3, `armored` 3, `chunky` 3, `dodge` 3, `heavy` 3, `molten` 3, `reflect` 3, `safeguard` 3, `tank` 3.
- [ ] **Step 2: Target armor set for every definition; update expected registry keys and target assertions.**
- [ ] **Step 3: Run registry tests; expected PASS.**
- [ ] **Step 4: Commit.**

---

### Task 10: Armor dispatcher integration

**Files:**
- Create: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/ArmorEnchantmentIntegrationTest.java`

- [ ] **Step 1: Exercise `dispatchEnvironmentalDamage` for Aegis and `dispatchArmorDefense` for a reduction/reactive handler using the default registry and mocked adapter.**
- [ ] **Step 2: Run focused integration test; expected PASS.**
- [ ] **Step 3: Run full module suite:** `./gradlew :merlin-paper:test`; expected BUILD SUCCESSFUL.
- [ ] **Step 4: Commit.**

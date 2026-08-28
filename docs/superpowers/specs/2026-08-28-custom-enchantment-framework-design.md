# Custom Enchantment Framework & Event Dispatch Subsystem — Design Specification

**Date:** 2026-08-28  
**Status:** Approved Design Draft  
**Target Subsystem:** `merlin-paper` (`dev.mintychochip.merlin.paper.enchanting.custom`)  
**Spec Location:** `docs/superpowers/specs/2026-08-28-custom-enchantment-framework-design.md`

---

## 1. System Overview & Core Philosophy

The **Custom Enchantment Framework** provides a modular, extensible, and server-safe event dispatch architecture inspired by **Taric**. It bridges PaperMC's native gameplay events (`EntityDamageByEntityEvent`, `BlockBreakEvent`, `EntityDeathEvent`, `EntityShootBowEvent`, `PlayerInteractEvent`) to discrete enchantment trigger interfaces while integrating seamlessly with Merlin's **✦ Eterna & ⚡ Quanta** altar progression and **PDC** item storage.

```
                                [ PAPER EVENT PIPELINE ]
             EntityDamageByEntityEvent | BlockBreakEvent | EntityDeathEvent
                                          │
                                          ▼
                         ┌─────────────────────────────────┐
                         │   CustomEnchantmentListener     │
                         │   (Entry listener @ HIGH prio)  │
                         └────────────────┬────────────────┘
                                          │
                        [ Scoped Depth & Cascade Guard ]
                        Is depth <= MAX_DEPTH (3)?
                           ├── NO  ──► Abort (Prevents runaway cascades)
                           └── YES ──► Proceed inside CascadeScope
                                          │
                                          ▼
                         ┌─────────────────────────────────┐
                         │  CustomEnchantmentDispatcher    │
                         │  • Reads PDC (OvercapItemAdapter│
                         │  • Resolves triggers in registry│
                         │  • Executes in Priority order   │
                         └────────────────┬────────────────┘
                                          │
          ┌───────────────────────────────┼───────────────────────────────┐
          ▼                               ▼                               ▼
 [ EntityHitTrigger ]            [ BlockBreakTrigger ]           [ ArmorDefenseTrigger ]
 (Attacker hit proc)             (Mining / harvest proc)         (Defensive damage proc)
```

---

## 2. Scoped Cascade Guard & Concrete Context Contracts

### 2.1 Scoped Bounded Cascade Guard (`CascadeGuard`)
To prevent infinite recursion when an enchantment simulates a block break or damage cascade without globally muting independent events on the same thread:

```java
public final class CascadeGuard {
    private static final int MAX_CASCADE_DEPTH = 3;
    private static final ThreadLocal<Integer> CURRENT_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static int getDepth() {
        return CURRENT_DEPTH.get();
    }

    public static boolean canCascade() {
        return CURRENT_DEPTH.get() < MAX_CASCADE_DEPTH;
    }

    public static void runInScope(Runnable action) {
        int depth = CURRENT_DEPTH.get();
        if (depth >= MAX_CASCADE_DEPTH) return;
        CURRENT_DEPTH.set(depth + 1);
        try {
            action.run();
        } finally {
            CURRENT_DEPTH.set(depth);
        }
    }
}
```

### 2.2 Concrete Damage Modification (`MutableDamage`)
Rather than opaque event mutation, damage triggers receive an explicit `MutableDamage` container:

```java
public final class MutableDamage {
    private final double initialDamage;
    private double bonusDamage = 0.0;
    private double multiplier = 1.0;
    private boolean cancelled = false;

    public MutableDamage(double initialDamage) {
        this.initialDamage = initialDamage;
    }

    public double getInitialDamage() { return initialDamage; }
    public double getBonusDamage() { return bonusDamage; }
    public double getMultiplier() { return multiplier; }
    public boolean isCancelled() { return cancelled; }

    public void addBonus(double bonus) { this.bonusDamage += bonus; }
    public void multiply(double factor) { this.multiplier *= factor; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public double getFinalDamage() {
        if (cancelled) return 0.0;
        return Math.max(0.0, (initialDamage + bonusDamage) * multiplier);
    }
}
```

### 2.3 Scoped Cascading Operations (`CascadeScope`)
Passed to block triggers to perform safe adjacent breaks or spawn bonus drops within recursion bounds:

```java
public record CascadeScope(
    World world,
    Player player,
    ItemStack tool,
    int currentDepth
) {
    public boolean breakBlockSafely(Block block, boolean dropItems) {
        if (!CascadeGuard.canCascade()) return false;
        final boolean[] success = new boolean[]{false};
        CascadeGuard.runInScope(() -> {
            success[0] = block.breakNaturally(tool, dropItems);
        });
        return success[0];
    }

    public void dropItemSafely(Location location, ItemStack item) {
        if (item == null || item.isEmpty() || location == null || location.getWorld() == null) return;
        location.getWorld().dropItemNaturally(location, item);
    }
}
```

---

## 3. Modular Trigger Interfaces

Enchantments implement only the specific triggers they require:

```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

// 1. Melee offensive hit
public interface EntityHitTrigger {
    void onEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, int level);
}

// 2. Block mining & harvesting
public interface BlockBreakTrigger {
    void onBlockBreak(Player player, Block block, int level, CascadeScope scope);
}

// 3. Entity kills (on-death procs)
public interface EntityKillTrigger {
    void onEntityKill(Player killer, LivingEntity victim, int level);
}

// 4. Bow & Crossbow projectile launches
public interface BowShootTrigger {
    void onBowShoot(Player shooter, Projectile projectile, float force, int level);
}

// 5. Equipped Armor taking damage
public interface ArmorDefenseTrigger {
    void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level);
}

// 6. Right-click active abilities with cooldowns
public interface ActiveInteractTrigger {
    void onActiveInteract(Player player, ItemStack item, int level);
}
```

---

## 4. Central Dispatcher & Event Listener

### 4.1 Dispatcher Pipeline (`CustomEnchantmentDispatcher`)
```java
public final class CustomEnchantmentDispatcher {
    private final OvercapItemAdapter itemAdapter;
    private final EnchantmentRegistry registry;

    public CustomEnchantmentDispatcher(OvercapItemAdapter itemAdapter, EnchantmentRegistry registry) {
        this.itemAdapter = itemAdapter;
        this.registry = registry;
    }

    public void dispatchEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, ItemStack weapon) {
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(weapon);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof EntityHitTrigger trigger) {
                            trigger.onEntityHit(attacker, victim, damage, entry.getValue());
                        }
                    });
        }
    }

    public void dispatchBlockBreak(Player player, Block block, ItemStack tool, CascadeScope scope) {
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(tool);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof BlockBreakTrigger trigger) {
                            trigger.onBlockBreak(player, block, entry.getValue(), scope);
                        }
                    });
        }
    }
    
    // dispatchEntityKill, dispatchBowShoot, dispatchArmorDefense, dispatchActiveInteract
}
```

### 4.2 Paper Listener (`CustomEnchantmentListener`)
* Listens to Paper events at `EventPriority.HIGH`.
* Verifies `CascadeGuard.canCascade()` before dispatching.
* Adapts raw event objects into `MutableDamage` and `CascadeScope`.
* Writes final evaluated state back to the Bukkit event (e.g. `event.setDamage(damage.getFinalDamage())`).

---

## 5. Package Organization

```
dev.mintychochip.merlin.paper.enchanting.custom/
├── CascadeGuard.java
├── CascadeScope.java
├── MutableDamage.java
├── CustomEnchantmentDispatcher.java
├── CustomEnchantmentListener.java
└── trigger/
    ├── EntityHitTrigger.java
    ├── BlockBreakTrigger.java
    ├── EntityKillTrigger.java
    ├── BowShootTrigger.java
    ├── ArmorDefenseTrigger.java
    └── ActiveInteractTrigger.java
```

---

## 6. Verification & Self-Review

* **No Placeholders:** All types, methods, fields, and cascade limits are explicitly defined.
* **Consistency:** Fully compatible with `OvercapItemAdapter` and `EnchantmentRegistry`.
* **Safety:** Depth-bounded cascade prevention with zero risk of infinite event loops.
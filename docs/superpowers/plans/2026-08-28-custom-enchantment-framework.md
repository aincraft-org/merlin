# Custom Enchantment Framework & Event Dispatch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a modular, server-safe custom enchantment event and dispatch framework under `dev.mintychochip.merlin.paper.enchanting.custom` featuring scoped cascade/recursion protection, concrete damage/scope contracts, granular trigger interfaces, and an event dispatcher pipeline.

**Architecture:** The framework introduces a scoped depth-tracking `CascadeGuard` and `CascadeScope` to prevent infinite event loops, concrete `MutableDamage` containers for arithmetic damage scaling, single-responsibility trigger interfaces in `custom.trigger`, and a central `CustomEnchantmentDispatcher` adapted via `CustomEnchantmentListener`.

**Tech Stack:** Java 21, PaperMC API (1.21.4), JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-08-28-custom-enchantment-framework-design.md`

## Global Constraints

* Java 21 records, sealed types, and pattern matching.
* Scoped cascade limit set to `MAX_CASCADE_DEPTH = 3`.
* Triggers and contexts interface directly with `EnchantmentRegistry` and `OvercapItemAdapter`.

---

### Task 1: Scoped Cascade Guard & Concrete Contexts

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/CascadeGuard.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/MutableDamage.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/CascadeScope.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CascadeGuardTest.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/MutableDamageTest.java`

- [ ] **Step 1: Write failing tests for CascadeGuard and MutableDamage**

Create `CascadeGuardTest.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CascadeGuardTest {
    @Test
    void tracksDepthAndBoundsRecursion() {
        assertEquals(0, CascadeGuard.getDepth());
        assertTrue(CascadeGuard.canCascade());

        CascadeGuard.runInScope(() -> {
            assertEquals(1, CascadeGuard.getDepth());
            CascadeGuard.runInScope(() -> {
                assertEquals(2, CascadeGuard.getDepth());
                CascadeGuard.runInScope(() -> {
                    assertEquals(3, CascadeGuard.getDepth());
                    assertFalse(CascadeGuard.canCascade());

                    // Nested 4th attempt should be blocked
                    CascadeGuard.runInScope(() -> {
                        assertEquals(3, CascadeGuard.getDepth());
                    });
                });
                assertEquals(2, CascadeGuard.getDepth());
            });
            assertEquals(1, CascadeGuard.getDepth());
        });
        assertEquals(0, CascadeGuard.getDepth());
    }
}
```

Create `MutableDamageTest.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class MutableDamageTest {
    @Test
    void calculatesBonusesAndMultipliersCorrectly() {
        MutableDamage dmg = new MutableDamage(10.0);
        assertEquals(10.0, dmg.getFinalDamage());

        dmg.addBonus(2.5); // 12.5
        assertEquals(12.5, dmg.getFinalDamage());

        dmg.multiply(1.5); // 12.5 * 1.5 = 18.75
        assertEquals(18.75, dmg.getFinalDamage(), 0.001);

        dmg.setCancelled(true);
        assertEquals(0.0, dmg.getFinalDamage());
    }
}
```

- [ ] **Step 2: Implement CascadeGuard, MutableDamage, and CascadeScope**

Create `CascadeGuard.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

public final class CascadeGuard {
    public static final int MAX_CASCADE_DEPTH = 3;
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

    private CascadeGuard() {}
}
```

Create `MutableDamage.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

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

Create `CascadeScope.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

- [ ] **Step 3: Run tests and verify**

Run: `./gradlew :merlin-paper:test --tests "dev.mintychochip.merlin.paper.enchanting.custom.*"`  
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/
git commit -m "feat(enchanting): add CascadeGuard, MutableDamage, and CascadeScope"
```

---

### Task 2: Granular Trigger Interfaces

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/EntityHitTrigger.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/BlockBreakTrigger.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/EntityKillTrigger.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/BowShootTrigger.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/ArmorDefenseTrigger.java`
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/ActiveInteractTrigger.java`

- [ ] **Step 1: Implement trigger interfaces**

Create `EntityHitTrigger.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface EntityHitTrigger {
    void onEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, int level);
}
```

Create `BlockBreakTrigger.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface BlockBreakTrigger {
    void onBlockBreak(Player player, Block block, int level, CascadeScope scope);
}
```

Create `EntityKillTrigger.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface EntityKillTrigger {
    void onEntityKill(Player killer, LivingEntity victim, int level);
}
```

Create `BowShootTrigger.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public interface BowShootTrigger {
    void onBowShoot(Player shooter, Projectile projectile, float force, int level);
}
```

Create `ArmorDefenseTrigger.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface ArmorDefenseTrigger {
    void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level);
}
```

Create `ActiveInteractTrigger.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ActiveInteractTrigger {
    void onActiveInteract(Player player, ItemStack item, int level);
}
```

- [ ] **Step 2: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/trigger/
git commit -m "feat(enchanting): add modular trigger interfaces"
```

---

### Task 3: Custom Enchantment Dispatcher Engine

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcher.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentDispatcherTest.java`

- [ ] **Step 1: Write failing tests for CustomEnchantmentDispatcher**

Create `CustomEnchantmentDispatcherTest.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentDispatcherTest {
    interface TestHitHandler extends OvercapEffectHandler, EntityHitTrigger {}

    @Test
    void dispatchesEntityHitToRegisteredTriggers() {
        NamespacedKey key = new NamespacedKey("merlin", "test_vampirism");
        TestHitHandler handler = mock(TestHitHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Test Vampirism", 0, 3, 10, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.of(handler));

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(def);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack sword = mock(ItemStack.class);
        when(adapter.readOvercap(sword)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);

        Player attacker = mock(Player.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage dmg = new MutableDamage(10.0);

        dispatcher.dispatchEntityHit(attacker, victim, dmg, sword);
        verify(handler).onEntityHit(attacker, victim, dmg, 2);
    }
}
```

- [ ] **Step 2: Implement CustomEnchantmentDispatcher**

Create `CustomEnchantmentDispatcher.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ActiveInteractTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BowShootTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityKillTrigger;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public final class CustomEnchantmentDispatcher {
    private final OvercapItemAdapter itemAdapter;
    private final EnchantmentRegistry registry;

    public CustomEnchantmentDispatcher(OvercapItemAdapter itemAdapter, EnchantmentRegistry registry) {
        this.itemAdapter = itemAdapter;
        this.registry = registry;
    }

    public void dispatchEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return;
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
        if (tool == null || tool.isEmpty()) return;
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

    public void dispatchEntityKill(Player killer, LivingEntity victim, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(weapon);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof EntityKillTrigger trigger) {
                            trigger.onEntityKill(killer, victim, entry.getValue());
                        }
                    });
        }
    }

    public void dispatchBowShoot(Player shooter, Projectile projectile, float force, ItemStack bow) {
        if (bow == null || bow.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(bow);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof BowShootTrigger trigger) {
                            trigger.onBowShoot(shooter, projectile, force, entry.getValue());
                        }
                    });
        }
    }

    public void dispatchArmorDefense(Player defender, Entity attacker, MutableDamage damage, ItemStack[] armor) {
        if (armor == null) return;
        for (ItemStack piece : armor) {
            if (piece == null || piece.isEmpty()) continue;
            Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(piece);
            for (var entry : enchants.entrySet()) {
                registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                        .ifPresent(handler -> {
                            if (handler instanceof ArmorDefenseTrigger trigger) {
                                trigger.onArmorDefense(defender, attacker, damage, entry.getValue());
                            }
                        });
            }
        }
    }

    public void dispatchActiveInteract(Player player, ItemStack item) {
        if (item == null || item.isEmpty()) return;
        Map<NamespacedKey, Integer> enchants = itemAdapter.readOvercap(item);
        for (var entry : enchants.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> {
                        if (handler instanceof ActiveInteractTrigger trigger) {
                            trigger.onActiveInteract(player, item, entry.getValue());
                        }
                    });
        }
    }
}
```

- [ ] **Step 3: Run test to verify it passes**

Run: `./gradlew :merlin-paper:test --tests "dev.mintychochip.merlin.paper.enchanting.custom.CustomEnchantmentDispatcherTest"`  
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/ merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/
git commit -m "feat(enchanting): add CustomEnchantmentDispatcher engine"
```

---

### Task 4: Paper Event Adapter & Plugin Wiring

**Files:**
* Create: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentListener.java`
* Modify: `merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java`
* Test: `merlin-paper/src/test/java/dev/mintychochip/merlin/paper/enchanting/custom/CustomEnchantmentListenerTest.java`

- [ ] **Step 1: Implement CustomEnchantmentListener**

Create `CustomEnchantmentListener.java`:
```java
package dev.mintychochip.merlin.paper.enchanting.custom;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class CustomEnchantmentListener implements Listener {
    private final CustomEnchantmentDispatcher dispatcher;

    public CustomEnchantmentListener(CustomEnchantmentDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            MutableDamage damage = new MutableDamage(event.getDamage());
            dispatcher.dispatchEntityHit(attacker, victim, damage, weapon);
            if (damage.isCancelled()) {
                event.setCancelled(true);
            } else {
                event.setDamage(damage.getFinalDamage());
            }
        }
        if (event.getEntity() instanceof Player defender) {
            ItemStack[] armor = defender.getInventory().getArmorContents();
            MutableDamage damage = new MutableDamage(event.getDamage());
            dispatcher.dispatchArmorDefense(defender, event.getDamager(), damage, armor);
            if (damage.isCancelled()) {
                event.setCancelled(true);
            } else {
                event.setDamage(damage.getFinalDamage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!CascadeGuard.canCascade()) return;
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        CascadeScope scope = new CascadeScope(event.getBlock().getWorld(), player, tool, CascadeGuard.getDepth());
        dispatcher.dispatchBlockBreak(player, event.getBlock(), tool, scope);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        if (!CascadeGuard.canCascade()) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            dispatcher.dispatchEntityKill(killer, event.getEntity(), weapon);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (event.getEntity() instanceof Player shooter && event.getBow() != null) {
            dispatcher.dispatchBowShoot(shooter, (org.bukkit.entity.Projectile) event.getProjectile(), event.getForce(), event.getBow());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && !item.isEmpty()) {
                dispatcher.dispatchActiveInteract(event.getPlayer(), item);
            }
        }
    }
}
```

- [ ] **Step 2: Wire CustomEnchantmentListener in MerlinPlugin.java**

In `MerlinPlugin.java`:
```java
var customDispatcher = new CustomEnchantmentDispatcher(overcapAdapter, enchantmentRegistry);
getServer().getPluginManager().registerEvents(new CustomEnchantmentListener(customDispatcher), this);
```

- [ ] **Step 3: Run full build and test suite**

Run: `./gradlew build test`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add merlin-paper/src/main/java/dev/mintychochip/merlin/paper/enchanting/custom/ merlin-paper/src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java
git commit -m "feat(enchanting): add CustomEnchantmentListener and wire dispatcher in MerlinPlugin"
```

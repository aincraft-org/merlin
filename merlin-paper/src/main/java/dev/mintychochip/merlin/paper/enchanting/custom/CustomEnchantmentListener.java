package dev.mintychochip.merlin.paper.enchanting.custom;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.FishHook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ArmoredHorseInventory;

public final class CustomEnchantmentListener implements Listener {
    private record BucketRestoration(Player player, EquipmentSlot hand, ItemStack item, Material result) {}

    private final CustomEnchantmentDispatcher dispatcher;
    private final Consumer<Runnable> schedulePostOperation;
    private final Map<UUID, ItemStack> projectileItems = new HashMap<>();
    private final Map<EntityShootBowEvent, ItemStack> pendingBowSources = new IdentityHashMap<>();
    private final Map<UUID, ItemStack> fishingRods = new HashMap<>();
    private final Map<PlayerBucketEvent, BucketRestoration> pendingBucketRestorations = new IdentityHashMap<>();

    public CustomEnchantmentListener(CustomEnchantmentDispatcher dispatcher) {
        this(dispatcher, Runnable::run);
    }

    public CustomEnchantmentListener(
            CustomEnchantmentDispatcher dispatcher, Consumer<Runnable> schedulePostOperation) {
        this.dispatcher = dispatcher;
        this.schedulePostOperation = schedulePostOperation;
    }

    private static ItemStack mainHandItem(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getInventory().getItemInMainHand();
        }
        EntityEquipment equipment = entity.getEquipment();
        return equipment == null ? null : equipment.getItemInMainHand();
    }

    private static ItemStack[] equippedItems(LivingEntity entity) {
        ArrayList<ItemStack> items = new ArrayList<>(9);
        if (entity instanceof Player player) {
            var inventory = player.getInventory();
            if (inventory != null) {
                items.add(inventory.getItemInMainHand());
                ItemStack[] extra = inventory.getExtraContents();
                if (extra != null) {
                    for (ItemStack item : extra) {
                        items.add(item);
                    }
                }
                ItemStack[] armor = inventory.getArmorContents();
                if (armor != null) {
                    for (ItemStack item : armor) {
                        items.add(item);
                    }
                }
            }
        } else {
            EntityEquipment equipment = entity.getEquipment();
            if (equipment != null) {
                items.add(equipment.getItemInMainHand());
                items.add(equipment.getItemInOffHand());
                ItemStack[] armor = equipment.getArmorContents();
                if (armor != null) {
                    for (ItemStack item : armor) {
                        items.add(item);
                    }
                }
            }
        }
        if (entity instanceof AbstractHorse horse) {
            var inventory = horse.getInventory();
            items.add(inventory.getSaddle());
            if (inventory instanceof ArmoredHorseInventory armored) {
                items.add(armored.getArmor());
            }
        }
        return items.toArray(ItemStack[]::new);
    }

    // 1. Combat & Damage
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            if (!(event.getEntity() instanceof LivingEntity victim)) return;

            MutableDamage damage = new MutableDamage(event.getDamage());
            if (event.getDamager() instanceof LivingEntity attacker) {
                dispatcher.dispatchEntityHit(attacker, victim, damage, mainHandItem(attacker));
            }

            dispatcher.dispatchEntityHitByEntity(
                    victim, event.getDamager(), damage, equippedItems(victim));
            Player defender = victim instanceof Player player ? player : null;
            ItemStack[] defenderArmor = defender == null ? null : defender.getInventory().getArmorContents();
            if (defender != null) {
                dispatcher.dispatchArmorDefense(defender, event.getDamager(), damage, defenderArmor);
            }

            if (damage.isCancelled()) {
                event.setCancelled(true);
            } else {
                double finalDamage = damage.getFinalDamage();
                event.setDamage(finalDamage);
                if (defender != null && finalDamage > 0.0) {
                    schedulePostOperation.accept(() -> {
                        if (!event.isCancelled() && Double.isFinite(event.getFinalDamage())
                                && event.getFinalDamage() > 0.0 && !defender.isDead()) {
                            dispatcher.dispatchArmorDefensePost(
                                    defender, event.getDamager(), damage, defenderArmor);
                        }
                    });
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (event instanceof EntityDamageByEntityEvent) return; // Handled above
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        CascadeGuard.runInScope(() -> {
            MutableDamage damage = new MutableDamage(event.getDamage());
            if (entity instanceof Player player) {
                dispatcher.dispatchEnvironmentalDamage(
                        player, event.getCause(), damage, player.getInventory().getArmorContents());
            }
            dispatcher.dispatchEntityEnvironmentalDamage(
                    entity, event.getCause(), damage, equippedItems(entity));
            if (damage.isCancelled()) {
                event.setCancelled(true);
            } else {
                event.setDamage(damage.getFinalDamage());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            ItemStack item = event.getItemDrop().getItemStack();
            event.setCancelled(dispatcher.dispatchPlayerDrop(event.getPlayer(), item));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        CascadeGuard.runInScope(() -> {
            int modified = dispatcher.dispatchFoodLevelChange(
                    player, player.getFoodLevel(), event.getFoodLevel(), equippedItems(player));
            event.setFoodLevel(Math.max(0, Math.min(20, modified)));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHorseJump(HorseJumpEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            float modified = dispatcher.dispatchHorseJump(
                    event.getEntity(), event.getPower(), equippedItems(event.getEntity()));
            if (Float.isNaN(modified)) modified = 0.0f;
            event.setPower(Math.max(0.0f, Math.min(1.0f, modified)));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (event.getDamageSource() == null ||
            !(event.getDamageSource().getCausingEntity() instanceof LivingEntity killer)) return;
        CascadeGuard.runInScope(() -> {
            ItemStack weapon = mainHandItem(killer);
            MutableExperience experience = new MutableExperience(event.getDroppedExp());
            dispatcher.dispatchEntityKill(killer, event.getEntity(), event.getDrops(), experience, weapon);
            event.setDroppedExp(experience.getAmount());
        });
    }

    // 2. Ranged & Projectiles
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!CascadeGuard.canCascade()) return;
        LivingEntity shooter = event.getEntity();
        ItemStack bow = event.getBow();
        if (bow == null) return;
        CascadeGuard.runInScope(() -> {
            ItemStack source = bow.clone();
            pendingBowSources.put(event, source);
            try {
                dispatcher.dispatchBowShoot(shooter, event.getProjectile(), source, event.getForce());
            } catch (RuntimeException | Error failure) {
                pendingBowSources.remove(event);
                throw failure;
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBowShootFinal(EntityShootBowEvent event) {
        ItemStack source = pendingBowSources.remove(event);
        if (source == null || event.isCancelled()) return;
        if (event.getProjectile() instanceof Projectile projectile) {
            projectileItems.put(projectile.getUniqueId(), source);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.isCancelled()) return;
        ItemStack weapon = projectileItems.remove(event.getEntity().getUniqueId());
        if (!CascadeGuard.canCascade() || weapon == null) return;
        if (event.getEntity().getShooter() instanceof LivingEntity shooter) {
            CascadeGuard.runInScope(() -> {
                dispatcher.dispatchProjectileHit(
                        shooter, event.getEntity(), event.getHitEntity(), event.getHitBlock(), weapon);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHitFinal(ProjectileHitEvent event) {
        projectileItems.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        UUID entityId = event.getEntity().getUniqueId();
        projectileItems.remove(entityId);
        fishingRods.remove(entityId);
    }

    // 3. Mining, Harvesting & Blocks
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!CascadeGuard.canCascade() || event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (player == null || block == null) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemStack toolSnapshot = tool == null ? null : tool.clone();
        final ItemStack postTool = toolSnapshot == null ? tool : toolSnapshot;
        final BlockState originalState = block.getState();
        CascadeGuard.runInScope(() -> {
            CascadeScope scope = new CascadeScope(block.getWorld(), player, tool, CascadeGuard.getDepth());
            dispatcher.dispatchBlockBreak(player, block, tool, scope);
        });
        if (event.isCancelled()) return;
        schedulePostOperation.accept(() -> {
            if (!event.isCancelled()) {
                dispatcher.dispatchBlockBreakPost(player, originalState, postTool);
            }
        });
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (!CascadeGuard.canCascade()) return;
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchBlockDrop(player, event.getBlockState(), event.getItems(), tool);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchBlockPlace(event.getPlayer(), event.getBlockPlaced(), event.getBlockAgainst(), event.getItemInHand());
        });
    }

    // 4. Gathering & Utility Tools
    private static boolean isTerminalFishingState(PlayerFishEvent.State state) {
        return state != PlayerFishEvent.State.FISHING
                && state != PlayerFishEvent.State.BITE
                && state != PlayerFishEvent.State.LURED;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        FishHook hook = event.getHook();
        UUID hookId = hook == null ? null : hook.getUniqueId();
        PlayerFishEvent.State state = event.getState();
        ItemStack rod;
        if (state == PlayerFishEvent.State.FISHING) {
            EquipmentSlot hand = event.getHand();
            ItemStack selectedRod = hand == null ? null : event.getPlayer().getInventory().getItem(hand);
            rod = selectedRod == null ? null : selectedRod.clone();
        } else {
            rod = hookId == null ? null : fishingRods.get(hookId);
        }

        if (state == PlayerFishEvent.State.FISHING && hookId != null && rod != null) {
            fishingRods.put(hookId, rod);
        }
        if (isTerminalFishingState(state) && hookId != null) {
            fishingRods.remove(hookId);
        }
        if (!CascadeGuard.canCascade() || rod == null) return;

        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchPlayerFish(event.getPlayer(), hook, event.getCaught(), state, rod);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFishFinal(PlayerFishEvent event) {
        PlayerFishEvent.State state = event.getState();
        if (!isTerminalFishingState(state)
                && !(event.isCancelled() && state == PlayerFishEvent.State.FISHING)) {
            return;
        }
        FishHook hook = event.getHook();
        if (hook != null) {
            fishingRods.remove(hook.getUniqueId());
        }

    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchShearEntity(event.getPlayer(), event.getEntity(), event.getItem(), event.getHand());
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!CascadeGuard.canCascade()) return;
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        Player player = event.getPlayer();
        ItemStack preActionItem = player.getInventory().getItem(hand);
        if (preActionItem == null) return;
        CascadeGuard.runInScope(() -> {
            if (dispatcher.dispatchBucketEmpty(
                    player, event.getBlock(), event.getBlockFace(), preActionItem, hand)) {
                pendingBucketRestorations.put(
                        event, new BucketRestoration(player, hand, preActionItem, Material.WATER_BUCKET));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBucketEmptyFinal(PlayerBucketEmptyEvent event) {
        finishBucketRestoration(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!CascadeGuard.canCascade()) return;
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        Player player = event.getPlayer();
        ItemStack preActionItem = player.getInventory().getItem(hand);
        if (preActionItem == null) return;
        CascadeGuard.runInScope(() -> {
            if (dispatcher.dispatchBucketFill(
                    player, event.getBlock(), event.getBlockFace(), preActionItem, hand)) {
                pendingBucketRestorations.put(
                        event, new BucketRestoration(player, hand, preActionItem, Material.BUCKET));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBucketFillFinal(PlayerBucketFillEvent event) {
        finishBucketRestoration(event);
    }

    private void finishBucketRestoration(PlayerBucketEvent event) {
        BucketRestoration restoration = pendingBucketRestorations.remove(event);
        if (restoration == null || event.isCancelled()) return;
        schedulePostOperation.accept(() -> {
            restoration.item().setType(restoration.result());
            var inventory = restoration.player().getInventory();
            if (inventory != null) {
                inventory.setItem(restoration.hand(), restoration.item());
            }
        });
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            int modified = dispatcher.dispatchItemDamage(event.getPlayer(), event.getItem(), event.getDamage());
            event.setDamage(Math.max(0, modified));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityItemDamage(EntityDamageItemEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            int modified = dispatcher.dispatchEntityItemDamage(
                    event.getEntity(), event.getItem(), event.getDamage());
            event.setDamage(Math.max(0, modified));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchItemConsume(event.getPlayer(), event.getItem());
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!CascadeGuard.canCascade() || event.isCancelled()) return;
        ItemStack item = event.getItem();
        if (item == null || item.isEmpty()) return;
        CascadeGuard.runInScope(() -> {
            if (event.useInteractedBlock() != Event.Result.DENY
                    && event.useItemInHand() != Event.Result.DENY) {
                dispatcher.dispatchActiveInteract(event.getPlayer(), event.getAction(), event.getClickedBlock(), item);
            }
            if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                    && event.useItemInHand() != Event.Result.DENY) {
                dispatcher.dispatchActivate(event.getPlayer(), item);
            }
        });
    }

    // 5. Movement & Traversal
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!CascadeGuard.canCascade()) return;

        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchPlayerMove(
                    event.getPlayer(), event.getFrom(), event.getTo(), equippedItems(event.getPlayer()));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        if (!CascadeGuard.canCascade()) return;

        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchEntityMove(
                    event.getEntity(), event.getFrom(), event.getTo(), equippedItems(event.getEntity()));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJump(PlayerJumpEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            ItemStack boots = event.getPlayer().getInventory().getBoots();
            dispatcher.dispatchPlayerJump(event.getPlayer(), event.getFrom(), event.getTo(), boots);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!CascadeGuard.canCascade()) return;
        if (event.getEntity() instanceof Player player) {
            CascadeGuard.runInScope(() -> {
                ItemStack chestplate = player.getInventory().getChestplate();
                dispatcher.dispatchToggleGlide(player, event.isGliding(), chestplate);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            ItemStack[] armor = event.getPlayer().getInventory().getArmorContents();
            dispatcher.dispatchToggleSneak(event.getPlayer(), event.isSneaking(), armor);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            ItemStack boots = event.getPlayer().getInventory().getBoots();
            dispatcher.dispatchToggleSprint(event.getPlayer(), event.isSprinting(), boots);
        });
    }

    // 6. Interaction & Exp
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!CascadeGuard.canCascade()) return;
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        CascadeGuard.runInScope(() -> {
            dispatcher.dispatchEntityInteract(event.getPlayer(), event.getRightClicked(), item, event.getHand());
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExpGain(PlayerExpChangeEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            ItemStack[] equipment = equippedItems(event.getPlayer());
            int modified = dispatcher.dispatchExpGain(event.getPlayer(), event.getAmount(), equipment);
            event.setAmount(Math.max(0, modified));
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dispatcher.clearActivationCooldowns(event.getPlayer().getUniqueId());
        dispatcher.purgeActivationCooldowns();
    }
}

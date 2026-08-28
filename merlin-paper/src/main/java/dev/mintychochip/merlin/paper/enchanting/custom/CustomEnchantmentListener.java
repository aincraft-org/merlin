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
        CascadeGuard.runInScope(() -> {
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
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            Player player = event.getPlayer();
            ItemStack tool = player.getInventory().getItemInMainHand();
            CascadeScope scope = new CascadeScope(event.getBlock().getWorld(), player, tool, CascadeGuard.getDepth());
            dispatcher.dispatchBlockBreak(player, event.getBlock(), tool, scope);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                ItemStack weapon = killer.getInventory().getItemInMainHand();
                dispatcher.dispatchEntityKill(killer, event.getEntity(), weapon);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            if (event.getEntity() instanceof Player shooter && event.getBow() != null) {
                dispatcher.dispatchBowShoot(shooter, (org.bukkit.entity.Projectile) event.getProjectile(), event.getForce(), event.getBow());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!CascadeGuard.canCascade()) return;
        CascadeGuard.runInScope(() -> {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = event.getItem();
                if (item != null && !item.isEmpty()) {
                    dispatcher.dispatchActiveInteract(event.getPlayer(), item);
                }
            }
        });
    }
}

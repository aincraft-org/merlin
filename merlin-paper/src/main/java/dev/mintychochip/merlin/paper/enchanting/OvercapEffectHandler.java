package dev.mintychochip.merlin.paper.enchanting;

import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public interface OvercapEffectHandler {
    NamespacedKey key();

    default void onDamageDealt(EntityDamageByEntityEvent event, int level) {}

    default void onBlockBreak(BlockBreakEvent event, int level) {}

    default void onArmorHurt(EntityDamageEvent event, int level) {}
}

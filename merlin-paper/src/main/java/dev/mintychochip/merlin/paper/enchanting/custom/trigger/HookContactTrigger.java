package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Triggered when a fishing hook with an enchant contacts a living entity. */
public interface HookContactTrigger {
    void onHookContact(Player player, LivingEntity hit, int level);
}
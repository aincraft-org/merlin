package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.PlayerFishTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;

public final class SnapHandler implements OvercapEffectHandler, PlayerFishTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("snap");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onPlayerFish(Player player, FishHook hook, Entity caught, State state, int level) {
        if (player == null || hook == null || caught == null || level <= 0) return;
        if (state != State.CAUGHT_ENTITY) return;
        if (caught.isDead()) return;

        caught.teleport(player.getLocation());
    }
}
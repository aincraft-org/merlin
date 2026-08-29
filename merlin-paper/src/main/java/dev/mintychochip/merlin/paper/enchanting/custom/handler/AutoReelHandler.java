package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.PlayerFishTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;

public final class AutoReelHandler implements OvercapEffectHandler, PlayerFishTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("auto_reel");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onPlayerFish(Player player, FishHook hook, Entity caught, State state, int level) {
        if (player == null || hook == null || level <= 0) return;
        if (state != State.BITE) return;

        hook.pullHookedEntity();
    }
}
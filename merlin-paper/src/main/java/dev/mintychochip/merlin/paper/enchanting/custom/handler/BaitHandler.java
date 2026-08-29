package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.PlayerFishTrigger;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;

public final class BaitHandler implements OvercapEffectHandler, PlayerFishTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("bait");

    private final Random random;

    public BaitHandler() {
        this(new Random());
    }

    public BaitHandler(Random random) {
        this.random = random == null ? new Random() : random;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onPlayerFish(Player player, FishHook hook, Entity caught, State state, int level) {
        if (player == null || hook == null || caught == null || level <= 0) return;
        if (state != State.CAUGHT_FISH && state != State.CAUGHT_ENTITY) return;

        for (int i = 0; i < level; i++) {
            if (!(caught instanceof Item item)) continue;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) continue;
            ItemStack copy = stack.clone();
            copy.setAmount(Math.min(copy.getMaxStackSize(), copy.getAmount()));
            Item drop = player.getWorld().dropItemNaturally(item.getLocation(), copy);
            if (drop != null) {
                drop.setPickupDelay(10);
            }
        }
    }
}
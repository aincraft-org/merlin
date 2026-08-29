package dev.mintychochip.merlin.paper.enchanting.custom.passive;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.CustomEnchantmentSupport;
import org.bukkit.NamespacedKey;

public final class ObsidianshieldHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("obsidianshield");

    @Override
    public NamespacedKey key() {
        return KEY;
    }
}
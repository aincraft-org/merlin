package dev.mintychochip.merlin.paper.enchanting.custom.passive;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.CustomEnchantmentSupport;
import org.bukkit.NamespacedKey;

public final class WingsHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("wings");

    @Override
    public NamespacedKey key() {
        return KEY;
    }
}
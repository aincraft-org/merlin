package dev.mintychochip.merlin.paper.enchanting.custom.passive;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.CustomEnchantmentSupport;
import org.bukkit.NamespacedKey;

/** Passive effects are applied by {@link PassiveEffectApplier}; handlers exist for registry wiring. */
public final class GearsHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("gears");

    @Override
    public NamespacedKey key() {
        return KEY;
    }
}
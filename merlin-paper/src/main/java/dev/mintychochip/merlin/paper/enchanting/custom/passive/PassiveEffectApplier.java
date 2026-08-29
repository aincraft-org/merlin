package dev.mintychochip.merlin.paper.enchanting.custom.passive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Applies and removes passive enchant effects (potion, attribute, food tick, flight) based on equipped gear. */
public final class PassiveEffectApplier {
    private static final String MODIFIER_PREFIX = "merlin_passive_";
    private static final int IMPLANTS_FOOD_TICK = 1;
    private static final float IMPLANTS_SATURATION_TICK = 0.5f;
    private static final int MAX_FOOD = 20;

    @FunctionalInterface
    public interface PlayerLookup {
        Player get(UUID id);
    }

    private final PlayerLookup playerLookup;

    private final Map<UUID, List<PotionEffect>> appliedPotionEffects = new ConcurrentHashMap<>();
    private final Map<UUID, List<AttributeModifier>> appliedAttributeModifiers = new ConcurrentHashMap<>();
    private final Set<UUID> implantsPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> wingsPlayers = ConcurrentHashMap.newKeySet();

    public PassiveEffectApplier() {
        this(Bukkit::getPlayer);
    }

    public PassiveEffectApplier(PlayerLookup playerLookup) {
        this.playerLookup = playerLookup == null ? Bukkit::getPlayer : playerLookup;
    }

    /** Recomputes all passive effects from the given equipped items and applies the delta. */
    public void refresh(Player player, List<ItemStack> equipped) {
        if (player == null) return;
        UUID id = player.getUniqueId();

        List<PotionEffect> desiredPotions = new ArrayList<>();
        List<AttributeModifier> desiredAttributes = new ArrayList<>();
        Set<PassiveEnchantKey> desiredKeys = ConcurrentHashMap.newKeySet();

        for (ItemStack piece : equipped) {
            if (piece == null || piece.isEmpty()) continue;
            for (var entry : PassiveEnchantKey.passiveEffectsFor(piece).entrySet()) {
                desiredKeys.add(entry.getKey());
                passiveEffect(entry.getKey(), entry.getValue(), desiredPotions, desiredAttributes);
            }
        }

        List<PotionEffect> previousPotions = appliedPotionEffects.getOrDefault(id, List.of());
        for (PotionEffect effect : previousPotions) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : desiredPotions) {
            player.addPotionEffect(effect, true);
        }
        appliedPotionEffects.put(id, List.copyOf(desiredPotions));

        List<AttributeModifier> previousAttributes = appliedAttributeModifiers.getOrDefault(id, List.of());
        for (AttributeModifier modifier : previousAttributes) {
            removeAttributeModifier(player, modifier);
        }
        for (AttributeModifier modifier : desiredAttributes) {
            addAttributeModifier(player, modifier);
        }
        appliedAttributeModifiers.put(id, List.copyOf(desiredAttributes));

        if (desiredKeys.contains(PassiveEnchantKey.IMPLANTS)) {
            if (implantsPlayers.add(id)) {
                restoreFood(player);
            }
        } else {
            implantsPlayers.remove(id);
        }

        if (desiredKeys.contains(PassiveEnchantKey.WINGS)) {
            if (wingsPlayers.add(id) && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else if (wingsPlayers.remove(id)) {
            player.setAllowFlight(false);
        }
    }

    /** Called periodically by the plugin scheduler to restore food for players with Implants equipped. */
    public void tickImplants() {
        for (UUID id : implantsPlayers) {
            Player player = playerLookup.get(id);
            if (player == null || !player.isOnline()) continue;
            restoreFood(player);
        }
    }

    private static void restoreFood(Player player) {
        player.setFoodLevel(Math.min(MAX_FOOD, player.getFoodLevel() + IMPLANTS_FOOD_TICK));
        player.setSaturation(Math.min(MAX_FOOD, player.getSaturation() + IMPLANTS_SATURATION_TICK));
    }

    public void removeAll(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        for (PotionEffect effect : appliedPotionEffects.getOrDefault(id, List.of())) {
            player.removePotionEffect(effect.getType());
        }
        for (AttributeModifier modifier : appliedAttributeModifiers.getOrDefault(id, List.of())) {
            removeAttributeModifier(player, modifier);
        }
        appliedPotionEffects.remove(id);
        appliedAttributeModifiers.remove(id);
        implantsPlayers.remove(id);
        if (wingsPlayers.remove(id)) {
            player.setAllowFlight(false);
        }
    }

    private static void passiveEffect(
            PassiveEnchantKey key,
            int level,
            List<PotionEffect> potions,
            List<AttributeModifier> attributes) {
        switch (key) {
            case GEARS -> potions.add(
                    new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, level - 1, true, true));
            case SPRINGS -> potions.add(new PotionEffect(
                    PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, level - 1, true, true));
            case AQUATIC -> potions.add(new PotionEffect(
                    PotionEffectType.WATER_BREATHING, PotionEffect.INFINITE_DURATION, 0, true, true));
            case GLOWING -> potions.add(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, true, true));
            case OBSIDIANSHIELD -> potions.add(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, true));
            case IMPLANTS, WINGS -> {
                // Food restoration and flight are handled directly in refresh()/tickImplants().
            }
            case OVERLOAD -> attributes.add(new AttributeModifier(
                    MODIFIER_PREFIX + key.name().toLowerCase(),
                    level * 2.0,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private static void addAttributeModifier(Player player, AttributeModifier modifier) {
        AttributeInstance instance = player.getAttribute(Attribute.MAX_HEALTH);
        if (instance == null) return;
        instance.getModifiers().stream()
                .filter(m -> m.getName().equals(modifier.getName()))
                .forEach(instance::removeModifier);
        instance.addModifier(modifier);
    }

    private static void removeAttributeModifier(Player player, AttributeModifier modifier) {
        AttributeInstance instance = player.getAttribute(Attribute.MAX_HEALTH);
        if (instance == null) return;
        instance.removeModifier(modifier);
    }
}
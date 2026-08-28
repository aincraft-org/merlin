package dev.mintychochip.merlin.paper.enchanting.gui;

import dev.mintychochip.merlin.paper.enchanting.AltarProfile;
import dev.mintychochip.merlin.paper.enchanting.EnchantingOffer;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OfferValidator;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.QuantaRollEngine;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AltarGuiSession {
    public static final int SLOT_ETERNA_METER = 2;
    public static final int SLOT_QUANTA_METER = 6;
    public static final int SLOT_TARGET = 20;
    public static final int SLOT_LAPIS = 22;
    public static final int SLOT_CATALYST = 24;
    public static final int SLOT_TIER_1 = 38;
    public static final int SLOT_TIER_2 = 40;
    public static final int SLOT_TIER_3 = 42;
    public static final int SLOT_REROLL = 44;

    private final Player player;
    private final Location altarLocation;
    private final AltarProfile profile;
    private final EnchantmentRegistry registry;
    private final QuantaRollEngine rollEngine;
    private final OvercapItemAdapter itemAdapter;
    private final Inventory inventory;
    private final Random random = new Random();

    private boolean closed = false;
    private EnchantingOffer offer1;
    private EnchantingOffer offer2;
    private EnchantingOffer offer3;

    public AltarGuiSession(Player player, Location altarLocation, AltarProfile profile,
                           EnchantmentRegistry registry, QuantaRollEngine rollEngine, OvercapItemAdapter itemAdapter) {
        this.player = player;
        this.altarLocation = altarLocation;
        this.profile = profile;
        this.registry = registry;
        this.rollEngine = rollEngine;
        this.itemAdapter = itemAdapter;

        AltarInventoryHolder holder = new AltarInventoryHolder(this);
        this.inventory = Bukkit.createInventory(holder, 54, Component.text("Enchanter's Altar Matrix"));
        holder.setInventory(inventory);
        populateDecorations();
        rerollOffers();
    }

    public boolean isClosed() {
        return closed;
    }

    public Player getPlayer() {
        return player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        if (closed) return;
        player.openInventory(inventory);
    }

    public void populateDecorations() {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < 54; i++) {
            if (i != SLOT_TARGET && i != SLOT_LAPIS && i != SLOT_CATALYST &&
                i != SLOT_ETERNA_METER && i != SLOT_QUANTA_METER &&
                i != SLOT_TIER_1 && i != SLOT_TIER_2 && i != SLOT_TIER_3 && i != SLOT_REROLL) {
                inventory.setItem(i, filler);
            }
        }

        ItemStack eterna = createItem(Material.CYAN_STAINED_GLASS_PANE,
                Component.text("✦ Eterna: " + String.format("%.1f", profile.totalEterna()), NamedTextColor.AQUA),
                List.of(Component.text("Altar Matrix Power Ceiling", NamedTextColor.GRAY)));
        inventory.setItem(SLOT_ETERNA_METER, eterna);

        ItemStack quanta = createItem(Material.YELLOW_STAINED_GLASS_PANE,
                Component.text("⚡ Quanta: " + String.format("+%.0f%%", profile.totalQuanta() * 100), NamedTextColor.YELLOW),
                List.of(Component.text("Roll Volatility & Critical Odds", NamedTextColor.GRAY)));
        inventory.setItem(SLOT_QUANTA_METER, quanta);

        ItemStack reroll = createItem(Material.LAPIS_LAZULI,
                Component.text("[ Reroll Seeds ]", NamedTextColor.GOLD),
                List.of(Component.text("Costs 1 Lapis Lazuli", NamedTextColor.GRAY)));
        inventory.setItem(SLOT_REROLL, reroll);
    }

    public void rerollOffers() {
        if (closed) return;
        ItemStack target = inventory.getItem(SLOT_TARGET);
        Material mat = target != null && !target.isEmpty() ? target.getType() : Material.DIAMOND_SWORD;
        offer1 = rollEngine.generateOffer(mat, profile, 1, random);
        offer2 = rollEngine.generateOffer(mat, profile, 2, random);
        offer3 = rollEngine.generateOffer(mat, profile, 3, random);
        updateOfferButtons();
    }

    public boolean handleRerollClick() {
        if (closed) return false;
        ItemStack lapis = inventory.getItem(SLOT_LAPIS);
        if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < 1) {
            player.sendMessage(Component.text("You need at least 1 Lapis Lazuli to reroll offers!", NamedTextColor.RED));
            return false;
        }

        // Deduct 1 lapis
        lapis.setAmount(lapis.getAmount() - 1);
        if (lapis.getAmount() <= 0) {
            inventory.setItem(SLOT_LAPIS, null);
        }

        rerollOffers();
        player.sendMessage(Component.text("Enchantment offers refreshed!", NamedTextColor.GREEN));
        return true;
    }

    private void updateOfferButtons() {
        inventory.setItem(SLOT_TIER_1, createOfferButton("Tier I Offer", offer1));
        inventory.setItem(SLOT_TIER_2, createOfferButton("Tier II Offer", offer2));
        inventory.setItem(SLOT_TIER_3, createOfferButton("Tier III Offer", offer3));
    }

    private ItemStack createOfferButton(String title, EnchantingOffer offer) {
        if (offer == null) return createItem(Material.BARRIER, Component.text("No Offer", NamedTextColor.RED), List.of());
        return createItem(Material.ENCHANTED_BOOK, Component.text(title, NamedTextColor.GREEN), List.of(
                Component.text("Requires: Level " + offer.xpLevelRequirement(), NamedTextColor.YELLOW),
                Component.text("Cost: " + offer.xpLevelCost() + " XP Levels + " + offer.lapisCost() + " Lapis", NamedTextColor.GREEN),
                Component.text("Preview: " + offer.previewHint(), NamedTextColor.LIGHT_PURPLE)
        ));
    }

    public void handleEnchantClick(int tier) {
        if (closed) return;
        EnchantingOffer offer = switch (tier) {
            case 1 -> offer1;
            case 2 -> offer2;
            default -> offer3;
        };

        ItemStack target = inventory.getItem(SLOT_TARGET);
        ItemStack lapis = inventory.getItem(SLOT_LAPIS);
        ItemStack catalyst = inventory.getItem(SLOT_CATALYST);

        OfferValidator.Result validation = OfferValidator.validate(closed, player, target, lapis, catalyst, offer, registry, itemAdapter);
        if (validation instanceof OfferValidator.Result.Invalid invalid) {
            player.sendMessage(Component.text(invalid.reason(), NamedTextColor.RED));
            return;
        }

        OfferValidator.Result.Valid valid = (OfferValidator.Result.Valid) validation;

        // Apply enchants first to guarantee atomic mutation
        boolean success = itemAdapter.applyEnchantments(target, valid.enchantsToApply());
        if (!success) {
            player.sendMessage(Component.text("Enchantment application failed.", NamedTextColor.RED));
            return;
        }

        // Atomically deduct costs only on verified success
        player.setLevel(Math.max(0, player.getLevel() - valid.xpCost()));
        lapis.setAmount(lapis.getAmount() - valid.lapisCost());
        if (lapis.getAmount() <= 0) {
            inventory.setItem(SLOT_LAPIS, null);
        }

        player.sendMessage(Component.text("Enchanting complete!", NamedTextColor.GREEN));
        rerollOffers();
    }

    public void handleClose() {
        if (closed) return;
        closed = true;
        returnOrDrop(SLOT_TARGET);
        returnOrDrop(SLOT_LAPIS);
        returnOrDrop(SLOT_CATALYST);
    }

    private void returnOrDrop(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && !item.isEmpty()) {
            inventory.setItem(slot, null);
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty() && player.getWorld() != null) {
                leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
            }
        }
    }

    private static ItemStack createItem(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

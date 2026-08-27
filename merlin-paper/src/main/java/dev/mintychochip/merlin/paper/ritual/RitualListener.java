package dev.mintychochip.merlin.paper.ritual;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphToken;
import dev.mintychochip.merlin.api.glyph.MagicalInk;
import dev.mintychochip.merlin.paper.ink.InkStore;
import dev.mintychochip.merlin.paper.ink.MortarPestle;
import java.util.List;
import java.util.Optional;
import dev.mintychochip.merlin.paper.mapgui.GlyphDraftStoreAdapter;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
public final class RitualListener implements Listener {
    private final RitualRecipeTable recipes;
    private final RitualProducts products;
    private final RitualAnchor anchorMarker;
    private final RitualPedestal pedestalMarker;
    private final InkStore inks;
    private final MortarPestle mortar;
    private final GlyphDraftStoreAdapter store;

    public RitualListener(RitualRecipeTable recipes, RitualProducts products, RitualAnchor anchorMarker,
                          RitualPedestal pedestalMarker, InkStore inks, MortarPestle mortar,
                          GlyphDraftStoreAdapter store) {
        this.recipes = recipes;
        this.products = products;
        this.anchorMarker = anchorMarker;
        this.pedestalMarker = pedestalMarker;
        this.inks = inks;
        this.mortar = mortar;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!mortar.isMortar(player.getInventory().getItemInMainHand())) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.DROPPER) return;

        RitualLayout layout = RitualCircle.inspect(block, anchorMarker, pedestalMarker).orElse(null);
        if (layout == null) return;

        event.setCancelled(true);

        ItemStack glyph = null;
        ItemStack material = null;
        for (ItemStack item : layout.anchor()) {
            if (item == null || item.getType().isAir()) continue;
            if (glyph == null && store.loadToken(item).isPresent()) {
                glyph = item;
            } else if (material == null) {
                material = item;
            }
        }

        if (glyph == null || material == null) {
            failure(player);
            return;
        }

        Optional<GlyphToken> token = store.loadToken(glyph);
        if (token.isEmpty()) {
            failure(player);
            return;
        }

        RitualRecipe recipe = recipes.lookup(token.get().label(), material.getType()).orElse(null);
        if (recipe == null) {
            failure(player);
            return;
        }

        FoundInk found = findInk(layout.pedestals(), recipe.school());
        if (found == null) {
            failure(player);
            return;
        }

        layout.anchor().remove(glyph);
        layout.anchor().remove(material);
        found.inventory().remove(found.ink());

        int yield = recipes.yield(recipe, token.get().pips());
        List<ItemStack> output = products.create(recipe, yield);
        for (ItemStack item : output) {
            player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.5, 0.5), item);
        }

        player.playSound(block.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.WITCH, block.getLocation().add(0.5, 1.5, 0.5), 10, 0.3, 0.3, 0.3, 0.02);
    }

    private record FoundInk(Inventory inventory, ItemStack ink) {}

    private FoundInk findInk(List<Inventory> pedestals, GlyphElement school) {
        for (Inventory inv : pedestals) {
            for (ItemStack item : inv) {
                if (item == null || item.getType() == Material.AIR) continue;
                Optional<MagicalInk> ink = inks.read(item);
                if (ink.isPresent() && ink.get().element() == school && !ink.get().empty()) {
                    return new FoundInk(inv, item);
                }
            }
        }
        return null;
    }

    private void failure(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.8f);
    }

}

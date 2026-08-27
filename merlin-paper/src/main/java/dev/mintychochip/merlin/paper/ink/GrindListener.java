package dev.mintychochip.merlin.paper.ink;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class GrindListener implements Listener {
    private final InkStore inks;
    private final MortarPestle mortar;

    public GrindListener(InkStore inks, MortarPestle mortar) {
        this.inks = inks;
        this.mortar = mortar;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemStack flower = player.getInventory().getItemInOffHand();
        Optional<GlyphElement> element = grindElement(tool, flower, mortar);
        if (element.isEmpty()) return;

        event.setCancelled(true);
        if (flower.getAmount() <= 1) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        } else {
            flower.setAmount(flower.getAmount() - 1);
            player.getInventory().setItemInOffHand(flower);
        }
        player.getInventory().addItem(inks.create(element.get())).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.8f);
        player.getWorld().spawnParticle(
                Particle.COMPOSTER, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
    }
    static Optional<GlyphElement> grindElement(ItemStack tool, ItemStack flower, MortarPestle mortar) {
        if (!mortar.isMortar(tool) || flower == null || flower.getType() == Material.AIR) return Optional.empty();
        return FlowerGrind.elementFor(flower.getType());
    }
}

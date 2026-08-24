package dev.mintychochip.merlin.paper.tome;

import dev.mintychochip.merlin.common.glyph.GlyphCompilerImpl;
import dev.mintychochip.merlin.paper.mapgui.GlyphCommand;
import dev.mintychochip.merlin.paper.mapgui.GlyphDraftStoreAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class GlyphTomeListener implements Listener {
    private final GlyphTomeStore tomes;
    private final GlyphDraftStoreAdapter maps;

    public GlyphTomeListener(GlyphTomeStore tomes, GlyphDraftStoreAdapter maps) {
        this.tomes = tomes;
        this.maps = maps;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var player = event.getPlayer();
        if (!player.hasPermission("merlin.glyph.tome") && !player.hasPermission("merlin.glyph.draw")) return;
        var map = player.getInventory().getItemInMainHand();
        var tome = player.getInventory().getItemInOffHand();
        var token = maps.loadToken(map);
        if (!tomes.isTome(tome) || token.isEmpty()) return;
        event.setCancelled(true);
        var inserted = tomes.insert(tome, map, player.isSneaking());
        if (inserted.isEmpty()) {
            player.sendMessage(GlyphCommand.bindFailureMessage(
                    tomes.pages(tome).rejection(token.get(), GlyphCompilerImpl.INSTANCE)));
            return;
        }
        player.sendMessage("Bound " + token.get().label().id() + " into the tome.");
    }
}

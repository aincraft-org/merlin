package dev.mintychochip.merlin.paper.mapgui;

import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphToken;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class GlyphMapSaveAction {
    private final GlyphDraftStoreAdapter store;

    public GlyphMapSaveAction(GlyphDraftStoreAdapter store) {
        this.store = java.util.Objects.requireNonNull(store);
    }

    public boolean save(Player player, UUID expectedId, GlyphDraft draft) {
        return save(player, expectedId, draft, null);
    }

    public boolean save(Player player, UUID expectedId, GlyphDraft draft, GlyphToken token) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!expectedId.equals(store.itemId(held))) return false;
        Optional<GlyphDraftStoreAdapter.PreparedMapSave> prepared = store.prepareMapSave(held, expectedId, draft);
        if (prepared.isEmpty() || !expectedId.equals(store.itemId(player.getInventory().getItemInMainHand()))) {
            return false;
        }
        Optional<ItemStack> replacement = store.commitMapSave(prepared.get(), player.getWorld());
        if (replacement.isEmpty()) return false;
        var item = replacement.get();
        if (token != null) store.saveToken(item, expectedId, token);
        player.getInventory().setItemInMainHand(item);
        return true;
    }
    static <T> boolean replaceIfStillHeld(
            UUID expectedId,
            Supplier<UUID> currentId,
            Supplier<Optional<T>> prepare,
            Consumer<T> replace) {
        if (!expectedId.equals(currentId.get())) return false;
        Optional<T> replacement = prepare.get();
        if (replacement.isEmpty() || !expectedId.equals(currentId.get())) return false;
        replace.accept(replacement.get());
        return true;
    }
}

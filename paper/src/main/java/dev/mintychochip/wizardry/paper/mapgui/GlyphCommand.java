package dev.mintychochip.wizardry.paper.mapgui;

import de.flog99.mapgui.MapGui;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.glyph.GlyphRoles;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.ml.Label;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GlyphCommand implements BasicCommand {
    private final GlyphDraftStoreAdapter store;
    private final GlyphMapSaveAction mapSaveAction;
    private final GlyphClassificationService classificationService;

    public GlyphCommand(
            GlyphDraftStoreAdapter store,
            GlyphMapSaveAction mapSaveAction,
            GlyphClassificationService classificationService) {
        this.store = store;
        this.mapSaveAction = mapSaveAction;
        this.classificationService = classificationService;
    }

    @Override
    public String permission() {
        return "wizardry.glyph.draw";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!sender.hasPermission(permission())) {
            sender.sendMessage("You do not have permission to draw glyphs.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Glyph drawing requires a player.");
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("book")) {
            player.getInventory().addItem(store.createGlyphItem());
            player.sendMessage("Created a glyph canvas item.");
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("stamp")) {
            stamp(player, args);
            return;
        }
        var held = player.getInventory().getItemInMainHand();
        store.restoreRenderer(held);
        var itemId = store.itemId(held);
        if (itemId == null) {
            player.sendMessage("Hold a glyph canvas item.");
            return;
        }
        var tracker = new GlyphStrokeTracker(store.load(held, itemId).orElse(GlyphDraft.empty()));
        var opened = new GlyphScreen[1];
        opened[0] = new GlyphScreen(
                tracker,
                () -> saveOpened(player, itemId, tracker, opened[0]),
                () -> player.sendMessage("Glyph draft closed. Use /glyph save before closing to persist changes."),
                draft -> classificationService.classify(draft, result -> {
                    if (result.accepted() && !result.candidates().isEmpty()) {
                        var label = result.candidates().getFirst().label();
                        opened[0].setPendingLabel(label);
                        opened[0].setClassification(result);
                        player.sendMessage("Glyph: " + label.id());
                    } else {
                        opened[0].setClassification(result);
                        player.sendMessage("Glyph rejected.");
                    }
                }),
                player::isSneaking);
        MapGui.get().open(player, opened[0]);
    }

    private void stamp(Player player, String[] args) {
        var token = parseStamp(args);
        if (token.isEmpty()) {
            player.sendMessage("Usage: /glyph stamp <label> [pips]");
            return;
        }
        var item = store.createGlyphItem();
        var id = store.itemId(item);
        var prepared = store.prepareMapSave(item, id, GlyphDraft.empty());
        if (prepared.isPresent()) {
            var committed = store.commitMapSave(prepared.get(), player.getWorld());
            if (committed.isPresent()) item = committed.get();
        }
        store.saveToken(item, id, token.get());
        player.getInventory().addItem(item);
        player.sendMessage("Stamped " + token.get().label().id() + ".");
    }

    private void saveOpened(Player player, UUID itemId, GlyphStrokeTracker tracker, GlyphScreen screen) {
        var pending = screen.pendingLabel();
        boolean saved;
        if (pending != null) {
            int pips = GlyphRoles.hasPips(GlyphRoles.of(pending)) ? screen.pips() : 1;
            saved = mapSaveAction.save(player, itemId, tracker.snapshot(), new GlyphToken(pending, pips));
        } else {
            saved = mapSaveAction.save(player, itemId, tracker.snapshot());
        }
        if (saved) player.sendMessage("Glyph saved to map.");
        else player.sendMessage("The original glyph item is no longer held.");
    }

    static Optional<GlyphToken> parseStamp(String[] args) {
        if (args == null || args.length < 2 || !args[0].equalsIgnoreCase("stamp")) return Optional.empty();
        Label label;
        try {
            label = Label.fromId(args[1]);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        if (GlyphRoles.reserved(label)) return Optional.empty();
        int pips = 1;
        if (args.length > 2) {
            try {
                pips = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        if (pips < 1 || pips > 5) return Optional.empty();
        if (!GlyphRoles.hasPips(GlyphRoles.of(label)) && pips != 1) return Optional.empty();
        return Optional.of(new GlyphToken(label, pips));
    }
}

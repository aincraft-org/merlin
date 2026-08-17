package dev.mintychochip.wizardry.paper.mapgui;

import de.flog99.mapgui.MapGui;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
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
        var held = player.getInventory().getItemInMainHand();
        store.restoreRenderer(held);
        var itemId = store.itemId(held);
        if (itemId == null) {
            player.sendMessage("Hold a glyph canvas item.");
            return;
        }
        var tracker = new GlyphStrokeTracker(
                store.load(held, itemId).orElse(dev.mintychochip.wizardry.api.glyph.GlyphDraft.empty()));
        MapGui.get().open(player, new GlyphScreen(
                tracker,
                () -> {
                    if (mapSaveAction.save(player, itemId, tracker.snapshot())) {
                        player.sendMessage("Glyph saved to map.");
                    } else {
                        player.sendMessage("The original glyph item is no longer held.");
                    }
                },
                () -> player.sendMessage("Glyph draft closed. Use /glyph save before closing to persist changes."),
                draft -> classificationService.classify(draft, result -> player.sendMessage(
                        result.accepted() && !result.candidates().isEmpty()
                                ? "Glyph: " + result.candidates().getFirst().label().id()
                                : "Glyph rejected.")),
                player::isSneaking));
    }
}

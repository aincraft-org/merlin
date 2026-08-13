package dev.jlo.wizardry.mapgui;

import de.flog99.mapgui.MapGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GlyphMapGuiPlugin extends JavaPlugin {
    private GlyphDraftStoreAdapter store;
    private GlyphClassificationService classificationService;
    @Override public void onEnable() {
        store = new GlyphDraftStoreAdapter(this);
        classificationService = createClassificationService();
        if (getCommand("glyph") != null) getCommand("glyph").setExecutor(this::command);
    }
    private GlyphClassificationService createClassificationService() {
        try {
            var bundle = dev.jlo.wizardry.ml.ModelBundle.load(getDataFolder().toPath().resolve("model"));
            var classifier = new dev.jlo.wizardry.ml.OnnxGlyphClassifier(bundle);
            return new GlyphClassificationService(classifier, task -> getServer().getScheduler().runTask(this, task));
        } catch (Exception unavailable) {
            getLogger().warning("Glyph classifier unavailable: " + unavailable.getMessage());
            return new GlyphClassificationService(draft -> dev.jlo.wizardry.ml.Classification.rejected(java.util.List.of()), task -> getServer().getScheduler().runTask(this, task));
        }
    }
    @Override public void onDisable() {
        if (classificationService != null) classificationService.close();
        classificationService = null;
    }
    private boolean command(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wizardry.glyph.draw")) { sender.sendMessage("You do not have permission to draw glyphs."); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage("Glyph drawing requires a player."); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("book")) { player.getInventory().addItem(store.createGlyphItem()); player.sendMessage("Created a glyph canvas item."); return true; }
        var held = player.getInventory().getItemInMainHand(); var itemId = store.itemId(held);
        if (itemId == null) { player.sendMessage("Hold a glyph canvas item."); return true; }
        var tracker = new GlyphStrokeTracker();
        store.load(held, itemId).ifPresent(draft -> draft.strokes().forEach(stroke -> { tracker.acceptClick((int) stroke.points().getFirst().x(), (int) stroke.points().getFirst().y(), stroke.startedAtMillis()); for (var p : stroke.points().subList(1, stroke.points().size())) tracker.acceptClick((int) p.x(), (int) p.y(), stroke.startedAtMillis()); tracker.pause(stroke.startedAtMillis() + 300); }));
        MapGui.get().open(player, new GlyphScreen(tracker,
                () -> { if (store.save(player.getInventory().getItemInMainHand(), itemId, tracker.snapshot())) player.sendMessage("Glyph draft saved."); else player.sendMessage("The original glyph item is no longer held."); },
                () -> player.sendMessage("Glyph draft closed. Use /glyph save before closing to persist changes."),
                draft -> classificationService.classify(draft, result -> player.sendMessage(result.accepted() && !result.candidates().isEmpty() ? "Glyph: " + result.candidates().getFirst().label().id() : "Glyph rejected."))));
        return true;
    }
}

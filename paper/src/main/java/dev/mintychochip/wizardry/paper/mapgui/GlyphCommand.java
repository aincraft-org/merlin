package dev.mintychochip.wizardry.paper.mapgui;

import de.flog99.mapgui.MapGui;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.glyph.GlyphRoles;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.ml.Label;
import dev.mintychochip.wizardry.paper.tome.GlyphTomeStore;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class GlyphCommand implements BasicCommand {
    private final GlyphDraftStoreAdapter store;
    private final GlyphMapSaveAction mapSaveAction;
    private final GlyphClassificationService classificationService;
    private final GlyphTomeStore tomes;

    public GlyphCommand(
            GlyphDraftStoreAdapter store,
            GlyphMapSaveAction mapSaveAction,
            GlyphClassificationService classificationService,
            GlyphTomeStore tomes) {
        this.store = store;
        this.mapSaveAction = mapSaveAction;
        this.classificationService = classificationService;
        this.tomes = tomes;
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
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "book" -> {
                    player.getInventory().addItem(store.createGlyphItem());
                    player.sendMessage("Created a glyph canvas item.");
                    return;
                }
                case "stamp" -> {
                    stamp(player, args);
                    return;
                }
                case "tome" -> {
                    giveTome(player);
                    return;
                }
                case "bind" -> {
                    bind(player);
                    return;
                }
                case "tear" -> {
                    tear(player);
                    return;
                }
                default -> { }
            }
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

    private void giveTome(Player player) {
        if (!hasTomePermission(player)) {
            player.sendMessage("You do not have permission to use glyph tomes.");
            return;
        }
        player.getInventory().addItem(tomes.createTome());
        player.sendMessage("Created a glyph tome.");
    }

    private void bind(Player player) {
        if (!hasTomePermission(player)) {
            player.sendMessage("You do not have permission to use glyph tomes.");
            return;
        }
        var map = player.getInventory().getItemInMainHand();
        var tome = player.getInventory().getItemInOffHand();
        if (!tomes.isTome(tome)) {
            player.sendMessage("Hold a glyph tome in your off hand.");
            return;
        }
        var token = store.loadToken(map);
        if (token.isEmpty()) {
            player.sendMessage("Hold a frozen glyph map in your main hand.");
            return;
        }
        var inserted = tomes.insert(tome, map, player.isSneaking());
        if (inserted.isEmpty()) {
            player.sendMessage("That glyph cannot bind into this tome.");
            return;
        }
        player.sendMessage("Bound " + token.get().label().id() + " into the tome.");
    }

    private void tear(Player player) {
        if (!hasTomePermission(player)) {
            player.sendMessage("You do not have permission to use glyph tomes.");
            return;
        }
        var tome = heldTome(player);
        if (tome == null) {
            player.sendMessage("Hold a glyph tome.");
            return;
        }
        var torn = tomes.tear(tome, player.getWorld());
        if (torn.isEmpty()) {
            player.sendMessage("The tome has no pages.");
            return;
        }
        var leftover = player.getInventory().addItem(torn.get());
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        var label = store.loadToken(torn.get()).map(token -> token.label().id()).orElse("glyph");
        player.sendMessage("Tore " + label + " from the tome.");
    }

    private ItemStack heldTome(Player player) {
        var main = player.getInventory().getItemInMainHand();
        if (tomes.isTome(main)) return main;
        var off = player.getInventory().getItemInOffHand();
        return tomes.isTome(off) ? off : null;
    }

    private static boolean hasTomePermission(Player player) {
        return player.hasPermission("wizardry.glyph.tome") || player.hasPermission("wizardry.glyph.draw");
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

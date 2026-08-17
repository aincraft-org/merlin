package dev.mintychochip.wizardry.paper;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.ml.Classification;
import dev.mintychochip.wizardry.api.ml.ModelBundle;
import dev.mintychochip.wizardry.common.ml.OnnxGlyphClassifier;
import dev.mintychochip.wizardry.paper.book.ScribeBookStore;
import dev.mintychochip.wizardry.paper.command.ScribeCommand;
import dev.mintychochip.wizardry.paper.dialog.ScribeDialog;
import dev.mintychochip.wizardry.paper.listener.ScribeBookListener;
import dev.mintychochip.wizardry.paper.listener.ScribeChatListener;
import dev.mintychochip.wizardry.paper.mapgui.GlyphClassificationService;
import dev.mintychochip.wizardry.paper.mapgui.GlyphCommand;
import dev.mintychochip.wizardry.paper.mapgui.GlyphDraftStoreAdapter;
import dev.mintychochip.wizardry.paper.mapgui.GlyphMapRehydrationListener;
import dev.mintychochip.wizardry.paper.mapgui.GlyphMapSaveAction;
import dev.mintychochip.wizardry.paper.runtime.SpellRuntime;
import dev.mintychochip.wizardry.paper.tome.GlyphTomeListener;
import dev.mintychochip.wizardry.paper.tome.GlyphTomeStore;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class WizardryPlugin extends JavaPlugin {
    private ScribeBookStore books;
    private SpellRuntime runtime;
    private ScribeDialog dialog;
    private GlyphDraftStoreAdapter store;
    private GlyphMapSaveAction mapSaveAction;
    private GlyphClassificationService classificationService;

    @Override
    public void onEnable() {
        books = new ScribeBookStore(this);
        runtime = new SpellRuntime((delay, task) -> getServer().getScheduler().runTaskLater(this, task, delay));
        dialog = new ScribeDialog(books, (playerId, compilation) -> {
            var player = getServer().getPlayer(playerId);
            if (player == null) return false;
            if (!(compilation instanceof CompileResult.Ok ok)) return false;
            var spell = ok.spell();
            var range = spell.actions().stream()
                    .filter(action -> action instanceof Action.LookAhead)
                    .mapToDouble(action -> ((Action.LookAhead) action).range())
                    .findFirst()
                    .orElse(32);
            var target = player.getTargetEntity((int) range);
            var living = target instanceof org.bukkit.entity.LivingEntity entity ? entity : null;
            return runtime.cast(player, living, spell, System.currentTimeMillis(), range);
        });
        registerCommand("scribe", new ScribeCommand(this, dialog));
        getServer().getPluginManager().registerEvents(new ScribeBookListener(this, dialog), this);
        getServer().getPluginManager().registerEvents(new ScribeChatListener(this, dialog), this);

        store = new GlyphDraftStoreAdapter(this);
        mapSaveAction = new GlyphMapSaveAction(store);
        getServer().getPluginManager().registerEvents(new GlyphMapRehydrationListener(store), this);
        classificationService = createClassificationService();
        var tomes = new GlyphTomeStore(this, store);
        getServer().getPluginManager().registerEvents(new GlyphTomeListener(tomes, store), this);
        registerCommand("glyph", new GlyphCommand(store, mapSaveAction, classificationService, tomes, runtime));
    }

    private GlyphClassificationService createClassificationService() {
        try {
            var bundle = ModelBundle.load(getDataFolder().toPath().resolve("model"));
            var classifier = new OnnxGlyphClassifier(bundle);
            return new GlyphClassificationService(
                    classifier, task -> getServer().getScheduler().runTask(this, task));
        } catch (Exception unavailable) {
            getLogger().warning("Glyph classifier unavailable: " + unavailable.getMessage());
            return new GlyphClassificationService(
                    draft -> Classification.rejected(List.of()),
                    task -> getServer().getScheduler().runTask(this, task));
        }
    }

    @Override
    public void onDisable() {
        if (classificationService != null) classificationService.close();
        classificationService = null;
    }

    public ScribeBookStore books() { return books; }
    public SpellRuntime runtime() { return runtime; }
    public ScribeDialog dialog() { return dialog; }
}

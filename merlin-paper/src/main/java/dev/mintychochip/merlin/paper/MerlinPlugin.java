package dev.mintychochip.merlin.paper;

import dev.mintychochip.merlin.api.dsl.Action;
import dev.mintychochip.merlin.api.dsl.CompileResult;
import dev.mintychochip.merlin.api.ml.ModelBundle;
import dev.mintychochip.merlin.common.ml.OnnxGlyphClassifier;
import dev.mintychochip.merlin.paper.book.ScribeBookStore;
import dev.mintychochip.merlin.paper.command.ScribeCommand;
import dev.mintychochip.merlin.paper.dialog.ScribeDialog;
import dev.mintychochip.merlin.paper.listener.ScribeBookListener;
import dev.mintychochip.merlin.paper.listener.ScribeChatListener;
import dev.mintychochip.merlin.paper.mapgui.GlyphClassificationService;
import dev.mintychochip.merlin.paper.mapgui.GlyphCommand;
import dev.mintychochip.merlin.paper.mapgui.GlyphDraftStoreAdapter;
import dev.mintychochip.merlin.paper.mapgui.GlyphMapRehydrationListener;
import dev.mintychochip.merlin.paper.ink.GrindListener;
import dev.mintychochip.merlin.paper.ink.InkStore;
import dev.mintychochip.merlin.paper.ink.MortarPestle;
import dev.mintychochip.merlin.paper.ritual.RitualAnchor;
import dev.mintychochip.merlin.paper.ritual.RitualBlockListener;
import dev.mintychochip.merlin.paper.ritual.RitualListener;
import dev.mintychochip.merlin.paper.ritual.RitualPedestal;
import dev.mintychochip.merlin.paper.ritual.RitualProducts;
import dev.mintychochip.merlin.paper.ritual.RitualRecipeTable;
import dev.mintychochip.merlin.paper.mapgui.GlyphMapSaveAction;
import dev.mintychochip.merlin.paper.mapgui.GlyphStrokeTracker;
import dev.mintychochip.merlin.paper.model.ModelBundleFetcher;
import dev.mintychochip.merlin.paper.runtime.SpellRuntime;
import dev.mintychochip.merlin.paper.tome.GlyphTomeListener;
import dev.mintychochip.merlin.paper.tome.GlyphTomeStore;
import java.io.IOException;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

public final class MerlinPlugin extends JavaPlugin {
  private ScribeBookStore books;
  private SpellRuntime runtime;
  private ScribeDialog dialog;
  private GlyphDraftStoreAdapter store;
  private GlyphMapSaveAction mapSaveAction;
  private GlyphClassificationService classificationService;

  @Override
  public void onEnable() {
    preloadBundledClasses();
    Path modelDirectory = ensureModel();
    books = new ScribeBookStore(this);
    runtime =
        new SpellRuntime(
            (delay, task) -> getServer().getScheduler().runTaskLater(this, task, delay));
    dialog =
        new ScribeDialog(
            books,
            (playerId, compilation) -> {
              var player = getServer().getPlayer(playerId);
              if (player == null) return false;
              if (!(compilation instanceof CompileResult.Ok ok)) return false;
              var spell = ok.spell();
              var range =
                  spell.actions().stream()
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
    getLogger().info("Glyph stroke tracker loaded via " + GlyphStrokeTracker.class.getClassLoader());
    getServer().getPluginManager().registerEvents(new GlyphMapRehydrationListener(store), this);
    classificationService = createClassificationService(modelDirectory);
    var tomes = new GlyphTomeStore(this, store);
    var inks = new InkStore(this);
    var mortar = new MortarPestle(this);
    mortar.registerRecipe();
    getServer().getPluginManager().registerEvents(new GrindListener(inks, mortar), this);

    var ritualAnchor = new RitualAnchor(this);
    var ritualPedestal = new RitualPedestal(this);
    ritualAnchor.registerRecipe();
    ritualPedestal.registerRecipe();
    getServer().getPluginManager().registerEvents(new RitualBlockListener(ritualAnchor, ritualPedestal), this);
    var recipes = new RitualRecipeTable();
    var products = new RitualProducts(this);
    getServer().getPluginManager().registerEvents(
            new RitualListener(recipes, products, ritualAnchor, ritualPedestal, inks, mortar, store), this);

    getServer().getPluginManager().registerEvents(new GlyphTomeListener(tomes, store), this);
    registerCommand(
            "glyph", new GlyphCommand(store, mapSaveAction, classificationService, tomes, runtime, inks));
  }

  private void preloadBundledClasses() {
    try {
      int loaded = PluginClassPreloader.loadAll(getClass().getClassLoader(), getFile());
      getLogger().info("Preloaded " + loaded + " plugin classes via " + getClass().getClassLoader());
    } catch (IOException error) {
      throw new IllegalStateException("Merlin cannot enable: failed to preload plugin classes", error);
    }
  }

  private Path ensureModel() {
    saveDefaultConfig();
    String repository =
        getConfig()
            .getString("model.repository", "https://github.com/aincraft-org/merlin-weights");
    String version = getConfig().getString("model.version", "2026.08.18.0");
    boolean allowUnreleased = getConfig().getBoolean("model.allow-unreleased", false);
    try {
      return new ModelBundleFetcher(
              repository, version, getDataFolder().toPath().resolve("models"), allowUnreleased)
          .ensureBundle();
    } catch (IOException error) {
      throw new IllegalStateException(
          "Merlin cannot enable: model "
              + version
              + " is unavailable or not release-ready: "
              + error.getMessage(),
          error);
    }
  }

  private GlyphClassificationService createClassificationService(Path modelDirectory) {
    try {
      var bundle =
          ModelBundle.load(modelDirectory, getConfig().getBoolean("model.allow-unreleased", false));
      var classifier = new OnnxGlyphClassifier(bundle);
      return new GlyphClassificationService(
          classifier, task -> getServer().getScheduler().runTask(this, task));
    } catch (Exception unavailable) {
      throw new IllegalStateException(
          "Merlin model validation failed: " + unavailable.getMessage(), unavailable);
    }
  }

  @Override
  public void onDisable() {
    if (classificationService != null) classificationService.close();
    classificationService = null;
  }

  public ScribeBookStore books() {
    return books;
  }

  public SpellRuntime runtime() {
    return runtime;
  }

  public ScribeDialog dialog() {
    return dialog;
  }
}

package dev.jlo.wizardry.mapgui;

import de.flog99.mapgui.Click;
import de.flog99.mapgui.Screen;
import de.flog99.mapgui.ui.Node;
import de.flog99.mapgui.ui.PaintContext;
import dev.jlo.wizardry.glyph.GlyphDraft;
import dev.jlo.wizardry.glyph.GlyphRasterizer;
import dev.jlo.wizardry.ml.Classification;
import java.awt.Color;
import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

import static de.flog99.mapgui.ui.Ui.Draw;

public final class GlyphScreen extends Screen {
    private final GlyphStrokeTracker tracker;
    private final Runnable saveAction;
    private final Runnable closeAction;
    private final Consumer<GlyphDraft> classifyAction;
    private boolean menu;
    private Classification latestClassification;

    public GlyphScreen(GlyphStrokeTracker tracker, Runnable saveAction, Runnable closeAction) {
        this(tracker, saveAction, closeAction, ignored -> {});
    }

    public GlyphScreen(GlyphStrokeTracker tracker, Runnable saveAction, Runnable closeAction, Consumer<GlyphDraft> classifyAction) {
        this.tracker = Objects.requireNonNull(tracker);
        this.saveAction = Objects.requireNonNull(saveAction);
        this.closeAction = Objects.requireNonNull(closeAction);
        this.classifyAction = Objects.requireNonNull(classifyAction);
    }
    @Override public Component title() { return Component.text("Glyphcraft"); }
    @Override public Color background() { return new Color(20, 22, 28); }
    @Override public Click activateOn() { return Click.BOTH; }
    @Override protected Node build() { return Draw(this::paintCanvas).onClick(this::stroke).fill(); }
    private void paintCanvas(PaintContext context) {
        var bitmap = GlyphRasterizer.renderFull(tracker.snapshot());
        var pixels = bitmap.pixels();
        var bounds = context.bounds();
        for (int y = 0; y < Math.min(128, bounds.height()); y++) for (int x = 0; x < Math.min(128, bounds.width()); x++) {
            if ((pixels[y * 128 + x] & 255) != 0) context.painter().pixel(bounds.x() + x, bounds.y() + y, Color.WHITE);
        }
    }
    private void stroke(int x, int y) {
        if (clickedWith() == Click.LEFT) { menu = !menu; tracker.pause(Long.MAX_VALUE); invalidate(); return; }
        tracker.acceptClick(x, y, System.currentTimeMillis());
        invalidate();
    }
    public void save() { saveAction.run(); }
    public void closeScreen() { closeAction.run(); close(); }
    public void clear() { tracker.clear(); invalidate(); }
    public void undo() { tracker.undo(); invalidate(); }
    public GlyphDraft draft() { return tracker.snapshot(); }
    public void classify() { classifyAction.accept(tracker.snapshot()); }
    public void setClassification(Classification classification) { latestClassification = classification; invalidate(); }
    public Classification latestClassification() { return latestClassification; }
}

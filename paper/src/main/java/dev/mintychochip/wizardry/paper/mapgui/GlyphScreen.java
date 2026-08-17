package dev.mintychochip.wizardry.paper.mapgui;

import de.flog99.mapgui.Click;
import de.flog99.mapgui.Screen;
import de.flog99.mapgui.ui.Align;
import de.flog99.mapgui.ui.Colors;
import de.flog99.mapgui.ui.Node;
import de.flog99.mapgui.ui.PaintContext;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.common.glyph.GlyphRasterizer;
import dev.mintychochip.wizardry.api.ml.Classification;
import java.awt.Color;
import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

import static de.flog99.mapgui.ui.Ui.Button;
import static de.flog99.mapgui.ui.Ui.Column;
import static de.flog99.mapgui.ui.Ui.Draw;
import static de.flog99.mapgui.ui.Ui.Overlay;
import static de.flog99.mapgui.ui.Ui.Row;
import static de.flog99.mapgui.ui.Ui.Spacer;

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
    @Override protected Node build() {
        Node canvas = Draw(this::paintCanvas).onClick(this::stroke).fill();
        if (!menu) return canvas;
        return Overlay(
                canvas,
                Column(
                        Spacer(),
                        Row(
                                Button("Save").background(new Color(38, 110, 78)).textColor(Color.WHITE)
                                        .hoverBackground(new Color(60, 150, 105)).onClick(this::saveFromMenu),
                                Button("Undo").background(new Color(48, 50, 62)).textColor(Color.WHITE)
                                        .onClick(this::undo),
                                Button("Clear").background(new Color(90, 44, 52)).textColor(Color.WHITE)
                                        .onClick(this::clear),
                                Button("Close").background(new Color(48, 50, 62)).textColor(Color.WHITE)
                                        .onClick(this::closeScreen)
                        ).gap(3).align(Align.CENTER)
                                .padding(3).background(Colors.alpha(Color.BLACK, 190)).radius(4)
                                .onClick(() -> {})
                ).align(Align.STRETCH).padding(4).fill().onClick(this::dismissMenu)
        ).fill();
    }
    private void paintCanvas(PaintContext context) {
        var bitmap = GlyphRasterizer.renderFull(tracker.snapshot());
        var pixels = bitmap.pixels();
        var bounds = context.bounds();
        for (int y = 0; y < Math.min(128, bounds.height()); y++) for (int x = 0; x < Math.min(128, bounds.width()); x++) {
            if ((pixels[y * 128 + x] & 255) != 0) context.painter().pixel(bounds.x() + x, bounds.y() + y, Color.WHITE);
        }
    }
    private void stroke(int x, int y) {
        if (clickedWith() == Click.LEFT) {
            toggleMenu();
            tracker.endStroke(System.currentTimeMillis());
            return;
        }
        tracker.acceptClick(x, y, System.currentTimeMillis());
        invalidate();
    }
    void toggleMenu() {
        menu = !menu;
        invalidate();
    }

    boolean menuOpen() {
        return menu;
    }

    void saveFromMenu() {
        save();
        menu = false;
        invalidate();
    }

    private void dismissMenu() {
        menu = false;
        invalidate();
    }

    public void beginStroke(int x, int y) {
        tracker.beginStroke(x, y, System.currentTimeMillis());
        invalidate();
    }
    public void endStroke() {
        tracker.endStroke(System.currentTimeMillis());
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

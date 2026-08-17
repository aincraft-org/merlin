package dev.mintychochip.wizardry.paper.mapgui;

import de.flog99.mapgui.Click;
import de.flog99.mapgui.Screen;
import de.flog99.mapgui.ui.Align;
import de.flog99.mapgui.ui.Colors;
import de.flog99.mapgui.ui.Justify;
import de.flog99.mapgui.ui.Node;
import de.flog99.mapgui.ui.PaintContext;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.common.glyph.GlyphRasterizer;
import dev.mintychochip.wizardry.api.ml.Classification;
import java.awt.Color;
import java.util.Objects;
import java.util.function.BooleanSupplier;
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
    private final BooleanSupplier sneaking;
    private boolean menu;
    private Classification latestClassification;
    private int pips = 1;

    public GlyphScreen(GlyphStrokeTracker tracker, Runnable saveAction, Runnable closeAction) {
        this(tracker, saveAction, closeAction, ignored -> {});
    }

    public GlyphScreen(GlyphStrokeTracker tracker, Runnable saveAction, Runnable closeAction, Consumer<GlyphDraft> classifyAction) {
        this(tracker, saveAction, closeAction, classifyAction, () -> false);
    }

    public GlyphScreen(
            GlyphStrokeTracker tracker,
            Runnable saveAction,
            Runnable closeAction,
            Consumer<GlyphDraft> classifyAction,
            BooleanSupplier sneaking) {
        this.tracker = Objects.requireNonNull(tracker);
        this.saveAction = Objects.requireNonNull(saveAction);
        this.closeAction = Objects.requireNonNull(closeAction);
        this.classifyAction = Objects.requireNonNull(classifyAction);
        this.sneaking = Objects.requireNonNull(sneaking);
    }
    @Override public Component title() { return Component.text("Glyphcraft"); }
    @Override public Color background() { return new Color(20, 22, 28); }
    @Override public Click activateOn() { return Click.BOTH; }
    @Override protected Node build() {
        Node canvas = Draw(this::paintCanvas).onClick(this::stroke).fill();
        Node chrome = menu
                ? Column(Spacer(), toolMenu(), pipStrip()).align(Align.STRETCH).padding(4).fill().onClick(this::dismissMenu)
                : Column(Spacer(), pipStrip()).align(Align.STRETCH).padding(4).fill();
        return Overlay(canvas, chrome).fill();
    }
    private Node toolMenu() {
        return Row(
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
                .onClick(() -> {});
    }
    private Node pipStrip() {
        return Row(
                Button("◀").background(new Color(48, 50, 62)).textColor(Color.WHITE)
                        .onClick(this::nudgePipsLeft),
                pipDisplay(0),
                pipDisplay(1),
                pipDisplay(2),
                pipDisplay(3),
                pipDisplay(4),
                Button("▶").background(new Color(48, 50, 62)).textColor(Color.WHITE)
                        .onClick(this::nudgePipsRight)
        ).gap(3).align(Align.CENTER).justify(Justify.CENTER)
                .padding(3).background(Colors.alpha(Color.BLACK, 190)).radius(4)
                .onClick(() -> {});
    }
    private Node pipDisplay(int index) {
        return Draw(context -> {
            var bounds = context.bounds();
            int radius = Math.max(1, Math.min(bounds.width(), bounds.height()) / 2 - 1);
            boolean filled = index < pips;
            context.painter().circle(
                    bounds.x() + bounds.width() / 2,
                    bounds.y() + bounds.height() / 2,
                    radius,
                    filled ? Color.WHITE : null,
                    filled ? Color.WHITE : new Color(120, 124, 136));
        }).size(7, 7);
    }
    private void nudgePipsLeft() {
        if (sneaking.getAsBoolean()) jumpPips(-1);
        else stepPips(-1);
    }
    private void nudgePipsRight() {
        if (sneaking.getAsBoolean()) jumpPips(1);
        else stepPips(1);
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
    public int pips() { return pips; }
    public void stepPips(int delta) {
        pips = Math.max(1, Math.min(5, pips + delta));
        invalidate();
    }
    public void jumpPips(int direction) {
        pips = direction < 0 ? 1 : 5;
        invalidate();
    }
    public GlyphDraft draft() { return tracker.snapshot(); }
    public void classify() { classifyAction.accept(tracker.snapshot()); }
    public void setClassification(Classification classification) { latestClassification = classification; invalidate(); }
    public Classification latestClassification() { return latestClassification; }
}

package dev.mintychochip.merlin.paper.mapgui;

import de.flog99.mapgui.Click;
import de.flog99.mapgui.Screen;
import de.flog99.mapgui.ui.Align;
import de.flog99.mapgui.ui.AwtFont;
import de.flog99.mapgui.ui.Colors;
import de.flog99.mapgui.ui.Justify;
import de.flog99.mapgui.ui.Node;
import de.flog99.mapgui.ui.PaintContext;
import de.flog99.mapgui.ui.Rect;
import de.flog99.mapgui.ui.TextFont;
import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.MagicalInk;
import dev.mintychochip.merlin.common.glyph.GlyphRasterizer;
import dev.mintychochip.merlin.api.ml.Classification;
import dev.mintychochip.merlin.api.ml.Label;
import dev.mintychochip.merlin.paper.ink.InkStore;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
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
    enum HoverAnchor { RANK, INK }

    private static final TextFont HOVER_FONT = AwtFont.named("SansSerif", java.awt.Font.PLAIN, 9, false);

    private final GlyphStrokeTracker tracker;
    private final Runnable saveAction;
    private final Runnable closeAction;
    private final Consumer<GlyphDraft> classifyAction;
    private final BooleanSupplier sneaking;
    private boolean menu;
    private boolean suppressHold;
    private Classification latestClassification;
    private Label pendingLabel;
    private int pips = 1;
    private List<MagicalInk> inks = List.of();
    private List<GlyphElement> availableInks = List.of();
    private GlyphElement selectedInk;
    private String hoverCaption;
    private HoverAnchor hoverAnchor;

    public GlyphScreen(GlyphStrokeTracker tracker, Runnable saveAction, Runnable closeAction) {
        this(tracker, saveAction, closeAction, ignored -> {}, () -> false);
    }

    @Override public Component title() { return Component.text("Glyphcraft"); }
    @Override public Color background() { return new Color(20, 22, 28); }
    @Override public Click activateOn() { return Click.BOTH; }
    @Override public boolean holdable() { return true; }
    @Override public int loopFps() { return 20; }
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
    @Override protected Node build() {
        Node canvas = Draw(this::paintCanvas)
                .onClick(this::pressCanvas)
                .fill();
        Node inkRail = Row(Spacer(), Column(Spacer(), inkStrip(), Spacer()))
                .align(Align.STRETCH)
                .padding(4)
                .fill();
        Node chrome = menu
                ? Column(Spacer(), toolMenu(), pipStrip()).align(Align.CENTER).padding(4).fill().onClick(this::dismissMenu)
                : Column(Spacer(), pipStrip()).align(Align.CENTER).padding(4).fill();
        Node hover = Draw(this::paintHover).fill();
        return Overlay(canvas, inkRail, chrome, hover).fill();
    }
    private Node toolMenu() {
        return Row(
                Button("Save").background(new Color(38, 110, 78)).textColor(Color.WHITE)
                        .hoverBackground(new Color(60, 150, 105)).onClick(this::saveFromMenu),
                Button("Classify").background(new Color(48, 50, 62)).textColor(Color.WHITE)
                        .onClick(this::classify),
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
    private Node inkStrip() {
        if (availableInks.isEmpty()) return Spacer().size(0, 0);
        var chips = new ArrayList<Node>();
        for (var element : availableInks) chips.add(inkChip(element));
        return Column(chips.toArray(Node[]::new)).gap(3).align(Align.CENTER)
                .padding(3).background(Colors.alpha(Color.BLACK, 190)).radius(4);
    }
    private Node inkChip(GlyphElement element) {
        boolean selected = element == selectedInk;
        return Draw(context -> {
            if (element == GlyphElement.FLAME) {
                InkFlame.draw(context.painter(), context.bounds(), phase(1000));
                if (selected) {
                    var b = context.bounds();
                    var p = context.painter();
                    p.line(b.x(), b.y(), b.right() - 1, b.y(), Color.WHITE, 1);
                    p.line(b.right() - 1, b.y(), b.right() - 1, b.bottom() - 1, Color.WHITE, 1);
                    p.line(b.right() - 1, b.bottom() - 1, b.x(), b.bottom() - 1, Color.WHITE, 1);
                    p.line(b.x(), b.bottom() - 1, b.x(), b.y(), Color.WHITE, 1);
                }
            } else {
                Color fill = inkColor(element);
                context.painter().rect(context.bounds(), fill, selected ? 1 : 0, selected ? Color.WHITE : fill, 2);
            }
        }).size(InkFlame.WIDTH, InkFlame.HEIGHT)
                .onHover(() -> showHover(inkCaption(element), HoverAnchor.INK), this::clearHover)
                .onClick(() -> selectInk(element));
    }
    private static Color inkColor(GlyphElement element) {
        return new Color(element.r(), element.g(), element.b());
    }
    private Node pipStrip() {
        int gap = 2;
        int width = 5 * PipFlame.WIDTH + 4 * gap;
        Node flames = Draw(this::paintPipFlames)
                .size(width, PipFlame.HEIGHT)
                .place(Justify.CENTER, Align.CENTER);
        Node hits = Row(
                pipHit(0),
                pipHit(1),
                pipHit(2),
                pipHit(3),
                pipHit(4)
        ).gap(gap).align(Align.CENTER).justify(Justify.CENTER)
                .place(Justify.CENTER, Align.CENTER);
        return Overlay(flames, hits)
                .padding(3).background(Colors.alpha(Color.BLACK, 190)).radius(4)
                .onClick(this::ignoreHold);
    }
    private Node pipHit(int index) {
        return Draw(ignored -> {}).size(PipFlame.WIDTH, PipFlame.HEIGHT)
                .onHover(() -> showHover("Rank " + (index + 1), HoverAnchor.RANK), this::clearHover)
                .onClick(() -> clickPip(index));
    }
    private void paintPipFlames(PaintContext context) {
        int[] frame = PipFlame.pixels(true, phase(1000));
        var bounds = context.bounds();
        int gap = 2;
        int total = 5 * PipFlame.WIDTH + 4 * gap;
        int x = bounds.x() + Math.max(0, (bounds.width() - total) / 2);
        int y = bounds.y() + Math.max(0, (bounds.height() - PipFlame.HEIGHT) / 2);
        for (int i = 0; i < 5; i++) {
            PipFlame.draw(
                    context.painter(),
                    new Rect(x, y, PipFlame.WIDTH, PipFlame.HEIGHT),
                    i < pips,
                    frame);
            x += PipFlame.WIDTH + gap;
        }
    }
    private void paintHover(PaintContext context) {
        String text = hoverCaption;
        if (text == null || text.isBlank() || hoverAnchor == null) return;
        var painter = context.painter();
        painter.font(HOVER_FONT);
        int pad = 2;
        int width = painter.font().widthOf(text) + pad * 2;
        int height = painter.font().lineHeight() + pad * 2;
        var box = hoverBox(hoverAnchor, context.bounds(), width, height);
        painter.rect(box, Colors.alpha(Color.BLACK, 210), 0, Color.BLACK, 1);
        painter.textLine(box.x() + pad, box.y() + pad, text, Color.WHITE, false);
    }

    static Rect hoverBox(HoverAnchor anchor, Rect area, int width, int height) {
        if (anchor == HoverAnchor.INK) {
            int chip = 4 + 3 + 9 + 3;
            int x = Math.max(area.x(), area.right() - chip - 2 - width);
            int y = area.y() + Math.max(0, (area.height() - height) / 2);
            return new Rect(x, y, width, height);
        }
        int strip = 4 + 3 + PipFlame.HEIGHT + 3;
        int x = area.x() + Math.max(0, (area.width() - width) / 2);
        int y = Math.max(area.y(), area.bottom() - strip - 2 - height);
        return new Rect(x, y, width, height);
    }

    private void paintCanvas(PaintContext context) {
        var rgb = GlyphRasterizer.renderEmissiveRgb(tracker.snapshot(), phase(1000));
        var bounds = context.bounds();
        for (int y = 0; y < Math.min(128, bounds.height()); y++) for (int x = 0; x < Math.min(128, bounds.width()); x++) {
            int o = (y * 128 + x) * 3;
            if (rgb[o] > 0 || rgb[o + 1] > 0 || rgb[o + 2] > 0) {
                context.painter().pixel(bounds.x() + x, bounds.y() + y, new Color(rgb[o], rgb[o + 1], rgb[o + 2]));
            }
        }
    }
    private void pressCanvas() {
        if (clickedWith() != Click.LEFT) return;
        toggleMenu();
        tracker.endStroke(System.currentTimeMillis());
    }

    @Override
    protected void onHold(int x, int y) {
        if (menu || suppressHold || x < 0 || y < 0) return;
        tracker.appendPoint(Math.min(127, x), Math.min(127, y), System.currentTimeMillis());
        invalidate();
    }

    @Override
    protected void onHoldEnd() {
        suppressHold = false;
        tracker.endStroke(System.currentTimeMillis());
    }

    private void ignoreHold() {
        suppressHold = true;
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
    public void closeScreen() { close(); }

    @Override
    protected void onClose() {
        tracker.endStroke(System.currentTimeMillis());
        save();
        closeAction.run();
    }
    public void clear() { tracker.clear(); invalidate(); }
    public void undo() { tracker.undo(); invalidate(); }
    public int pips() { return pips; }
    public void setPips(int pips) {
        this.pips = Math.max(1, Math.min(5, pips));
        invalidate();
    }
    void clickPip(int index) {
        ignoreHold();
        int target = index + 1;
        setPips(pips == target ? target - 1 : target);
    }
    public void stepPips(int delta) {
        setPips(pips + delta);
    }
    public void jumpPips(int direction) {
        pips = direction < 0 ? 1 : 5;
        invalidate();
    }
    public GlyphDraft draft() { return tracker.snapshot(); }
    public void classify() { classifyAction.accept(tracker.snapshot()); }
    public void setClassification(Classification classification) { latestClassification = classification; invalidate(); }
    public Classification latestClassification() { return latestClassification; }
    public void setPendingLabel(Label label) {
        pendingLabel = label;
        invalidate();
    }
    public void clearPendingLabel() {
        setPendingLabel(null);
    }
    public Label pendingLabel() { return pendingLabel; }

    public void setAvailableInks(List<GlyphElement> available) {
        var fills = new ArrayList<MagicalInk>();
        if (available != null) {
            for (var element : available) {
                if (element != null) fills.add(MagicalInk.full(element));
            }
        }
        setInks(fills);
    }

    public void setInks(List<MagicalInk> fills) {
        var kept = new ArrayList<MagicalInk>();
        if (fills != null) {
            for (var ink : fills) {
                if (ink != null && !ink.empty()) kept.add(ink);
            }
        }
        inks = List.copyOf(kept);
        availableInks = MagicalInk.filledElements(inks);
        selectedInk = MagicalInk.afterSpend(selectedInk, availableInks).orElse(null);
        invalidate();
    }

    public List<GlyphElement> availableInks() {
        return availableInks;
    }

    public String inkCaption(GlyphElement element) {
        var ink = inkOf(element);
        if (ink == null) return null;
        return InkStore.displayName(element) + " " + ink.remaining() + "/" + ink.max();
    }

    public String selectedInkCaption() {
        return selectedInk == null ? null : inkCaption(selectedInk);
    }

    public String hoverCaption() {
        return hoverCaption;
    }

    HoverAnchor hoverAnchor() {
        return hoverAnchor;
    }

    void showHover(String text, HoverAnchor anchor) {
        if (Objects.equals(hoverCaption, text) && hoverAnchor == anchor) return;
        hoverCaption = text;
        hoverAnchor = text == null ? null : anchor;
        invalidate();
    }

    void clearHover() {
        showHover(null, null);
    }

    private MagicalInk inkOf(GlyphElement element) {
        if (element == null) return null;
        for (var ink : inks) {
            if (ink.element() == element) return ink;
        }
        return null;
    }

    public void selectInk(GlyphElement element) {
        if (element == null || !availableInks.contains(element)) return;
        selectedInk = element;
        invalidate();
    }

    public GlyphElement selectedInk() {
        return selectedInk;
    }
}

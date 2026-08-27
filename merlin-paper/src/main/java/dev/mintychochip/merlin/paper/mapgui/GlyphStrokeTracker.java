package dev.mintychochip.merlin.paper.mapgui;

import dev.mintychochip.merlin.api.glyph.GlyphCaptureSession;
import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphLimits;
import dev.mintychochip.merlin.api.glyph.GlyphPoint;
import dev.mintychochip.merlin.api.glyph.MagicalInk;
import java.util.Objects;
import java.util.Optional;

public final class GlyphStrokeTracker {
    public static final double MIN_BRUSH_WIDTH = 1;
    public static final double MAX_BRUSH_WIDTH = 6;
    public static final long STROKE_PAUSE_MILLIS = 600;
    private static final double SMOOTHING_TIME_MILLIS = 12.0;
    private static final InkSupply NONE = new InkSupply() {
        @Override public Optional<MagicalInk> read() { return Optional.empty(); }
        @Override public boolean write(MagicalInk spent) { return false; }
    };
    private final GlyphCaptureSession capture = new GlyphCaptureSession();
    private final InkSupply ink;
    private GlyphElement paidElement = GlyphElement.PHYSICAL;
    private boolean active;
    private int lastX;
    private int lastY;
    private long lastAt;
    private boolean hasSmoothedWidth;
    private double smoothedWidth;
    private int sampleCount;
    private int originX;
    private int originY;
    private double originIntervalWidth;
    private double committedX;
    private double committedY;
    private int tailPoints;

    public interface InkSupply {
        Optional<MagicalInk> read();
        boolean write(MagicalInk spent);
    }

    public GlyphStrokeTracker() {
        this(NONE);
    }

    public GlyphStrokeTracker(InkSupply ink) {
        this(GlyphDraft.empty(), ink);
    }

    public GlyphStrokeTracker(GlyphDraft draft) {
        this(draft, NONE);
    }

    public GlyphStrokeTracker(GlyphDraft draft, InkSupply ink) {
        this.ink = Objects.requireNonNull(ink, "ink");
        for (var stroke : draft.strokes()) {
            capture.brushWidth(stroke.brushWidth());
            capture.beginStroke(stroke.startedAtMillis(), stroke.element());
            capture.appendPoint(stroke.points().getFirst());
            for (int i = 1; i < stroke.points().size(); i++) {
                capture.appendPoint(stroke.points().get(i));
                capture.segmentWidth(stroke.segmentWidths().get(i - 1));
            }
            capture.endStroke();
        }
    }

    public static InkSupply boxSupply(MagicalInk[] box) {
        Objects.requireNonNull(box, "box");
        return new InkSupply() {
            @Override public Optional<MagicalInk> read() {
                return Optional.ofNullable(box[0]);
            }
            @Override public boolean write(MagicalInk spent) {
                box[0] = spent;
                return true;
            }
        };
    }

    public static GlyphStrokeTracker withSupply(MagicalInk[] box) {
        return new GlyphStrokeTracker(boxSupply(box));
    }

    public static GlyphStrokeTracker unconstrained() {
        return withSupply(new MagicalInk[] { new MagicalInk(GlyphElement.PHYSICAL, 10_000, 10_000) });
    }

    public synchronized void beginStroke(int x, int y, long nowMillis) {
        checkPoint(x, y);
        finishActive();
        if (!chargeNewStroke()) return;
        resetSmoothing();
        capture.brushWidth(MAX_BRUSH_WIDTH);
        capture.beginStroke(nowMillis, paidElement);
        capture.appendPoint(new GlyphPoint(x, y));
        active = true;
        lastX = x;
        lastY = y;
        lastAt = nowMillis;
        resetPathState(x, y);
    }

    public synchronized void appendPoint(int x, int y, long nowMillis) {
        checkPoint(x, y);
        if (!active) {
            beginStroke(x, y, nowMillis);
            return;
        }
        int steps = Math.max(Math.abs(x - lastX), Math.abs(y - lastY));
        if (steps == 0) {
            lastAt = nowMillis;
            return;
        }
        long elapsedMillis = elapsedMillis(lastAt, nowMillis);
        double velocity = steps / Math.max(1.0, (double) elapsedMillis);
        double targetWidth = widthForVelocity(velocity);
        double width = smoothWidth(targetWidth, elapsedMillis);
        if (steps <= 1) {
            emitLine(lastX, lastY, x, y, width, steps, nowMillis, targetWidth, elapsedMillis, false);
            if (sampleCount == 1) originIntervalWidth = width;
        } else if (sampleCount == 1) {
            originIntervalWidth = width;
            emitLine(lastX, lastY, x, y, width, steps, nowMillis, targetWidth, elapsedMillis, true);
        } else {
            boolean reshapeOrigin = sampleCount == 2 && tailPoints > 0;
            popTail();
            if (reshapeOrigin) {
                double midX = (originX + lastX) / 2.0;
                double midY = (originY + lastY) / 2.0;
                int firstSteps = densifySteps(originX, originY, midX, midY);
                emitLine(originX, originY, midX, midY, originIntervalWidth, firstSteps, nowMillis, targetWidth, elapsedMillis, false);
            }
            double midX = (lastX + x) / 2.0;
            double midY = (lastY + y) / 2.0;
            emitQuadraticAndTail(committedX, committedY, lastX, lastY, midX, midY, x, y, width, steps, nowMillis, targetWidth, elapsedMillis);
        }
        lastX = x;
        lastY = y;
        lastAt = nowMillis;
        sampleCount++;
    }

    public synchronized void acceptClick(int x, int y, long nowMillis) {
        if (active && elapsedMillis(lastAt, nowMillis) >= STROKE_PAUSE_MILLIS) endStroke(nowMillis);
        appendPoint(x, y, nowMillis);
    }

    public synchronized void endStroke(long nowMillis) {
        finishActive();
    }

    public synchronized void pause(long nowMillis) { endStroke(nowMillis); }
    public synchronized void undo() { finishActive(); capture.undo(); }
    public synchronized void clear() { active = false; resetSmoothing(); sampleCount = 0; tailPoints = 0; capture.clear(); }
    public synchronized GlyphDraft snapshot() { return capture.snapshot(); }
    static double widthForVelocity(double velocity) {
        double normalized = Math.min(1.0, Math.max(0.0, velocity / 0.5));
        return MAX_BRUSH_WIDTH - normalized * (MAX_BRUSH_WIDTH - MIN_BRUSH_WIDTH);
    }
    public synchronized GlyphDraft close() { finishActive(); return capture.close(); }
    private boolean chargeNewStroke() {
        var held = ink.read();
        if (held.isEmpty()) return false;
        var spent = held.get().spend(1);
        if (spent.isEmpty()) return false;
        if (!ink.write(spent.get())) return false;
        paidElement = held.get().element();
        return true;
    }
    private void finishActive() {
        if (active) {
            capture.endStroke();
            active = false;
        }
        resetSmoothing();
        sampleCount = 0;
        tailPoints = 0;
    }

    private void emitLine(
            double x0, double y0, double x1, double y1, double width, int steps,
            long nowMillis, double targetWidth, long elapsedMillis, boolean asTail) {
        if (steps < 1) steps = 1;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            emitPoint(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, width, nowMillis, targetWidth, elapsedMillis, asTail);
        }
    }

    private void emitQuadraticAndTail(
            double x0, double y0, double cx, double cy, double midX, double midY,
            double endX, double endY, double width, int budget,
            long nowMillis, double targetWidth, long elapsedMillis) {
        double quadLen = approxQuadraticLength(x0, y0, cx, cy, midX, midY);
        double tailLen = Math.hypot(endX - midX, endY - midY);
        double total = Math.max(1e-9, quadLen + tailLen);
        double split = quadLen / total;
        if (budget < 1) budget = 1;
        for (int i = 1; i <= budget; i++) {
            double u = (double) i / budget;
            double px;
            double py;
            boolean tail = u > split;
            if (!tail && split > 0) {
                double t = Math.min(1.0, u / split);
                double omt = 1.0 - t;
                px = omt * omt * x0 + 2 * omt * t * cx + t * t * midX;
                py = omt * omt * y0 + 2 * omt * t * cy + t * t * midY;
            } else {
                double t = split >= 1 ? 1.0 : (u - split) / (1.0 - split);
                px = midX + (endX - midX) * t;
                py = midY + (endY - midY) * t;
            }
            emitPoint(px, py, width, nowMillis, targetWidth, elapsedMillis, tail);
        }
    }

    private void emitPoint(
            double x, double y, double width, long nowMillis, double targetWidth, long elapsedMillis, boolean tail) {
        x = clampCanvas(x);
        y = clampCanvas(y);
        if (capture.currentPointCount() >= GlyphLimits.MAX_POINTS_PER_STROKE) {
            if (capture.completedCount() >= GlyphLimits.MAX_STROKES - 1) return;
            capture.endStroke();
            if (capture.completedCount() >= GlyphLimits.MAX_STROKES) return;
            capture.beginStroke(nowMillis, paidElement);
            capture.appendPoint(new GlyphPoint(lastX, lastY));
            resetSmoothing();
            width = smoothWidth(targetWidth, elapsedMillis);
            resetPathState(lastX, lastY);
            originIntervalWidth = width;
        }
        capture.appendPoint(new GlyphPoint(x, y));
        capture.segmentWidth(width);
        if (tail) {
            tailPoints++;
        } else {
            committedX = x;
            committedY = y;
            tailPoints = 0;
        }
    }

    private void popTail() {
        int n = Math.min(tailPoints, Math.max(0, currentPointCount() - 1));
        for (int i = 0; i < n; i++) capture.popLastPoint();
        tailPoints = 0;
    }

    private int currentPointCount() {
        return capture.currentPointCount();
    }

    private void resetPathState(int x, int y) {
        sampleCount = 1;
        originX = x;
        originY = y;
        committedX = x;
        committedY = y;
        tailPoints = 0;
    }

    private static int densifySteps(double x0, double y0, double x1, double y1) {
        return Math.max(1, (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
    }

    private static double approxQuadraticLength(
            double x0, double y0, double cx, double cy, double x1, double y1) {
        double chord = Math.hypot(x1 - x0, y1 - y0);
        double net = Math.hypot(cx - x0, cy - y0) + Math.hypot(x1 - cx, y1 - cy);
        return (chord + net) / 2.0;
    }

    private static double clampCanvas(double value) {
        if (value <= 0) return 0;
        if (value >= GlyphLimits.CANVAS_WIDTH) return Math.nextDown((double) GlyphLimits.CANVAS_WIDTH);
        return value;
    }

    private double smoothWidth(double targetWidth, long elapsedMillis) {
        if (!hasSmoothedWidth) {
            smoothedWidth = targetWidth;
            hasSmoothedWidth = true;
            return smoothedWidth;
        }
        double elapsed = Math.min(1000.0, (double) elapsedMillis);
        double factor = 1.0 - Math.exp(-elapsed / SMOOTHING_TIME_MILLIS);
        factor = Math.max(0.0, Math.min(1.0, factor));
        smoothedWidth += (targetWidth - smoothedWidth) * factor;
        smoothedWidth = Math.max(MIN_BRUSH_WIDTH, Math.min(MAX_BRUSH_WIDTH, smoothedWidth));
        return smoothedWidth;
    }

    private void resetSmoothing() {
        hasSmoothedWidth = false;
        smoothedWidth = 0.0;
    }

    private static long elapsedMillis(long earlier, long later) {
        if (later <= earlier) return 0;
        long elapsed = later - earlier;
        return elapsed < 0 ? Long.MAX_VALUE : elapsed;
    }
    private static void checkPoint(int x, int y) {
        if (x < 0 || x >= 128 || y < 0 || y >= 128) throw new IllegalArgumentException("cursor outside glyph canvas");
    }
}

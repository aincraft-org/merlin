package dev.mintychochip.merlin.paper.mapgui;

import de.flog99.mapgui.ui.Painter;
import de.flog99.mapgui.ui.Rect;
import dev.mintychochip.merlin.common.glyph.FlameOrb;
import java.awt.Color;

/** One Zelda-style orb flame. Lit pips reuse the current frame; unlit pips hide it.
 *  The 5-tier shade ramp is the single source of truth in {@link FlameOrb}. */
final class PipFlame {
    static final int WIDTH = 11;
    static final int HEIGHT = 11;
    static final int FRAME_COUNT = 20;

    private static final int CLEAR = 0;
    private static final int CORE = pack(FlameOrb.CORE);
    private static final int YELLOW = pack(FlameOrb.YELLOW);
    private static final int ORANGE = pack(FlameOrb.ORANGE);
    private static final int RED = pack(FlameOrb.RED);
    private static final int RIM = pack(FlameOrb.RIM);
    private static final int UNLIT_CORE = rgb(118, 22, 14);
    private static final int UNLIT_RIM = rgb(72, 14, 12);

    private static final int[] UNLIT = renderUnlit();
    private static final int[][] FRAMES = renderFrames();

    private PipFlame() {}

    static int frameCount() {
        return FRAME_COUNT;
    }

    static int[] pixels(boolean lit, double phase) {
        if (!lit) return UNLIT;
        double t = wrap(phase);
        int frame = Math.min(FRAMES.length - 1, (int) Math.floor(t * FRAMES.length));
        return FRAMES[frame];
    }

    static int litPixelCount(int[] pixels) {
        int count = 0;
        for (int pixel : pixels) if (pixel != CLEAR) count++;
        return count;
    }

    static void draw(Painter painter, Rect bounds, boolean lit, double phase) {
        draw(painter, bounds, lit, pixels(true, phase));
    }

    static void draw(Painter painter, Rect bounds, boolean lit, int[] litFrame) {
        int[] pixels = lit ? litFrame : UNLIT;
        int ox = bounds.x() + Math.max(0, (bounds.width() - WIDTH) / 2);
        int oy = bounds.y() + Math.max(0, (bounds.height() - HEIGHT) / 2);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int rgb = pixels[y * WIDTH + x];
                if (rgb == CLEAR) continue;
                painter.pixel(ox + x, oy + y, new Color(rgb));
            }
        }
    }

    private static int[][] renderFrames() {
        var frames = new int[FRAME_COUNT][];
        for (int i = 0; i < FRAME_COUNT; i++) frames[i] = renderLit(i);
        return frames;
    }

    private static int[] renderLit(int frame) {
        int[] pixels = new int[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                pixels[y * WIDTH + x] = shade(FlameOrb.radialAt(frame, x, y));
            }
        }
        return pixels;
    }

    private static int[] renderUnlit() {
        int[] pixels = new int[WIDTH * HEIGHT];
        double cx = (WIDTH - 1) / 2.0;
        double cy = (HEIGHT - 1) / 2.0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double dx = (x + 0.5) - cx;
                double dy = (y + 0.5) - cy;
                double n = Math.sqrt(dx * dx + dy * dy) / 1.55;
                if (n > 1) pixels[y * WIDTH + x] = CLEAR;
                else if (n < 0.55) pixels[y * WIDTH + x] = UNLIT_CORE;
                else pixels[y * WIDTH + x] = UNLIT_RIM;
            }
        }
        return pixels;
    }

    private static int shade(double n) {
        if (n > FlameOrb.CLEAR_RADIUS) return CLEAR;
        return switch (FlameOrb.tier(n)) {
            case CORE -> CORE;
            case YELLOW -> YELLOW;
            case ORANGE -> ORANGE;
            case RED -> RED;
            case RIM -> RIM;
        };
    }

    private static double wrap(double value) {
        double t = value - Math.floor(value);
        return t < 0 ? t + 1 : t;
    }

    private static int pack(float[] rgb) {
        int r = Math.round(Math.max(0f, Math.min(1f, rgb[0])) * 255f);
        int g = Math.round(Math.max(0f, Math.min(1f, rgb[1])) * 255f);
        int b = Math.round(Math.max(0f, Math.min(1f, rgb[2])) * 255f);
        return (r << 16) | (g << 8) | b;
    }

    private static int rgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
}

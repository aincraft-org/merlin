package dev.mintychochip.merlin.paper.mapgui;

import de.flog99.mapgui.ui.Painter;
import de.flog99.mapgui.ui.Rect;

/** Flame ink chip. Reuses {@link PipFlame} so the chip animates identically to the rank pips. */
final class InkFlame {
    static final int WIDTH = PipFlame.WIDTH;
    static final int HEIGHT = PipFlame.HEIGHT;
    static final int FRAME_COUNT = PipFlame.FRAME_COUNT;

    private InkFlame() {}

    static int[] pixels(double phase) {
        return PipFlame.pixels(true, phase);
    }

    static void draw(Painter painter, Rect bounds, double phase) {
        PipFlame.draw(painter, bounds, true, phase);
    }
}

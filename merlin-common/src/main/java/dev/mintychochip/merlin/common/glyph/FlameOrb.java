package dev.mintychochip.merlin.common.glyph;

/**
 * The animated flame orb, shared by the rank pips and flame ink.
 *
 * <p>The orb is a {@link #SIZE}×{@link #SIZE} sprite evaluated over
 * {@link #FRAME_COUNT} frames. Each frame breathes (radius pulse), drifts
 * (centre jitter) and squashes (vertical stretch with a flattened top), then
 * every cell resolves to a normalized radial coordinate. That coordinate maps
 * onto five tiers, giving the concentric bands that make the pip read as fire.
 *
 * <p>{@code PipFlame} draws this sprite as-is. Flame ink runs the same field over a
 * stroke instead of a square, via {@link #strokeRadialAt}: the orb stretched
 * {@link #LENGTH_RATIO} times longer than it is wide, then repeated down the spine so
 * a long stroke reads as a rope of orbs rather than one smear. Because every
 * measurement is taken in the stroke's own half-widths, a bead is as long as the
 * stroke is wide times that ratio, and the tiers stay in proportion at any brush
 * size. Both are the one orb, breathing on the one clock.
 */
public final class FlameOrb {
    public enum Tier { CORE, YELLOW, ORANGE, RED, RIM }

    public static final int SIZE = 11;
    public static final int FRAME_COUNT = 20;

    /** Radial coordinate beyond which the sprite is transparent. */
    public static final double CLEAR_RADIUS = 1.02;

    /**
     * How many times longer than wide the orb is drawn when it follows a stroke.
     * Pulling it out this far is what spreads the core from a dot into a run, while
     * the tiers stay stacked across the width. Measured in half-widths, so a hair-thin
     * stroke gets short beads and a fat one gets long ones — the same shape either way.
     */
    public static final double LENGTH_RATIO = 2.6;

    /**
     * How far a bead's field reaches past its own cell, in bead half-lengths. At 1 a
     * bead stops where its neighbour starts, so every join cools to the rim and the run
     * reads as beads on a wire. Reaching further lets neighbours overlap, keeping the
     * joins warm and the spine continuous while the cores stay distinct.
     */
    public static final double BEAD_SPREAD = 1.35;

    public static final float THRESHOLD_CORE = 0.28f;
    public static final float THRESHOLD_YELLOW = 0.48f;
    public static final float THRESHOLD_ORANGE = 0.72f;
    public static final float THRESHOLD_RED = 0.90f;

    public static final float[] CORE = new float[] { 255 / 255f, 248 / 255f, 170 / 255f };
    public static final float[] YELLOW = new float[] { 255 / 255f, 210 / 255f, 36 / 255f };
    public static final float[] ORANGE = new float[] { 255 / 255f, 128 / 255f, 16 / 255f };
    public static final float[] RED = new float[] { 255 / 255f, 48 / 255f, 10 / 255f };
    public static final float[] RIM = new float[] { 232 / 255f, 24 / 255f, 8 / 255f };

    /** The sprite's middle cell, which the orb drifts around. */
    private static final double CENTRE = (SIZE - 1) / 2.0;

    private FlameOrb() {}

    /** Resolves a wall-clock phase in [0, 1) to a frame index. */
    public static int frameAt(double phase) {
        double t = phase - Math.floor(phase);
        if (t < 0) t += 1;
        return Math.min(FRAME_COUNT - 1, (int) Math.floor(t * FRAME_COUNT));
    }

    /**
     * The orb's shape on one frame: how big it has breathed, where it has drifted
     * to, and how far it is squashed vertically and flattened on top.
     */
    private record Shape(double radius, double cx, double cy, double stretchY, double topSquash) {}

    private static Shape shape(int frame) {
        double turn = frame / (double) FRAME_COUNT * Math.PI * 2;
        double pulse = 0.5 + 0.5 * Math.sin(turn);
        return new Shape(
                3.35 + 0.55 * pulse,
                CENTRE + 0.32 * Math.sin(turn),
                CENTRE + 0.22 * Math.cos(turn * 2) - 0.1,
                0.94 - 0.05 * pulse,
                0.86 - 0.06 * pulse);
    }

    /**
     * Normalized radial coordinate of sprite cell {@code (x, y)} on {@code frame}.
     * Values below {@link #CLEAR_RADIUS} are inside the orb; larger values are
     * outside it and read as transparent.
     */
    public static double radialAt(int frame, int x, int y) {
        Shape s = shape(frame);
        double dx = (x + 0.5) - s.cx();
        double dy = ((y + 0.5) - s.cy()) / s.stretchY();
        if (dy < 0) dy *= s.topSquash();
        return Math.sqrt(dx * dx + dy * dy) / s.radius();
    }

    /**
     * How far the orb's rim sits from its centre, in sprite pixels, averaged over the
     * cycle. A stroke's half-width maps onto this, so the orb squeezed across a stroke
     * lands its rim on the stroke's edge.
     */
    private static final double MEAN_EXTENT = meanExtent();

    private static double meanExtent() {
        double total = 0;
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            Shape s = shape(frame);
            total += CLEAR_RADIUS * s.radius() * s.stretchY();
        }
        return total / FRAME_COUNT;
    }

    /**
     * Normalized radial coordinate for a point on a stroke, in the stroke's own frame.
     *
     * <p>This is {@link #radialAt} with the sprite's square domain swapped for a
     * stroke's. Both coordinates are scaled so that length 1 lands on the orb's rim,
     * then run through the orb's drift, stretch and squash for the frame. So the orb
     * keeps breathing inside the stroke, its hot line still wanders, and its flattened
     * top still leaves one flank cooler than the other — the pip's behaviour, wearing
     * the stroke's shape.
     *
     * @param along  distance from the nearest bead's centre measured down the spine, in
     *               bead half-lengths: 0 at the centre, ±1 where the bead meets its
     *               neighbour
     * @param across distance from the spine, in half-widths: 0 on the spine, ±1 at the
     *               stroke's edge
     */
    public static double strokeRadialAt(int frame, double along, double across) {
        Shape s = shape(frame);
        double dx = along / BEAD_SPREAD * MEAN_EXTENT - (s.cx() - CENTRE);
        double dy = (across * MEAN_EXTENT - (s.cy() - CENTRE)) / s.stretchY();
        if (dy < 0) dy *= s.topSquash();
        return Math.sqrt(dx * dx + dy * dy) / s.radius();
    }

    /** Maps a normalized radial coordinate to one of the five tiers. */
    public static Tier tier(double n) {
        if (n < THRESHOLD_CORE) return Tier.CORE;
        if (n < THRESHOLD_YELLOW) return Tier.YELLOW;
        if (n < THRESHOLD_ORANGE) return Tier.ORANGE;
        if (n < THRESHOLD_RED) return Tier.RED;
        return Tier.RIM;
    }

    public static float[] color(Tier tier) {
        return switch (tier) {
            case CORE -> CORE;
            case YELLOW -> YELLOW;
            case ORANGE -> ORANGE;
            case RED -> RED;
            case RIM -> RIM;
        };
    }
}

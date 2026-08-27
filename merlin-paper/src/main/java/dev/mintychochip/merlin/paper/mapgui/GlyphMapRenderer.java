package dev.mintychochip.merlin.paper.mapgui;

import dev.mintychochip.merlin.api.glyph.GlyphBitmap;
import java.awt.Color;
import java.util.List;
import java.util.function.IntFunction;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public final class GlyphMapRenderer extends MapRenderer {
    private static final int MAP_SIZE = 128;
    private final byte[] colors;
    private final byte[][] animatedFrames;
    private final IntFunction<float[]> frameSource;
    /** One frame per tick, matching the orb's one-second cycle on the live canvas. */
    private static final long FRAME_MILLIS = 50;

    public GlyphMapRenderer(GlyphBitmap bitmap) {
        this(quantize(validateBitmap(bitmap)));
    }

    public GlyphMapRenderer(float[] rgb) {
        this(rgbToBytes(rgb));
    }

    /**
     * Cycles through the supplied flame phase frames on every render so the saved
     * map item visibly animates. Non-flame pixels are byte-identical across frames.
     * {@link #colorAt(int, int)} continues to return the first phase frame.
     */
    public GlyphMapRenderer(List<float[]> phaseFrames) {
        this(source(phaseFrames), size(phaseFrames));
    }

    /**
     * Animates over {@code frameCount} phase frames pulled from {@code frameSource}
     * on demand. Only frame 0 is rasterized up front; the rest are quantized the
     * first time the cycle reaches them, so a saved map nobody looks at costs one
     * frame instead of the whole clock.
     */
    public GlyphMapRenderer(IntFunction<float[]> frameSource, int frameCount) {
        super(false);
        if (frameSource == null) throw new IllegalArgumentException("glyph map frame source required");
        if (frameCount < 1) throw new IllegalArgumentException("glyph map phase frames required");
        this.frameSource = frameSource;
        this.animatedFrames = new byte[frameCount][];
        this.colors = rgbToBytes(frameSource.apply(0));
        this.animatedFrames[0] = this.colors;
    }

    private GlyphMapRenderer(byte[] colors) {
        super(false);
        this.colors = colors;
        this.animatedFrames = null;
        this.frameSource = null;
    }

    private static IntFunction<float[]> source(List<float[]> phaseFrames) {
        if (phaseFrames == null || phaseFrames.isEmpty()) {
            throw new IllegalArgumentException("glyph map phase frames required");
        }
        var copy = List.copyOf(phaseFrames);
        return copy::get;
    }

    private static int size(List<float[]> phaseFrames) {
        if (phaseFrames == null || phaseFrames.isEmpty()) {
            throw new IllegalArgumentException("glyph map phase frames required");
        }
        return phaseFrames.size();
    }
    private static byte[] validateBitmap(GlyphBitmap bitmap) {
        if (bitmap.width() != MAP_SIZE || bitmap.height() != MAP_SIZE) {
            throw new IllegalArgumentException("glyph map must be 128x128");
        }
        return bitmap.pixels();
    }

    private static byte[] quantize(byte[] pixels) {
        if (pixels == null || pixels.length != MAP_SIZE * MAP_SIZE) {
            throw new IllegalArgumentException("glyph map must be 128x128");
        }
        byte[] out = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) out[i] = mapColor(pixels[i]);
        return out;
    }

    private static byte[] rgbToBytes(float[] rgb) {
        if (rgb == null || rgb.length != MAP_SIZE * MAP_SIZE * 3) {
            throw new IllegalArgumentException("glyph map rgb must be 128x128");
        }
        byte[] out = new byte[MAP_SIZE * MAP_SIZE];
        for (int i = 0; i < out.length; i++) {
            int o = i * 3;
            out[i] = mapColor(channel(rgb[o]), channel(rgb[o + 1]), channel(rgb[o + 2]));
        }
        return out;
    }

    private byte[] frame(int index) {
        byte[] cached = animatedFrames[index];
        if (cached != null) return cached;
        byte[] built = rgbToBytes(frameSource.apply(index));
        animatedFrames[index] = built;
        return built;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        byte[] frame = animatedFrames == null ? colors : frame(frameForNow());
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) canvas.setPixel(x, y, frame[y * MAP_SIZE + x]);
        }
    }

    /**
     * Returns the color at the given coordinate from the *static* frame. For
     * animated renderers this is always the first phase frame; use
     * {@link #animatedFrameAt(int, int)} to peek the cycling buffer.
     */
    byte colorAt(int x, int y) {
        return colors[y * MAP_SIZE + x];
    }

    /**
     * Returns the color at the given coordinate from the supplied animated frame
     * index, or the static frame if the renderer is not animated.
     */
    byte animatedFrameAt(int frameIndex, int x, int y) {
        byte[] source = animatedFrames != null ? frame(clampFrame(frameIndex)) : colors;
        return source[y * MAP_SIZE + x];
    }

    int animatedFrameCount() {
        return animatedFrames == null ? 1 : animatedFrames.length;
    }

    /**
     * The frame the cycle is on right now. Driven by wall-clock rather than a render
     * counter because Bukkit calls {@link #render} once per viewing player: a counter
     * would run at double speed for two viewers and show each of them a different
     * frame.
     */
    int frameForNow() {
        if (animatedFrames == null) return 0;
        long tick = System.currentTimeMillis() / FRAME_MILLIS;
        return (int) Math.floorMod(tick, animatedFrames.length);
    }

    private int clampFrame(int frameIndex) {
        if (animatedFrames == null) return 0;
        if (frameIndex < 0) return 0;
        if (frameIndex >= animatedFrames.length) return animatedFrames.length - 1;
        return frameIndex;
    }

    static byte mapColor(byte intensity) {
        int channel = Byte.toUnsignedInt(intensity);
        return mapColor(channel, channel, channel);
    }

    static byte mapColor(int red, int green, int blue) {
        return MapPalette.matchColor(new Color(clamp(red), clamp(green), clamp(blue)));
    }

    private static int channel(float value) {
        return Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

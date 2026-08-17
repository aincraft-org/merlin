package dev.mintychochip.wizardry.api.glyph;

import java.util.Arrays;

public record GlyphBitmap(int width, int height, byte[] pixels) {
    public GlyphBitmap {
        if (width <= 0 || height <= 0 || pixels.length != width * height) throw new IllegalArgumentException("invalid bitmap");
        pixels = pixels.clone();
    }
    @Override public byte[] pixels() { return pixels.clone(); }
    @Override public boolean equals(Object other) {
        return other instanceof GlyphBitmap b && width == b.width && height == b.height && Arrays.equals(pixels, b.pixels);
    }
    @Override public int hashCode() { return 31 * (31 * width + height) + Arrays.hashCode(pixels); }
}

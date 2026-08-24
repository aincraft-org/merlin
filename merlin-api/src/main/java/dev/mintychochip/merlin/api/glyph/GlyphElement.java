package dev.mintychochip.merlin.api.glyph;

public enum GlyphElement {
    FIRE(255, 77, 0),
    FROST(61, 220, 255),
    ARCANE(180, 74, 255),
    PHYSICAL(232, 228, 217);

    private final float r;
    private final float g;
    private final float b;

    GlyphElement(int red, int green, int blue) {
        this.r = red / 255f;
        this.g = green / 255f;
        this.b = blue / 255f;
    }

    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }

    public float[] rgb() { return new float[] {r, g, b}; }

    public static float coverage(double distance, double radius) {
        double raw = radius + 0.5 - distance;
        if (raw <= 0) return 0f;
        if (raw >= 1) return 1f;
        return (float) raw;
    }

    public static void blend(float[] rgb, int index, GlyphElement element, float coverage) {
        if (coverage <= 0) return;
        if (coverage >= 1) {
            rgb[index] = element.r;
            rgb[index + 1] = element.g;
            rgb[index + 2] = element.b;
            return;
        }
        float keep = 1f - coverage;
        rgb[index] = keep * rgb[index] + coverage * element.r;
        rgb[index + 1] = keep * rgb[index + 1] + coverage * element.g;
        rgb[index + 2] = keep * rgb[index + 2] + coverage * element.b;
    }
}

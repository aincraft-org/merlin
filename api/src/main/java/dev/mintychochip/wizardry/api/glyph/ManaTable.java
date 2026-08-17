package dev.mintychochip.wizardry.api.glyph;

public final class ManaTable {
    public static ManaTable v1() { return new ManaTable(); }
    public int mana(GlyphToken token) {
        return switch (token.role()) {
            case EFFECT -> 2 + token.pips();
            case SCHOOL -> 2;
            case PATIENT -> 1;
            case CHARM -> 2 + 2 * token.pips();
            default -> 0;
        };
    }
    private ManaTable() {}
}

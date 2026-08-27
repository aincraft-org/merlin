package dev.mintychochip.merlin.api.glyph;

import dev.mintychochip.merlin.api.ml.Label;

public final class GlyphRoles {
    public static GlyphRole of(Label label) {
        return switch (label) {
            case DAMAGE, HEAL, PUSH, SHIELD -> GlyphRole.EFFECT;
            case PHYSICAL, FLAME, FROST, ARCANE -> GlyphRole.SCHOOL;
            case SELF, TARGET -> GlyphRole.PATIENT;
            case ATTACKER -> GlyphRole.PATIENT;
            case SHARPNESS -> GlyphRole.CHARM;
            case TARGET_RAY -> GlyphRole.AIM;
            case ON_HIT, ON_HURT, ON_USE, PERIODIC -> GlyphRole.TRIGGER;
            case IF_HEALTH, IF_UNDEAD, IF_OUTDOORS -> GlyphRole.CONDITION;
            case AREA, REPEAT -> GlyphRole.SCOPE;
            case COOLDOWN, CHARGES -> GlyphRole.LIMIT;
            case REJECT -> GlyphRole.BOTTOM;
        };
    }
    public static boolean hasPips(GlyphRole role) {
        return role == GlyphRole.EFFECT || role == GlyphRole.CHARM;
    }
    public static boolean grammatical(GlyphRole role) {
        return role == GlyphRole.EFFECT || role == GlyphRole.SCHOOL
                || role == GlyphRole.PATIENT || role == GlyphRole.CHARM;
    }
    public static boolean reserved(Label label) {
        if (label == Label.ATTACKER) return true;
        var role = of(label);
        return role != GlyphRole.EFFECT && role != GlyphRole.SCHOOL
                && role != GlyphRole.PATIENT && role != GlyphRole.CHARM;
    }
    private GlyphRoles() {}
}

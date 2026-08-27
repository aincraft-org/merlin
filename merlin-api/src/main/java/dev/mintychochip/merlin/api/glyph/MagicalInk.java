package dev.mintychochip.merlin.api.glyph;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MagicalInk(GlyphElement element, int remaining, int max) {
    public static final int DEFAULT_MAX = 64;
    public static final List<GlyphElement> CHIP_ORDER = List.of(
            GlyphElement.PHYSICAL, GlyphElement.FLAME, GlyphElement.FROST, GlyphElement.ARCANE);

    public MagicalInk {
        Objects.requireNonNull(element, "element");
        if (max <= 0) throw new IllegalArgumentException("max must be positive");
        if (remaining < 0 || remaining > max) throw new IllegalArgumentException("remaining out of range");
    }

    public static MagicalInk full(GlyphElement element) {
        return new MagicalInk(element, DEFAULT_MAX, DEFAULT_MAX);
    }

    public boolean empty() {
        return remaining == 0;
    }

    public Optional<MagicalInk> spend(int units) {
        if (units <= 0) throw new IllegalArgumentException("spend units must be positive");
        if (remaining < units) return Optional.empty();
        return Optional.of(new MagicalInk(element, remaining - units, max));
    }

    public static List<GlyphElement> filledElements(Iterable<MagicalInk> inks) {
        var present = EnumSet.noneOf(GlyphElement.class);
        if (inks != null) {
            for (var ink : inks) {
                if (ink != null && !ink.empty()) present.add(ink.element());
            }
        }
        var filled = new ArrayList<GlyphElement>();
        for (var element : CHIP_ORDER) if (present.contains(element)) filled.add(element);
        return List.copyOf(filled);
    }

    public static Optional<GlyphElement> pick(GlyphElement offhand, List<GlyphElement> filled) {
        if (filled == null || filled.isEmpty()) return Optional.empty();
        if (offhand != null && filled.contains(offhand)) return Optional.of(offhand);
        return Optional.of(filled.getFirst());
    }

    public static Optional<GlyphElement> afterSpend(GlyphElement selected, List<GlyphElement> filled) {
        if (filled == null || filled.isEmpty()) return Optional.empty();
        if (selected != null && filled.contains(selected)) return Optional.of(selected);
        return Optional.of(filled.getFirst());
    }
}

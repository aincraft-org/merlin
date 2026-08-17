package dev.mintychochip.wizardry.api.glyph;

import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.ml.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TomePages {
    private static final int MAX_PAGES = 3;
    private static final String UNFINISHED = "G0107";

    private final List<GlyphToken> tokens;

    private TomePages(List<GlyphToken> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    public static TomePages empty() {
        return new TomePages(List.of());
    }

    public List<GlyphToken> tokens() {
        return tokens;
    }

    public Optional<TomePages> insert(GlyphToken token) {
        Objects.requireNonNull(token, "token");
        if (token.role() == GlyphRole.CHARM || token.label() == Label.SHARPNESS) {
            return Optional.empty();
        }
        if (tokens.size() >= MAX_PAGES) {
            return Optional.empty();
        }
        var candidate = new ArrayList<>(tokens);
        candidate.add(token);
        return switch (compiler().compile(candidate)) {
            case CompileResult.Ok unused -> Optional.of(new TomePages(candidate));
            case CompileResult.Error error -> unfinishedOnly(error)
                    ? Optional.of(new TomePages(candidate))
                    : Optional.empty();
        };
    }

    public Torn tear(int index) {
        var remaining = new ArrayList<>(tokens);
        var torn = remaining.remove(index);
        return new Torn(new TomePages(remaining), torn);
    }

    public record Torn(TomePages pages, GlyphToken torn) {
        public Torn {
            pages = Objects.requireNonNull(pages, "pages");
            torn = Objects.requireNonNull(torn, "torn");
        }
    }

    private static boolean unfinishedOnly(CompileResult.Error error) {
        return error.diagnostics().stream().allMatch(diagnostic -> UNFINISHED.equals(diagnostic.code()));
    }

    private static GlyphCompiler compiler() {
        return CompilerHolder.INSTANCE;
    }

    private static final class CompilerHolder {
        private static final GlyphCompiler INSTANCE = load();

        private static GlyphCompiler load() {
            try {
                return (GlyphCompiler) Class.forName("dev.mintychochip.wizardry.common.glyph.GlyphCompilerImpl")
                        .getField("INSTANCE")
                        .get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("GlyphCompiler unavailable", e);
            }
        }
    }
}

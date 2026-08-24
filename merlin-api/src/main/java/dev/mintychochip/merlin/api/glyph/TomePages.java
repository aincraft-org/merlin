package dev.mintychochip.merlin.api.glyph;

import dev.mintychochip.merlin.api.dsl.CompileResult;
import dev.mintychochip.merlin.api.dsl.Diagnostic;
import dev.mintychochip.merlin.api.ml.Label;
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

    public Optional<TomePages> insert(GlyphToken token, GlyphCompiler compiler) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(compiler, "compiler");
        if (token.role() == GlyphRole.CHARM || token.label() == Label.SHARPNESS) {
            return Optional.empty();
        }
        if (tokens.size() >= MAX_PAGES) {
            return Optional.empty();
        }
        var candidate = new ArrayList<>(tokens);
        candidate.add(token);
        return switch (compiler.compile(candidate)) {
            case CompileResult.Ok unused -> Optional.of(new TomePages(candidate));
            case CompileResult.Error error -> unfinishedOnly(error)
                    ? Optional.of(new TomePages(candidate))
                    : Optional.empty();
        };
    }

    public Optional<Diagnostic> rejection(GlyphToken token, GlyphCompiler compiler) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(compiler, "compiler");
        if (insert(token, compiler).isPresent()) {
            return Optional.empty();
        }
        var candidate = new ArrayList<>(tokens);
        candidate.add(token);
        return switch (compiler.compile(candidate)) {
            case CompileResult.Ok unused -> Optional.empty();
            case CompileResult.Error error -> firstRejection(error);
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

    private static Optional<Diagnostic> firstRejection(CompileResult.Error error) {
        return error.diagnostics().stream()
                .filter(diagnostic -> !UNFINISHED.equals(diagnostic.code()))
                .findFirst()
                .or(() -> error.diagnostics().stream().findFirst());
    }
}

package dev.mintychochip.merlin.api.glyph;

import dev.mintychochip.merlin.api.dsl.CompileResult;
import java.util.List;
import java.util.Optional;

public interface GlyphCompiler {
    CompileResult compile(List<GlyphToken> pages);
    Optional<CharmBind> charm(GlyphToken token);
}

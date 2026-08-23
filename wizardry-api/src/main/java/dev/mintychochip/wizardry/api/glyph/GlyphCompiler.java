package dev.mintychochip.wizardry.api.glyph;

import dev.mintychochip.wizardry.api.dsl.CompileResult;
import java.util.List;
import java.util.Optional;

public interface GlyphCompiler {
    CompileResult compile(List<GlyphToken> pages);
    Optional<CharmBind> charm(GlyphToken token);
}

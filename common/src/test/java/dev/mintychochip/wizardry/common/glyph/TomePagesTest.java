package dev.mintychochip.wizardry.common.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.glyph.TomePages;
import dev.mintychochip.wizardry.api.ml.Label;
import org.junit.jupiter.api.Test;

final class TomePagesTest {
    @Test void insertSecondEffectIsRejected() {
        var empty = TomePages.empty();
        var once = empty.insert(new GlyphToken(Label.DAMAGE, 1), GlyphCompilerImpl.INSTANCE);
        assertTrue(once.isPresent());
        assertTrue(once.get().insert(new GlyphToken(Label.HEAL, 1), GlyphCompilerImpl.INSTANCE).isEmpty());
    }

    @Test void loneFireMaySitUnfinished() {
        var pages = TomePages.empty().insert(new GlyphToken(Label.FIRE, 1), GlyphCompilerImpl.INSTANCE).orElseThrow();
        assertEquals(1, pages.tokens().size());
        assertInstanceOf(CompileResult.Error.class, GlyphCompilerImpl.INSTANCE.compile(pages.tokens()));
    }

    @Test void fireThenDamageIsCastable() {
        var pages = TomePages.empty()
                .insert(new GlyphToken(Label.FIRE, 1), GlyphCompilerImpl.INSTANCE).orElseThrow()
                .insert(new GlyphToken(Label.DAMAGE, 5), GlyphCompilerImpl.INSTANCE).orElseThrow();
        assertInstanceOf(CompileResult.Ok.class, GlyphCompilerImpl.INSTANCE.compile(pages.tokens()));
    }

    @Test void charmWillNotInsert() {
        assertTrue(TomePages.empty().insert(new GlyphToken(Label.SHARPNESS, 5), GlyphCompilerImpl.INSTANCE).isEmpty());
    }

    @Test void tearRemovesPage() {
        var pages = TomePages.empty().insert(new GlyphToken(Label.DAMAGE, 1), GlyphCompilerImpl.INSTANCE).orElseThrow();
        var torn = pages.tear(0);
        assertEquals(0, torn.pages().tokens().size());
        assertEquals(Label.DAMAGE, torn.torn().label());
    }
}

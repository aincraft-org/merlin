package dev.jlo.wizardry.mapgui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import dev.jlo.wizardry.glyph.GlyphDraft;
import org.junit.jupiter.api.Test;

final class GlyphDraftStoreAdapterTest {
    @Test void persistenceAdapterAndEmptyDraftContractExist() {
        assertNotNull(GlyphDraftStoreAdapter.class);
        assertNotNull(GlyphDraft.empty());
    }
}

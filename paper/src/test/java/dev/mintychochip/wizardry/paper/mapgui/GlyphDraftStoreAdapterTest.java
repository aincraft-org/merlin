package dev.mintychochip.wizardry.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.glyph.GlyphPoint;
import dev.mintychochip.wizardry.api.glyph.GlyphStroke;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class GlyphDraftStoreAdapterTest {
    @Test void persistenceAdapterAndEmptyDraftContractExist() {
        assertNotNull(GlyphDraftStoreAdapter.class);
        assertNotNull(GlyphDraft.empty());
    }

    @Test void glyphCanvasMaterialAcceptsPaperAndFilledMapsOnly() {
        assertTrue(GlyphDraftStoreAdapter.isGlyphCanvasMaterial(Material.PAPER));
        assertTrue(GlyphDraftStoreAdapter.isGlyphCanvasMaterial(Material.FILLED_MAP));
        assertFalse(GlyphDraftStoreAdapter.isGlyphCanvasMaterial(Material.MAP));
        assertFalse(GlyphDraftStoreAdapter.isGlyphCanvasMaterial(Material.STONE));
    }

    @Test void mapPreparationRequestRequiresStableIdentityAndDraft() {
        UUID id = UUID.randomUUID();
        var request = new GlyphDraftStoreAdapter.MapSaveRequest(id, GlyphDraft.empty());
        assertEquals(id, request.itemId());
        assertEquals(GlyphDraft.empty(), request.draft());
    }
    @Test void encodingPreservesVelocityDerivedSegmentWidths() {
        var draft = new GlyphDraft(java.util.List.of(new GlyphStroke(
                java.util.List.of(new GlyphPoint(1, 1), new GlyphPoint(2, 2), new GlyphPoint(3, 3)),
                4,
                10,
                java.util.List.of(5.5, 1.25))));

        assertEquals(draft, GlyphDraftStoreAdapter.decode(GlyphDraftStoreAdapter.encode(draft)));
    }

}

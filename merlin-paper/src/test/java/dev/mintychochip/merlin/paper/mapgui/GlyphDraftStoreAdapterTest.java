package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphPoint;
import dev.mintychochip.merlin.api.glyph.GlyphStroke;
import dev.mintychochip.merlin.api.glyph.GlyphToken;
import dev.mintychochip.merlin.api.glyph.ManaTable;
import dev.mintychochip.merlin.api.ml.Label;
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

    @Test void legacyTextEncodingStillDecodes() {
        var draft = new GlyphDraft(java.util.List.of(new GlyphStroke(
                java.util.List.of(new GlyphPoint(1, 1), new GlyphPoint(2, 2)),
                4, 10, java.util.List.of(4.0), GlyphElement.FLAME)));
        byte[] v3 = java.util.Base64.getEncoder().encode(
                "3;4.0,10,FLAME:1.0,1.0;2.0,2.0;:4.0,|".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(draft, GlyphDraftStoreAdapter.decode(v3));
    }

    @Test void encodingPreservesStrokeElement() {
        var draft = new GlyphDraft(java.util.List.of(new GlyphStroke(
                java.util.List.of(new GlyphPoint(1, 1), new GlyphPoint(2, 2)),
                4, 10, java.util.List.of(4.0), GlyphElement.FLAME)));
        assertEquals(draft, GlyphDraftStoreAdapter.decode(GlyphDraftStoreAdapter.encode(draft)));
        assertEquals(GlyphElement.PHYSICAL, GlyphDraftStoreAdapter.decode(
                GlyphDraftStoreAdapter.encode(new GlyphDraft(java.util.List.of(
                        new GlyphStroke(java.util.List.of(new GlyphPoint(1, 1)), 2, 0))))).strokes().getFirst().element());
    }

    @Test void tokenCodecRoundTrips() {
        var token = new GlyphToken(Label.DAMAGE, 5);
        var encoded = GlyphDraftStoreAdapter.encodeToken(token, ManaTable.v1());
        assertEquals("damage", encoded.label());
        assertEquals(5, encoded.pips());
        assertEquals(7, encoded.mana());
        var decoded = GlyphDraftStoreAdapter.decodeToken(encoded);
        assertEquals(token, decoded);
    }

    @Test void unclassifiedSavedMapIsNamedGlyphNotMap() {
        assertEquals("Glyph", GlyphDraftStoreAdapter.savedMapTitle(null));
    }

    @Test void classifiedSavedMapKeepsTokenTitle() {
        var token = new GlyphToken(Label.DAMAGE, 3);
        assertEquals(GlyphDraftStoreAdapter.tokenTitle(token), GlyphDraftStoreAdapter.savedMapTitle(token));
    }

    @Test void stampParseAcceptsGrammaticalLabels() {
        assertEquals(new GlyphToken(Label.SHARPNESS, 5), GlyphCommand.parseStamp(new String[]{"stamp", "sharpness", "5"}).orElseThrow());
        assertEquals(new GlyphToken(Label.FLAME, 1), GlyphCommand.parseStamp(new String[]{"stamp", "flame"}).orElseThrow());
        assertEquals(new GlyphToken(Label.FLAME, 1), GlyphCommand.parseStamp(new String[]{"stamp", "fire"}).orElseThrow());
        assertTrue(GlyphCommand.parseStamp(new String[]{"stamp", "on-hit"}).isEmpty());
        assertTrue(GlyphCommand.parseStamp(new String[]{"stamp", "nope"}).isEmpty());
    }

}

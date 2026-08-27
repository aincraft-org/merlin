package dev.mintychochip.merlin.common.glyph;
import dev.mintychochip.merlin.api.glyph.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class GlyphCaptureSessionTest {
    @Test void snapshotIncludesActiveStrokeUndoRemovesItClosePreservesIt() {
        var session = new GlyphCaptureSession();
        session.beginStroke(0);
        session.appendPoint(new GlyphPoint(1, 1));
        assertEquals(1, session.snapshot().strokes().size());
        session.undo();
        assertTrue(session.snapshot().strokes().isEmpty());
        session.beginStroke(1);
        session.appendPoint(new GlyphPoint(2, 2));
        assertEquals(1, session.close().strokes().size());
    }

    @Test void beginStrokeTagsElementOnSnapshot() {
        var session = new GlyphCaptureSession();
        session.beginStroke(0, GlyphElement.FLAME);
        session.appendPoint(new GlyphPoint(1, 1));
        assertEquals(GlyphElement.FLAME, session.snapshot().strokes().getFirst().element());
        session.endStroke();
        assertEquals(GlyphElement.FLAME, session.snapshot().strokes().getFirst().element());
    }

    @Test void beginStrokeWithoutElementStaysPhysical() {
        var session = new GlyphCaptureSession();
        session.beginStroke(0);
        session.appendPoint(new GlyphPoint(1, 1));
        session.endStroke();
        assertEquals(GlyphElement.PHYSICAL, session.snapshot().strokes().getFirst().element());
    }

    @Test void popLastPointRetractsActiveGeometry() {
        var session = new GlyphCaptureSession();
        session.beginStroke(0);
        session.appendPoint(new GlyphPoint(1, 1));
        session.appendPoint(new GlyphPoint(2, 2));
        session.segmentWidth(3);
        session.popLastPoint();
        var stroke = session.snapshot().strokes().getFirst();
        assertEquals(1, stroke.points().size());
        assertTrue(stroke.segmentWidths().isEmpty());
        assertEquals(new GlyphPoint(1, 1), stroke.points().getFirst());
    }
}

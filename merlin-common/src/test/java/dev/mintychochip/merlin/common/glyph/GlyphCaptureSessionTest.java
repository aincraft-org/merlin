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
}

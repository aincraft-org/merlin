package dev.jlo.wizardry.ml;

import dev.jlo.wizardry.glyph.*; import org.junit.jupiter.api.Test; import java.nio.file.*; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
final class MlContractsTest {
 @Test void preprocessingShapeOrderAndMasks(){var d=new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(0,0),new GlyphPoint(127,127)),2,0))); var p=new GlyphPreprocessor().preprocess(d); assertEquals(64,p.vectors().length); assertEquals(32,p.vectors()[0].length); assertEquals(8,p.vectors()[0][0].length); assertTrue(p.mask()[0][0]); assertEquals(1f,p.vectors()[0][0][6]); assertEquals(1f,p.vectors()[0][31][4]); assertEquals(1f,p.raster()[0][63][63]); }
 @Test void emptyRejected(){assertThrows(IllegalArgumentException.class,()->new GlyphPreprocessor().preprocess(GlyphDraft.empty()));}
 @Test void corruptBundleRejected() throws Exception {Path d=Files.createTempDirectory("bundle"); Files.writeString(d.resolve("manifest.json"),"{}"); assertThrows(ModelBundle.BundleException.class,()->ModelBundle.load(d));}
}

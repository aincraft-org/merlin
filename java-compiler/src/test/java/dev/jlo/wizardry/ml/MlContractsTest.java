package dev.jlo.wizardry.ml;

import dev.jlo.wizardry.glyph.*; import org.junit.jupiter.api.Test; import java.nio.file.*; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
final class MlContractsTest {
 @Test void preprocessingShapeOrderAndMasks(){var d=new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(0,0),new GlyphPoint(127,127)),2,0))); var p=new GlyphPreprocessor().preprocess(d); assertEquals(64,p.vectors().length); assertEquals(32,p.vectors()[0].length); assertEquals(8,p.vectors()[0][0].length); assertTrue(p.mask()[0][0]); assertEquals(1f,p.vectors()[0][0][6]); assertEquals(1f,p.vectors()[0][31][4]); assertEquals(1f,p.raster()[0][63][63]); }
 @Test void rasterIsConnectedBrushBitImageNotResampledDots() {
  var draft = new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(8, 32), new GlyphPoint(120, 32)), 6, 0)));
  var row = new GlyphPreprocessor().preprocess(draft).raster()[0][16];
  int first = -1, last = -1, ink = 0;
  for (int x = 0; x < 64; x++) if (row[x] > 0) { if (first < 0) first = x; last = x; ink++; }
  assertTrue(ink >= 50, "expected a solid brush shaft, got " + ink + " pixels");
  assertTrue(first <= 5 && last >= 58);
  assertEquals(last - first + 1, ink);
 }
 @Test void thickerBrushCoversMoreRasterPixels() {
  var thin = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(20, 64), new GlyphPoint(100, 64)), 2, 0)))).raster()[0];
  var thick = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(20, 64), new GlyphPoint(100, 64)), 8, 0)))).raster()[0];
  assertTrue(countInk(thick) > countInk(thin));
 }
 @Test void onePointStrokeRasterIsRoundDab() {
  var raster = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(64, 64)), 6, 0)))).raster()[0];
  assertEquals(1f, raster[32][32]);
  assertEquals(1f, raster[32][31]);
  assertEquals(1f, raster[31][32]);
  assertEquals(0f, raster[0][0]);
 }
 @Test void emptyRejected(){assertThrows(IllegalArgumentException.class,()->new GlyphPreprocessor().preprocess(GlyphDraft.empty()));}
 private static int countInk(float[][] raster) {
  int ink = 0;
  for (float[] row : raster) for (float value : row) if (value > 0) ink++;
  return ink;
 }
 @Test void corruptBundleRejected() throws Exception {Path d=Files.createTempDirectory("bundle"); Files.writeString(d.resolve("manifest.json"),"{}"); assertThrows(ModelBundle.BundleException.class,()->ModelBundle.load(d));}
}

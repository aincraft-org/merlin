package dev.mintychochip.wizardry.common.ml;

import dev.mintychochip.wizardry.api.glyph.*;
import dev.mintychochip.wizardry.common.glyph.*;
import java.util.*;

public final class GlyphPreprocessor {
    public static final String VERSION = "preprocessing-v1";
    public static final int NORMALIZE_PADDING = 4;
    public PreprocessedGlyph preprocess(GlyphDraft draft) {
        if (draft == null || draft.strokes().isEmpty()) throw new IllegalArgumentException("cannot preprocess empty glyph");
        GlyphBitmap full = GlyphRasterizer.renderFull(draft);
        int[] bounds = GlyphRasterizer.paddedInkBounds(full.pixels(), NORMALIZE_PADDING);
        float[][][] v = new float[64][32][8]; boolean[][] m = new boolean[64][32]; float[][][] r = toRaster(GlyphRasterizer.resample(full, bounds, 64));
        double minX = bounds[2] < 0 ? 0 : bounds[0], minY = bounds[2] < 0 ? 0 : bounds[1];
        double spanX = bounds[2] < 0 ? 128 : bounds[2] - bounds[0] + 1, spanY = bounds[2] < 0 ? 128 : bounds[3] - bounds[1] + 1;
        int si = 0;
        for (var stroke : draft.strokes()) {
            if (si == 64) break; var raw = stroke.points(); if (raw.isEmpty()) continue;
            for (int pi = 0; pi < 32; pi++) {
                double t = pi / 31.0; var p = sample(raw, t); var prev = sample(raw, pi == 0 ? 0 : (pi - 1) / 31.0);
                double x = clamp(p.x(), 0, 127.999999), y = clamp(p.y(), 0, 127.999999);
                v[si][pi] = new float[]{(float)((x-minX)/spanX), (float)((y-minY)/spanY), (float)((x-prev.x())/spanX), (float)((y-prev.y())/spanY), (float)t, pi == 0 ? 0 : 1, pi == 0 ? 1 : 0, (float)(clamp(stroke.brushWidth(),0,32)/32)};
                m[si][pi] = true;
            } si++;
        }
        if (si == 0) throw new IllegalArgumentException("cannot preprocess empty glyph"); return new PreprocessedGlyph(v,m,r);
    }
    private static float[][][] toRaster(GlyphBitmap bitmap) {
        byte[] pixels = bitmap.pixels();
        float[][][] raster = new float[1][64][64];
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
            raster[0][y][x] = (pixels[y * 64 + x] & 0xff) != 0 ? 1f : 0f;
        return raster;
    }
    private static GlyphPoint sample(List<GlyphPoint> p, double t) { if (p.size()==1) return p.getFirst(); double[] d=new double[p.size()]; for(int i=1;i<p.size();i++) d[i]=d[i-1]+Math.hypot(p.get(i).x()-p.get(i-1).x(),p.get(i).y()-p.get(i-1).y()); double target=t*d[p.size()-1]; int i=1; while(i<p.size()&&d[i]<target)i++; double span=d[i]-d[i-1]; double f=span==0?0:(target-d[i-1])/span; return new GlyphPoint(p.get(i-1).x()+f*(p.get(i).x()-p.get(i-1).x()),p.get(i-1).y()+f*(p.get(i).y()-p.get(i-1).y())); }
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
}

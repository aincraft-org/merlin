package dev.jlo.wizardry.ml;

import dev.jlo.wizardry.glyph.*;
import java.util.*;

public final class GlyphPreprocessor {
    public static final String VERSION = "preprocessing-v1";
    public PreprocessedGlyph preprocess(GlyphDraft draft) {
        if (draft == null || draft.strokes().isEmpty()) throw new IllegalArgumentException("cannot preprocess empty glyph");
        float[][][] v = new float[64][32][8]; boolean[][] m = new boolean[64][32]; float[][][] r = new float[1][64][64];
        int si = 0;
        for (var stroke : draft.strokes()) {
            if (si == 64) break; var raw = stroke.points(); if (raw.isEmpty()) continue;
            for (int pi = 0; pi < 32; pi++) {
                double t = pi / 31.0; var p = sample(raw, t); var prev = sample(raw, pi == 0 ? 0 : (pi - 1) / 31.0);
                double x = clamp(p.x(), 0, 127.999999), y = clamp(p.y(), 0, 127.999999);
                v[si][pi] = new float[]{(float)(x/128), (float)(y/128), (float)((x-prev.x())/128), (float)((y-prev.y())/128), (float)t, pi == 0 ? 0 : 1, pi == 0 ? 1 : 0, (float)(clamp(stroke.brushWidth(),0,32)/32)};
                m[si][pi] = true; r[0][Math.min(63, Math.max(0, (int)(y/2)))][Math.min(63, Math.max(0, (int)(x/2)))] = 1;
            } si++;
        }
        if (si == 0) throw new IllegalArgumentException("cannot preprocess empty glyph"); return new PreprocessedGlyph(v,m,r);
    }
    private static GlyphPoint sample(List<GlyphPoint> p, double t) { if (p.size()==1) return p.getFirst(); double[] d=new double[p.size()]; for(int i=1;i<p.size();i++) d[i]=d[i-1]+Math.hypot(p.get(i).x()-p.get(i-1).x(),p.get(i).y()-p.get(i-1).y()); double target=t*d[p.size()-1]; int i=1; while(i<p.size()&&d[i]<target)i++; double span=d[i]-d[i-1]; double f=span==0?0:(target-d[i-1])/span; return new GlyphPoint(p.get(i-1).x()+f*(p.get(i).x()-p.get(i-1).x()),p.get(i-1).y()+f*(p.get(i).y()-p.get(i-1).y())); }
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
}

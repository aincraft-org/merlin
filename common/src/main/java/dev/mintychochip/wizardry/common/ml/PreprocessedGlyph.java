package dev.mintychochip.wizardry.common.ml;

public record PreprocessedGlyph(float[][][] vectors, boolean[][] mask, float[][][] raster) {
    public PreprocessedGlyph {
        if (vectors.length != 64 || mask.length != 64 || raster.length != 1 || raster[0].length != 64 || raster[0][0].length != 64) throw new IllegalArgumentException("invalid tensor shape");
        for (int i = 0; i < 64; i++) { if (vectors[i].length != 32 || mask[i].length != 32) throw new IllegalArgumentException("invalid tensor shape"); for (int j = 0; j < 32; j++) if (vectors[i][j].length != 8) throw new IllegalArgumentException("invalid tensor shape"); }
    }
}

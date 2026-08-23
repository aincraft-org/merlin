package dev.mintychochip.wizardry.paper.mapgui;

import dev.mintychochip.wizardry.api.glyph.GlyphBitmap;
import java.awt.Color;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public final class GlyphMapRenderer extends MapRenderer {
    private static final int MAP_SIZE = 128;
    private final byte[] colors;

    public GlyphMapRenderer(GlyphBitmap bitmap) {
        super(false);
        if (bitmap.width() != MAP_SIZE || bitmap.height() != MAP_SIZE) {
            throw new IllegalArgumentException("glyph map must be 128x128");
        }
        byte[] pixels = bitmap.pixels();
        colors = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) colors[i] = mapColor(pixels[i]);
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) canvas.setPixel(x, y, colors[y * MAP_SIZE + x]);
        }
    }

    static byte mapColor(byte intensity) {
        int channel = Byte.toUnsignedInt(intensity);
        return MapPalette.matchColor(new Color(channel, channel, channel));
    }
}

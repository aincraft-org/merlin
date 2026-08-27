package dev.mintychochip.merlin.paper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PluginClassPreloaderTest {
    @Test
    void pluginJarClassNamesIncludeGlyphCaptureSession() throws Exception {
        var jar = Path.of("build/libs/merlin-paper-1.0.0-SNAPSHOT.jar");
        var names = PluginClassPreloader.classNames(jar);
        assertTrue(names.contains("dev.mintychochip.merlin.api.glyph.GlyphCaptureSession"));
        assertTrue(names.contains("dev.mintychochip.merlin.api.glyph.GlyphDraft"));
        assertTrue(names.contains("dev.mintychochip.merlin.paper.mapgui.GlyphScreen"));
        assertTrue(
                names.stream().noneMatch(name -> name.startsWith("dev.mintychochip.merlin.paper.loader.")),
                "loader classes stay in the isolated Paper loader classloader");
    }

    @Test
    void merlinPluginPreloadsEveryBundledClassOnEnable() throws Exception {
        var source = java.nio.file.Files.readString(
                Path.of("src/main/java/dev/mintychochip/merlin/paper/MerlinPlugin.java"));
        assertTrue(source.contains("PluginClassPreloader.loadAll("));
    }
}

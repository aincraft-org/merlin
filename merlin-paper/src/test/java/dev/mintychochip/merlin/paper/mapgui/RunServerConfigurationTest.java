package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RunServerConfigurationTest {
    @Test
    void runServerRemovesStaleMapGuiJarsBeforeInstallingLocalSnapshot() throws IOException {
        var testBuild = Files.readString(Path.of("../merlin-test/build.gradle.kts"));

        assertTrue(testBuild.contains("delete(fileTree("));
        assertTrue(testBuild.contains("include(\"MapGUI-*.jar\")"));
        assertFalse(testBuild.contains("MapGUI-1.0.0.jar"));
    }

    @Test
    void catalogPinsPublishedMapGuiApiWithoutSeparateLayoutCoordinate() throws IOException {
        var catalog = Files.readString(Path.of("../gradle/libs.versions.toml"));
        var paperBuild = Files.readString(Path.of("build.gradle.kts"));
        var testBuild = Files.readString(Path.of("../merlin-test/build.gradle.kts"));

        assertTrue(catalog.contains("mapgui = \"2.0.0\""));
        assertTrue(catalog.contains("mapgui-api = { module = \"io.github.flog99:mapgui-api\", version.ref = \"mapgui\" }"));
        assertFalse(catalog.contains("mapgui-layout"));
        assertFalse(paperBuild.contains("libs.mapgui.layout"));
        assertFalse(testBuild.contains("libs.mapgui.layout"));
    }
}

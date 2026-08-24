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
}

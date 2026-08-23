package dev.mintychochip.wizardry.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RunServerConfigurationTest {
    @Test
    void runServerRemovesStaleMapGuiJarsBeforeInstallingLocalSnapshot() throws IOException {
        var rootBuild = Files.readString(Path.of("../build.gradle.kts"));

        assertTrue(rootBuild.contains("delete(fileTree(\"run/plugins\")"));
        assertTrue(rootBuild.contains("include(\"MapGUI-*.jar\")"));
        assertFalse(rootBuild.contains("MapGUI-1.0.0.jar"));
    }
}

package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.*;

import ai.onnxruntime.OrtEnvironment;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

final class OnnxRuntimePackagingTest {
    @Test
    void unifiedDescriptorDeclaresMainLoaderDependencyAndPermissions() throws IOException {
        var descriptor = Files.readString(Path.of("src/main/resources/paper-plugin.yml"));
        assertTrue(descriptor.lines().anyMatch(line -> line.equals("main: dev.mintychochip.merlin.paper.MerlinPlugin")));
        assertTrue(descriptor.lines().anyMatch(line -> line.equals("loader: dev.mintychochip.merlin.paper.mapgui.GlyphPluginLoader")));
        assertTrue(descriptor.contains("MapGUI:"));
        assertTrue(descriptor.contains("merlin.scribe.book:"));
        assertTrue(descriptor.contains("merlin.glyph.draw:"));
        assertTrue(descriptor.contains("merlin.glyph.tome:"));
    }
    @Test
    void glyphPermissionRemainsAvailableToOrdinaryPlayers() throws IOException {
        var descriptor = Files.readString(Path.of("src/main/resources/paper-plugin.yml"));
        assertTrue(descriptor.contains("merlin.glyph.draw:"));
        assertTrue(descriptor.contains("default: true"));
    }


    @Test
    void loaderResolvesExactCoordinateFromCentralMirror() throws Exception {
        var loader = new GlyphPluginLoader();
        var builder = new RecordingClasspathBuilder();
        loader.classloader(builder);
        assertEquals(1, builder.libraries.size());
        var resolver = builder.libraries.getFirst();
        assertInstanceOf(io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver.class, resolver);
        var repositories = privateField(resolver, "repositories");
        assertEquals(1, repositories.size());
        var repository = (RemoteRepository) repositories.getFirst();
        assertEquals("central", repository.getId());
        assertEquals(io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR, repository.getUrl());
        var dependencies = privateField(resolver, "dependencies");
        assertEquals(1, dependencies.size());
        var artifact = ((Dependency) dependencies.getFirst()).getArtifact();
        assertEquals("com.microsoft.onnxruntime", artifact.getGroupId());
        assertEquals("onnxruntime", artifact.getArtifactId());
        assertEquals("1.29.0", artifact.getVersion());
    }

    @SuppressWarnings("unchecked")
    private static List<Object> privateField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (List<Object>) field.get(target);
    }

    @Test
    void ortEnvironmentInitializes() {
        assertNotNull(OrtEnvironment.getEnvironment());
    }
    @Test
    void pluginJarDoesNotEmbedOnnxRuntime() throws IOException {
        var jar = Path.of("build/libs/merlin-paper-1.0.0-SNAPSHOT.jar");
        assertTrue(Files.exists(jar), "build the plugin jar before running this packaging check");
        try (var fs = java.nio.file.FileSystems.newFileSystem(jar)) {
            try (Stream<Path> paths = Files.walk(fs.getPath("/"))) {
                assertFalse(paths.anyMatch(path -> {
                    var entry = path.toString();
                    return entry.contains("ai/onnxruntime/")
                            || entry.endsWith(".so")
                            || entry.endsWith(".dll")
                            || entry.endsWith(".dylib");
                }));
            }
        }
    }
    private static final class RecordingClasspathBuilder implements io.papermc.paper.plugin.loader.PluginClasspathBuilder {
        final List<Object> libraries = new java.util.ArrayList<>();
        @Override public io.papermc.paper.plugin.loader.PluginClasspathBuilder addLibrary(io.papermc.paper.plugin.loader.library.ClassPathLibrary library) { libraries.add(library); return this; }
        @Override public io.papermc.paper.plugin.bootstrap.PluginProviderContext getContext() { return null; }
    }
}

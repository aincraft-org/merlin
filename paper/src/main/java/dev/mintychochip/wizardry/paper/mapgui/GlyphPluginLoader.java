package dev.mintychochip.wizardry.paper.mapgui;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;
/** Resolves ONNX Runtime outside the deployable plugin jar. */
public final class GlyphPluginLoader implements PluginLoader {
    static final String ONNX_COORDINATE = "com.microsoft.onnxruntime:onnxruntime:1.29.0";

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        var resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        resolver.addDependency(new Dependency(new DefaultArtifact(ONNX_COORDINATE), JavaScopes.RUNTIME));
        classpathBuilder.addLibrary(resolver);
    }
}

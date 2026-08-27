package dev.mintychochip.merlin.paper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/** Loads bundled plugin classes during enable so Paper's isolated loader cannot miss them later. */
final class PluginClassPreloader {
    private static final String LOADER_PREFIX = "dev.mintychochip.merlin.paper.loader.";

    private PluginClassPreloader() {}

    static List<String> classNames(Path jar) throws IOException {
        var names = new ArrayList<String>();
        try (var file = new JarFile(jar.toFile())) {
            var entries = file.entries();
            while (entries.hasMoreElements()) {
                var name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.contains("module-info")) continue;
                var className = name.substring(0, name.length() - 6).replace('/', '.');
                if (className.startsWith(LOADER_PREFIX)) continue;
                names.add(className);
            }
        }
        return names;
    }

    static int loadAll(ClassLoader loader, File jar) throws IOException {
        int loaded = 0;
        for (var className : classNames(jar.toPath())) {
            try {
                Class.forName(className, false, loader);
                loaded++;
            } catch (ClassNotFoundException missing) {
                throw new IllegalStateException("Plugin jar is missing " + className, missing);
            }
        }
        return loaded;
    }
}

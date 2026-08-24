package dev.mintychochip.merlin.paper.model;

import dev.mintychochip.merlin.api.ml.ModelBundle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ModelBundleFetcher {
  private static final Set<String> REQUIRED_FILES =
      Set.of("manifest.json", "model.onnx", "model.pt", "sha256sums.json");
  private final String repository;
  private final String version;
  private final Path cacheRoot;
  private final Function<URI, Path> archiveSource;

  public ModelBundleFetcher(String repository, String version, Path cacheRoot) {
    this(repository, version, cacheRoot, uri -> download(uri));
  }

  ModelBundleFetcher(
      String repository, String version, Path cacheRoot, Function<URI, Path> archiveSource) {
    this.repository = repository;
    this.version = version;
    this.cacheRoot = cacheRoot;
    this.archiveSource = archiveSource;
  }

  public Path ensureBundle() throws IOException {
    Path destination = cacheRoot.resolve(version).toAbsolutePath().normalize();
    if (Files.isDirectory(destination)) {
      return validate(destination);
    }
    Files.createDirectories(cacheRoot);
    Path archive = archiveSource.apply(archiveUri());
    Path temporary = Files.createTempDirectory(cacheRoot, version + ".");
    try {
      extract(archive, temporary);
      Path validated = validate(temporary);
      Files.move(validated, destination, StandardCopyOption.ATOMIC_MOVE);
      return destination;
    } catch (IOException error) {
      deleteTree(temporary);
      throw new IOException(
          "model " + version + " is unavailable or invalid: " + error.getMessage(), error);
    } finally {
      Files.deleteIfExists(archive);
    }
  }

  private Path validate(Path root) throws IOException {
    for (String file : REQUIRED_FILES) {
      if (!Files.isRegularFile(root.resolve(file))) throw new IOException("missing " + file);
    }
    try {
      ModelBundle.load(root);
    } catch (ModelBundle.BundleException error) {
      throw new IOException(
          "model "
              + version
              + " failed release validation: release_ready must be true; "
              + error.getMessage(),
          error);
    }
    return root;
  }

  private URI archiveUri() {
    return URI.create(repository + "/archive/refs/tags/" + version + ".zip");
  }

  private static Path download(URI uri) {
    try {
      Path target = Files.createTempFile("merlin-model-", ".zip");
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      HttpRequest request =
          HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build();
      HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
      if (response.statusCode() / 100 != 2)
        throw new IOException("download returned HTTP " + response.statusCode());
      return response.body();
    } catch (IOException error) {
      throw new IllegalStateException(
          "failed to download model archive: " + error.getMessage(), error);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("model download interrupted", error);
    }
  }

  private static void extract(Path archive, Path destination) throws IOException {
    Set<String> extracted = new HashSet<>();
    try (InputStream input = Files.newInputStream(archive);
        ZipInputStream zip = new ZipInputStream(input)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        String name = entry.getName();
        int slash = name.indexOf('/');
        String relative = slash >= 0 ? name.substring(slash + 1) : name;
        if (!REQUIRED_FILES.contains(relative)) continue;
        Path output = destination.resolve(relative).normalize();
        if (!output.getParent().equals(destination) || !extracted.add(relative))
          throw new IOException("unsafe model archive entry");
        Files.copy(zip, output);
      }
    }
  }

  private static void deleteTree(Path root) throws IOException {
    if (!Files.exists(root)) return;
    try (var paths = Files.walk(root)) {
      paths
          .sorted((left, right) -> right.compareTo(left))
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException error) {
                  throw new DeleteFailure(error);
                }
              });
    } catch (DeleteFailure error) {
      throw error.error;
    }
  }

  private static final class DeleteFailure extends RuntimeException {
    private final IOException error;

    private DeleteFailure(IOException error) {
      this.error = error;
    }
  }
}

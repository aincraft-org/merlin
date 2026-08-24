package dev.mintychochip.merlin.paper.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

final class ModelBundleFetcherTest {
  @Test
  void rejectsDevelopmentBundleWithExplicitReleaseMessage() throws Exception {
    Path archive = Files.createTempFile("merlin-model-", ".zip");
    writeArchive(archive, "2026.08.18.0", false);

    ModelBundleFetcher fetcher =
        new ModelBundleFetcher(
            "https://example.invalid/weights",
            "2026.08.18.0",
            Files.createTempDirectory("merlin-cache-"),
            ignored -> archive);

    IOException error = assertThrows(IOException.class, fetcher::ensureBundle);
    assertTrue(error.getMessage().contains("2026.08.18.0"));
    assertTrue(error.getMessage().contains("release_ready"));
  }

  @Test
  void rejectsUnsafeArchiveEntry() throws Exception {
    Path archive = Files.createTempFile("merlin-model-", ".zip");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      add(output, "bundle/../model.onnx", "unsafe");
    }
    ModelBundleFetcher fetcher =
        new ModelBundleFetcher(
            "https://example.invalid/weights",
            "2026.08.18.0",
            Files.createTempDirectory("merlin-cache-"),
            ignored -> archive);
    assertThrows(IOException.class, fetcher::ensureBundle);
  }

  private static void writeArchive(Path archive, String version, boolean releaseReady)
      throws IOException {
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      add(output, version + "/manifest.json", manifest(releaseReady));
      add(output, version + "/model.onnx", "onnx");
      add(output, version + "/model.pt", "pt");
      add(output, version + "/sha256sums.json", "{}");
    }
  }

  private static String manifest(boolean releaseReady) {
    return "{\"schema_id\":\"glyph-bundle-v1\",\"model_id\":\"model-v1\",\"catalog_id\":\"catalog-v1\",\"preprocessing_id\":\"preprocessing-v1\",\"training_id\":\"train-v1\",\"dataset_id\":\"dataset-v1\",\"release_ready\":"
        + releaseReady
        + ",\"opset\":17,\"labels\":[],\"input_schema\":{},\"output_schema\":{},\"calibration\":{},\"metrics\":{},\"golden_fixture\":{},\"files\":{}}";
  }

  private static void add(ZipOutputStream output, String name, String content) throws IOException {
    output.putNextEntry(new ZipEntry(name));
    output.write(content.getBytes());
    output.closeEntry();
  }
}

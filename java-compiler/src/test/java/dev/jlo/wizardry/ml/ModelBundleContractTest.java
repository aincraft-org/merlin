package dev.jlo.wizardry.ml;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

final class ModelBundleContractTest {
    @Test void validCompleteManifestLoads() throws Exception {
        assertDoesNotThrow(() -> ModelBundle.load(fixture(), true));
    }

    @Test void rejectsMissingOrMismatchedChecksumsAndTraversal() throws Exception {
        Path dir = fixture(); Files.delete(dir.resolve("model.onnx")); assertRejected(dir, true);
        dir = fixture(); Files.write(dir.resolve("model.onnx"), new byte[]{9, 8, 7}); assertRejected(dir, true);
        dir = fixture(); Files.writeString(dir.resolve("manifest.json"), Files.readString(dir.resolve("manifest.json")).replace("model.onnx", "../model.onnx")); assertRejected(dir, true);
    }

    @Test void rejectsLabelsCalibrationAndReleasePolicy() throws Exception {
        Path dir = fixture(); String m = Files.readString(dir.resolve("manifest.json"));
        Files.writeString(dir.resolve("manifest.json"), m.replace("\"reject\"]", "\"target-ray\"]")); assertRejected(dir, true);
        dir = fixture(); m = Files.readString(dir.resolve("manifest.json"));
        Files.writeString(dir.resolve("manifest.json"), m.replace("\"temperature\":1.0", "\"temperature\":0.0")); assertRejected(dir, true);
        Path fixtureOnly = fixture(); assertThrows(ModelBundle.BundleException.class, () -> ModelBundle.load(fixtureOnly));
    }

    private static void assertRejected(Path dir, boolean fixtures) { assertThrows(ModelBundle.BundleException.class, () -> ModelBundle.load(dir, fixtures)); }

    static Path fixture() throws Exception {
        Path d = Files.createTempDirectory("bundle"); byte[] model = new byte[]{1,2,3}; Files.write(d.resolve("model.onnx"), model);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(model));
        String labels = "[\"target-ray\",\"damage\",\"heal\",\"push\",\"cooldown\",\"self\",\"target\",\"physical\",\"fire\",\"frost\",\"arcane\",\"reject\"]";
        String manifest = "{\"schema_id\":\"glyph-bundle-v1\",\"model_id\":\"model-v1\",\"catalog_id\":\"glyph-catalog-v1\",\"preprocessing_id\":\"preprocessing-v1\",\"dataset_id\":\"dataset-v1\",\"training_id\":\"train-v1\",\"release_ready\":false,\"files\":{\"model.onnx\":\""+hash+"\"},\"labels\":"+labels+",\"input_schema\":{\"vectors\":{\"shape\":[null,64,32,8],\"dtype\":\"float32\"},\"mask\":{\"shape\":[null,64,32],\"dtype\":\"float32\"},\"raster\":{\"shape\":[null,1,64,64],\"dtype\":\"float32\"}},\"output_schema\":{\"logits\":{\"shape\":[null,12],\"dtype\":\"float32\"}},\"calibration\":{\"temperature\":1.0,\"top_threshold\":0.75,\"margin\":0.1},\"metrics\":{\"accuracy\":0.9},\"golden_fixture\":{\"input\":\"x\"},\"opset\":17}";
        Files.writeString(d.resolve("manifest.json"), manifest); return d;
    }
}

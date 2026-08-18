package dev.mintychochip.wizardry.api.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class ModelBundle {
    private static final List<String> LABEL_IDS = List.of(
            "target-ray", "damage", "heal", "push", "cooldown", "self", "target",
            "physical", "fire", "frost", "arcane",
            "on-hit", "on-hurt", "on-use", "periodic",
            "if-health", "if-undead", "if-outdoors",
            "shield", "attacker", "area", "repeat", "charges",
            "reject");
    private final Path model;
    private final List<Label> labels;
    private final double temperature;
    private final double topThreshold;
    private final double margin;

    private ModelBundle(Path model, List<Label> labels, double temperature, double topThreshold, double margin) {
        this.model = model; this.labels = List.copyOf(labels); this.temperature = temperature; this.topThreshold = topThreshold; this.margin = margin;
    }
    public Path modelPath() { return model; }
    public List<Label> labels() { return labels; }
    public double temperature() { return temperature; }
    public double topThreshold() { return topThreshold; }
    public double margin() { return margin; }
    public static ModelBundle load(Path directory) throws IOException { return load(directory, false); }
    static ModelBundle load(Path directory, boolean allowFixture) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        Path manifestPath = root.resolve("manifest.json");
        if (!Files.isRegularFile(manifestPath)) throw new BundleException("missing manifest");
        JsonNode manifest;
        try { manifest = new ObjectMapper().readTree(Files.readString(manifestPath)); }
        catch (Exception error) { throw new BundleException("invalid manifest", error); }
        requireText(manifest, "schema_id", "glyph-bundle-v1");
        for (String field : List.of("model_id", "catalog_id", "preprocessing_id", "training_id", "dataset_id")) if (manifest.path(field).asText().isBlank()) throw new BundleException("missing " + field);
        if (manifest.path("opset").asInt(-1) != 17) throw new BundleException("incompatible opset");
        boolean releaseReady = manifest.path("release_ready").asBoolean(false);
        if (!releaseReady && !allowFixture) throw new BundleException("bundle is not release ready");
        JsonNode labelArray = manifest.path("labels");
        if (!labelArray.isArray() || labelArray.size() != LABEL_IDS.size()) throw new BundleException("incompatible labels");
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < LABEL_IDS.size(); i++) { if (!LABEL_IDS.get(i).equals(labelArray.get(i).asText())) throw new BundleException("incompatible labels"); labels.add(Label.fromId(labelArray.get(i).asText())); }
        requireTensor(manifest.path("input_schema"), "vectors", List.of(-1, 64, 32, 8));
        requireTensor(manifest.path("input_schema"), "mask", List.of(-1, 64, 32));
        requireTensor(manifest.path("input_schema"), "raster", List.of(-1, 3, 64, 64));
        requireTensor(manifest.path("output_schema"), "logits", List.of(-1, LABEL_IDS.size()));
        JsonNode calibration = manifest.path("calibration");
        double temperature = finite(calibration, "temperature"), threshold = finite(calibration, "top_threshold"), margin = finite(calibration, "margin");
        if (temperature <= 0 || threshold < 0 || threshold > 1 || margin < 0 || margin > 1) throw new BundleException("invalid calibration");
        if (releaseReady && (threshold == 0 || margin == 0)) throw new BundleException("uncalibrated release bundle");
        if (!manifest.path("metrics").isObject() || manifest.path("metrics").isEmpty()) throw new BundleException("missing metrics");
        if (!manifest.path("golden_fixture").isObject() || manifest.path("golden_fixture").isEmpty()) throw new BundleException("missing golden fixture");
        JsonNode files = manifest.path("files");
        if (!files.isObject() || files.isEmpty()) throw new BundleException("missing file checksums");
        Path model = null;
        var fields = files.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next(); Path file = root.resolve(entry.getKey()).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new BundleException("missing bundle file");
            String expected = entry.getValue().asText(); if (expected.isBlank() || !expected.equalsIgnoreCase(sha256(file))) throw new BundleException("bundle checksum mismatch");
            if (entry.getKey().equals("model.onnx")) model = file;
        }
        if (model == null) throw new BundleException("missing model");
        return new ModelBundle(model, labels, temperature, threshold, margin);
    }
    private static void requireText(JsonNode node, String field, String expected) throws BundleException { if (!expected.equals(node.path(field).asText())) throw new BundleException("incompatible " + field); }
    private static void requireTensor(JsonNode schemas, String name, List<Integer> expected) throws BundleException {
        JsonNode tensor = schemas.path(name); if (!"float32".equals(tensor.path("dtype").asText())) throw new BundleException("incompatible tensor " + name);
        JsonNode shape = tensor.path("shape"); if (!shape.isArray() || shape.size() != expected.size()) throw new BundleException("incompatible tensor " + name);
        for (int i = 0; i < expected.size(); i++) if (expected.get(i) == -1 ? !shape.get(i).isNull() : shape.get(i).asInt(-1) != expected.get(i)) throw new BundleException("incompatible tensor " + name);
    }
    private static double finite(JsonNode node, String field) throws BundleException { JsonNode value = node.get(field); if (value == null || !value.isNumber() || !Double.isFinite(value.asDouble())) throw new BundleException("invalid calibration"); return value.asDouble(); }
    private static String sha256(Path path) throws IOException { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); } }
    public static final class BundleException extends IOException { public BundleException(String message) { super(message); } public BundleException(String message, Throwable cause) { super(message, cause); } }
}

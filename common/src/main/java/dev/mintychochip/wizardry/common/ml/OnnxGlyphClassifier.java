package dev.mintychochip.wizardry.common.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.ml.Classification;
import dev.mintychochip.wizardry.api.ml.ClassificationCandidate;
import dev.mintychochip.wizardry.api.ml.GlyphClassifier;
import dev.mintychochip.wizardry.api.ml.Label;
import dev.mintychochip.wizardry.api.ml.ModelBundle;
import dev.mintychochip.wizardry.common.ml.GlyphPreprocessor;
import dev.mintychochip.wizardry.common.ml.PreprocessedGlyph;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public final class OnnxGlyphClassifier implements GlyphClassifier, AutoCloseable {
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final ModelBundle bundle;
    private final GlyphPreprocessor preprocessor;

    public OnnxGlyphClassifier(ModelBundle bundle) throws OrtException {
        environment = OrtEnvironment.getEnvironment();
        session = environment.createSession(bundle.modelPath().toString(), new OrtSession.SessionOptions());
        this.bundle = bundle;
        preprocessor = new GlyphPreprocessor();
    }

    public Classification classify(GlyphDraft draft) {
        try {
            PreprocessedGlyph glyph = preprocessor.preprocess(draft);
            float[] vectors = new float[64 * 32 * 8];
            float[] mask = new float[64 * 32];
            float[] raster = new float[64 * 64];
            int offset = 0;
            for (float[][] stroke : glyph.vectors()) for (float[] point : stroke) for (float value : point) vectors[offset++] = value;
            offset = 0;
            for (boolean[] stroke : glyph.mask()) for (boolean value : stroke) mask[offset++] = value ? 1f : 0f;
            offset = 0;
            for (float[][] channel : glyph.raster()) for (float[] row : channel) for (float value : row) raster[offset++] = value;
            try (OnnxTensor vectorTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(vectors), new long[]{1, 64, 32, 8});
                 OnnxTensor maskTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(mask), new long[]{1, 64, 32});
                 OnnxTensor rasterTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(raster), new long[]{1, 1, 64, 64})) {
                var inputs = new LinkedHashMap<String, OnnxTensor>();
                inputs.put("vectors", vectorTensor);
                inputs.put("mask", maskTensor);
                inputs.put("raster", rasterTensor);
                try (OrtSession.Result result = session.run(inputs)) {
                    Object value = result.get("logits").orElseThrow().getValue();
                    if (!(value instanceof float[][] rows) || rows.length != 1 || rows[0].length != bundle.labels().size()) return Classification.rejected(List.of());
                    return classifyLogits(rows[0]);
                }
            }
        } catch (Exception failure) {
            return Classification.rejected(List.of());
        }
    }

    Classification classifyLogits(float[] logits) {
        if (logits.length != bundle.labels().size()) return Classification.rejected(List.of());
        double max = Double.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (!Float.isFinite(logit)) return Classification.rejected(List.of());
            max = Math.max(max, logit / bundle.temperature());
        }
        double sum = 0;
        double[] probabilities = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = Math.exp(logits[i] / bundle.temperature() - max);
            sum += probabilities[i];
        }
        if (!Double.isFinite(sum) || sum <= 0) return Classification.rejected(List.of());
        List<ClassificationCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < probabilities.length; i++) candidates.add(new ClassificationCandidate(bundle.labels().get(i), (float) (probabilities[i] / sum)));
        candidates.sort(Comparator.comparingDouble(ClassificationCandidate::score).reversed().thenComparing(candidate -> candidate.label().id()));
        ClassificationCandidate top = candidates.getFirst();
        double runnerUp = candidates.get(1).score();
        boolean accepted = top.label() != Label.REJECT && top.score() >= bundle.topThreshold() && top.score() - runnerUp >= bundle.margin();
        return new Classification(candidates, accepted);
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}

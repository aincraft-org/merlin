package dev.jlo.wizardry.mapgui;

import dev.jlo.wizardry.glyph.GlyphDraft;
import dev.jlo.wizardry.ml.Classification;
import dev.jlo.wizardry.ml.GlyphClassifier;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Schedules bounded, off-thread glyph inference and delivers results on the supplied executor. */
public final class GlyphClassificationService implements AutoCloseable {
    private final GlyphClassifier classifier;
    private final Executor mainThread;
    private final ExecutorService worker;

    public GlyphClassificationService(GlyphClassifier classifier, Executor mainThread) {
        this(classifier, mainThread, new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.AbortPolicy()));
    }

    GlyphClassificationService(GlyphClassifier classifier, Executor mainThread, ExecutorService worker) {
        this.classifier = Objects.requireNonNull(classifier);
        this.mainThread = Objects.requireNonNull(mainThread);
        this.worker = Objects.requireNonNull(worker);
    }

    public void classify(GlyphDraft draft, Consumer<Classification> callback) {
        Objects.requireNonNull(draft);
        Objects.requireNonNull(callback);
        try {
            worker.execute(() -> {
                Classification result;
                try {
                    result = classifier.classify(draft);
                    if (result == null) result = Classification.rejected(List.of());
                } catch (Throwable failure) {
                    result = Classification.rejected(List.of());
                }
                var delivered = result;
                try { mainThread.execute(() -> callback.accept(delivered)); }
                catch (RuntimeException ignored) { }
            });
        } catch (RejectedExecutionException ignored) {
            try { mainThread.execute(() -> callback.accept(Classification.rejected(List.of()))); }
            catch (RuntimeException ignoredAgain) { }
        }
    }

    @Override public void close() { worker.shutdownNow(); }
}

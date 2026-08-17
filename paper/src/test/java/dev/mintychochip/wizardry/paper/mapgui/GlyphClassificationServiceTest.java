package dev.mintychochip.wizardry.paper.mapgui;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.glyph.GlyphPoint;
import dev.mintychochip.wizardry.api.glyph.GlyphStroke;
import dev.mintychochip.wizardry.api.ml.Classification;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class GlyphClassificationServiceTest {
    @Test void runsInferenceOffCallerAndDeliversThroughMainExecutor() throws Exception {
        Thread caller = Thread.currentThread();
        var inferenceThread = new AtomicReference<Thread>();
        var mainQueue = new ArrayBlockingQueue<Runnable>(1);
        var delivered = new AtomicReference<Classification>();
        var worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        try (var service = new GlyphClassificationService(draft -> {
            inferenceThread.set(Thread.currentThread());
            return Classification.rejected(List.of());
        }, mainQueue::add, worker)) {
            service.classify(draft(), delivered::set);
            Runnable callback = mainQueue.poll(2, TimeUnit.SECONDS);
            assertNotNull(callback);
            assertNotEquals(caller, inferenceThread.get());
            assertNull(delivered.get());
            callback.run();
            assertNotNull(delivered.get());
        }
    }

    @Test void classifierFailureReturnsSafeRejection() throws Exception {
        var mainQueue = new ArrayBlockingQueue<Runnable>(1);
        try (var service = new GlyphClassificationService(draft -> { throw new IllegalStateException(); }, mainQueue::add)) {
            var result = new AtomicReference<Classification>();
            service.classify(draft(), result::set);
            Runnable callback = mainQueue.poll(2, TimeUnit.SECONDS);
            assertNotNull(callback);
            callback.run();
            assertFalse(result.get().accepted());
        }
    }

    private static GlyphDraft draft() {
        return new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(1, 1), new GlyphPoint(2, 2)), 1, 0)));
    }
}

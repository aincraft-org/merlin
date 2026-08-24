package dev.mintychochip.merlin.paper.book;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.merlin.api.dsl.CompileResult;
import dev.mintychochip.merlin.common.dsl.ScribeCompiler;
import org.junit.jupiter.api.Test;

final class ScribeBookStoreTest {
    @Test
    void starterSourceIsALegalPhrasebookPage() {
        assertInstanceOf(CompileResult.Ok.class,
                ScribeCompiler.INSTANCE.compile(ScribeBookStore.STARTER_SOURCE));
        assertTrue(ScribeBookStore.STARTER_SOURCE.contains("look ahead"));
        assertTrue(ScribeBookStore.STARTER_SOURCE.contains("burn target"));
    }
}

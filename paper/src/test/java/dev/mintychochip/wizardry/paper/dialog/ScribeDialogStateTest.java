package dev.mintychochip.wizardry.paper.dialog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.paper.book.ScribeBookStore;
import io.papermc.paper.dialog.DialogResponseView;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class ScribeDialogStateTest {
    @Test void constantsMatchClientContract() {
        assertEquals(1024, ScribeDialog.WIDTH_PIXELS);
        assertEquals(512, ScribeDialog.HEIGHT_PIXELS);
        assertEquals(128, ScribeDialog.MAX_LINES);
        assertEquals(4096, ScribeDialog.MAX_SCALARS);
        assertEquals(15, ScribeDialog.MAX_CALLBACK_LIFETIME.toMinutes());
    }
    @Test void inputLimitsAreScalarAndLineBased() {
        var dialog = new ScribeDialog(null);
        assertTrue(dialog.validInput("é".repeat(4096)));
        assertFalse(dialog.validInput("é".repeat(4097)));
        assertTrue(dialog.validInput("x\n".repeat(128)));
        assertFalse(dialog.validInput("x\n".repeat(129)));
    }

    @Test
    void sourceKeyAndLimitsMatchDialogContract() {
        assertEquals("source", ScribeDialog.SOURCE_KEY);
        assertEquals(1024, ScribeDialog.WIDTH_PIXELS);
        assertEquals(512, ScribeDialog.HEIGHT_PIXELS);
        assertEquals(128, ScribeDialog.MAX_LINES);
        assertEquals(4096, ScribeDialog.MAX_SCALARS);
    }

    @Test
    void readsPreservedNewlinesFromResponse() {
        var view = mock(DialogResponseView.class);
        when(view.getText("source")).thenReturn("summon sheep\n    riding rocket");
        assertEquals("summon sheep\n    riding rocket", ScribeDialog.readSource(view));
    }

    @Test
    void saveAndCastWithErrorDoesNotCallCaster() {
        var books = mock(ScribeBookStore.class);
        var book = mock(ItemStack.class);
        var bookId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        when(books.bookId(book)).thenReturn(bookId);
        when(books.source(book)).thenReturn("");
        when(books.save(eq(book), eq(bookId), eq("send skyward"))).thenReturn(true);

        var cast = new AtomicBoolean();
        var dialog = new ScribeDialog(books, (id, compilation) -> {
            cast.set(true);
            return true;
        });
        assertNotNull(dialog.open(playerId, book, 0L));

        var outcome = dialog.submit(playerId, book, "send skyward", ScribeDialog.Action.SAVE_AND_CAST, 0L);

        assertFalse(outcome.cast());
        assertInstanceOf(CompileResult.Error.class, outcome.compilation());
        assertFalse(cast.get());
        verify(books).save(book, bookId, "send skyward");
    }
}

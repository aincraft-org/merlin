package dev.mintychochip.wizardry.paper.dialog;

import static org.junit.jupiter.api.Assertions.*;

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
}

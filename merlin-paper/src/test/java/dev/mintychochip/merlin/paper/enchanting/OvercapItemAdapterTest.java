package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class OvercapItemAdapterTest {
    @Test
    void formatsRomanNumerals() {
        assertEquals("I", QuantaRollEngine.toRoman(1));
        assertEquals("IV", QuantaRollEngine.toRoman(4));
        assertEquals("VI", QuantaRollEngine.toRoman(6));
        assertEquals("VII", QuantaRollEngine.toRoman(7));
    }
}

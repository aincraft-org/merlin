package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class MutableExperienceTest {
    @Test
    void clampsSetAndAddOperationsToNonNegativeAmounts() {
        MutableExperience experience = new MutableExperience(5);

        experience.add(-10);
        assertEquals(0, experience.getAmount());

        experience.setAmount(-3);
        assertEquals(0, experience.getAmount());

        experience.add(7);
        assertEquals(7, experience.getAmount());
    }
}

package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class GrindListenerTest {
    @Test
    void routesMortarAndMappedFlowerToElement() {
        MortarPestle mortar = mock(MortarPestle.class);
        ItemStack tool = mock(ItemStack.class);
        ItemStack flower = mock(ItemStack.class);
        when(mortar.isMortar(tool)).thenReturn(true);
        when(flower.getType()).thenReturn(Material.TORCHFLOWER);

        assertEquals(
                Optional.of(GlyphElement.FLAME),
                GrindListener.grindElement(tool, flower, mortar));
    }

    @Test
    void rejectsWrongToolUnmappedFlowerAndNullItems() {
        MortarPestle mortar = mock(MortarPestle.class);
        ItemStack tool = mock(ItemStack.class);
        when(tool.getType()).thenReturn(Material.BOWL);

        ItemStack mapped = mock(ItemStack.class);
        when(mapped.getType()).thenReturn(Material.POPPY);
        when(mortar.isMortar(tool)).thenReturn(false);

        assertTrue(GrindListener.grindElement(tool, mapped, mortar).isEmpty());

        ItemStack mortarItem = mock(ItemStack.class);
        when(mortar.isMortar(mortarItem)).thenReturn(true);
        ItemStack unmapped = mock(ItemStack.class);
        when(unmapped.getType()).thenReturn(Material.ROSE_BUSH);

        assertTrue(GrindListener.grindElement(mortarItem, unmapped, mortar).isEmpty());
        assertTrue(GrindListener.grindElement(null, null, mortar).isEmpty());
    }
}

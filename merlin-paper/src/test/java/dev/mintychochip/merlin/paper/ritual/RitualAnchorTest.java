package dev.mintychochip.merlin.paper.ritual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class RitualAnchorTest {
    @Test
    void recognizesMarkedAnchorTile() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        when(plugin.namespace()).thenReturn("merlin");
        RitualAnchor anchor = new RitualAnchor(plugin);

        TileState state = mock(TileState.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(state.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(new NamespacedKey("merlin", "ritual_anchor"), PersistentDataType.BYTE)).thenReturn(true);

        assertTrue(anchor.isAnchor(state));

        TileState plain = mock(TileState.class);
        PersistentDataContainer plainPdc = mock(PersistentDataContainer.class);
        when(plain.getPersistentDataContainer()).thenReturn(plainPdc);
        assertFalse(anchor.isAnchor(plain));
        assertFalse(anchor.isAnchor(null));
    }
}

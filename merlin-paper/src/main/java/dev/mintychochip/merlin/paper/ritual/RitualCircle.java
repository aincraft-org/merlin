package dev.mintychochip.merlin.paper.ritual;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.inventory.Inventory;

public final class RitualCircle {
    private static final BlockFace[] ADJACENT = {
            BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST,
            BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    private RitualCircle() {}

    public static Optional<RitualLayout> inspect(Block anchor, RitualAnchor anchorMarker, RitualPedestal pedestalMarker) {
        if (anchor.getType() != Material.DROPPER) return Optional.empty();
        if (!(anchor.getState() instanceof TileState tile)) return Optional.empty();
        if (!anchorMarker.isAnchor(tile)) return Optional.empty();
        if (!(tile instanceof Container anchorContainer)) return Optional.empty();

        List<Inventory> pedestals = new ArrayList<>();
        for (BlockFace face : ADJACENT) {
            Block relative = anchor.getRelative(face);
            if (relative.getType() != Material.DROPPER) continue;
            if (!(relative.getState() instanceof TileState relTile)) continue;
            if (!pedestalMarker.isPedestal(relTile)) continue;
            if (!(relTile instanceof Container container)) continue;
            pedestals.add(container.getInventory());
        }

        if (pedestals.isEmpty()) return Optional.empty();
        return Optional.of(new RitualLayout(anchorContainer.getInventory(), pedestals));
    }
}

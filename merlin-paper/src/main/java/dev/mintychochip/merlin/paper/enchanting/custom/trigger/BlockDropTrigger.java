package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import java.util.List;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public interface BlockDropTrigger {
    void onBlockDrop(Player player, BlockState blockState, List<Item> items, int level);
}

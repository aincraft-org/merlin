package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

public interface BlockBreakTrigger {
    void onBlockBreak(Player player, Block block, int level, CascadeScope scope);

    default void onBlockBreakPost(Player player, BlockState state, int level) {}
}

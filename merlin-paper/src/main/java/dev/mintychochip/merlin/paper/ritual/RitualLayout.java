package dev.mintychochip.merlin.paper.ritual;

import java.util.List;
import org.bukkit.inventory.Inventory;

public record RitualLayout(Inventory anchor, List<Inventory> pedestals) {
}

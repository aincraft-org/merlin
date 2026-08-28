package dev.mintychochip.merlin.paper.enchanting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class AltarRerollService {
    public sealed interface Result {
        record Success(int remainingLapis) implements Result {}
        record Failure(String reason) implements Result {}
    }

    public static Result processReroll(boolean isClosed, ItemStack lapis) {
        if (isClosed) {
            return new Result.Failure("Session is closed.");
        }
        if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < 1) {
            return new Result.Failure("You need at least 1 Lapis Lazuli to reroll offers!");
        }

        int newAmount = lapis.getAmount() - 1;
        lapis.setAmount(newAmount);
        return new Result.Success(newAmount);
    }

    private AltarRerollService() {}
}

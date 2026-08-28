package dev.mintychochip.merlin.paper.enchanting;

import org.bukkit.Material;

public final class AltarRerollService {
    public sealed interface Result {
        record Success(int newAmount) implements Result {}
        record Failure(String reason) implements Result {}
    }

    public static Result processReroll(boolean isClosed, Material material, int amount) {
        if (isClosed) {
            return new Result.Failure("Session is closed.");
        }
        if (material != Material.LAPIS_LAZULI || amount < 1) {
            return new Result.Failure("You need at least 1 Lapis Lazuli to reroll offers!");
        }
        return new Result.Success(amount - 1);
    }

    private AltarRerollService() {}
}

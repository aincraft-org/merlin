package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.AbstractHorse;

public interface HorseJumpTrigger {
    float onHorseJump(AbstractHorse horse, float power, int level);
}

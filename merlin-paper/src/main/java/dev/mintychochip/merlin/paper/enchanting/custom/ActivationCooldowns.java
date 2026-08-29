package dev.mintychochip.merlin.paper.enchanting.custom;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.NamespacedKey;

/** Main-thread activation admission keyed by player and enchantment. */
public final class ActivationCooldowns {
    private final Clock clock;
    private final Map<ActivationKey, Instant> cooldowns = new HashMap<>();

    public ActivationCooldowns() {
        this(Clock.systemUTC());
    }

    public ActivationCooldowns(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized boolean tryAcquire(UUID playerId, NamespacedKey enchantment, Duration duration) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(enchantment, "enchantment");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("activation cooldown cannot be negative");
        }

        Instant now = clock.instant();
        cooldowns.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        ActivationKey key = new ActivationKey(playerId, enchantment);
        Instant expiresAt = cooldowns.get(key);
        if (expiresAt != null) {
            return false;
        }
        if (duration.isZero()) {
            cooldowns.remove(key);
        } else {
            cooldowns.put(key, now.plus(duration));
        }
        return true;
    }

    public synchronized void purgeExpired() {
        Instant now = clock.instant();
        cooldowns.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    synchronized void clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        cooldowns.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    synchronized void release(UUID playerId, NamespacedKey enchantment) {
        cooldowns.remove(new ActivationKey(playerId, enchantment));
    }

    private record ActivationKey(UUID playerId, NamespacedKey enchantment) {}
}

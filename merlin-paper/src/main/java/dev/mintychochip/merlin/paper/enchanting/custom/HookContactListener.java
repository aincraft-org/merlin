package dev.mintychochip.merlin.paper.enchanting.custom;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Tracks active fishing hooks and dispatches {@code HookContactTrigger} when a hook
 * contacts a living entity. Driven by a periodic tick task (spec §5.4).
 * Each hook/entity pair is dispatched once per contact; stale hooks are removed.
 */
public final class HookContactListener {
    private final CustomEnchantmentDispatcher dispatcher;
    private final Map<UUID, HookState> hooks = new ConcurrentHashMap<>();
    private final Set<String> contactedEntities = ConcurrentHashMap.newKeySet();

    private record HookState(Player owner, ItemStack rod) {}

    public HookContactListener(CustomEnchantmentDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /** Registers a hook when it is cast. */
    public void register(UUID hookId, Player owner, ItemStack rod) {
        if (hookId == null || owner == null || rod == null) return;
        hooks.put(hookId, new HookState(owner, rod));
    }

    /** Removes a hook when fishing ends. */
    public void unregister(UUID hookId) {
        if (hookId != null) hooks.remove(hookId);
    }

    /** Periodic tick: checks each active hook for entity contact and dispatches each pair once. */
    public void tick() {
        for (var entry : hooks.entrySet()) {
            UUID hookId = entry.getKey();
            HookState state = entry.getValue();
            Player owner = state.owner();
            if (owner == null || !owner.isOnline()) continue;
            Entity hookEntity = owner.getWorld() == null ? null : owner.getWorld().getEntity(hookId);
            if (!(hookEntity instanceof FishHook hook)) {
                // Stale hook no longer in world: drop it.
                hooks.remove(hookId);
                continue;
            }
            if (!hook.isValid()) {
                hooks.remove(hookId);
                continue;
            }

            Collection<Entity> nearby = hook.getNearbyEntities(1.0, 1.0, 1.0);
            for (Entity entity : nearby) {
                if (entity == owner || !(entity instanceof LivingEntity living)) continue;
                if (living.isDead()) continue;
                UUID entityId = living.getUniqueId();
                if (!contactedEntities.add(hookId + ":" + entityId)) continue;
                dispatcher.dispatchHookContact(owner, living, state.rod());
            }
        }
    }

    /** Clears all state (e.g. on plugin disable). */
    public void clear() {
        hooks.clear();
        contactedEntities.clear();
    }
}
package dev.mintychochip.wizardry.paper.runtime;

import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.api.dsl.Operation;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class SpellRuntime {
    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean cast(Player caster, LivingEntity target, CompiledSpell spell, long nowMillis) {
        return cast(caster, target, spell, nowMillis, 32.0);
    }

    public boolean cast(Player caster, LivingEntity target, CompiledSpell spell, long nowMillis, double rayRange) {
        if (caster == null || !caster.isValid() || spell == null) return false;
        String key = caster.getUniqueId() + ":" + spell.identitySha256();
        if (cooldowns.getOrDefault(key, 0L) > nowMillis) return false;
        boolean needsTarget = spell.operations().stream().anyMatch(op -> op instanceof Operation.Damage || op instanceof Operation.Push);
        if (needsTarget && (target == null || !target.isValid())) return false;
        for (var operation : spell.operations()) {
            if (operation instanceof Operation.TargetRay x && (rayRange < 0 || x.range() > rayRange)) return false;
            if (operation instanceof Operation.Damage x && x.target() != Operation.Target.TARGET) return false;
            if (operation instanceof Operation.Heal x && x.target() != Operation.Target.SELF) return false;
            if (operation instanceof Operation.Push x && x.target() != Operation.Target.TARGET) return false;
        }
        long cooldownUntil = 0;
        for (var operation : spell.operations()) {
            switch (operation) {
                case Operation.TargetRay ignored -> { }
                case Operation.Damage x -> {
                    target.damage(x.amount(), caster);
                    if (x.damageType() == Operation.DamageType.FIRE) target.setFireTicks(Math.max(target.getFireTicks(), (int) Math.round(x.amount() * 20)));
                }
                case Operation.Heal x -> caster.setHealth(Math.min(caster.getMaxHealth(), caster.getHealth() + x.amount()));
                case Operation.Push x -> {
                    var delta = target.getLocation().toVector().subtract(caster.getLocation().toVector());
                    if (delta.lengthSquared() > 0) target.setVelocity(delta.normalize().multiply(x.strength()));
                }
                case Operation.Cooldown x -> cooldownUntil = Math.max(cooldownUntil, nowMillis + Math.round(x.seconds() * 1000));
            }
        }
        if (cooldownUntil > nowMillis) cooldowns.put(key, cooldownUntil);
        return true;
    }

    public boolean onCooldown(Player caster, CompiledSpell spell, long nowMillis) {
        return cooldowns.getOrDefault(caster.getUniqueId() + ":" + spell.identitySha256(), 0L) > nowMillis;
    }
}

package dev.mintychochip.wizardry.paper.runtime;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public final class SpellRuntime {
    private static final NamespacedKey SUMMON_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("wizardry:scribe_summon"));

    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean cast(Player caster, LivingEntity target, CompiledSpell spell, long nowMillis) {
        return cast(caster, target, spell, nowMillis, lookRange(spell));
    }

    public boolean cast(Player caster, LivingEntity target, CompiledSpell spell, long nowMillis, double rayRange) {
        if (caster == null || !caster.isValid() || spell == null) {
            return false;
        }
        String key = caster.getUniqueId() + ":" + spell.identitySha256();
        if (cooldowns.getOrDefault(key, 0L) > nowMillis) {
            return false;
        }
        boolean needsTarget = spell.actions().stream().anyMatch(SpellRuntime::needsLookedAtTarget);
        if (needsTarget && (target == null || !target.isValid())) {
            return false;
        }
        double look = rayRange > 0 ? rayRange : lookRange(spell);
        World world = caster.getWorld();
        Entity lastVehicle = null;
        long cooldownUntil = 0;
        for (var action : spell.actions()) {
            switch (action) {
                case Action.LookAhead ignored -> { }
                case Action.Burn x -> applyBurn(x, caster, target);
                case Action.Mend x -> applyMend(x, caster, target);
                case Action.Shove x -> applyShove(x, caster, target);
                case Action.Strike x -> world.strikeLightning(resolvePlace(x.place(), caster, target, look));
                case Action.Summon x -> lastVehicle = spawnSummon(x, caster, target, look);
                case Action.SendSkyward ignored -> {
                    if (lastVehicle != null) {
                        lastVehicle.setVelocity(new Vector(0, 1.5, 0));
                    }
                }
                case Action.Vanish x -> applyVanish(x, caster, target);
                case Action.Rest x -> cooldownUntil = Math.max(cooldownUntil, nowMillis + Math.round(x.seconds() * 1000));
            }
        }
        if (cooldownUntil > nowMillis) {
            cooldowns.put(key, cooldownUntil);
        }
        return true;
    }

    public boolean onCooldown(Player caster, CompiledSpell spell, long nowMillis) {
        return cooldowns.getOrDefault(caster.getUniqueId() + ":" + spell.identitySha256(), 0L) > nowMillis;
    }

    private static boolean needsLookedAtTarget(Action action) {
        return switch (action) {
            case Action.Burn x -> x.patient() == Action.Patient.TARGET;
            case Action.Mend x -> x.patient() == Action.Patient.TARGET;
            case Action.Shove x -> x.patient() == Action.Patient.TARGET;
            case Action.Vanish x -> x.patient() == Action.Patient.TARGET;
            case Action.Summon x -> x.place() instanceof Action.Place.Target;
            case Action.Strike x -> x.place() instanceof Action.Place.Target;
            default -> false;
        };
    }

    private static double lookRange(CompiledSpell spell) {
        return spell.actions().stream()
                .filter(action -> action instanceof Action.LookAhead)
                .mapToDouble(action -> ((Action.LookAhead) action).range())
                .findFirst()
                .orElse(32);
    }

    private static void applyBurn(Action.Burn action, Player caster, LivingEntity target) {
        LivingEntity entity = patient(action.patient(), caster, target);
        entity.damage(action.amount(), caster);
        entity.setFireTicks(Math.max(entity.getFireTicks(), (int) Math.round(action.amount() * 20)));
    }

    private static void applyMend(Action.Mend action, Player caster, LivingEntity target) {
        LivingEntity entity = patient(action.patient(), caster, target);
        entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + action.amount()));
    }

    private static void applyShove(Action.Shove action, Player caster, LivingEntity target) {
        LivingEntity entity = patient(action.patient(), caster, target);
        var delta = entity.getLocation().toVector().subtract(caster.getLocation().toVector());
        if (delta.lengthSquared() > 0) {
            entity.setVelocity(delta.normalize().multiply(action.amount()));
        }
    }

    private static void applyVanish(Action.Vanish action, Player caster, LivingEntity target) {
        patient(action.patient(), caster, target).setInvisible(true);
    }

    private static LivingEntity patient(Action.Patient patient, Player caster, LivingEntity target) {
        return patient == Action.Patient.SELF ? caster : target;
    }

    private static Entity spawnSummon(Action.Summon action, Player caster, LivingEntity target, double look) {
        Location place = resolvePlace(action.place(), caster, target, look);
        World world = Objects.requireNonNull(place.getWorld());
        Entity vehicle = null;
        if (action.riding() != null) {
            vehicle = spawnTagged(world, place, action.riding());
        }
        Entity passenger = spawnTagged(world, place, action.noun());
        if (vehicle != null) {
            vehicle.addPassenger(passenger);
            return vehicle;
        }
        return passenger;
    }

    private static Entity spawnTagged(World world, Location place, Action.Noun noun) {
        Entity entity = world.spawnEntity(place, entityType(noun));
        entity.getPersistentDataContainer().set(SUMMON_KEY, PersistentDataType.BYTE, (byte) 1);
        return entity;
    }

    private static EntityType entityType(Action.Noun noun) {
        return switch (noun) {
            case SHEEP -> EntityType.SHEEP;
            case ROCKET -> EntityType.FIREWORK_ROCKET;
            case FANGS -> EntityType.EVOKER_FANGS;
        };
    }

    private static Location resolvePlace(Action.Place place, Player caster, LivingEntity target, double look) {
        return switch (place) {
            case Action.Place.Caster ignored -> caster.getLocation();
            case Action.Place.Self ignored -> caster.getLocation();
            case Action.Place.Target ignored -> target.getLocation();
            case Action.Place.Ahead ahead -> caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(ahead.range()));
        };
    }
}

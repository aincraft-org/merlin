package dev.mintychochip.wizardry.paper.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.common.dsl.ScribeCompiler;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

final class SpellRuntimeTest {
    @Test
    void missingTargetIsAtomic() {
        var world = mock(World.class);
        var caster = mock(Player.class);
        when(caster.isValid()).thenReturn(true);
        when(caster.getWorld()).thenReturn(world);
        var spell = compile("summon sheep at target\nburn target");
        var runtime = new SpellRuntime();
        assertFalse(runtime.cast(caster, null, spell, 0L));
        verify(world, never()).spawnEntity(any(), any());
    }

    @Test
    void ridingSpawnsVehicleThenPassenger() {
        var world = mock(World.class);
        var place = new Location(world, 0, 64, 0);
        var caster = player(world, place);
        var rocket = entity();
        var sheep = entity();
        when(world.spawnEntity(any(Location.class), eq(EntityType.FIREWORK_ROCKET))).thenReturn(rocket);
        when(world.spawnEntity(any(Location.class), eq(EntityType.SHEEP))).thenReturn(sheep);

        var spell = compile("summon sheep\n    riding rocket\nsend skyward");
        assertTrue(new SpellRuntime().cast(caster, null, spell, 0L));

        var order = inOrder(world, rocket);
        order.verify(world).spawnEntity(any(Location.class), eq(EntityType.FIREWORK_ROCKET));
        order.verify(world).spawnEntity(any(Location.class), eq(EntityType.SHEEP));
        order.verify(rocket).addPassenger(sheep);
        order.verify(rocket).setVelocity(new Vector(0, 1.5, 0));
    }

    @Test
    void strikeUsesTargetLocation() {
        var world = mock(World.class);
        var casterPlace = new Location(world, 0, 64, 0);
        var targetPlace = new Location(world, 8, 64, 2);
        var caster = player(world, casterPlace);
        var target = living(world, targetPlace);

        var spell = compile("strike target");
        assertTrue(new SpellRuntime().cast(caster, target, spell, 0L));

        verify(world).strikeLightning(targetPlace);
    }

    @Test
    void vanishClearsAfterDuration() {
        var tasks = new ArrayList<Runnable>();
        var runtime = new SpellRuntime((delay, task) -> {
            assertEquals(60L, delay);
            tasks.add(task);
        });
        var world = mock(World.class);
        var caster = player(world, new Location(world, 0, 64, 0));
        var spell = compile("vanish self for 3 seconds");

        assertTrue(runtime.cast(caster, null, spell, 0L));
        verify(caster).setInvisible(true);
        verify(caster, never()).setInvisible(false);
        assertEquals(1, tasks.size());

        tasks.forEach(Runnable::run);
        verify(caster).setInvisible(false);
    }

    @Test
    void restBlocksSecondCast() {
        var world = mock(World.class);
        var place = new Location(world, 0, 64, 0);
        var caster = player(world, place);
        var target = living(world, new Location(world, 3, 64, 0));
        var spell = compile("burn target\nrest 3 seconds");
        var runtime = new SpellRuntime();

        assertTrue(runtime.cast(caster, target, spell, 0L));
        assertTrue(runtime.onCooldown(caster, spell, 1_000L));
        assertFalse(runtime.cast(caster, target, spell, 1_000L));
        assertTrue(runtime.cast(caster, target, spell, 3_001L));
    }

    private static CompiledSpell compile(String src) {
        return ((CompileResult.Ok) ScribeCompiler.INSTANCE.compile(src)).spell();
    }

    private static Player player(World world, Location location) {
        var player = mock(Player.class);
        stubLiving(player, world, location);
        when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return player;
    }

    private static LivingEntity living(World world, Location location) {
        var entity = mock(LivingEntity.class);
        stubLiving(entity, world, location);
        return entity;
    }

    private static void stubLiving(LivingEntity entity, World world, Location location) {
        when(entity.isValid()).thenReturn(true);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getEyeLocation()).thenReturn(location);
        when(entity.getMaxHealth()).thenReturn(20.0);
        when(entity.getHealth()).thenReturn(20.0);
        when(entity.getFireTicks()).thenReturn(0);
        when(entity.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
    }

    private static Entity entity() {
        var entity = mock(Entity.class);
        when(entity.isValid()).thenReturn(true);
        when(entity.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        return entity;
    }
}

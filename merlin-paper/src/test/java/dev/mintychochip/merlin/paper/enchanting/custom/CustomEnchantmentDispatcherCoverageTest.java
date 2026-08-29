package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentDispatcherCoverageTest {
    private static final NamespacedKey KEY = new NamespacedKey("merlin", "all-triggers");

    @Test
    void dispatchesEveryRegisteredTriggerContract() {
        RecordingHandler handler = new RecordingHandler();
        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                KEY, "All Triggers", 0, 3, 10, 5, 10,
                Set.of(Material.DIAMOND_SWORD), Optional.of(handler)));
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack item = mock(ItemStack.class);
        when(adapter.readOvercap(any(ItemStack.class))).thenReturn(Map.of(KEY, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        LivingEntity living = mock(LivingEntity.class);
        Entity entity = mock(Entity.class);
        Projectile projectile = mock(Projectile.class);
        Block block = mock(Block.class);
        BlockState blockState = mock(BlockState.class);
        Item dropEntity = mock(Item.class);
        FishHook hook = mock(FishHook.class);
        Location from = mock(Location.class);
        Location to = mock(Location.class);
        MutableDamage damage = new MutableDamage(10.0);

        dispatcher.dispatchEntityHit(player, living, damage, item);
        dispatcher.dispatchEntityHitByEntity(living, entity, damage, new ItemStack[]{item});
        dispatcher.dispatchArmorDefense(player, entity, damage, new ItemStack[]{item});
        dispatcher.dispatchEnvironmentalDamage(player, DamageCause.FIRE, damage, new ItemStack[]{item});
        dispatcher.dispatchPlayerDrop(player, item);
        dispatcher.dispatchFoodLevelChange(player, 10, 5, new ItemStack[]{item});
        dispatcher.dispatchHorseJump(mock(org.bukkit.entity.AbstractHorse.class), 0.5f, new ItemStack[]{item});
        dispatcher.dispatchEntityEnvironmentalDamage(living, DamageCause.FIRE, damage, new ItemStack[]{item});

        dispatcher.dispatchEntityKill(player, living, new ArrayList<>(), new MutableExperience(3), item);
        dispatcher.dispatchBowShoot(player, entity, item, 1.0f);
        dispatcher.dispatchProjectileHit(player, projectile, entity, block, item);
        dispatcher.dispatchBlockBreak(player, block, item, mock(CascadeScope.class));
        dispatcher.dispatchBlockDrop(player, blockState, List.of(dropEntity), item);
        dispatcher.dispatchBlockPlace(player, block, block, item);
        dispatcher.dispatchPlayerFish(player, hook, entity, State.CAUGHT_ENTITY, item);
        dispatcher.dispatchShearEntity(player, entity, item, EquipmentSlot.HAND);
        dispatcher.dispatchBucketEmpty(player, block, BlockFace.UP, item, EquipmentSlot.HAND);
        dispatcher.dispatchBucketFill(player, block, BlockFace.UP, item, EquipmentSlot.HAND);
        assertEquals(2, dispatcher.dispatchItemDamage(player, item, 2));
        assertEquals(2, dispatcher.dispatchEntityItemDamage(entity, item, 2));
        dispatcher.dispatchItemConsume(player, item);
        dispatcher.dispatchActiveInteract(player, Action.RIGHT_CLICK_AIR, block, item);
        assertTrue(dispatcher.dispatchActivate(player, item));
        dispatcher.dispatchPlayerMove(player, from, to, new ItemStack[]{item});
        dispatcher.dispatchEntityMove(living, from, to, new ItemStack[]{item});
        dispatcher.dispatchPlayerJump(player, from, to, item);
        dispatcher.dispatchToggleGlide(player, true, item);
        dispatcher.dispatchToggleSneak(player, true, new ItemStack[]{item});
        dispatcher.dispatchToggleSprint(player, true, item);
        dispatcher.dispatchEntityInteract(player, entity, item, EquipmentSlot.HAND);
        assertEquals(2, dispatcher.dispatchExpGain(player, 2, new ItemStack[]{item}));

        assertEquals(Set.of(
                "entityHit", "entityHitByEntity", "armorDefense", "environmentalDamage", "playerDrop",
                "foodLevelChange", "horseJump", "entityEnvironmentalDamage", "entityKill", "bowShoot",
                "projectileHit", "blockBreak", "blockDrop", "blockPlace", "playerFish", "shearEntity",
                "bucketEmpty", "bucketFill", "itemDamage", "entityItemDamage", "itemConsume", "activeInteract",
                "activate", "playerMove", "entityMove", "playerJump", "toggleGlide", "toggleSneak",
                "toggleSprint", "entityInteract", "expGain"), handler.seen);
    }

    private static final class RecordingHandler implements
            OvercapEffectHandler,
            EntityHitTrigger,
            EntityHitByEntityTrigger,
            ArmorDefenseTrigger,
            EnvironmentalDamageTrigger,
            EntityEnvironmentalDamageTrigger,
            EntityKillTrigger,
            PlayerDropItemTrigger,
            FoodLevelChangeTrigger,
            HorseJumpTrigger,
            BowShootTrigger,
            ProjectileHitTrigger,
            BlockBreakTrigger,
            BlockDropTrigger,
            BlockPlaceTrigger,
            PlayerFishTrigger,
            ShearEntityTrigger,
            BucketEmptyTrigger,
            BucketFillTrigger,
            ItemDamageTrigger,
            EntityItemDamageTrigger,
            ItemConsumeTrigger,
            ActiveInteractTrigger,
            ActivateTrigger,
            PlayerMoveTrigger,
            EntityMoveTrigger,
            PlayerJumpTrigger,
            PlayerToggleGlideTrigger,
            PlayerToggleSneakTrigger,
            PlayerToggleSprintTrigger,
            EntityInteractTrigger,
            ExpGainTrigger {
        private final Set<String> seen = new java.util.LinkedHashSet<>();

        @Override
        public NamespacedKey key() {
            return KEY;
        }

        @Override
        public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
            seen.add("entityHit");
        }

        @Override
        public void onEntityHitByEntity(LivingEntity victim, Entity attacker, MutableDamage damage, int level) {
            seen.add("entityHitByEntity");
        }

        @Override
        public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
            seen.add("armorDefense");
        }

        @Override
        public void onEnvironmentalDamage(Player player, DamageCause cause, MutableDamage damage, int level) {
            seen.add("environmentalDamage");
        }

        @Override
        public void onEnvironmentalDamage(LivingEntity entity, DamageCause cause, MutableDamage damage, int level) {
            seen.add("entityEnvironmentalDamage");
        }

        @Override
        public void onEntityKill(LivingEntity killer, LivingEntity victim, List<ItemStack> drops, MutableExperience experience, int level) {
            seen.add("entityKill");
        }
        @Override
        public boolean shouldCancelDrop(Player player, ItemStack item, int level) {
            seen.add("playerDrop");
            return false;
        }

        @Override
        public int onFoodLevelChange(Player player, int currentFoodLevel, int proposedFoodLevel, int level) {
            seen.add("foodLevelChange");
            return proposedFoodLevel;
        }

        @Override
        public float onHorseJump(org.bukkit.entity.AbstractHorse horse, float power, int level) {
            seen.add("horseJump");
            return power;
        }


        @Override
        public void onBowShoot(LivingEntity shooter, Entity projectile, ItemStack bow, float force, int level) {
            seen.add("bowShoot");
        }

        @Override
        public void onProjectileHit(LivingEntity shooter, Projectile projectile, Entity hitEntity, Block hitBlock, int level) {
            seen.add("projectileHit");
        }

        @Override
        public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {
            seen.add("blockBreak");
        }

        @Override
        public void onBlockDrop(Player player, BlockState blockState, List<Item> items, int level) {
            seen.add("blockDrop");
        }

        @Override
        public void onBlockPlace(Player player, Block placedBlock, Block placedAgainst, ItemStack itemInHand, int level) {
            seen.add("blockPlace");
        }

        @Override
        public void onPlayerFish(Player player, FishHook hook, Entity caught, State state, int level) {
            seen.add("playerFish");
        }

        @Override
        public void onShearEntity(Player player, Entity shearedEntity, ItemStack shears, EquipmentSlot hand, int level) {
            seen.add("shearEntity");
        }

        @Override
        public void onBucketEmpty(
                Player player, Block clickedBlock, BlockFace face, ItemStack bucket, EquipmentSlot hand, int level) {
            seen.add("bucketEmpty");
        }

        @Override
        public void onBucketFill(
                Player player, Block clickedBlock, BlockFace face, ItemStack bucket, EquipmentSlot hand, int level) {
            seen.add("bucketFill");
        }

        @Override
        public int onItemDamage(Player player, ItemStack item, int originalDamageAmount, int level) {
            seen.add("itemDamage");
            return originalDamageAmount;
        }

        @Override
        public int onEntityItemDamage(Entity entity, ItemStack item, int originalDamageAmount, int level) {
            seen.add("entityItemDamage");
            return originalDamageAmount;
        }

        @Override
        public void onItemConsume(Player player, ItemStack consumedItem, int level) {
            seen.add("itemConsume");
        }

        @Override
        public void onActiveInteract(Player player, Action action, Block clickedBlock, ItemStack item, int level) {
            seen.add("activeInteract");
        }

        @Override
        public Duration activationCooldown() {
            return Duration.ZERO;
        }

        @Override
        public boolean onActivate(int level, Player player, ItemStack item) {
            seen.add("activate");
            return true;
        }

        @Override
        public void onPlayerMove(Player player, Location from, Location to, int level) {
            seen.add("playerMove");
        }

        @Override
        public void onEntityMove(LivingEntity entity, Location from, Location to, int level) {
            seen.add("entityMove");
        }

        @Override
        public void onPlayerJump(Player player, Location from, Location to, int level) {
            seen.add("playerJump");
        }

        @Override
        public void onToggleGlide(Player player, boolean isGliding, int level) {
            seen.add("toggleGlide");
        }

        @Override
        public void onToggleSneak(Player player, boolean isSneaking, int level) {
            seen.add("toggleSneak");
        }

        @Override
        public void onToggleSprint(Player player, boolean isSprinting, int level) {
            seen.add("toggleSprint");
        }

        @Override
        public void onEntityInteract(Player player, Entity rightClicked, ItemStack item, EquipmentSlot hand, int level) {
            seen.add("entityInteract");
        }

        @Override
        public int onExpGain(Player player, int originalExpAmount, int level) {
            seen.add("expGain");
            return originalExpAmount;
        }
    }
}

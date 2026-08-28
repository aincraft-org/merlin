package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class OfferValidatorTest {
    private Player createPlayer(int level) {
        Player player = mock(Player.class);
        when(player.getLevel()).thenReturn(level);
        return player;
    }

    @Test
    void rejectsClosedSession() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        Player player = createPlayer(15);
        OfferValidator.Result res = OfferValidator.validate(true, player, sword, lapis, null, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("closed"));
    }

    @Test
    void rejectsNullPlayer() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        OfferValidator.Result res = OfferValidator.validate(false, null, sword, lapis, null, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("Player cannot be null"));
    }

    @Test
    void rejectsMissingTarget() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        Player player = createPlayer(15);
        OfferValidator.Result res = OfferValidator.validate(false, player, null, lapis, null, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("missing"));
    }

    @Test
    void rejectsIncompatibleMaterial() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        // Sharpness on Pickaxe is invalid
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack pickaxe = mock(ItemStack.class);
        when(pickaxe.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        when(pickaxe.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        Player player = createPlayer(15);
        OfferValidator.Result res = OfferValidator.validate(false, player, pickaxe, lapis, null, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("cannot be applied"));
    }

    @Test
    void rejectsUnrecognizedCatalyst() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);
        ItemStack badCatalyst = mock(ItemStack.class);
        when(badCatalyst.getType()).thenReturn(Material.DIRT);
        when(badCatalyst.isEmpty()).thenReturn(false);

        Player player = createPlayer(15);
        OfferValidator.Result res = OfferValidator.validate(false, player, sword, lapis, badCatalyst, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("Unrecognized catalyst"));
    }

    @Test
    void rejectsInsufficientLapis() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(2, 2, 20, 2, Map.of(NamespacedKey.minecraft("sharpness"), 2), "Sharpness II");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(1); // requires 2

        Player player = createPlayer(25);
        OfferValidator.Result res = OfferValidator.validate(false, player, sword, lapis, null, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("Insufficient Lapis"));
    }

    @Test
    void rejectsInsufficientPlayerLevel() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(2, 2, 20, 2, Map.of(NamespacedKey.minecraft("sharpness"), 2), "Sharpness II");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(5);

        Player player = createPlayer(15); // requires 20
        OfferValidator.Result res = OfferValidator.validate(false, player, sword, lapis, null, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("Insufficient XP Level"));
    }

    @Test
    void rejectsConflictingEnchantments() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        when(sword.hasItemMeta()).thenReturn(false);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        when(adapter.readOvercap(sword)).thenReturn(Map.of(NamespacedKey.minecraft("smite"), 3));

        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        Player player = createPlayer(20);
        OfferValidator.Result res = OfferValidator.validate(false, player, sword, lapis, null, offer, registry, adapter);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("Conflicting enchantment"));
    }

    @Test
    void rejectsEqualOrLesserDowngrade() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 2), "Sharpness II");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        when(sword.hasItemMeta()).thenReturn(false);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        when(adapter.readOvercap(sword)).thenReturn(Map.of(NamespacedKey.minecraft("sharpness"), 5));

        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        Player player = createPlayer(20);
        OfferValidator.Result res = OfferValidator.validate(false, player, sword, lapis, null, offer, registry, adapter);
        assertInstanceOf(OfferValidator.Result.Invalid.class, res);
        assertTrue(((OfferValidator.Result.Invalid) res).reason().contains("equal or greater"));
    }

    @Test
    void acceptsValidInputAndReturnsPrecomputedPlan() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        EnchantingOffer offer = new EnchantingOffer(1, 1, 10, 1, Map.of(NamespacedKey.minecraft("sharpness"), 1), "Sharpness I");
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.isEmpty()).thenReturn(false);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);
        ItemStack catalyst = mock(ItemStack.class);
        when(catalyst.getType()).thenReturn(Material.AMETHYST_SHARD);
        when(catalyst.isEmpty()).thenReturn(false);

        Player player = createPlayer(12);

        OfferValidator.Result res = OfferValidator.validate(false, player, sword, lapis, catalyst, offer, registry, null);
        assertInstanceOf(OfferValidator.Result.Valid.class, res);
        OfferValidator.Result.Valid valid = (OfferValidator.Result.Valid) res;
        assertEquals(1, valid.enchantsToApply().get(NamespacedKey.minecraft("sharpness")));
        assertEquals(1, valid.lapisCost());
        assertEquals(1, valid.xpCost());
    }
}

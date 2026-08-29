package dev.mintychochip.merlin.paper.enchanting.custom.passive;

	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.List;
	import java.util.function.Consumer;
	import org.bukkit.entity.Player;
	import org.bukkit.event.EventHandler;
	import org.bukkit.event.EventPriority;
	import org.bukkit.event.Listener;
	import org.bukkit.event.entity.PlayerDeathEvent;
	import org.bukkit.event.inventory.InventoryClickEvent;
	import org.bukkit.event.inventory.InventoryType;
	import org.bukkit.event.player.PlayerDropItemEvent;
	import org.bukkit.event.player.PlayerItemHeldEvent;
	import org.bukkit.event.player.PlayerQuitEvent;
	import org.bukkit.inventory.ItemStack;
	
	/** Tracks equipped gear changes and refreshes passive enchant effects. */
	public final class PassiveEquipListener implements Listener {
	    private final PassiveEffectApplier applier;
	    private final Consumer<Runnable> scheduler;
	
	    public PassiveEquipListener(PassiveEffectApplier applier) {
	        this(applier, Runnable::run);
	    }
	
	    public PassiveEquipListener(PassiveEffectApplier applier, Consumer<Runnable> scheduler) {
	        this.applier = applier;
	        this.scheduler = scheduler;
	    }
	
	    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	    public void onItemHeld(PlayerItemHeldEvent event) {
	        refresh(event.getPlayer());
	    }
	
	    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	    public void onInventoryClick(InventoryClickEvent event) {
	        if (!(event.getWhoClicked() instanceof Player player)) return;
	        if (event.getClickedInventory() == null) return;
	        // Armor slots and cursor swaps change equipped gear; defer until the click is committed.
	        if (event.getSlotType() == InventoryType.SlotType.ARMOR
	                || event.getSlotType() == InventoryType.SlotType.QUICKBAR
	                || event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
	                || event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP) {
	            scheduler.accept(() -> refresh(player));
	        }
	    }
	
	    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	    public void onItemDrop(PlayerDropItemEvent event) {
	        refresh(event.getPlayer());
	    }
	
	    @EventHandler(priority = EventPriority.MONITOR)
	    public void onDeath(PlayerDeathEvent event) {
	        Player player = event.getEntity();
	        applier.removeAll(player);
	    }
	
	    @EventHandler(priority = EventPriority.MONITOR)
	    public void onQuit(PlayerQuitEvent event) {
	        applier.removeAll(event.getPlayer());
	    }
	
	    private void refresh(Player player) {
	        if (player == null || !player.isOnline()) return;
	        applier.refresh(player, equippedItems(player));
	    }
	
	    private static List<ItemStack> equippedItems(Player player) {
	        List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
	        items.add(player.getInventory().getItemInOffHand());
	        return items;
	    }
	}
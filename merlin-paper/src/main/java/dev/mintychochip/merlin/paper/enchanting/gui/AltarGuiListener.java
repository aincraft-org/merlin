package dev.mintychochip.merlin.paper.enchanting.gui;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class AltarGuiListener implements Listener {
    private static final Set<Material> VALID_CATALYSTS = Set.of(
            Material.AMETHYST_SHARD,
            Material.ECHO_SHARD,
            Material.GLOWSTONE_DUST
    );

    private final Map<UUID, AltarGuiSession> activeSessions = new ConcurrentHashMap<>();

    public void registerSession(AltarGuiSession session) {
        if (session != null && session.getPlayer() != null) {
            activeSessions.put(session.getPlayer().getUniqueId(), session);
        }
    }

    public void unregisterSession(UUID playerId) {
        activeSessions.remove(playerId);
    }

    public void closeAllSessions() {
        for (AltarGuiSession session : activeSessions.values()) {
            session.handleClose();
        }
        activeSessions.clear();
    }

    public Map<UUID, AltarGuiSession> getActiveSessions() {
        return activeSessions;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof AltarInventoryHolder holder) {
            registerSession(holder.session());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof AltarInventoryHolder holder)) return;
        AltarGuiSession session = holder.session();

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;

        // Disallow double click collection across the custom GUI
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // Shift-click handling
        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (rawSlot >= 54) {
                handlePlayerInventoryShiftClick(event, inv);
                session.rerollOffers();
            } else if (rawSlot == AltarGuiSession.SLOT_TARGET || rawSlot == AltarGuiSession.SLOT_LAPIS || rawSlot == AltarGuiSession.SLOT_CATALYST) {
                // Return item to player inventory
                ItemStack item = inv.getItem(rawSlot);
                if (item != null && !item.isEmpty() && event.getWhoClicked() instanceof Player player) {
                    inv.setItem(rawSlot, null);
                    var leftover = player.getInventory().addItem(item);
                    leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                    session.rerollOffers();
                }
            }
            return;
        }

        // Click inside GUI (top inventory)
        if (rawSlot < 54) {
            if (rawSlot == AltarGuiSession.SLOT_TARGET) {
                handleInputSlotClick(event, this::isEnchantable);
                session.rerollOffers();
            } else if (rawSlot == AltarGuiSession.SLOT_LAPIS) {
                handleInputSlotClick(event, mat -> mat == Material.LAPIS_LAZULI);
            } else if (rawSlot == AltarGuiSession.SLOT_CATALYST) {
                handleInputSlotClick(event, VALID_CATALYSTS::contains);
            } else {
                // Button or decorative pane
                event.setCancelled(true);
                if (rawSlot == AltarGuiSession.SLOT_TIER_1) session.handleEnchantClick(1);
                else if (rawSlot == AltarGuiSession.SLOT_TIER_2) session.handleEnchantClick(2);
                else if (rawSlot == AltarGuiSession.SLOT_TIER_3) session.handleEnchantClick(3);
                else if (rawSlot == AltarGuiSession.SLOT_REROLL) session.handleRerollClick();
            }
        }
    }

    private void handleInputSlotClick(InventoryClickEvent event, java.util.function.Predicate<Material> validator) {
        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && event.getWhoClicked() instanceof Player player) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && !hotbarItem.isEmpty() && !validator.test(hotbarItem.getType())) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.isEmpty()) {
            if (!validator.test(cursor.getType())) {
                event.setCancelled(true);
            }
        }
    }

    private void handlePlayerInventoryShiftClick(InventoryClickEvent event, Inventory topInv) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.isEmpty()) return;
        Material mat = clicked.getType();

        if (mat == Material.LAPIS_LAZULI) {
            routeToSlot(event, topInv, AltarGuiSession.SLOT_LAPIS, 64);
        } else if (VALID_CATALYSTS.contains(mat)) {
            routeToSlot(event, topInv, AltarGuiSession.SLOT_CATALYST, 64);
        } else if (isEnchantable(mat)) {
            ItemStack existing = topInv.getItem(AltarGuiSession.SLOT_TARGET);
            if (existing == null || existing.isEmpty()) {
                // Target slot strictly accepts exactly 1 item (e.g. 1 book from a stack of 16)
                if (clicked.getAmount() == 1) {
                    topInv.setItem(AltarGuiSession.SLOT_TARGET, clicked.clone());
                    event.setCurrentItem(null);
                } else {
                    ItemStack singleItem = clicked.clone();
                    singleItem.setAmount(1);
                    topInv.setItem(AltarGuiSession.SLOT_TARGET, singleItem);
                    clicked.setAmount(clicked.getAmount() - 1);
                }
            }
        }
    }

    private void routeToSlot(InventoryClickEvent event, Inventory topInv, int targetSlot, int maxStack) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.isEmpty()) return;

        ItemStack existing = topInv.getItem(targetSlot);
        if (existing == null || existing.isEmpty()) {
            topInv.setItem(targetSlot, clicked.clone());
            event.setCurrentItem(null);
        } else if (existing.isSimilar(clicked)) {
            int space = maxStack - existing.getAmount();
            if (space > 0) {
                int toMove = Math.min(space, clicked.getAmount());
                existing.setAmount(existing.getAmount() + toMove);
                clicked.setAmount(clicked.getAmount() - toMove);
                if (clicked.getAmount() <= 0) {
                    event.setCurrentItem(null);
                }
            }
        }
    }

    private boolean isEnchantable(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE") ||
               name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET") ||
               name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
               mat == Material.BOW || mat == Material.CROSSBOW || mat == Material.TRIDENT || mat == Material.BOOK;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof AltarInventoryHolder)) return;
        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                if (slot == AltarGuiSession.SLOT_TARGET) {
                    if (!isEnchantable(dragged.getType())) {
                        event.setCancelled(true);
                        return;
                    }
                } else if (slot == AltarGuiSession.SLOT_LAPIS) {
                    if (dragged.getType() != Material.LAPIS_LAZULI) {
                        event.setCancelled(true);
                        return;
                    }
                } else if (slot == AltarGuiSession.SLOT_CATALYST) {
                    if (!VALID_CATALYSTS.contains(dragged.getType())) {
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AltarInventoryHolder holder) {
            holder.session().handleClose();
            unregisterSession(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AltarGuiSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.handleClose();
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        AltarGuiSession session = activeSessions.remove(event.getEntity().getUniqueId());
        if (session != null) {
            session.handleClose();
        }
    }
}

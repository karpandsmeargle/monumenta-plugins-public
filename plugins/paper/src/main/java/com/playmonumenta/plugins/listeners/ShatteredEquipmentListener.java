package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ShatteredEquipmentListener implements Listener {
	// Player interacts with a block in the world
	// via left-click, right-click, or stepping on a pressure plate
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useItemInHand() != Event.Result.DENY) {
			Player player = event.getPlayer();
			ItemStack item = event.getItem();
			if (item != null && ItemStatUtils.isShattered(item) && !item.containsEnchantment(Enchantment.RIPTIDE)) {
				MessagingUtils.sendActionBarMessage(player, "Shattered items must be reforged before use");
				event.setUseItemInHand(Event.Result.DENY);
				event.setCancelled(true);
			}
		}
	}

	// Player right-clicks an entity
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		ItemStack item;
		if (event.getHand() == EquipmentSlot.HAND) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getInventory().getItemInOffHand();
		}
		if (ItemStatUtils.isShattered(item)) {
			MessagingUtils.sendActionBarMessage(player, "Shattered items must be reforged before use");
			event.setCancelled(true);
		}
	}

	// One entity attacks another
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player player) {
			if (ItemStatUtils.isShattered(player.getInventory().getItemInMainHand())) {
				MessagingUtils.sendActionBarMessage(player, "Shattered items must be reforged before use");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			Inventory inventory = event.getClickedInventory();
			if (inventory instanceof PlayerInventory &&
				event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
				// Prevent equipping armor if the armor is shattered
				// We only need to worry about armor slots if they are visible
				ItemStack item = null;
				boolean equipping = false;
				ClickType click = event.getClick();
				if ((click.equals(ClickType.LEFT) || click.equals(ClickType.RIGHT))) {
					item = event.getCursor();
					if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.getSlot() == 40) {
						equipping = true;
					}
				} else if ((click.equals(ClickType.SHIFT_LEFT) || click.equals(ClickType.SHIFT_RIGHT))) {
					item = event.getCurrentItem();
					if (item != null) {
						EquipmentSlot targetSlotType = ItemUtils.getEquipmentSlot(item);
						if ((targetSlotType == EquipmentSlot.FEET && inventory.getItem(36) == null) ||
							(targetSlotType == EquipmentSlot.LEGS && inventory.getItem(37) == null) ||
							(targetSlotType == EquipmentSlot.CHEST && inventory.getItem(38) == null) ||
							(targetSlotType == EquipmentSlot.HEAD && inventory.getItem(39) == null) ||
							(targetSlotType == EquipmentSlot.OFF_HAND && inventory.getItem(40) == null)) {
							equipping = true;
						}
					}
				} else if (click.equals(ClickType.NUMBER_KEY)) {
					item = inventory.getItem(event.getHotbarButton());
					if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.getSlot() == 40) {
						equipping = true;
					}
				} else if (click.equals(ClickType.SWAP_OFFHAND)) {
					item = event.getCurrentItem();
					equipping = true;
				}
				if (equipping && ItemStatUtils.isShattered(item)) {
					event.setCancelled(true);
					MessagingUtils.sendActionBarMessage(player, "Shattered items must be reforged before use");
				}
			}
		}
	}

	// If an item is being dragged in an inventory
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			// Check if the player tried to be sneaky and drag shattered armor into a slot
			if (ItemUtils.isShatteredWearable(event.getNewItems().getOrDefault(5, null)) || // Head  Slot
					ItemUtils.isShatteredWearable(event.getNewItems().getOrDefault(6, null)) || // Chest Slot
					ItemUtils.isShatteredWearable(event.getNewItems().getOrDefault(7, null)) || // Legs  Slot
					ItemUtils.isShatteredWearable(event.getNewItems().getOrDefault(8, null)) || // Feet  Slot
					ItemStatUtils.isShattered(event.getNewItems().getOrDefault(45, null))) { // Offhand Slot
				event.setCancelled(true);
				MessagingUtils.sendActionBarMessage(player, "Shattered items must be reforged before use");
			}

		}
	}

	// Player swapped hand items
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (ItemStatUtils.isShattered(event.getOffHandItem()) || ItemStatUtils.isShattered(event.getMainHandItem())) {
			MessagingUtils.sendActionBarMessage(event.getPlayer(), "Shattered items must be reforged before use");
			event.setCancelled(true);
		}
	}

	// Block Dispense Event
	// Cancel dispensers/droppers dropping specific items
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (ItemStatUtils.isShattered(event.getItem())) {
			event.setCancelled(true);
		}
	}

	// Block Dispense Armor Event
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDispenseArmorEvent(BlockDispenseArmorEvent event) {
		// Cancel dispensers equipping shattered armor to a player
		if (ItemStatUtils.isShattered(event.getItem())) {
			if (event.getTargetEntity() instanceof Player) {
				MessagingUtils.sendActionBarMessage((Player) event.getTargetEntity(), "Shattered items must be reforged before use");
			}
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemStatUtils.isShattered(item)) {
			event.setCancelled(true);
		}
	}
}

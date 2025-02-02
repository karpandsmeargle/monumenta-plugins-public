package com.playmonumenta.plugins.integrations;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public class MonumentaRedisSyncIntegration implements Listener {
	private static final String IDENTIFIER = "Monumenta";

	private static boolean mEnabled = false;

	private final Plugin mPlugin;
	private final Logger mLogger;

	public MonumentaRedisSyncIntegration(Plugin plugin) {
		mPlugin = plugin;
		mLogger = plugin.getLogger();

		mLogger.info("Enabling MonumentaRedisSync integration");
		mEnabled = true;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mLogger.info("PlayerTransferEvent: Player: " + player + "   Target: " + event.getTarget());
		event.getPlayer().clearTitle();

		player.closeInventory();

		NmsUtils.getVersionAdapter().forceDismountVehicle(player);

		if (ServerProperties.getPreventDungeonItemTransfer()) {
			int dropped = InventoryUtils.removeSpecialItems(player, false, true);
			if (dropped == 1) {
				player.sendMessage(ChatColor.RED + "The dungeon key you were carrying was dropped!");
			} else if (dropped > 1) {
				player.sendMessage(ChatColor.RED + "The dungeon keys you were carrying were dropped!");
			}
		} else {
			InventoryUtils.removeSpecialItems(player, true, true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), IDENTIFIER);
		if (data != null) {
			if (data.has("potions")) {
				try {
					mPlugin.mPotionManager.loadFromJsonObject(player, data.get("potions").getAsJsonObject());

					/* TODO LEVEL */
					mLogger.info("Loaded potion data for player " + player.getName());
				} catch (Exception ex) {
					mLogger.severe("Failed to load potion data for player " + player.getName() + ":" + ex.getMessage());
					ex.printStackTrace();
				}
			}

			if (data.has("effects")) {
				try {
					mPlugin.mEffectManager.loadFromJsonObject(player, data.get("effects").getAsJsonObject(), mPlugin);

					/* TODO LEVEL */
					mLogger.info("Loaded effects data for player " + player.getName());
				} catch (Exception ex) {
					mLogger.severe("Failed to load effects data for player " + player.getName() + ":" + ex.getMessage());
					ex.printStackTrace();
				}
			}

			SeasonalEventManager.loadPlayerProgressJson(player, data.get("season_pass_progress"));
		} else {
			// data is null
			SeasonalEventManager.loadPlayerProgressJson(player, null);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();

		JsonObject pluginData = new JsonObject();
		pluginData.add("potions", mPlugin.mPotionManager.getAsJsonObject(player, false));
		pluginData.add("effects", mPlugin.mEffectManager.getAsJsonObject(player));
		pluginData.add("season_pass_progress", SeasonalEventManager.getPlayerProgressJson(player));
		event.setPluginData(IDENTIFIER, pluginData);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (!player.isOnline()) {
				mPlugin.mPotionManager.clearAllPotions(player);
				SeasonalEventManager.unloadPlayerData(player);
			}
		}, 5);
	}

	public static @Nullable String cachedUuidToName(UUID uuid) {
		if (mEnabled) {
			return MonumentaRedisSyncAPI.cachedUuidToName(uuid);
		} else {
			return Bukkit.getOfflinePlayer(uuid).getName();
		}
	}

	public static String cachedUuidToNameOrUuid(UUID uuid) {
		String name = cachedUuidToName(uuid);
		if (name != null) {
			return name;
		}
		return uuid.toString();
	}

	public static @Nullable UUID cachedNameToUuid(String name) {
		if (mEnabled) {
			return MonumentaRedisSyncAPI.cachedNameToUuid(name);
		} else {
			return null;
		}
	}

}

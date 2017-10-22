package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Constants;
import pe.project.Main;
import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.playerdata.PlayerData;
import pe.project.point.Point;
import pe.project.utils.InventoryUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.ScoreboardUtils;
import pe.project.utils.NetworkUtils;

public class PlayerTracking implements EntityTracking {
	Main mPlugin = null;
	private Set<Player> mEntities = new HashSet<Player>();

	PlayerTracking(Main plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		Player player = (Player)entity;
		try {
			PlayerData.loadPlayerData(mPlugin, player);
		} catch (Exception e) {
			mPlugin.getLogger().severe("Failed to load playerdata for player '" + player.getName() + "'");
			e.printStackTrace();

			player.sendMessage(ChatColor.RED + "Something very bad happened while transferring your player data.");
			player.sendMessage(ChatColor.RED + "  As a precaution, the server has attempted to move you to Purgatory.");
			player.sendMessage(ChatColor.RED + "  If for some reason you aren't on purgatory, take a screenshot and log off.");
			player.sendMessage(ChatColor.RED + "  Please post in #moderator-help and tag @admin");
			player.sendMessage(ChatColor.RED + "  Include details about what you were doing");
			player.sendMessage(ChatColor.RED + "  such as joining or leaving a dungeon (and which one!)");

			try {
				NetworkUtils.sendPlayer(mPlugin, player, "purgatory");
			} catch (Exception ex) {
				mPlugin.getLogger().severe("CRITICAL: Failed to send failed player '" + player.getName() + "' to purgatory");
				ex.printStackTrace();
			}

		}

		mEntities.add(player);
	}

	@Override
	public void removeEntity(Entity entity) {
		PlayerData.savePlayerData(mPlugin, (Player)entity);

		mEntities.remove(entity);
	}

	public Set<Player> getPlayers() {
		return mEntities;
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Player> playerIter = mEntities.iterator();
		while (playerIter.hasNext()) {
			Player player = playerIter.next();

			boolean inSafeZone = false;
			boolean inCapital = false;
			boolean applyEffects = true;
			GameMode mode = player.getGameMode();

			if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
				Location location = player.getLocation();
				Point loc = new Point(location);

				//	First we'll check if the player is too high, if so they shouldn't be here.
				if (loc.mY >= 255 && player.isOnGround()) {
					//	Double check to make sure their on the ground as it can trigger a false positive.
					Block below = world.getBlockAt(location.subtract(0, 1, 0));
					if (below != null && below.getType() == Material.AIR) {
						continue;
					}

					PlayerUtils.awardStrike(player, "breaking rule #5, leaving the bounds of the map.");
				} else {
					// If the world is a town world, treat the player like they are in the capital
					// Otherwise figure out which (if any) safezone the player is in
					SafeZones safeZone = SafeZones.Capital;
					if (!mPlugin.mServerProporties.getIsTownWorld()) {
						safeZone = LocationManager.withinAnySafeZone(player);
					}

					inSafeZone = (safeZone != SafeZones.None);
					inCapital = (safeZone == SafeZones.Capital);
					applyEffects = (inSafeZone && SafeZoneConstants.safeZoneAppliesEffects(safeZone));

					if (inSafeZone) {
						if (safeZone == SafeZones.Capital) {
							Material mat = world.getBlockAt(location.getBlockX(), 10, location.getBlockZ()).getType();
							boolean neededMat = mat == Material.SPONGE || mat == Material.OBSIDIAN;

							if (mode == GameMode.SURVIVAL && !neededMat) {
								_transitionToAdventure(player);

							// TODO UGLY HACK - Only the capital cares about loc.mY > 95, r1plots doesn't need this restriction.
							// Thus the derpy || conditional - and only r1plots gets the IsTownWorld set to true for now
							} else if (mode == GameMode.ADVENTURE && neededMat && (mPlugin.mServerProporties.getIsTownWorld() || loc.mY > 95)) {
								int apartment = ScoreboardUtils.getScoreboardValue(player, "Apartment");
								if (apartment == 0) {
									_transitionToSurvival(player);
								}
							}
						} else {
							if (mode == GameMode.SURVIVAL) {
								_transitionToAdventure(player);
							}
						}
					}
				}

				//	Give potion effects to those in a City;
				if (inSafeZone) {
					if (applyEffects) {
						if (inCapital) {
							mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CAPITAL_SPEED_EFFECT);
						}

						mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_RESISTENCE_EFFECT);

						PotionEffect effect = player.getPotionEffect(PotionEffectType.JUMP);
						if (effect != null) {
							if (effect.getAmplifier() <= 5) {
								mPlugin.mPotionManager.removePotion(player, PotionID.SAFE_ZONE, PotionEffectType.JUMP);
							}
						}

						int food = ScoreboardUtils.getScoreboardValue(player, "Food");
						if (food <= 17) {
							mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_SATURATION_EFFECT);
						}
					}
				} else {
					if (mode == GameMode.ADVENTURE) {
						_transitionToSurvival(player);
					}
				}
			}

			//	Extra Effects.
			_updateExtraEffects(player, world);

			mPlugin.mPotionManager.updatePotionStatus(player, ticks);
		}
	}

	void _transitionToAdventure(Player player) {
		player.setGameMode(GameMode.ADVENTURE);

		mPlugin.mPotionManager.removePotion(player, PotionID.ALL, PotionEffectType.JUMP);

		_removeSpecialItems(player);

		Entity vehicle = player.getVehicle();
		if (vehicle != null) {
			if (vehicle instanceof Boat) {
				vehicle.remove();
			}
		}
	}

	void _transitionToSurvival(Player player) {
		player.setGameMode(GameMode.SURVIVAL);

		mPlugin.getClass(player).setupClassPotionEffects(player);
	}

	void _updateExtraEffects(Player player, World world) {
		_updatePatreonEffects(player, world);
		_updateItemEffects(player, world);
	}

	//	TODO: We should move this out of being ticked and into an event based system as well as store all
	//	Patrons in a list so we're not testing against every player 4 times a second.
	void _updatePatreonEffects(Player player, World world) {
		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		if (patreon > 0) {
			int shinyWhite = ScoreboardUtils.getScoreboardValue(player, "ShinyWhite");
			if (shinyWhite == 1 && patreon >= 5) {
				ParticleUtils.playParticlesInWorld(world, Particle.SPELL_INSTANT, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyPurple = ScoreboardUtils.getScoreboardValue(player, "ShinyPurple");
			if (shinyPurple == 1 && patreon >= 10) {
				ParticleUtils.playParticlesInWorld(world, Particle.DRAGON_BREATH, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyGreen = ScoreboardUtils.getScoreboardValue(player, "ShinyGreen");
			if (shinyGreen == 1 && patreon >= 10) {
				ParticleUtils.playParticlesInWorld(world, Particle.VILLAGER_HAPPY, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}
		}
	}

	//	TODO: This is the incorrect way to handle this, this should only be test against when an event triggers that might
	//	cause a change in the inventory that might change the item. (Item Break, Item Moved, Item Dropped, etc)
	void _updateItemEffects(Player player, World world) {
		ItemStack chest = player.getInventory().getChestplate();
		if (InventoryUtils.testForItemWithLore(chest, "* Stylish *")) {
			ParticleUtils.playParticlesInWorld(world, Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
		}
	}

	void _removeSpecialItems(Player player) {
		//	Clear inventory
		_removeSpecialItemsFromInventory(player.getInventory());

		//	Clear Ender Chest
		_removeSpecialItemsFromInventory(player.getEnderChest());
	}

	void _removeSpecialItemsFromInventory(Inventory inventory) {
		for (ItemStack item : inventory.getContents()) {
			if (item != null) {
				if (InventoryUtils.testForItemWithLore(item, "D4 Key")) {
					inventory.removeItem(item);
				} else {
					if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
						BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
						if (meta.getBlockState() instanceof ShulkerBox) {
							ShulkerBox shulker = (ShulkerBox)meta.getBlockState();
							_removeSpecialItemsFromInventory(shulker.getInventory());

							meta.setBlockState(shulker);
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		Iterator<Player> players = mEntities.iterator();
		while (players.hasNext()) {
			Player player = players.next();
			PlayerData.savePlayerData(mPlugin, player);
		}

		mEntities.clear();
	}
}

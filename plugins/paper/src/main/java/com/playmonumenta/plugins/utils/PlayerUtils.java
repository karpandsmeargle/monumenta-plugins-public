package com.playmonumenta.plugins.utils;

import com.destroystokyo.paper.MaterialSetTag;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;


public class PlayerUtils {
	public static void callAbilityCastEvent(Player player, ClassAbility spell) {
		AbilityCastEvent event = new AbilityCastEvent(player, spell);
		Bukkit.getPluginManager().callEvent(event);
	}

	public static void awardStrike(Plugin plugin, Player player, String reason) {
		int strikes = ScoreboardUtils.getScoreboardValue(player, "Strikes").orElse(0);
		strikes++;
		ScoreboardUtils.setScoreboardValue(player, "Strikes", strikes);

		Location loc = player.getLocation();
		String oobLoc = "[" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + "]";

		player.sendMessage(ChatColor.RED + "WARNING: " + reason);
		player.sendMessage(ChatColor.RED + "Location: " + oobLoc);
		player.sendMessage(ChatColor.YELLOW + "This is an automated message generated by breaking a game rule.");
		player.sendMessage(ChatColor.YELLOW + "You have been teleported to spawn and given slowness for 5 minutes.");
		player.sendMessage(ChatColor.YELLOW + "There is no further punishment, but please do follow the rules.");

		plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
		                                new PotionEffect(PotionEffectType.SLOW, 5 * 20 * 60, 3, false, true));

		player.teleport(player.getWorld().getSpawnLocation());
	}

	public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable) {
		List<Player> players = new ArrayList<Player>();

		for (Player player : loc.getWorld().getPlayers()) {
			if (player.getLocation().distanceSquared(loc) < range * range && player.getGameMode() != GameMode.SPECTATOR && player.getHealth() > 0) {
				if (includeNonTargetable || !AbilityUtils.isStealthed(player)) {
					players.add(player);
				}
			}
		}

		return players;
	}

	public static List<Player> otherPlayersInRange(Player player, double radius, boolean includeNonTargetable) {
		List<Player> players = playersInRange(player.getLocation(), radius, includeNonTargetable);
		players.remove(player);
		return players;
	}

	public static boolean isCursed(Plugin plugin, Player p) {
		return plugin.mEffectManager.hasEffect(p, "CurseEffect");
	}

	public static void removeCursed(Plugin plugin, Player p) {
		setCursedTicks(plugin, p, 0);
		p.removePotionEffect(PotionEffectType.BAD_OMEN);
		p.removePotionEffect(PotionEffectType.UNLUCK);
	}

	public static void setCursedTicks(Plugin plugin, Player p, int ticks) {
		NavigableSet<Effect> cursed = plugin.mEffectManager.getEffects(p, "CurseEffect");
		if (cursed != null) {
			for (Effect curse : cursed) {
				curse.setDuration(ticks);
			}
		}
	}

	public static void healPlayer(Plugin plugin, Player player, double healAmount) {
		healPlayer(plugin, player, healAmount, null);
	}

	public static void healPlayer(Plugin plugin, Player player, double healAmount, @Nullable Player sourcePlayer) {
		if ((sourcePlayer != null) && (player != sourcePlayer)) {
			double healBonus = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TRIAGE) * 0.05;
			healAmount *= 1 + healBonus;
		}
		if (!player.isDead()) {
			EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, healAmount, EntityRegainHealthEvent.RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				double newHealth = Math.min(player.getHealth() + event.getAmount(), EntityUtils.getMaxHealth(player));
				player.setHealth(newHealth);
			}
		}
	}

	public static Location getRightSide(Location location, double distance) {
		float angle = location.getYaw() / 60;
		return location.clone().subtract(new Vector(FastUtils.cos(angle), 0, FastUtils.sin(angle)).normalize().multiply(distance));
	}

	/* Command should use @s for targeting selector */
	public static void executeCommandOnNearbyPlayers(Location loc, int radius, String command) {
		for (Player player : loc.getNearbyPlayers(radius)) {
			// getNearbyPlayers returns players in a cube, not a sphere, so we need this additional check
			if (loc.distanceSquared(player.getLocation()) > radius * radius) {
				continue;
			}
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
			                                   "execute as " + player.getUniqueId() + " at @s run " + command);
		}
	}

	// How far back the player drew their bow,
	// vs what its max launch speed would be.
	// Launch velocity used to calculate is specifically for PLAYERS shooting BOWS!
	// Returns between 0 to 1, with 1 being full draw
	public static double calculateBowDraw(AbstractArrow arrowlike) {
		double currentSpeed = arrowlike.getVelocity().length();
		double maxLaunchSpeed = Constants.PLAYER_BOW_INITIAL_SPEED;

		return Math.min(
			1,
			currentSpeed / maxLaunchSpeed
		);
	}

	/*
	 * Whether the player meets the conditions for a crit,
	 * emulating the vanilla check in full.
	 *
	 * Ie, no critting while sprinting.
	 */
	public static boolean isCriticalAttack(Player player) {
		// NMS EntityHuman:
		// float f = (float)this.b((AttributeBase)GenericAttributes.ATTACK_DAMAGE);
		//     f1 = EnchantmentManager.a(this.getItemInMainHand(), ((EntityLiving)entity).getMonsterType());
		// float f2 = this.getAttackCooldown(0.5F);
		// f *= 0.2F + f2 * f2 * 0.8F;
		// f1 *= f2;
		// if (f > 0.0F || f1 > 0.0F) {
		//     boolean flag = f2 > 0.9F;
		//     boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround && !this.isClimbing() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && entity instanceof EntityLiving;
		//     flag2 = flag2 && !this.isSprinting();
		return (
			isFallingAttack(player)
			&& !player.isInWater()
			&& !player.isSprinting()
		);
	}

	/*
	 * Whether the player meets the conditions for a crit,
	 * emulating the vanilla check,
	 * except the not in water and not sprinting requirements.
	 *
	 * This is used because MM has historically had a non-exact crit check,
	 * that allowed things like crit-triggered abilities to trigger off non-crit
	 * melee damage while sprinting.
	 */
	public static boolean isFallingAttack(Player player) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
				&& player.getFallDistance() > 0
				&& isFreeFalling(player)
				&& !player.hasPotionEffect(PotionEffectType.BLINDNESS)
				&& !player.isInsideVehicle()
			//TODO pass in the Entity in question to check if LivingEntity
		);
	}

	/*
	 * Whether the player is considered to be freely falling in air or liquid.
	 * They cannot be on the ground or climbing.
	 */
	public static boolean isFreeFalling(Player player) {
		if (!player.isOnGround() && player.getLocation().isChunkLoaded()) {
			Material playerFeetMaterial = player.getLocation().getBlock().getType();
			// Accounts for vines, ladders, nether vines, scaffolding etc
			if (!MaterialSetTag.CLIMBABLE.isTagged(playerFeetMaterial)) {
				return true;
			}
		}

		return false;
	}

	/*
	 * Whether the player meets the conditions for a sweeping attack,
	 * emulating the vanilla check, except the on ground,
	 * movement increment limit, sword, and proximity requirements.
	 *
	 * Used as the custom Arcane Thrust requirement,
	 * similar to how enchant/ability falling attack requirements are a subset
	 * of crit requirements.
	 */
	public static boolean isNonFallingAttack(
		Player player,
		Entity enemy
	) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
			&& !isCriticalAttack(player)
			&& !player.isSprinting()
			// Last check on horizontal movement increment requires an internal
			// vanilla collision adjustment Vec3D.
			// It is not simply player.getVelocity(); that is used elsewhere
		);
	}

	/*
	 * Whether the player meets the conditions for a sweeping attack,
	 * emulating the vanilla check, except the movement increment limit, sword,
	 * and proximity requirements.
	 */
	public static boolean isSweepingAttack(
		Player player,
		Entity enemy
	) {
		// NMS Entity:
		// this.z = this.A;
		// this.A = (float)((double)this.A + (double)MathHelper.sqrt(c(vec3d1)) * 0.6D);
		// public static double c(Vec3D vec3d) {
		//     return vec3d.x * vec3d.x + vec3d.z * vec3d.z;
		// }
		//
		// NMS EntityHuman:
		// if (this.isSprinting() && flag) {
		//     flag1 = true;
		// }
		// double d0 = (double)(this.A - this.z);
		// if (flag && !flag2 && !flag1 && this.onGround && d0 < (double)this.dN()) {
		//     ItemStack itemstack = this.b((EnumHand)EnumHand.MAIN_HAND);
		//     if (itemstack.getItem() instanceof ItemSword) {
		//     List<EntityLiving> list = this.world.a(EntityLiving.class, entity.getBoundingBox().grow(1.0D, 0.25D, 1.0D));
		return (
			isNonFallingAttack(player, enemy)
			&& player.isOnGround()
		);
	}

	public static boolean checkPlayer(Player player) {
		return player.isValid() && player.getGameMode() != GameMode.SPECTATOR;
	}

	/*
	 * Returns players within a bounding box of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInBox(
		Location boxCenter,
		double totalWidth,
		double totalHeight
	) {
		return boxCenter.getNearbyPlayers(
			totalWidth / 2,
			totalHeight / 2,
			PlayerUtils::checkPlayer
		);
	}

	/*
	 * Returns players within a cube of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInCube(
		Location cubeCenter,
		double sideLength
	) {
		return playersInBox(cubeCenter, sideLength, sideLength);
	}

	/*
	 * Returns players within a sphere of the specified dimensions.
	 *
	 * Measures based on feet location.
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInSphere(
		Location sphereCenter,
		double radius
	) {
		Collection<Player> spherePlayers = playersInCube(sphereCenter, radius * 2);
		double radiusSquared = radius * radius;
		spherePlayers.removeIf((Player player) -> {
			if (sphereCenter.distanceSquared(player.getLocation()) > radiusSquared) {
				return true;
			} else {
				return false;
			}
		});

		return spherePlayers;
	}

	/*
	 * Returns players within an upright cylinder of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInCylinder(
		Location cylinderCenter,
		double radius,
		double totalHeight
	) {
		Collection<Player> cylinderPlayers = playersInBox(cylinderCenter, radius * 2, totalHeight);
		double centerY = cylinderCenter.getY();
		cylinderPlayers.removeIf((Player player) -> {
			Location flattenedLocation = player.getLocation();
			flattenedLocation.setY(centerY);
			if (cylinderCenter.distanceSquared(flattenedLocation) > radius * radius) {
				return true;
			} else {
				return false;
			}
		});

		return cylinderPlayers;
	}

	public static boolean isMage(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 1;
	}

	public static boolean isWarrior(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 2;
	}

	public static boolean isCleric(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 3;
	}

	public static boolean isRogue(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 4;
	}

	public static boolean isAlchemist(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 5;
	}

	public static boolean isScout(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 6;
	}

	public static boolean isWarlock(Player player) {
		Optional<Integer> opt = ScoreboardUtils.getScoreboardValue(player, "Class");
		return opt.orElse(0) == 7;
	}
}

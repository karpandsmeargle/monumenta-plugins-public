package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Bodyguard extends Ability {
	private static final int COOLDOWN = 30 * 20;
	private static final int RANGE = 25;
	private static final int RADIUS = 4;
	private static final int ABSORPTION_HEALTH_1 = 8;
	private static final int ABSORPTION_HEALTH_2 = 12;
	private static final int BUFF_DURATION = 20 * 10;
	private static final int STUN_DURATION = 20 * 3;

	private final int mAbsorptionHealth;

	private int mLeftClicks = 0;

	public Bodyguard(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Bodyguard");
		mInfo.mScoreboardId = "Bodyguard";
		mInfo.mShorthandName = "Bg";
		mInfo.mDescriptions.add("Left-click the air twice while looking directly at another player within 25 blocks to charge to them (cannot be used in safezones). Upon arriving, knock away all mobs within 4 blocks. Both you and the other player gain 4 Absorption hearts for 10 seconds. Left-click twice while looking down to cast on yourself. Cooldown: 30s.");
		mInfo.mDescriptions.add("Absorption increased to 6 hearts. Additionally, affected mobs are stunned for 3 seconds.");
		mInfo.mLinkedSpell = ClassAbility.BODYGUARD;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.IRON_CHESTPLATE, 1);
		mAbsorptionHealth = getAbilityScore() == 1 ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2;
	}

	@Override
	public void cast(Action action) {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				|| ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
				|| ItemUtils.isPickaxe(mainHand)) {
			return;
		}

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Location oLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		boolean lookingDown = oLoc.getPitch() > 50;
		Vector dir = oLoc.getDirection();
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getEyeLocation(), RANGE, true);
		players.remove(mPlayer);
		for (int i = 0; i < RANGE; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				if (lookingDown) {
					mLeftClicks++;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
				}

				break;
			}

			boolean hasTeleported = false;
			for (Player player : players) {
				//Prevents bodyguarding to multiple people
				if (hasTeleported) {
					break;
				}

				// If looking at another player, or reached the end of range check and looking down
				if (player.getBoundingBox().overlaps(box) && !ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					// Double LClick detection
					mLeftClicks++;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
					if (mLeftClicks < 2) {
						return;
					}
					// Don't set mLeftClicks to 0, self cast below handles that

					Location loc = mPlayer.getEyeLocation();
					for (int j = 0; j < 45; j++) {
						loc.add(dir.clone().multiply(0.33));
						world.spawnParticle(Particle.FLAME, loc, 4, 0.25, 0.25, 0.25, 0f);
						if (loc.distance(bLoc) < 1) {
							break;
						}
					}

					// Flame
					for (int k = 0; k < 120; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						world.spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.4));
					}

					// Explosion_Normal
					for (int k = 0; k < 60; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						world.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.15, 0.5));
					}

					Location userLoc = mPlayer.getLocation();
					Location targetLoc = player.getLocation().setDirection(mPlayer.getEyeLocation().getDirection()).subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
					if (userLoc.distance(player.getLocation()) > 1) {
						mPlayer.teleport(targetLoc);
						hasTeleported = true;
					}

					world.playSound(targetLoc, Sound.ENTITY_BLAZE_SHOOT, 0.75f, 0.75f);
					world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.75f, 0.9f);

					AbsorptionUtils.addAbsorption(player, mAbsorptionHealth, mAbsorptionHealth, BUFF_DURATION);
				}
			}
		}

		// Self trigger
		if (mLeftClicks < 2) {
			return;
		}
		mLeftClicks = 0;
		putOnCooldown();

		world.playSound(oLoc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);
		world.spawnParticle(Particle.FLAME, oLoc.add(0, 0.15, 0), 25, 0.2, 0, 0.2, 0.1);

		AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionHealth, mAbsorptionHealth, BUFF_DURATION);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS)) {
			MovementUtils.knockAway(mPlayer, mob, 0.45f, true);
			if (getAbilityScore() > 1) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
			}
		}
	}
}

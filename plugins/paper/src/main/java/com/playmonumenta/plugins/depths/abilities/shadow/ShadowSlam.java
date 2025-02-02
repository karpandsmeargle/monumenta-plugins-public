package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class ShadowSlam extends DepthsAbility {

	public static final String ABILITY_NAME = "Shadow Slam";
	public static final double[] DAMAGE = {2, 2.5, 3, 3.5, 4, 5.5};
	public static final int SIZE = 3;
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";
	public static final int REDUCED_THRESHOLD = 128; //No reduced damage for depths
	public static final double MANUAL_THRESHOLD = 1.5; // Minimum fall distance for attacks to trigger slam attack
	public static final int AUTOMATIC_THRESHOLD = 3;

	public static final DepthsAbilityInfo<ShadowSlam> INFO =
		new DepthsAbilityInfo<>(ShadowSlam.class, ABILITY_NAME, ShadowSlam::new, DepthsTree.SHADOWDANCER, DepthsTrigger.PASSIVE)
			.displayItem(Material.ANVIL)
			.descriptions(ShadowSlam::getDescription);

	private final BukkitRunnable mSlamAttackRunner;
	private double mFallFromY = -7050;

	public ShadowSlam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}
				if (
					DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, player) == 0
						|| !player.isValid() // Ensure player is not dead, is still online?
				) {
					// If reached this point but not silenced, then proceed with cancelling
					// If silenced, only return to not run anything, but don't cancel runnable
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				if (!PlayerUtils.isOnGround(player)) {
					updateFallFrom(); // Vanilla fall distance would be 0 if on ground
				} else {
					// Currently on ground

					// If first tick landing, should still have old mFallFromY to calculate using
					// Therefore can damage if eligible
					if (
						calculateFallDistance() > AUTOMATIC_THRESHOLD
					) {
						// Only for checking in LivingEntityDamagedByPlayerEvent below,
						// so doesn't slam twice, since this doesn't yet set fall distance to 0
						MetadataUtils.checkOnceThisTick(plugin, player, SLAM_ONCE_THIS_TICK_METAKEY);

						doSlamAttack(player.getLocation().add(0, 0.15, 0));
					}

					// Whether or not did attack, now that on ground, forget mFallFromY
					mFallFromY = -7050;
				}
			}
		};
		cancelOnDeath(mSlamAttackRunner.runTaskTimer(plugin, 0, 1));
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	private void updateFallFrom() {
		if (mPlayer.getFallDistance() <= 0) {
			mFallFromY = -10000;
		} else {
			mFallFromY = Math.max(mFallFromY, mPlayer.getLocation().getY());
		}
	}

	private double calculateFallDistance() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private void doSlamAttack(Location location) {
		double fallDistance = calculateFallDistance();
		double slamDamage = Math.min(REDUCED_THRESHOLD, fallDistance) * DAMAGE[mRarity - 1] + Math.max(0, (fallDistance - REDUCED_THRESHOLD)) * 0.0;

		for (LivingEntity enemy : EntityUtils.getNearbyMobs(location, SIZE)) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, slamDamage, mInfo.getLinkedSpell());
		}

		World world = mPlayer.getWorld();
		float volumeScale = (float) Math.min(0.1 + fallDistance / 16 * 0.9, 1);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 1.3F, 0);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 2, 1.25F);
		new PartialParticle(Particle.SPELL_WITCH, location, 60, 0F, 0F, 0F, 0.2F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, location, 20, 0F, 0F, 0F, 0.3F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 3 * SIZE * SIZE, SIZE, 0.25f, SIZE, 0).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && calculateFallDistance() > MANUAL_THRESHOLD && MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SLAM_ONCE_THIS_TICK_METAKEY)) {
			doSlamAttack(enemy.getLocation().add(0, 0.15, 0));
			mFallFromY = -7050;
			// Also reset fall damage, mFallFromY can continue updating from there
			mPlayer.setFallDistance(0);
			return true;
		}
		return false;
	}


	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("When you fall more than " + AUTOMATIC_THRESHOLD + " blocks, landing causes a slam, dealing ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]), color))
			.append(Component.text(" melee damage per block fallen in a " + SIZE + " block radius. Falling more than " + MANUAL_THRESHOLD + " blocks and damaging an enemy also generates a slam and cancels fall damage."));
	}
}


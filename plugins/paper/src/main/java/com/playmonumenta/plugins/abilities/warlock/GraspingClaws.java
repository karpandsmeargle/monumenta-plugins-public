package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class GraspingClaws extends Ability {

	private static final int RADIUS = 8;
	private static final float PULL_SPEED = 0.175f;
	private static final double AMPLIFIER_1 = 0.2;
	private static final double AMPLIFIER_2 = 0.3;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 8;
	private static final int DURATION = 8 * 20;
	private static final int COOLDOWN = 16 * 20;
	private static final int CAGE_RADIUS = 6;
	private static final int CAGE_DURATION = 6 * 20;
	private static final double HEAL_AMOUNT = 0.05;
	private static final int CAGE_DELAY = 1 * 20;
	private static final BlockData CHAIN_PARTICLE = Material.CHAIN.createBlockData();

	public static final String CHARM_DAMAGE = "Grasping Claws Damage";
	public static final String CHARM_COOLDOWN = "Grasping Claws Cooldown";
	public static final String CHARM_PULL = "Grasping Claws Pull Strength";
	public static final String CHARM_SLOW = "Grasping Claws Slowness Amplifier";
	public static final String CHARM_RADIUS = "Grasping Claws Radius";
	public static final String CHARM_DURATION = "Grasping Claws Slowness Duration";
	public static final String CHARM_PROJ_SPEED = "Grasping Claws Projectile Speed";
	public static final String CHARM_CAGE_RADIUS = "Grasping Claws Cage Radius";

	private final double mAmplifier;
	private final double mDamage;
	private final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public GraspingClaws(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Grasping Claws");
		mInfo.mScoreboardId = "GraspingClaws";
		mInfo.mShorthandName = "GC";
		mInfo.mDescriptions.add("Left-clicking while shifted while holding a projectile weapon fires an arrow " +
			                        "that pulls nearby enemies towards your arrow once it makes contact with a mob or block. " +
			                        "Mobs caught in the arrow's 8 block radius are given 20% Slowness for 8 seconds and take 3 magic damage. Cooldown: 16s.");
		mInfo.mDescriptions.add("The pulled enemies now take 8 damage, and their Slowness is increased to 30%.");
		mInfo.mDescriptions.add("At the location that the arrow lands, summon an impenetrable cage. " +
			                        "Non-boss mobs within a 6 block radius of the location cannot enter or exit the cage, " +
			                        "and players within the cage are granted 5% max health healing every 2 seconds. " +
			                        "The cage disappears after 6 seconds. Mobs that are immune to crowd control cannot be trapped.");
		mInfo.mLinkedSpell = ClassAbility.GRASPING_CLAWS;
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BOW, 1);
		mAmplifier = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + (isLevelOne() ? AMPLIFIER_1 : AMPLIFIER_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ClassAbility.GRASPING_CLAWS) && mPlayer.isSneaking() && ItemUtils.isProjectileWeapon(inMainHand)) {
			World world = mPlayer.getWorld();
			Location eyeLoc = mPlayer.getEyeLocation();
			Vector direction = mPlayer.getLocation().getDirection();
			float speed = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PROJ_SPEED, 1.5);
			Snowball proj = world.spawn(eyeLoc, Snowball.class);
			proj.setVelocity(direction.normalize().multiply(speed));
			proj.setShooter(mPlayer);
			proj.setCustomName("Grasping Claws Projectile");
			mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SPELL_WITCH);
			mPlayerItemStatsMap.put(proj, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
			putOnCooldown();
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
		if (mPlayer != null && playerItemStats != null) {
			Location loc = proj.getLocation();
			World world = proj.getWorld();

			world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.25f, 1.25f);
			world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.25f, 1.45f);
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.25f, 0.65f);
			new PartialParticle(Particle.PORTAL, loc, 125, 2, 2, 2, 0.25).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.PORTAL, loc, 400, 0, 0, 0, 1.45).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.DRAGON_BREATH, loc, 85, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FALLING_DUST, loc, 150, 2, 2, 2, CHAIN_PARTICLE).spawnAsPlayerActive(mPlayer);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS), mPlayer)) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), mDamage, true, true, false);
				MovementUtils.pullTowards(proj, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_SPEED));
				EntityUtils.applySlow(mPlugin, DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION), mAmplifier, mob);
			}

			if (isEnhanced()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						createCage(loc);
						loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.8f, 0.9f);
					}
				}.runTaskLater(mPlugin, CAGE_DELAY);
			}

			proj.remove();
		}
	}

	private void createCage(Location loc) {
		new BukkitRunnable() {
			int mT = 0;
			final List<BoundingBox> mBoxes = new ArrayList<>();
			List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();
			final List<LivingEntity> mMobsHitThisTick = new ArrayList<>();
			boolean mHitboxes = false;
			World mWorld = loc.getWorld();
			double mRadius = CharmManager.getRadius(mPlayer, CHARM_CAGE_RADIUS, CAGE_RADIUS);

			List<Integer> mDegrees1 = new ArrayList<>();
			List<Integer> mDegrees2 = new ArrayList<>();
			List<Integer> mDegrees3 = new ArrayList<>();

			@Override
			public void run() {
				mT++;

				// Wall Portion (Particles + Hitbox Definition)
				Vector vec;
				for (int y = 0; y < 5; y++) {
					for (double degree = 0; degree < 360; degree += 20) {
						double radian1 = Math.toRadians(degree);
						vec = new Vector(FastUtils.cos(radian1) * mRadius, y, FastUtils.sin(radian1) * mRadius);
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

						Location l = loc.clone().add(vec);
						if (mT % 4 == 0) {
							new PartialParticle(Particle.FALLING_DUST, l, 1, 0.1, 0.2, 0.1, CHAIN_PARTICLE).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						}
						if (!mHitboxes) {
							mBoxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
								l.clone().add(0.6, 5, 0.6)));
						}
					}
					mHitboxes = true;
				}

				// Hitbox Detection + Knocback
				for (BoundingBox box : mBoxes) {
					for (Entity e : mWorld.getNearbyEntities(box)) {
						Location eLoc = e.getLocation();
						if (e instanceof LivingEntity le && EntityUtils.isHostileMob(e)) {
							// Stores mobs hit this tick
							mMobsHitThisTick.add(le);
							// This list does not update to the mobs hit this tick until after everything runs
							if (!mMobsAlreadyHit.contains(le)) {
								mMobsAlreadyHit.add(le);
								Vector v = le.getVelocity();

								if (!e.getScoreboardTags().contains("Boss") && !e.getScoreboardTags().contains("boss_ccimmune")) {
									if (loc.distance(eLoc) > mRadius) {
										MovementUtils.knockAway(loc, le, 0.3f, true);
									} else {
										MovementUtils.pullTowards(loc, le, 0.15f);
									}
									mWorld.playSound(eLoc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, .75f, 0.8f);
									new PartialParticle(Particle.FALLING_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 3,
										le.getWidth() / 2, (le.getHeight() / 2) + 0.1, le.getWidth() / 2, CHAIN_PARTICLE)
										.spawnAsPlayerActive(mPlayer);
								} else {
									le.setVelocity(v);
								}
							}
						}
					}
				}
				/*
				 * Compare the two lists of mobs and only remove from the
				 * actual hit tracker if the mob isn't detected as hit this
				 * tick, meaning it is no longer in the shield wall hitbox
				 * and is thus eligible for another hit.
				 */
				List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
				for (LivingEntity mob : mMobsAlreadyHit) {
					if (mMobsHitThisTick.contains(mob)) {
						mobsAlreadyHitAdjusted.add(mob);
					}
				}
				mMobsAlreadyHit = mobsAlreadyHitAdjusted;
				mMobsHitThisTick.clear();
				if (mT >= CAGE_DURATION) {
					this.cancel();
					mBoxes.clear();
				}

				// Player Effect + Outline Particles
				if (mT % 5 == 0) {
					List<Player> affectedPlayers = PlayerUtils.playersInRange(loc, CAGE_RADIUS, true);
					for (Player p : affectedPlayers) {
						if (mT % 20 == 0) {
							PlayerUtils.healPlayer(mPlugin, p, p.getMaxHealth() * HEAL_AMOUNT, mPlayer);
						}
					}

					List<Integer> degreesToKeep = new ArrayList<>();
					for (int d = 0; d < 360; d += 3) {
						mWorld.spawnParticle(Particle.FALLING_DUST, loc.clone().add(mRadius * FastUtils.cosDeg(d), 0, mRadius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE);
						mWorld.spawnParticle(Particle.FALLING_DUST, loc.clone().add(mRadius * FastUtils.cosDeg(d), 5, mRadius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE);

						if (mDegrees1.contains(d)) {
							mWorld.spawnParticle(Particle.FALLING_DUST, loc.clone().add(mRadius * FastUtils.cosDeg(d), 5.5, mRadius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE);
							if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
								mDegrees1.remove((Integer) d);
							}
						}

						if (mDegrees2.contains(d)) {
							mWorld.spawnParticle(Particle.FALLING_DUST, loc.clone().add(mRadius * FastUtils.cosDeg(d), 6, mRadius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE);
							if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
								mDegrees2.remove((Integer) d);
							}
						}

						if (mDegrees3.contains(d)) {
							mWorld.spawnParticle(Particle.FALLING_DUST, loc.clone().add(mRadius * FastUtils.cosDeg(d), 6.75, mRadius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE);
						}

						if (FastUtils.randomDoubleInRange(0, 1) < 0.25) {
							degreesToKeep.add(d);
						}
					}

					mDegrees3 = new ArrayList<>(mDegrees2);
					mDegrees2 = new ArrayList<>(mDegrees1);
					mDegrees1 = new ArrayList<>(degreesToKeep);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}

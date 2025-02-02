package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class VolcanicDemiseTowerAbility extends TowerAbility {

	private static final int DAMAGE = 20;
	private static final int METEOR_COUNT = 1;
	private static final int METEOR_RATE = 10;
	private static final int COOLDOWN = 160;

	public VolcanicDemiseTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);


		Spell spell = new Spell() {
			@Override
			public void run() {
				EntityUtils.selfRoot(mBoss, 1000);


				BukkitRunnable runnable = new BukkitRunnable() {

					final World mWorld = mBoss.getWorld();
					int mTicks = 0;
					@Override
					public void run() {
						if (mBoss.isDead() || !mBoss.isValid() || mGame.isTurnEnded()) {
							cancel();
							return;
						}

						float ft = ((float) mTicks) / 25;

						new PartialParticle(Particle.LAVA, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125).spawnAsEntityActive(mBoss);
						mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.5f + ft);


						if (mTicks >= 20 * 2) {
							cancel();
							mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 0.5f);
							mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1, 0.7f);

							BukkitRunnable runnable1 = new BukkitRunnable() {

								int mI = 0;

								int mMeteors = 0;
								@Override
								public void run() {
									if (mBoss.isDead() || !mBoss.isValid() || mGame.isTurnEnded()) {
										cancel();
										return;
									}


									mI++;
									if (mI % METEOR_RATE == 0) {
										mMeteors++;
										List<LivingEntity> targets = mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs();

										for (int j = 0; j < 8; j++) {
											rainMeteor(mGame.getRandomLocation(), 20);
										}

										// Target one random player. Have a meteor rain nearby them.
										if (targets.size() >= 1) {
											LivingEntity rPlayer = targets.get(FastUtils.RANDOM.nextInt(targets.size()));
											Location loc = rPlayer.getLocation();
											rainMeteor(loc.add(FastUtils.randomDoubleInRange(-8, 8), 0, FastUtils.randomDoubleInRange(-8, 8)), 15);
										}

										if (mMeteors >= METEOR_COUNT) {
											this.cancel();
										}
									}

								}

								@Override
								public synchronized void cancel() throws IllegalStateException {
									super.cancel();

									EntityUtils.cancelSelfRoot(mBoss);
								}
							};

							runnable1.runTaskTimer(mPlugin, 0, 1);
						}

						mTicks += 2;
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 2);


			}

			@Override
			public int castTicks() {
				return 20 * 5;
			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};


		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, 20 * 9);

	}

	private void rainMeteor(Location locInput, double spawnY) {

		BukkitRunnable runnable = new BukkitRunnable() {
			double mY = spawnY;
			final Location mLoc = locInput.clone();
			final World mWorld = locInput.getWorld();

			@Override
			public void run() {

				if (mBoss.isDead() || !mBoss.isValid() || mGame.isTurnEnded()) {
					cancel();
					return;
				}

				mY -= 1;
				if (mY % 2 == 0) {
					for (double deg = 0; deg < 360; deg += 30) {
						new PartialParticle(Particle.FLAME, mLoc.clone().add(FastUtils.cos(deg), 0, FastUtils.sin(deg)), 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
					}
				}
				Location particle = mLoc.clone().add(0, mY, 0);
				new PartialParticle(Particle.FLAME, particle, 3, 0.2f, 0.2f, 0.2f, 0.05, null, true).spawnAsEntityActive(mBoss);
				if (FastUtils.RANDOM.nextBoolean()) {
					new PartialParticle(Particle.SMOKE_LARGE, particle, 1, 0, 0, 0, 0, null, true).spawnAsEntityActive(mBoss);
				}
				mWorld.playSound(particle, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);
				if (mY <= 0) {
					this.cancel();
					new PartialParticle(Particle.FLAME, mLoc, 50, 0, 0, 0, 0.175, null, true).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 10, 0, 0, 0, 0.25, null, true).spawnAsEntityActive(mBoss);
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.9f);
					BoundingBox box = BoundingBox.of(mLoc, 4, 10, 4);
					for (LivingEntity target : mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs()) {
						if (target.isValid() && !target.isDead() && !mGame.isTurnEnded()) {

							BoundingBox pBox = target.getBoundingBox();
							if (pBox.overlaps(box)) {
								DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false);
								MovementUtils.knockAway(mLoc, target, 0.5f, 0.65f);
							}
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}



}

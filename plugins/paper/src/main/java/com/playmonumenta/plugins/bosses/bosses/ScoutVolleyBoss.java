package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ScoutVolleyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_scout_volley";

	public static class Parameters extends BossParameters {
		public float SPEED = 2.0f;
		public int CONE = 90;
		public int PROJECTILE_NUMBER = 10;
		public int COOLDOWN = 16 * 20;

		public int SPELL_DELAY = 2 * 20;
		public int DELAY = 4 * 20;

		public int PIERCING = 0;

		public int DAMAGE = 0;

		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET;

		public SoundsList SOUND_START = SoundsList.fromString("[(ITEM_CROSSBOW_LOADING_START,5,0.5)]");
		public SoundsList SOUND_END = SoundsList.fromString("[(ITEM_CROSSBOW_LOADING_MIDDLE,5,0.5)]");
		public SoundsList SOUND_SHOOT = SoundsList.fromString("[(ITEM_CROSSBOW_SHOOT,5,0.5)]");

		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.fromString("[(CRIT,1)]");
	}

	private final Set<AbstractArrow> mVolleyArrowSet = new HashSet<>();

	public ScoutVolleyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		float spacing = ((float) p.CONE) / p.PROJECTILE_NUMBER;

		Spell spell = new Spell() {

			@Override
			public void run() {
				new BukkitRunnable() {
					int mTimer = 0;
					boolean mHasGlowing = false;
					boolean mHasPlayedEndAesthetic = false;

					@Override
					public void run() {
						if (mBoss.isDead() || !mBoss.isValid()) {
							cancel();
							return;
						}

						if (mTimer == 0) {
							//STARTING aesthetic
							mHasGlowing = mBoss.isGlowing();
							mBoss.setGlowing(true);
							p.SOUND_START.play(mBoss.getLocation());
						}

						if (mTimer >= p.SPELL_DELAY - 10 && !mHasPlayedEndAesthetic) {
							mHasPlayedEndAesthetic = true;
							mBoss.setGlowing(mHasGlowing);
							p.SOUND_END.play(mBoss.getLocation());
						}

						if (mTimer >= p.SPELL_DELAY) {
							mVolleyArrowSet.clear();

							p.SOUND_SHOOT.play(mBoss.getLocation());
							Location eyeLoc = mBoss.getEyeLocation();
							List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);
							if (!targets.isEmpty()) {
								Location targetEyeLoc = targets.get(0).getEyeLocation();
								Vector dir = targetEyeLoc.toVector().subtract(eyeLoc.toVector()).normalize();
								for (int i = 0; i < p.PROJECTILE_NUMBER; i++) {
									double yaw = spacing * (i - (p.PROJECTILE_NUMBER - 1) / 2f);
									AbstractArrow arrow = spawnArrow(mBoss, dir, yaw, p.SPEED);
									arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
									arrow.setPierceLevel(p.PIERCING);
									arrow.setDamage(p.DAMAGE);
									mVolleyArrowSet.add(arrow);
									if (!p.PARTICLE_PROJECTILE.isEmpty()) {
										new BukkitRunnable() {

											@Override
											public void run() {
												p.PARTICLE_PROJECTILE.spawn(boss, arrow.getLocation());

												if (arrow.isInBlock() || !arrow.isValid()) {
													this.cancel();
												}
											}
										}.runTaskTimer(mPlugin, 0, 1);
									}
								}
							}

							cancel();
						}

						mTimer++;
					}
				}.runTaskTimer(mPlugin, 0, 1);

			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}

			@Override
			public boolean canRun() {
				return !p.TARGETS.getTargetsList(mBoss).isEmpty();
			}
		};

		super.constructBoss(spell, -1, null, p.DELAY);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageEvent.DamageType.PROJECTILE && damager instanceof AbstractArrow arrow && mVolleyArrowSet.contains(arrow)) {
			if (MetadataUtils.happenedThisTick(damagee, "ScoutVolleyBoss")) {
				event.setCancelled(true);
			}
		}
	}

	private static AbstractArrow spawnArrow(LivingEntity entity, Vector dir, double yawOffset, float speed) {
		Location loc = entity.getEyeLocation();
		// Apply pitch/yaw offset to get arrow pattern
		dir = VectorUtils.rotateYAxis(dir, yawOffset);
		// Change the location's direction to match the arrow's direction
		loc.setDirection(dir);
		World world = loc.getWorld();

		// Spawn the arrow at the specified location, direction, and speed
		AbstractArrow arrow = world.spawnArrow(loc, dir, speed, 0.0f, Arrow.class);
		arrow.setShooter(entity);
		return arrow;
	}
}

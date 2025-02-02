package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CoordinatedAttackBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_coordinatedattack";
	public static final int detectionRange = 24;

	private static final int TARGET_RADIUS = 20;
	private static final int DELAY = 10;
	private static final int COOLDOWN = 20 * 3;
	private static final int AFFECTED_MOB_CAP = 4;

	private int mLastTriggered = 0;

	public CoordinatedAttackBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();

		if (target instanceof Player) {
			if (mBoss.getTicksLived() - mLastTriggered < COOLDOWN) {
				return;
			}

			mLastTriggered = mBoss.getTicksLived();

			World world = mBoss.getWorld();
			Location loc = target.getLocation();
			world.playSound(loc, Sound.EVENT_RAID_HORN, SoundCategory.HOSTILE, 50f, 1.5f);
			new PartialParticle(Particle.VILLAGER_ANGRY, loc, 30, 2, 0, 2, 0).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 0.5, 0), 30, 2, 0.5, 2, 0).spawnAsEntityActive(mBoss);

			new BukkitRunnable() {
				final Player mTarget = (Player) target;

				@Override
				public void run() {
					if (mTarget.isDead() || !mTarget.isValid() || !mTarget.isOnline()) {
						return;
					}

					Location locTarget = mTarget.getLocation();

					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(locTarget, TARGET_RADIUS);
					Collections.shuffle(mobs);

					int i = 0;
					for (LivingEntity le : EntityUtils.getNearbyMobs(locTarget, TARGET_RADIUS)) {
						if (le instanceof Mob mob && mob.hasLineOfSight(mTarget)) {
							if (!AbilityUtils.isStealthed(mTarget)) {
								Set<String> tags = mob.getScoreboardTags();
								// Don't set target of mobs with this ability, or else infinite loop
								if (!tags.contains(identityTag) && !tags.contains(DelvesManager.AVOID_MODIFIERS) && !tags.contains(AbilityUtils.IGNORE_TAG) && !EntityUtils.isBoss(mob)) {
									mob.setTarget(mTarget);
									Location loc = mob.getLocation();
									double distance = loc.distance(locTarget);
									Vector velocity = locTarget.clone().subtract(loc).toVector().multiply(0.19);
									velocity.setY(velocity.getY() * 0.5 + distance * 0.08);
									mob.setVelocity(velocity);

									new PartialParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.VILLAGER_ANGRY, mob.getEyeLocation(), 8, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);

									i++;
									if (i >= AFFECTED_MOB_CAP) {
										break;
									}
								}
							}
						}
					}
				}
			}.runTaskLater(mPlugin, DELAY);

		}
	}
}

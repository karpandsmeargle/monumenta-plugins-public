package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.Collections;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FriendlyBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_friendly";

	@BossParam(help = "Make this LivingEntity target other hostile mobs")
	public static class Parameters extends BossParameters {
		public double ATTACK_RANGE = 3;

		@BossParam(help = "Attack of this mob")
		public double DAMAGE = 0;

		@BossParam(help = "Attack % of this mob")
		public double DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Damage type of this mob attack")
		public DamageEvent.DamageType TYPE = DamageEvent.DamageType.MELEE;

		@BossParam(help = "Particles summon at player eye")
		public ParticlesList PARTICLES = ParticlesList.EMPTY;

		@BossParam(help = "Sounds player when deal damage")
		public SoundsList SOUNDS = SoundsList.EMPTY;

	}

	public FriendlyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		try {
			if (boss instanceof Creature creature) {
				NmsUtils.getVersionAdapter().setFriendly(creature, (LivingEntity target) -> {
						if (p.DAMAGE != 0) {
							DamageUtils.damage(mBoss, target, p.TYPE, p.DAMAGE);
						}

						if (p.DAMAGE_PERCENTAGE != 0) {
							BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE);
						}

						if (p.SOUNDS != SoundsList.EMPTY) {
							p.SOUNDS.play(target.getEyeLocation());
						}

						if (p.PARTICLES != ParticlesList.EMPTY) {
							p.PARTICLES.spawn(boss, target.getEyeLocation());
						}

					},
					EntityUtils::isHostileMob,
					p.ATTACK_RANGE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}
}
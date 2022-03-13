package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit.LIMITSENUM;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.PLAYERFILTER;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

//generalized class for all bosses with laser
public class LaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_laser";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;

		@BossParam(help = "not written")
		public int DETECTION = 30;

		@BossParam(help = "not written")
		public int DELAY = 5 * 20;

		@BossParam(help = "not written")
		public int DURATION = 5 * 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 12 * 20;

		@BossParam(help = "not written")
		public boolean CAN_BLOCK = true;

		@BossParam(help = "not written")
		public boolean CAN_MOVE = false;

		@BossParam(help = "not written")
		public boolean SINGLE_TARGET = false;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Effects apply to player after the laser end")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		//particle & sound used!
		@BossParam(help = "Sound used each tick on each player")
		public SoundsList SOUND_TICKS = SoundsList.fromString("[(ENTITY_SHULKER_BULLET_HIT)]");

		@BossParam(help = "Particle used for the laser")
		public ParticlesList PARTICLE_LASER = ParticlesList.fromString("[(CRIT,1),(CRIT_MAGIC,1)]");

		@BossParam(help = "Particle used when the cast is over")
		public ParticlesList PARTICLE_END = ParticlesList.fromString("[(EXPLOSION_NORMAL,35)]");

		@BossParam(help = "Sound used when the cast is over")
		public SoundsList SOUND_END = SoundsList.fromString("[(ENTITY_DRAGON_FIREBALL_EXPLODE,0.6,1.5)]");

		@BossParam(help = "not written")
		public int PARTICLE_FREQUENCY = 1;
		@BossParam(help = "not written")
		public int PARTICLE_CHANCE = 6;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LaserBoss(plugin, boss);
	}

	public LaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, p.SINGLE_TARGET ? new Limit(1) : new Limit(LIMITSENUM.ALL), Arrays.asList(PLAYERFILTER.HAS_LINEOFSIGHT));
			//by default LaserBoss don't take player in stealt and need LINEOFSIGHT to cast.
		}
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseLaser(plugin, boss, p.DURATION, false, p.COOLDOWN,
					() -> {
						return p.TARGETS.getTargetsList(mBoss);
					},
					// Tick action per player
					(LivingEntity target, int ticks, boolean blocked) -> {

						p.SOUND_TICKS.play(target.getLocation(), 0.8f, 0.5f + (ticks / 80f) * 1.5f);
						p.SOUND_TICKS.play(boss.getLocation(), 0.8f, 0.5f + (ticks / 80f) * 1.5f);

						if (ticks == 0 && !p.CAN_MOVE) {
							boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, p.DURATION, 4, false, true));
						}
					},
					// Particles generated by the laser
					(Location loc) -> {
						p.PARTICLE_LASER.spawn(loc, 0.03, 0.03, 0.03, 0.5d);
					},
					p.PARTICLE_FREQUENCY,
					p.PARTICLE_CHANCE,
					// Damage generated at the end of the attack
					(LivingEntity target, Location loc, boolean blocked) -> {
						p.SOUND_END.play(loc, 0.6f, 1.5f);
						p.PARTICLE_END.spawn(loc, 0, 0, 0, 0.25);

						if (p.CAN_BLOCK) {
							if (blocked) {
								return;
							}
						}

						if (target != null) {
							if (p.DAMAGE > 0) {
								BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
							}

							if (p.DAMAGE_PERCENTAGE > 0.0) {
								BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.SPELL_NAME);
							}

							p.EFFECTS.apply(target, mBoss);
						}


					})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}


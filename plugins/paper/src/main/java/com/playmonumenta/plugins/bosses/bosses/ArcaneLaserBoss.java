package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ArcaneLaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_arcanelaser";

	public static class Parameters extends BossParameters {
		public int DETECTION = 30;
		public int SLOW_LEVEL = 1;
		public int DURATION = 5 * 20;
		public int COOLDOWN = 12 * 20;
		public int SLOW_DURATION = 50;
		public boolean SINGLE_TARGET = false;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ArcaneLaserBoss(plugin, boss);
	}

	public ArcaneLaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseLaser(plugin, boss, p.DETECTION, p.DURATION, false, p.SINGLE_TARGET, p.COOLDOWN,
					// Tick action per player
					(LivingEntity target, int ticks, boolean blocked) -> {
						target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 0.8f, 0.5f + (ticks / 80f) * 1.5f);
						boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 0.8f, 0.5f + (ticks / 80f) * 1.5f);
						if (ticks == 0) {
							boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 110, 4, false, true));
						}
					},
					// Particles generated by the laser
					(Location loc) -> {
						loc.getWorld().spawnParticle(Particle.CRIT, loc, 1, 0.02, 0.02, 0.02, 0);
						loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.04, 0.04, 0.04, 1);
					},
					// Damage generated at the end of the attack
					(LivingEntity target, Location loc, boolean blocked) -> {
						loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6f, 1.5f);
						loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0, 0, 0, 0.25);
						if (!blocked) {
							BossUtils.blockableDamage(boss, target, DamageType.MAGIC, 18);
							target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, p.SLOW_DURATION, p.SLOW_LEVEL));
						}
					})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null);
	}
}

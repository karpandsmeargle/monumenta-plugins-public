package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.jetbrains.annotations.Nullable;

public class Galvanic implements Infusion {

	public static final double STUN_CHANCE_PER_LVL = 0.0125;
	public static final int DURATION_NORMAL = 2 * 20; // 2 seconds
	public static final int DURATION_ELITE = 10; // 0.5 seconds

	private static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOR_FAINT_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	@Override
	public String getName() {
		return "Galvanic";
	}

	@Override
	public double getPriorityAmount() {
		return 14;
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.GALVANIC;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();

		// Only apply infusion to basic melee and projectile attacks.
		if (!(type == DamageType.MELEE || type == DamageType.PROJECTILE) || event.getAbility() != null) {
			return;
		}

		// If used with arrow, must be critical
		if (event.getDamager() instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
			return;
		}

		apply(plugin, player, value, enemy, type == DamageType.MELEE);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity enemy) {
		DamageType type = event.getType();

		if (!(type == DamageType.MELEE || type == DamageType.PROJECTILE) || event.isBlocked() || event.isCancelled()) {
			return;
		}

		if (enemy != null) {
			apply(plugin, player, value, enemy, false);
		}
	}

	public static void apply(Plugin plugin, Player player, double value, LivingEntity enemy, boolean isMelee) {
		double chance = STUN_CHANCE_PER_LVL * value * (isMelee ? player.getCooledAttackStrength(0) : 1);
		if (FastUtils.RANDOM.nextDouble() < chance) {
			if (EntityUtils.isElite(enemy)) {
				EntityUtils.applyStun(plugin, DURATION_ELITE, enemy);
			} else {
				EntityUtils.applyStun(plugin, DURATION_NORMAL, enemy);
			}

			if (!EntityUtils.isCCImmuneMob(enemy)) {
				Location loc = enemy.getLocation();
				World world = enemy.getWorld();
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.65f, 1.5f);
				loc = loc.add(0, 1, 0);
				new PartialParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_YELLOW).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_FAINT_YELLOW).spawnAsPlayerActive(player);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.15).spawnAsPlayerActive(player);
			}
		}
	}
}

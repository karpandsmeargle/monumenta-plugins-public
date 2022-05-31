package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public class ScorchedEarthDamage extends SingleArgumentEffect {

	public ScorchedEarthDamage(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		DamageType type = event.getType();
		if (type != DamageType.MAGIC && type != DamageType.FIRE && type != DamageType.OTHER) {
			event.setDamage(event.getDamage() + mAmount);
			World world = entity.getWorld();
			Location loc = entity.getLocation().clone().add(0, 1, 0);
			world.spawnParticle(Particle.FLAME, loc, 5, 0.25, 0.5, 0.25, 0.05);
			world.spawnParticle(Particle.REDSTONE, loc, 15, 0.35, 0.5, 0.35, new Particle.DustOptions(ScorchedEarth.SCORCHED_EARTH_COLOR_DARK, 1.0f));
			world.spawnParticle(Particle.LAVA, loc, 3, 0.25, 0.5, 0.25, 0);
		}
	}

	@Override
	public String toString() {
		return String.format("ScorchedEarthDamage duration=%d", this.getDuration());
	}
}

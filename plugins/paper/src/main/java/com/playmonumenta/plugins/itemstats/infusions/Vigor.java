package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Vigor implements Infusion {

	public static final double DAMAGE_MOD_PER_LEVEL = 0.0075;

	@Override
	public String getName() {
		return "Vigor";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.VIGOR;
	}

	@Override
	public double getPriorityAmount() {
		return 25;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (type.equals(DamageType.MELEE) || type.equals(DamageType.MELEE_SKILL)) {
			double damageBuffPct = value * DAMAGE_MOD_PER_LEVEL;
			event.setDamage(event.getDamage() * (1.0 + damageBuffPct));
		}
	}

}

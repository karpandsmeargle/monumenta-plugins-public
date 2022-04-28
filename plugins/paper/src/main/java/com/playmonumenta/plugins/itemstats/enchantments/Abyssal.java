package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Abyssal implements Enchantment {

	private static final double DAMAGE_BONUS_PER_LEVEL = 0.1;
	public static final String CHARM_DAMAGE = "Abyssal Damage";

	@Override
	public String getName() {
		return "Abyssal";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ABYSSAL;
	}

	@Override
	public double getPriorityAmount() {
		return 27;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ABYSSAL);
		if (EntityUtils.isInWater(enemy) || EntityUtils.isInWater(player)) {
			double multiplier = 1 + CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, DAMAGE_BONUS_PER_LEVEL * level);
			event.setDamage(event.getDamage() * multiplier);
		}
	}
}

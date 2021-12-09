package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;
import java.util.NavigableSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;

public class Natant implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Natant";
	private static final int DURATION = 10;
	private static final double PERCENT_SPEED_PER_LEVEL = 0.04;
	private static final String PERCENT_SPEED_EFFECT_NAME = "NatantPercentSpeedEffect";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		if (player.isInWater()) {
			NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
			if (speedEffects != null) {
				for (Effect effect : speedEffects) {
					if (effect.getMagnitude() == PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level)) {
						effect.setDuration(DURATION);
					} else {
						effect.setDuration(1);
						plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), PERCENT_SPEED_EFFECT_NAME));
					}
				}
			} else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), PERCENT_SPEED_EFFECT_NAME));
			}
	    }
	}

}

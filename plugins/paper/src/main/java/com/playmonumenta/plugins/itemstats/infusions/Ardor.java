package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Ardor implements Infusion {

	private static final int DURATION = 4 * 20;
	public static final int AIR_INCREASE = 15; //Each breath bubble counts as 30
	public static final double PERCENT_SPEED_PER_LEVEL = 0.03;
	private static final String PERCENT_SPEED_EFFECT_NAME = "ArdorPercentSpeedEffect";

	@Override
	public String getName() {
		return "Ardor";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ARDOR;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		//If we break a spawner with a pickaxe
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			player.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1, 0.8f);
		    if (player.isInWaterOrBubbleColumn()) {
		        int currAir = player.getRemainingAir();
		        player.setRemainingAir(Math.min(300, currAir + (int) (AIR_INCREASE * value)));
		    } else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(DURATION, getMovementSpeedBonus(value),
						PERCENT_SPEED_EFFECT_NAME));
		    }
		}
	}

	public static double getMovementSpeedBonus(double level) {
		return PERCENT_SPEED_PER_LEVEL * level;
	}

}

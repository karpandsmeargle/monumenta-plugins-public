package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ArrowSaving extends SingleArgumentEffect {
	public static final String GENERIC_NAME = "ArrowSaving";

	public ArrowSaving(int duration, double amount) {
		super(duration, amount);
	}

	@Override
	public void onProjectileLaunch(Player player, AbstractArrow arrow) {
		if (FastUtils.RANDOM.nextDouble() < mAmount) {
			AbilityUtils.refundArrow(player, arrow);
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Arrow Saving";
	}

	@Override
	public String toString() {
		return String.format("ArrowSaving duration:%d amount:%f", getDuration(), mAmount);
	}
}

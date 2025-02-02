package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;

public class Legionary {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.15;

	public static final String DESCRIPTION = "Enemies come in larger numbers.";

	public static String[] rankDescription(int level) {
			return new String[]{
				"Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance",
				"to spawn a copy of an enemy."
			};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (DelvesUtils.isDelveMob(mob) || EntityUtils.isBoss(mob)) {
			return;
		}

		for (int i = FastUtils.roundRandomly(SPAWN_CHANCE_PER_LEVEL * level); i > 0; i--) {
			DelvesUtils.duplicateLibraryOfSoulsMob(mob);
		}
	}

}

package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellFloat;

public class FloatBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_float";
	private static final int detectionRange = 16;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FloatBoss(plugin, boss);
	}

	public FloatBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellFloat(mBoss)
		                            );

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}

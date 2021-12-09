package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShadeParticle;

public final class ShadeParticleBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_shadeparticle";
	public static final int detectionRange = 16;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ShadeParticleBoss(plugin, boss);
	}

	public ShadeParticleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellShadeParticle(boss)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}

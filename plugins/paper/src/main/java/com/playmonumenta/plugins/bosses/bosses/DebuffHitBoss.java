package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.utils.FastUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DebuffHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_debuffhit";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DebuffHitBoss(plugin, boss);
	}

	public DebuffHitBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			LivingEntity target = (LivingEntity) event.getEntity();
			int rand = FastUtils.RANDOM.nextInt(4);
			if (rand == 0) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0, false, true));
			} else if (rand == 1) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
			} else if (rand == 2) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true));
			} else if (rand == 3) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, false, true));
			}
		}
	}
}


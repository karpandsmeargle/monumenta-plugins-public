package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @deprecated use boss_onhit instead, like this:
 * <blockquote><pre>
 * /boss var Tags add boss_onhit
 * /boss var Tags add boss_onhit[effects=[(WITHER,80,1)]]
 * CARE this ability has some particle and sound, fix those too if you don't want the default values
 * </pre></blockquote>
 * G3m1n1Boy
 */
@Deprecated
public class WitherHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_witherhit";
	public static final int detectionRange = 20;

	public WitherHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			if (BossUtils.bossDamageBlocked(player, mBoss.getLocation()) && event.getType() != DamageType.MAGIC) {
				return;
			}
		}
		damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
	}
}


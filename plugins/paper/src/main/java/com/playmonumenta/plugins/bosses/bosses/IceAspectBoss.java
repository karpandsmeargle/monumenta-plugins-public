package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
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
 * /boss var Tags add boss_onhit[EFFECTS=[(SLOW,80,1)]]
 * CARE this ability has some particle and sound, fix those too if you don't want the default values
 * </pre></blockquote>
 * G3m1n1Boy
 */
@Deprecated
public final class IceAspectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_iceaspect";

	public static class Parameters extends BossParameters {
		public int DETECTION = 50;
		public int SLOW_AMPLIFIER = 1;
		public int SLOW_DURATION = 80;
	}

	private final Parameters mParams;

	public IceAspectBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			if (BossUtils.bossDamageBlocked(player, mBoss.getLocation()) && event.getType() != DamageType.MAGIC) {
				return;
			}
		}
		PotionUtils.applyPotion(mBoss, damagee, new PotionEffect(PotionEffectType.SLOW, mParams.SLOW_DURATION, mParams.SLOW_AMPLIFIER, false, true));
	}
}

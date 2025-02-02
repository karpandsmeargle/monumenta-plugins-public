package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDrawAggro;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class DummyDecoyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dummydecoy";
	public static final int detectionRange = 30;

	private int mStunTime = 0;

	public DummyDecoyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passives = List.of(new SpellDrawAggro(boss, DummyDecoy.AGGRO_RADIUS));

		super.constructBoss(SpellManager.EMPTY, passives, detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.7f);
		new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 10, 0.5, 1, 0.5, 0.05).spawnAsEntityActive(mBoss);
		List<LivingEntity> mobsToStun = EntityUtils.getNearbyMobs(mBoss.getLocation(), DummyDecoy.STUN_RADIUS);
		for (LivingEntity le : mobsToStun) {
			EntityUtils.applyStun(Plugin.getInstance(), mStunTime, le);
		}
	}

	public void spawn(int stunTime) {
		mStunTime = stunTime;
	}
}

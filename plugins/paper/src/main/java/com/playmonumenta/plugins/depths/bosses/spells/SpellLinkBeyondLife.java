package com.playmonumenta.plugins.depths.bosses.spells;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellLinkBeyondLife extends Spell {
	private static final String SUMMON_NAME_1 = "AbyssalTrilobite";
	private static final String SUMMON_NAME_2 = "DistortedScoundrel";
	private static final String SUMMON_NAME_3 = "DistortedCrewman";
	private static final int SPAWN_COUNT = 4; // Summon count 4-8 depending on players alive
	private static final int RANGE = 10;

	private final LivingEntity mBoss;
	private int mCooldownTicks;

	public SpellLinkBeyondLife(LivingEntity boss, int cooldown) {
		mBoss = boss;
		mCooldownTicks = cooldown;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 20, 1);
		int summonCount = SPAWN_COUNT + PlayerUtils.playersInRange(mBoss.getLocation(), Davey.detectionRange, true).size();

		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), Davey.detectionRange, "tellraw @s [\"\",{\"text\":\"[Davey]\",\"color\":\"gold\"},{\"text\":\" Now ye've done it. She be watchin'. Help me, heathens of \",\"color\":\"blue\"},{\"text\":\"ngbgbggb\",\"obfuscated\":\"true\",\"color\":\"blue\"},{\"text\":\"!\",\"color\":\"blue\"}]");

		new BukkitRunnable() {
			int mTicks = 0;
			int mSummons = 0;

			@Override
			public void run() {
				mTicks++;

				if (mSummons >= summonCount) {
					this.cancel();
					return;
				}

				if (mTicks % 20 == 0) {
					double x = -1;
					double z = -1;
					int attempts = 0;
					//Summon the mob, every second
					//Try until we have air space to summon the mob
					while (x == -1 || loc.getWorld().getBlockAt(loc.clone().add(x, .25, z)).getType() != Material.AIR) {
						x = FastUtils.randomDoubleInRange(-RANGE, RANGE);
						z = FastUtils.randomDoubleInRange(-RANGE, RANGE);

						attempts++;
						//Prevent infinite loop
						if (attempts > 20) {
							break;
						}
					}
					//Summon the mob using our location
					Location sLoc = loc.clone().add(x, 0.25, z);
					loc.getWorld().playSound(sLoc, Sound.BLOCK_GRAVEL_BREAK, 1, 0.75f);
					loc.getWorld().spawnParticle(Particle.BLOCK_DUST, sLoc, 16, 0.25, 0.1, 0.25, 0.25, Material.GRAVEL.createBlockData());
					Random r = new Random();
					int roll = r.nextInt(3);
					if (roll == 0) {
						LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_1);
					} else if (roll == 1) {
						LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_2);
					} else if (roll == 2) {
						LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_3);
					}
					mSummons++;
				}

			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
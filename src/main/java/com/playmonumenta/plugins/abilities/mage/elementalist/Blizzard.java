package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Blizzard extends Ability {

	/*
	 * Blizzard: Create an aura of ice and snow in a range of 4 that lasts 10s
	 * (similar to Rains) each enemy that enters the aura gets slowness 1, 2 after
	 * 3s and 6s freezes them (except bosses) and take 4/6 damage for second.
	 * Cooldown: 28 s / 24 s
	 */

	public Blizzard(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Blizzard";
		mInfo.linkedSpell = Spells.BLIZZARD;
		mInfo.cooldown = getAbilityScore() == 1 ? 20 * 28 : 20 * 24;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	private Map<UUID, Integer> affected = new HashMap<UUID, Integer>();

	@Override
	public boolean cast() {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.75f);
		double damage = getAbilityScore() == 1 ? 4 : 6;
		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				Location loc = mPlayer.getLocation();
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, 4, mPlayer);
				t++;
				if (t % 10 == 0) {
					for (LivingEntity mob : mobs) {
						if (!affected.containsKey(mob.getUniqueId())) {
							mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 0, false, true));
							affected.put(mob.getUniqueId(), 1);
						} else {
							int duration = affected.get(mob.getUniqueId());
							int amp = -1;
							if (mob.hasPotionEffect(PotionEffectType.SLOW)) {
								PotionEffect effect = mob.getPotionEffect(PotionEffectType.SLOW);
								amp = effect.getAmplifier();
							}
							if (duration >= 6) {
								if (amp < 1) {
									mob.removePotionEffect(PotionEffectType.SLOW);
									mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1, false, true));
								}
							} else if (duration >= 12) {
								if (!EntityUtils.isBoss(mob) && !EntityUtils.isFrozen(mob)) {
									EntityUtils.applyFreeze(mPlugin, 20 * 5, mob);
								}
							}
							affected.put(mob.getUniqueId(), duration + 1);
						}
					}
				}

				if (t % 20 == 0) {
					for (LivingEntity mob : mobs) {
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ICE);
					}
				}

				mWorld.spawnParticle(Particle.SNOWBALL, loc, 8, 2, 2, 2, 0.1);
				mWorld.spawnParticle(Particle.CLOUD, loc, 5, 2, 2, 2, 0.05);
				mWorld.spawnParticle(Particle.CLOUD, loc, 5, 0.1, 0.1, 0.1, 0.15);
				if (t >= 20 * 10 || mPlayer.isDead() || !mPlayer.isValid()) {
					this.cancel();
					affected.clear();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50 && (InventoryUtils.isWandItem(mHand) || InventoryUtils.isWandItem(oHand));
	}

}

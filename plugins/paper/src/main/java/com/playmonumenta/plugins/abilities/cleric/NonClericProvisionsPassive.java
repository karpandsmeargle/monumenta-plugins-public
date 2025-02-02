package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class NonClericProvisionsPassive extends Ability {
	private static final EnumSet<Material> PROHIBITED_FOOD = EnumSet.of(
		Material.GOLDEN_APPLE,
		Material.ENCHANTED_GOLDEN_APPLE
	);

	public static final AbilityInfo<NonClericProvisionsPassive> INFO =
		new AbilityInfo<>(NonClericProvisionsPassive.class, null, NonClericProvisionsPassive::new)
			.canUse(player -> true)
			.ignoresSilence(true); // checks for Sacred Provisions on nearby players, which may be silenced and then affect this ability

	public NonClericProvisionsPassive(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static boolean testRandomChance(Player player) {
		double bestChance = 0;

		for (Player p : PlayerUtils.playersInRange(player.getLocation(), 100, true)) {
			SacredProvisions provisions = AbilityManager.getManager().getPlayerAbility(p, SacredProvisions.class);
			if (provisions != null && provisions.isInRange(player)) {
				double chance = provisions.getChance();
				if (chance > bestChance) {
					bestChance = chance;
				}
			}
		}

		return bestChance > 0 && FastUtils.RANDOM.nextDouble() < bestChance;
	}

	public static boolean testEnhanced(Player player) {
		for (Player p : PlayerUtils.playersInRange(player.getLocation(), 100, true)) {
			SacredProvisions provisions = AbilityManager.getManager().getPlayerAbility(p, SacredProvisions.class);
			if (provisions != null && provisions.isEnhanced() && provisions.isInRange(player)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (!PROHIBITED_FOOD.contains(event.getItem().getType()) && testRandomChance(mPlayer)) {
			event.setReplacement(event.getItem());
			sacredProvisionsSound(mPlayer);
		}
	}

	@Override
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		if (testRandomChance(mPlayer)) {
			event.setDamage(0);
		}
	}

	@Override
	public boolean playerConsumeArrowEvent() {
		return !testRandomChance(mPlayer);
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (testRandomChance(mPlayer)) {
			if (AbilityUtils.refundPotion(mPlayer, potion)) {
				sacredProvisionsSound(mPlayer);
			}
		}
		return true;
	}

	@Override
	public boolean playerThrewLingeringPotionEvent(ThrownPotion potion) {
		if (testRandomChance(mPlayer)) {
			if (AbilityUtils.refundPotion(mPlayer, potion)) {
				sacredProvisionsSound(mPlayer);
			}
		}
		return true;
	}

	public static void sacredProvisionsSound(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (player.getFoodLevel() >= SacredProvisions.ENHANCEMENT_HEALING_HUNGER_REQUIREMENT && testEnhanced(player)) {
				event.setAmount(event.getAmount() * (1 + SacredProvisions.ENHANCEMENT_HEALING_BONUS));
			}
		}
	}
}

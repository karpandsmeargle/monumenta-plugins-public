package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SoothingCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Soothing Combos";
	public static final double[] SPEED_PERCENT = {0.1, 0.125, 0.15, 0.175, 0.2, 0.25};
	public static final String SPEED_EFFECT_NAME = "SoothingCombosPercentSpeedEffect";
	public static final double[] DURATION = {2.0, 2.5, 3.0, 3.5, 4.0, 6.0}; //seconds
	public static final int RANGE = 12;

	public static final DepthsAbilityInfo<SoothingCombos> INFO =
		new DepthsAbilityInfo<>(SoothingCombos.class, ABILITY_NAME, SoothingCombos::new, DepthsTree.DAWNBRINGER, DepthsTrigger.COMBO)
			.displayItem(Material.HONEYCOMB)
			.descriptions(SoothingCombos::getDescription);

	private int mComboCount = 0;

	public SoothingCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {

				mComboCount = 0;
				PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (20 * DURATION[mRarity - 1]), 0, false, true);

				if (mRarity == 6) {
					hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (20 * DURATION[mRarity - 1]), 1, false, true);
				}
				List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), RANGE, true);

				for (Player p : players) {
					p.addPotionEffect(hasteEffect);
					mPlugin.mEffectManager.addEffect(p, SPEED_EFFECT_NAME, new PercentSpeed((int) (20 * DURATION[mRarity - 1]), SPEED_PERCENT[mRarity - 1], SPEED_EFFECT_NAME));
					new PartialParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 5, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
					mPlayer.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
				}

				Location loc = mPlayer.getLocation().add(0, 1, 0);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
				new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
			}
			return true;
		}
		return false;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component haste = rarity == 6 ? Component.text("II", color) : Component.text("I");
		return Component.text("Every third melee attack applies ")
			.append(Component.text(StringUtils.multiplierToPercentage(SPEED_PERCENT[rarity - 1]) + "%", color))
			.append(Component.text(" speed and Haste "))
			.append(haste)
			.append(Component.text(" for "))
			.append(Component.text(StringUtils.to2DP(DURATION[rarity - 1]), color))
			.append(Component.text(" seconds to players within " + RANGE + " blocks, including the user."));
	}


}


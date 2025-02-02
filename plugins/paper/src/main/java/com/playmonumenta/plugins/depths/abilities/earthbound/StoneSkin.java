package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class StoneSkin extends DepthsAbility {

	public static final String ABILITY_NAME = "Stone Skin";
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "StoneSkinPercentDamageReceivedEffect";
	private static final double[] PERCENT_DAMAGE_RECEIVED = {-.15, -.18, -.22, -.26, -.30, -.38};
	private static final String KNOCKBACK_RESISTANCE_EFFECT_NAME = "StoneSkinKnockbackResistanceEffect";
	private static final double[] KNOCKBACK_RESISTANCE = {0.4, 0.5, 0.6, 0.7, 0.8, 1.0};
	private static final int DURATION = 20 * 5;
	private static final int COOLDOWN = 20 * 12;

	public static final DepthsAbilityInfo<StoneSkin> INFO =
		new DepthsAbilityInfo<>(StoneSkin.class, ABILITY_NAME, StoneSkin::new, DepthsTree.EARTHBOUND, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.STONE_SKIN)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", StoneSkin::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.POLISHED_ANDESITE)
			.descriptions(StoneSkin::getDescription);

	public StoneSkin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		new BukkitRunnable() {
			@Override
			public void run() {
				World world = mPlayer.getWorld();
				Location loc = mPlayer.getLocation();
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.2, 0, 0.2, 0.25).spawnAsPlayerBuff(mPlayer);
				new PartialParticle(Particle.BLOCK_DUST, loc, 20, 0.2, 0, 0.2, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsPlayerBuff(mPlayer);
				loc = loc.add(0, 1, 0);
				world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.25f, 1.35f);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.25f, 1.1f);
				new PartialParticle(Particle.SPELL_INSTANT, loc, 35, 0.4, 0.4, 0.4, 0.25).spawnAsPlayerBuff(mPlayer);
				mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED[mRarity - 1]));
				mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESISTANCE_EFFECT_NAME, new PercentKnockbackResist(DURATION, KNOCKBACK_RESISTANCE[mRarity - 1], KNOCKBACK_RESISTANCE_EFFECT_NAME));
			}
		}.runTaskLater(mPlugin, 1);

	}


	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right click while sneaking to gain ")
			.append(Component.text(StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RECEIVED[rarity - 1]) + "%", color))
			.append(Component.text(" resistance and +"))
			.append(Component.text(StringUtils.multiplierToPercentage(KNOCKBACK_RESISTANCE[rarity - 1] / 10), color))
			.append(Component.text(" knockback resistance for " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}

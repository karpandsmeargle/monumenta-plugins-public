package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class Slipstream extends DepthsAbility {

	public static final String ABILITY_NAME = "Slipstream";
	public static final int[] COOLDOWN = {16 * 20, 14 * 20, 12 * 20, 10 * 20, 8 * 20, 6 * 20};
	private static final int DURATION = 8 * 20;
	private static final double SPEED_AMPLIFIER = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "SlipstreamSpeedEffect";
	private static final int JUMP_AMPLIFIER = 2;
	private static final int RADIUS = 4;
	private static final float KNOCKBACK_SPEED = 0.9f;

	public static final DepthsAbilityInfo<Slipstream> INFO =
		new DepthsAbilityInfo<>(Slipstream.class, ABILITY_NAME, Slipstream::new, DepthsTree.WINDWALKER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.SLIPSTREAM)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Slipstream::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.PHANTOM_MEMBRANE)
			.descriptions(Slipstream::getDescription);

	public Slipstream(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, SPEED_AMPLIFIER, PERCENT_SPEED_EFFECT_NAME));
		int jump = JUMP_AMPLIFIER;
		if (mRarity == 6) {
			jump++;
		}
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION, jump));

		Location loc = mPlayer.getEyeLocation();
		loc.add(0, -0.75, 0);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.25f);

		new BukkitRunnable() {
			double mRadius = 0;
			@Override
			public void run() {
				mRadius += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					loc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.CLOUD, loc, 3, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
					loc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}
				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer.getLocation(), mob, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2, true);
			}
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component jump = rarity == 6 ? Component.text("IV", color) : Component.text("III");
		return Component.text("Right click to knock all enemies within " + RADIUS + " blocks away from you and gain Jump Boost ")
			.append(jump)
			.append(Component.text(" and " + StringUtils.multiplierToPercentage(SPEED_AMPLIFIER) + "% speed for " + DURATION / 20 + " seconds. Cooldown: "))
			.append(Component.text(StringUtils.ticksToSeconds(COOLDOWN[rarity - 1]) + "s", color))
			.append(Component.text("."));
	}

}


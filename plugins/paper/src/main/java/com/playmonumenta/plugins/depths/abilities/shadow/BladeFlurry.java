package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
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
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BladeFlurry extends DepthsAbility {

	public static final String ABILITY_NAME = "Blade Flurry";
	public static final int COOLDOWN = 20 * 6;
	public static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 3;
	public static final int[] SILENCE_DURATION = {20, 25, 30, 35, 40, 50};

	public static final DepthsAbilityInfo<BladeFlurry> INFO =
		new DepthsAbilityInfo<>(BladeFlurry.class, ABILITY_NAME, BladeFlurry::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.BLADE_FLURRY)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BladeFlurry::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.IRON_SWORD)
			.descriptions(BladeFlurry::getDescription);

	public BladeFlurry(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		putOnCooldown();

		World mWorld = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation().add(0, -0.5, 0);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, 3);
		for (LivingEntity mob : mobs) {
			EntityUtils.applySilence(mPlugin, SILENCE_DURATION[mRarity - 1], mob);
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
			MovementUtils.knockAway(mPlayer, mob, 0.8f, true);
		}
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.75f);

		cancelOnDeath(new BukkitRunnable() {
			final Vector mEyeDir = loc.getDirection();

			double mStartAngle = Math.atan(mEyeDir.getZ() / mEyeDir.getX());
			int mIncrementDegrees = 0;

			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (mEyeDir.getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI * 90 / 180;
				}
				Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
				Vector direction = new Vector(Math.cos(mStartAngle - Math.PI * mIncrementDegrees / 180), 0, Math.sin(mStartAngle - Math.PI * mIncrementDegrees / 180));
				Location bladeLoc = mLoc.clone().add(direction.clone().multiply(3.0));

				new PartialParticle(Particle.SPELL_WITCH, bladeLoc, 10, 0.35, 0, 0.35, 1).spawnAsPlayerActive(mPlayer);
				mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.5f);

				if (mIncrementDegrees >= 360) {
					this.cancel();
				}

				mIncrementDegrees += 30;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right click while sneaking and holding a weapon to deal ")
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text(" melee damage in a " + RADIUS + " block radius around you. Affected mobs are silenced for "))
			.append(Component.text(StringUtils.to2DP(SILENCE_DURATION[rarity - 1] / 20.0), color))
			.append(Component.text(" seconds and knocked away slightly. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}


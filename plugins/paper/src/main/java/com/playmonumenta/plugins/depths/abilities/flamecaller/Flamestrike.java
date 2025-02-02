package com.playmonumenta.plugins.depths.abilities.flamecaller;

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
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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

public class Flamestrike extends DepthsAbility {
	public static final String ABILITY_NAME = "Flamestrike";
	public static final int COOLDOWN = 10 * 20;
	public static final double[] DAMAGE = {14, 17.5, 21, 24.5, 28, 35};
	public static final int HEIGHT = 6;
	public static final int RADIUS = 7;
	public static final int ANGLE = 70;
	public static final int FIRE_TICKS = 4 * 20;
	public static final float KNOCKBACK = 0.5f;

	public static final DepthsAbilityInfo<Flamestrike> INFO =
		new DepthsAbilityInfo<>(Flamestrike.class, ABILITY_NAME, Flamestrike::new, DepthsTree.FLAMECALLER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.FLAMESTRIKE)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Flamestrike::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.FLINT_AND_STEEL)
			.descriptions(Flamestrike::getDescription);

	public Flamestrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Hitbox hitbox = Hitbox.approximateCylinderSegment(
			LocationUtils.getHalfHeightLocation(mPlayer).add(0, -HEIGHT, 0), 2 * HEIGHT, RADIUS, Math.toRadians(ANGLE));
		for (LivingEntity mob : hitbox.getHitMobs()) {
			EntityUtils.applyFire(mPlugin, FIRE_TICKS, mob, mPlayer);
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true, true);
			MovementUtils.knockAway(mPlayer, mob, KNOCKBACK, true);
		}

		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1).spawnAsPlayerActive(mPlayer);
				}

				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.5f);
		putOnCooldown();
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right click while sneaking to create a torrent of flames, dealing ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]), color))
			.append(Component.text(" magic damage to all enemies in front of you within " + RADIUS + " blocks, setting them on fire for " + FIRE_TICKS / 20 + " seconds and knocking them away. Cooldown: " + COOLDOWN / 20 + "s."));
	}

}

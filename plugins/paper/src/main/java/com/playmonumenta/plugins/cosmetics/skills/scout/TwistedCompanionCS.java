package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
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
import org.jetbrains.annotations.Nullable;

public class TwistedCompanionCS extends HuntingCompanionCS {
	//Twisted theme

	public static final String NAME = "Twisted Companion";

	private final String FOX_NAME = "TwistedCompanion";

	private static final double HELIX_RADIUS = 0.4;
	private static final Color TWIST_COLOR_BASE = Color.fromRGB(130, 66, 66);
	private static final Color TWIST_COLOR_TIP = Color.fromRGB(127, 0, 0);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Bound to you by a twisted energy,",
			"this companion will execute your orders",
			"and your targets.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_ROSE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String getFoxName() {
		return FOX_NAME;
	}

	@Override
	public void foxOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 0.7f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 0.85f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, SoundCategory.NEUTRAL, 2.0f, 0.9f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.5f, 0.85f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.NEUTRAL, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 1.5f);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-1, 1), 1,
			FastUtils.randomDoubleInRange(-1, 1)), player.getLocation().add(0, 1.35, 0), player, summon);

		spawnRing(player.getLocation(), player, 3);
	}

	@Override
	public void foxTick(LivingEntity summon, Player player, LivingEntity target, int t) {
		Location loc = LocationUtils.getHalfHeightLocation(summon);
		for (int i = 0; i < 2; i++) {
			double rotation = FastMath.toRadians((t * 10) + (i * 180));
			Vector vec = new Vector(FastUtils.cos(rotation) * HELIX_RADIUS, 0,
				FastUtils.sin(rotation) * HELIX_RADIUS);
			vec = VectorUtils.rotateXAxis(vec, 90);
			vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
			Location l = loc.clone().add(vec);
			new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0,
				new Particle.DustOptions(TWIST_COLOR_TIP, 1)).spawnAsPlayerActive(player);
		}

		if (target != null && !target.isDead() && target.isValid()) {
			loc = LocationUtils.getHalfHeightLocation(target);
			for (int i = 0; i < 2; i++) {
				double rotation = FastMath.toRadians((t * 10) + (i * 180));
				Vector vec = new Vector(FastUtils.cos(rotation), 0,
					FastUtils.sin(rotation));
				Location l = loc.clone().add(vec);
				new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0.05, 0.05, 0,
					new Particle.DustOptions(TWIST_COLOR_TIP, 1)).spawnAsPlayerActive(player);
			}
		}
	}

	@Override
	public void onAggroParticles(Player mPlayer, LivingEntity summon) {
		new PartialParticle(Particle.SOUL, summon.getEyeLocation(), 15, 0.25, 0.25, 0.25, 0.005).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void foxOnDespawn(World world, Location loc, Player player, LivingEntity summon) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.NEUTRAL, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 1.5f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.NEUTRAL, 1.5f, 0.7f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 35, 0.15, 0.15, 0.15, 0.125F).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 30, 0, 0, 0, 0.6F).spawnAsPlayerActive(player);

		spawnRing(loc, player, 2);
		if (player.isOnline() && player.getWorld() == summon.getWorld()) {
			createOrb(new Vector(FastUtils.randomDoubleInRange(-1, 1), 1,
				FastUtils.randomDoubleInRange(-1, 1)), LocationUtils.getHalfHeightLocation(summon), player, player);
		}
	}

	private void spawnRing(Location loc, Player mPlayer, double r) {
		Location l = loc.clone().add(0, 0.1, 0);
		l.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 60, 0.2f,
			true, 0, Particle.SMOKE_NORMAL);
		new BukkitRunnable() {

			double mRadius = 0;
			final double RADIUS = r;
			@Override
			public void run() {

				for (int i = 0; i < 2; i++) {
					mRadius += 0.35;
					for (int degree = 0; degree < 360; degree += 5) {
						double radian = FastMath.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
							FastUtils.sin(radian) * mRadius);
						Location loc = l.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0,
							new Particle.DustOptions(
								ParticleUtils.getTransition(TWIST_COLOR_BASE, TWIST_COLOR_TIP, mRadius / RADIUS),
								0.75f
							)).spawnAsPlayerActive(mPlayer);
					}
				}

				if (mRadius >= RADIUS) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void createOrb(Vector dir, Location loc, Player mPlayer, LivingEntity target) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				if (!mL.getWorld().equals(target.getWorld())) {
					cancel();
					return;
				}

				mT++;

				Location to = LocationUtils.getHalfHeightLocation(target);

				for (int i = 0; i < 3; i++) {
					if (mT <= 4) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.1;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.4) {
						mD.normalize().multiply(0.4);
					}

					mL.add(mD);

					new PartialParticle(Particle.REDSTONE, mL, 3, 0.15, 0.15, 0.15, 0, new Particle.DustOptions(TWIST_COLOR_TIP, 1.5f))

						.spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, mL, 2, 0.15, 0.15, 0.15, 0.05F)

						.spawnAsPlayerActive(mPlayer);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(mL, Sound.ENTITY_FOX_AGGRO, SoundCategory.NEUTRAL, 1.25f, 0);
						world.playSound(mL, Sound.ENTITY_FOX_BITE, SoundCategory.NEUTRAL, 1.25f, 0.5f);
						new PartialParticle(Particle.CRIT, mL, 20, 0, 0, 0, 0.6F)
							.spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMOKE_NORMAL, mL, 25, 0, 0, 0, 0.1F)
							.spawnAsPlayerActive(mPlayer);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}

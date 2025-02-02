package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GraspingClawsCS implements CosmeticSkill {
	private static final BlockData CHAIN_PARTICLE = Material.CHAIN.createBlockData();

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.GRASPING_CLAWS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BOW;
	}

	public void onLand(Player player, World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.2f, 2.0f);
		new PartialParticle(Particle.PORTAL, loc, 125, 2, 2, 2, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.PORTAL, loc, 400, 0, 0, 0, 1.45).spawnAsPlayerActive(player);
		new PartialParticle(Particle.DRAGON_BREATH, loc, 85, 0, 0, 0, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, loc, 150, 2, 2, 2, CHAIN_PARTICLE).spawnAsPlayerActive(player);
	}

	public void onCageCreation(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.8f, 0.9f);
	}

	public void cageParticle(Player player, Location loc) {
		new PartialParticle(Particle.FALLING_DUST, loc, 1, 0.1, 0.2, 0.1, CHAIN_PARTICLE).spawnAsPlayerActive(player);
	}

	public void onCagedMob(Player player, World world, Location loc, LivingEntity mob) {
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, .75f, 0.8f);
		double halfHeight = mob.getHeight() / 2;
		double halfWidth = mob.getWidth() / 2;
		new PartialParticle(Particle.FALLING_DUST, loc.clone().add(0, halfHeight, 0), 3,
			halfWidth, halfHeight + 0.1, halfWidth, CHAIN_PARTICLE)
			.spawnAsPlayerActive(player);
	}

	// Modifies the lists provided!
	public List<Integer> cageTick(Player player, Location loc, double radius, List<Integer> degrees1, List<Integer> degrees2, List<Integer> degrees3) {
		List<Integer> degreesToKeep = new ArrayList<>();
		for (int d = 0; d < 360; d += 3) {
			new PartialParticle(Particle.FALLING_DUST, loc.clone().add(radius * FastUtils.cosDeg(d), 0, radius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE).spawnAsPlayerActive(player);
			new PartialParticle(Particle.FALLING_DUST, loc.clone().add(radius * FastUtils.cosDeg(d), 5, radius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE).spawnAsPlayerActive(player);

			if (degrees1.contains(d)) {
				new PartialParticle(Particle.FALLING_DUST, loc.clone().add(radius * FastUtils.cosDeg(d), 5.5, radius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE).spawnAsPlayerActive(player);
				if (FastUtils.RANDOM.nextBoolean()) {
					degrees1.remove((Integer) d);
				}
			}

			if (degrees2.contains(d)) {
				new PartialParticle(Particle.FALLING_DUST, loc.clone().add(radius * FastUtils.cosDeg(d), 6, radius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE).spawnAsPlayerActive(player);
				if (FastUtils.RANDOM.nextBoolean()) {
					degrees2.remove((Integer) d);
				}
			}

			if (degrees3.contains(d)) {
				new PartialParticle(Particle.FALLING_DUST, loc.clone().add(radius * FastUtils.cosDeg(d), 6.75, radius * FastUtils.sinDeg(d)), 1, CHAIN_PARTICLE).spawnAsPlayerActive(player);
			}

			if (FastUtils.randomDoubleInRange(0, 1) < 0.25) {
				degreesToKeep.add(d);
			}
		}

		return degreesToKeep;
	}

	public String getProjectileName() {
		return "Grasping Claws Projectile";
	}

	public @Nullable Particle getProjectileParticle() {
		return Particle.SPELL_WITCH;
	}
}

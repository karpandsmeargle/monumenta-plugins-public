package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class Detonation extends DepthsAbility {

	public static final String ABILITY_NAME = "Detonation";
	public static final double[] DAMAGE = {3.5, 4.0, 4.5, 5, 5.5, 7};
	public static final int DEATH_RADIUS = 8;
	public static final int DAMAGE_RADIUS = 2;

	public static final DepthsAbilityInfo<Detonation> INFO =
		new DepthsAbilityInfo<>(Detonation.class, ABILITY_NAME, Detonation::new, DepthsTree.FLAMECALLER, DepthsTrigger.PASSIVE)
			.displayItem(Material.TNT)
			.descriptions(Detonation::getDescription);

	public Detonation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		Entity entity = event.getEntity();
		if (entity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) && !DummyDecoy.DUMMY_NAME.equals(entity.getName())) {
			return;
		}
		Location location = entity.getLocation();
		World world = mPlayer.getWorld();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, DAMAGE_RADIUS)) {
			new PartialParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, mob.getLocation().add(0, 1, 0), 2).spawnAsPlayerActive(mPlayer);
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true, false);
		}
		new PartialParticle(Particle.EXPLOSION_LARGE, location.add(0, 0.5, 0), 1).minimumCount(1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location.add(0, 1, 0), 3).spawnAsPlayerActive(mPlayer);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1);
	}

	@Override
	public double entityDeathRadius() {
		return DEATH_RADIUS;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("If an enemy dies within " + DEATH_RADIUS + " blocks of you it explodes, dealing ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]), color))
			.append(Component.text(" magic damage in a " + DAMAGE_RADIUS + " block radius to other enemies."));
	}
}

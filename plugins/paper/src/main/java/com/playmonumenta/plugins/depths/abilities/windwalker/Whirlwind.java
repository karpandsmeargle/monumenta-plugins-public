package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;


public class Whirlwind extends DepthsAbility {

	public static final String ABILITY_NAME = "Whirlwind";
	private static final int RADIUS = 3;
	private static final double[] KNOCKBACK_SPEED = {0.8, 1.0, 1.2, 1.4, 1.6, 2.0};
	private static final double[] SPEED = {0.1, 0.125, 0.15, 0.175, 0.2, 0.3};
	private static final int SPEED_DURATION = 6 * 20;
	private static final String SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";

	public static final DepthsAbilityInfo<Whirlwind> INFO =
		new DepthsAbilityInfo<>(Whirlwind.class, ABILITY_NAME, Whirlwind::new, DepthsTree.WINDWALKER, DepthsTrigger.SPAWNER)
			.displayItem(Material.IRON_PICKAXE)
			.descriptions(Whirlwind::getDescription);

	public Whirlwind(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		//If we break a spawner with a pickaxe
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			World world = event.getPlayer().getWorld();
			Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.25f);
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.35f);
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.45f);
			new PartialParticle(Particle.CLOUD, loc, 30, 1, 1, 1, 0.8).spawnAsPlayerActive(mPlayer);
			for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS)) {
				e.setVelocity(e.getVelocity().add(e.getLocation().toVector().subtract(loc.subtract(0, 0.5, 0).toVector()).normalize().multiply(KNOCKBACK_SPEED[mRarity - 1]).add(new Vector(0, 0.3, 0))));
				new PartialParticle(Particle.EXPLOSION_NORMAL, e.getLocation(), 5, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
			}
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(SPEED_DURATION, SPEED[mRarity - 1], SPEED_EFFECT_NAME));
		}
		return true;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Breaking a spawner knocks back all mobs within " + RADIUS + " blocks with a speed of ")
			.append(Component.text(StringUtils.to2DP(KNOCKBACK_SPEED[rarity - 1]), color))
			.append(Component.text(". Additionally, you receive "))
			.append(Component.text(StringUtils.multiplierToPercentage(SPEED[rarity - 1]) + "%", color))
			.append(Component.text(" speed for " + SPEED_DURATION / 20 + " seconds."));
	}

}


package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
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
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class Entrench extends DepthsAbility {

	public static final String ABILITY_NAME = "Entrench";

	public static final int[] DURATION = {20, 25, 30, 35, 40, 60};
	public static final int RADIUS = 6;
	public static final double SLOW_MODIFIER = 0.99;

	public static final DepthsAbilityInfo<Entrench> INFO =
		new DepthsAbilityInfo<>(Entrench.class, ABILITY_NAME, Entrench::new, DepthsTree.EARTHBOUND, DepthsTrigger.SPAWNER)
			.displayItem(Material.SOUL_SAND)
			.descriptions(Entrench::getDescription);

	public Entrench(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();
		World world = block.getWorld();
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			Location centerLoc = block.getLocation().add(0.5, 0, 0.5);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(centerLoc, RADIUS)) {
				EntityUtils.applySlow(mPlugin, DURATION[mRarity - 1], SLOW_MODIFIER, mob);
			}

			world.playSound(centerLoc, Sound.BLOCK_NETHER_BRICKS_BREAK, SoundCategory.PLAYERS, 1.2f, 0.45f);
			world.playSound(centerLoc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundCategory.PLAYERS, 1, 0.6f);
			new PartialParticle(Particle.BLOCK_DUST, centerLoc, 35, 1.5, 1.5, 1.5, 1, Material.SOUL_SOIL.createBlockData()).spawnAsPlayerActive(mPlayer);
		}
		return true;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Breaking a spawner roots mobs within " + RADIUS + " blocks for ")
			.append(Component.text(StringUtils.to2DP(DURATION[rarity - 1] / 20.0), color))
			.append(Component.text(" seconds."));
	}


}

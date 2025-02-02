package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FrigidCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Frigid Combos";
	public static final int TIME = 2 * 20;
	public static final double[] SLOW_AMPLIFIER = {0.2, 0.25, 0.3, 0.35, 0.4, 0.5};
	public static final int[] DAMAGE = {2, 3, 4, 5, 6, 8};
	public static final int RADIUS = 4;

	public static final DepthsAbilityInfo<FrigidCombos> INFO =
		new DepthsAbilityInfo<>(FrigidCombos.class, ABILITY_NAME, FrigidCombos::new, DepthsTree.FROSTBORN, DepthsTrigger.COMBO)
			.displayItem(Material.BLUE_DYE)
			.descriptions(FrigidCombos::getDescription);

	private int mComboCount = 0;

	public FrigidCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {
				Location targetLoc = enemy.getLocation();
				World world = targetLoc.getWorld();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, RADIUS)) {
					if (!(mob.getHealth() <= 0)) {
						new PartialParticle(Particle.CRIT_MAGIC, mob.getLocation(), 25, .5, .2, .5, 0.65).spawnAsPlayerActive(mPlayer);
						EntityUtils.applySlow(mPlugin, TIME, SLOW_AMPLIFIER[mRarity - 1], mob);
						DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true);
					}
				}

				mComboCount = 0;

				//Particles
				Location playerLoc = mPlayer.getLocation().add(0, 1, 0);
				world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.65f);
				world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.45f);
				new PartialParticle(Particle.SNOW_SHOVEL, targetLoc, 25, .5, .2, .5, 0.65).spawnAsPlayerActive(mPlayer);
			}
			return true;
		}
		return false;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Every third melee attack deals ")
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text(" magic damage to all mobs within " + RADIUS + " blocks and applies "))
			.append(Component.text(StringUtils.multiplierToPercentage(SLOW_AMPLIFIER[rarity - 1]) + "%", color))
			.append(Component.text(" slowness for " + TIME / 20.0 + " seconds to affected mobs."));
	}


}


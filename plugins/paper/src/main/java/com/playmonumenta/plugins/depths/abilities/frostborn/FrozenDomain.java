package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FrozenDomain extends DepthsAbility {

	public static final String ABILITY_NAME = "Frozen Domain";
	public static final double[] EXTRA_SPEED_PCT = {.1, .125, .15, .175, .20, .25};
	public static final double[] REGEN_TIME = {2, 1.75, 1.5, 1.25, 1, .75}; //seconds
	private static final int DURATION_TICKS = 100;
	private static final double PERCENT_HEAL = .05;
	private static final String ATTR_NAME = "FrozenDomainExtraSpeedAttr";

	public static final DepthsAbilityInfo<FrozenDomain> INFO =
		new DepthsAbilityInfo<>(FrozenDomain.class, ABILITY_NAME, FrozenDomain::new, DepthsTree.FROSTBORN, DepthsTrigger.PASSIVE)
			.displayItem(Material.IRON_BOOTS)
			.descriptions(FrozenDomain::getDescription);
	private boolean mWasOnIce = false;
	private int mSecondWhenIce = 0;
	private int mSeconds = 0;

	public FrozenDomain(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (twoHertz && isOnIce(mPlayer)) {
			new PartialParticle(Particle.SNOW_SHOVEL, mPlayer.getLocation(), 8, 0, 0, 0, 0.65).spawnAsPlayerPassive(mPlayer);
		}
		if (oneSecond) {
			if (PlayerUtils.isOnGround(mPlayer) && isOnIce(mPlayer)) {
				mWasOnIce = true;
				mSecondWhenIce = mSeconds;
				handleSpeed();
				handleHeal();
			} else {
				offIceHeal();
			}
			if (mSeconds >= mSecondWhenIce + DURATION_TICKS / 20) {
				mWasOnIce = false;
			}
			mSeconds++;
		}

	}

	public void handleParticles() {
		new PartialParticle(Particle.HEART, mPlayer.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.65).spawnAsPlayerPassive(mPlayer);
		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.SNOW_SHOVEL, mPlayer.getLocation().add(0, 1, 0), 8, 0, 0, 0, 0.65).spawnAsPlayerPassive(mPlayer);
				if (mCount >= 5) {
					this.cancel();
				}
				mCount++;
			}
		}.runTaskTimer(mPlugin, 0, 4);
	}

	public void handleHeal() {
		if (mSeconds % REGEN_TIME[mRarity - 1] == 0) {
			applyHealing();
		}
	}

	public void offIceHeal() {
		if (mWasOnIce && mSeconds % REGEN_TIME[mRarity - 1] == 0) {
			applyHealing();
		}
	}

	public void applyHealing() {
		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		PlayerUtils.healPlayer(mPlugin, mPlayer, PERCENT_HEAL * maxHealth, mPlayer);
		handleParticles();
	}

	public void handleSpeed() {
		mPlugin.mEffectManager.addEffect(mPlayer, "FrozenDomainExtraSpeed", new PercentSpeed(DURATION_TICKS, EXTRA_SPEED_PCT[mRarity - 1], ATTR_NAME));
	}

	public boolean isOnIce(LivingEntity entity) {
		Location loc = entity.getLocation();
		return DepthsUtils.isIce(loc.getBlock().getRelative(BlockFace.DOWN).getType()) &&
			DepthsUtils.iceActive.containsKey(loc.getBlock().getRelative(BlockFace.DOWN).getLocation());
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component s = REGEN_TIME[rarity - 1] == 20 ? Component.empty() : Component.text("s");
		return Component.text("When standing on ice, gain ")
			.append(Component.text(StringUtils.multiplierToPercentage(EXTRA_SPEED_PCT[rarity - 1]) + "%", color))
			.append(Component.text(" speed and regain " + StringUtils.multiplierToPercentage(PERCENT_HEAL) + "% of your max health every "))
			.append(Component.text(StringUtils.to2DP(REGEN_TIME[rarity - 1]), color))
			.append(Component.text(" second"))
			.append(s)
			.append(Component.text(". Effects last for " + DURATION_TICKS / 20 + " seconds after leaving ice."));
	}
}


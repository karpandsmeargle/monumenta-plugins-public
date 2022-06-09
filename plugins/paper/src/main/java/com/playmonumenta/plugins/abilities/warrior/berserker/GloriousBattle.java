package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class GloriousBattle extends Ability implements AbilityWithChargesOrStacks {
	private static final int LEVEL_ONE_CHARGES = 1;
	private static final int LEVEL_TWO_CHARGES = 2;
	private static final int DAMAGE = 20;
	private static final double RADIUS = 3;
	private static final double BLEED_PERCENT = 0.2;
	private static final int BLEED_TIME = 4 * 20;

	private int mStacks = 0;
	private int mStackLimit;
	private LivingEntity mTarget;

	public GloriousBattle(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Glorious Battle");
		mInfo.mLinkedSpell = ClassAbility.GLORIOUS_BATTLE;
		mInfo.mCooldown = 0;
		mStackLimit = getAbilityScore() == 1 ? LEVEL_ONE_CHARGES : LEVEL_TWO_CHARGES;
		mInfo.mScoreboardId = "GloriousBattle";
		mInfo.mShorthandName = "GB";
		mInfo.mDescriptions.add("Dealing indirect damage with an ability grants you a Glorious Battle stack. Shift and swap hands while looking at an enemy to consume a stack and charge forwards. Landing within a " + RADIUS + " block radius of the targeted mob will deal " + DAMAGE + " damage and " +
				"apply " + (int) DepthsUtils.roundPercent(BLEED_PERCENT) + "% bleed to the mob for " + (BLEED_TIME / 20) + " seconds.");
		mInfo.mDescriptions.add("Max charges increased to 2.");
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (!isValidTimeToCast()) {
			return;
		}

		if (mPlayer == null || mTarget == null) {
			return;
		}

		mStacks--;
		Vector dir = mPlayer.getLocation().getDirection();
		Vector yVelocity = new Vector(0, dir.getY() * 0.2 + 0.3, 0);
		mPlayer.setVelocity(dir.multiply(1.4).add(yVelocity));
		MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle Stacks: " + mStacks);
		Location location = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2f, 0.5f);
		new PartialParticle(Particle.CRIMSON_SPORE, location, 25, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, location, 15, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				if (mPlayer.isOnGround()) {
					new BukkitRunnable() {
						@Override
						public void run() {
							doBattle();
						}
					}.runTaskLater(mPlugin, 0);
					this.cancel();
				}

				//Logged off or something probably
				if (mT > 100) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 10, 2);
	}

	public void doBattle() {
		if (!EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer).contains(mTarget)) {
			return;
		}
		Location location = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1f, 1f);
		new PartialParticle(Particle.SWEEP_ATTACK, location, 20, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
		EntityUtils.applyBleed(mPlugin, BLEED_TIME, BLEED_PERCENT, mTarget);
		DamageUtils.damage(mPlayer, mTarget, DamageType.MELEE_SKILL, DAMAGE, ClassAbility.GLORIOUS_BATTLE, true);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.isWarriorAoe()) {
			int previousStacks = mStacks;
			if (mStacks < mStackLimit) {
				mStacks++;
				MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle Stacks: " + mStacks);
			}
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		return false;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mStackLimit;
	}

	public boolean isValidTimeToCast() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isSomeBow(inMainHand) || ItemUtils.isSomePotion(inMainHand) || inMainHand.getType().isBlock()
			|| inMainHand.getType().isEdible() || inMainHand.getType() == Material.TRIDENT || !mPlayer.isSneaking()) {
			return false;
		}

		if (mStacks < 1) {
			return false;
		}

		Location eyeLoc = mPlayer.getEyeLocation();
		Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), 10);
		ray.mThroughBlocks = false;
		ray.mThroughNonOccluding = false;
		if (AbilityManager.getManager().isPvPEnabled(mPlayer)) {
			ray.mTargetPlayers = true;
		}

		RaycastData data = ray.shootRaycast();

		List<LivingEntity> rayEntities = data.getEntities();
		if (rayEntities != null && !rayEntities.isEmpty()) {
			for (LivingEntity t : rayEntities) {
				if (!t.getUniqueId().equals(mPlayer.getUniqueId()) && t.isValid() && !t.isDead() && EntityUtils.isHostileMob(t)) {
					mTarget = t;
					return true;
				}
			}
		}
		return false;
	}
}
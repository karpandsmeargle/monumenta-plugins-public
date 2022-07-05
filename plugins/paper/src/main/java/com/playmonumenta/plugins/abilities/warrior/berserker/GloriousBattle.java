package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
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
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class GloriousBattle extends Ability implements AbilityWithChargesOrStacks {
	private static final int DAMAGE_1 = 20;
	private static final int DAMAGE_2 = 30;
	private static final double RADIUS = 3;
	private static final double BLEED_PERCENT = 0.2;
	private static final int BLEED_TIME = 4 * 20;
	private static final float KNOCK_AWAY_SPEED = 0.4f;
	private static final String KBR_EFFECT = "GloriousBattleKnockbackResistanceEffect";
	private static final double DAMAGE_PER = 0.05;
	private static final int MAX_TARGETING = 6;
	private static final double TARGET_RANGE = 8;

	private static final EnumSet<ClassAbility> AFFECTED_ABILITIES = EnumSet.of(
		ClassAbility.BRUTE_FORCE,
		ClassAbility.COUNTER_STRIKE_AOE,
		ClassAbility.SHIELD_BASH_AOE,
		ClassAbility.METEOR_SLAM,
		ClassAbility.RAMPAGE);

	private int mStacks;
	private final int mStackLimit;
	private final double mDamage;
	private @Nullable LivingEntity mTarget;

	public GloriousBattle(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Glorious Battle");
		mInfo.mLinkedSpell = ClassAbility.GLORIOUS_BATTLE;
		mInfo.mCooldown = 0;
		mInfo.mScoreboardId = "GloriousBattle";
		mInfo.mShorthandName = "GB";
		mInfo.mDescriptions.add("Dealing indirect damage with an ability grants you a Glorious Battle stack. Shift and swap hands while looking at an enemy to consume a stack and charge forwards, gaining full knockback resistance until landing. Landing within a 3 block radius of the targeted mob will deal " + DAMAGE_1 + " damage and " +
				"apply " + (int) DepthsUtils.roundPercent(BLEED_PERCENT) + "% bleed to the mob for " + (BLEED_TIME / 20) + " seconds. Additionally when landing, knock back all mobs within 3 blocks.");
		mInfo.mDescriptions.add("Damage increased to 30. Additionally, you now passively gain 5% melee damage for each mob targeting you within 8 blocks, up to 6 mobs.");
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mStackLimit = 1;
		mStacks = 0;
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
		mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT, new PercentKnockbackResist(100, 1, KBR_EFFECT));
		MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle Stacks: " + mStacks);
		ClientModHandler.updateAbility(mPlayer, this);
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
		if (mPlayer == null) {
			return;
		}

		mPlugin.mEffectManager.clearEffects(mPlayer, KBR_EFFECT);

		double radius = RADIUS;
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
			MovementUtils.knockAway(mPlayer, mob, KNOCK_AWAY_SPEED, true);
		}

		if (mTarget == null || mPlayer.getLocation().distance(mTarget.getLocation()) > radius) {
			return;
		}
		Location location = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1f, 1f);
		new PartialParticle(Particle.SWEEP_ATTACK, location, 20, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
		EntityUtils.applyBleed(mPlugin, BLEED_TIME, BLEED_PERCENT, mTarget);
		DamageUtils.damage(mPlayer, mTarget, DamageType.MELEE_SKILL, mDamage, ClassAbility.GLORIOUS_BATTLE, true);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (AFFECTED_ABILITIES.contains(event.getAbility()) && !MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "GloriousBattleStackIncrease")) {
			int previousStacks = mStacks;
			if (mStacks < mStackLimit) {
				mStacks++;
				if (mStackLimit > 1) {
					MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle Stacks: " + mStacks);
				} else {
					MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle is ready!");
				}
			}
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		DamageEvent.DamageType type = event.getType();
		if (type == DamageType.MELEE || type == DamageType.MELEE_SKILL || type == DamageType.MELEE_ENCH) {
			int count = 0;
			for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), TARGET_RANGE)) {
				if (le instanceof Mob mob && mob.getTarget() == mPlayer) {
					count++;
				}
			}
			if (count > 0) {
				if (count > MAX_TARGETING) {
					count = MAX_TARGETING;
				}
				event.setDamage(event.getDamage() * (1 + count * DAMAGE_PER));
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
		if (ItemUtils.isBowOrTrident(inMainHand) || ItemUtils.isSomePotion(inMainHand) || inMainHand.getType().isBlock()
			|| inMainHand.getType().isEdible() || !mPlayer.isSneaking()) {
			return false;
		}

		if (mStacks < 1) {
			return false;
		}

		Location eyeLoc = mPlayer.getEyeLocation();
		Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), 10);
		ray.mThroughBlocks = false;
		ray.mThroughNonOccluding = false;

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

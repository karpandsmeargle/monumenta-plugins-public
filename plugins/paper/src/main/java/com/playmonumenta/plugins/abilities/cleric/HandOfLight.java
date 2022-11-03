package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HandOfLightCS;
import com.playmonumenta.plugins.effects.CrusadeEnhancementTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;


public class HandOfLight extends Ability {

	public static final int RANGE = 12;
	private static final double HEALING_ANGLE = 70; // (half) angle of the healing cone
	private static final double HEALING_DOT_ANGLE = Math.cos(Math.toRadians(HEALING_ANGLE));
	private static final int HEALING_1_COOLDOWN = 14 * 20;
	private static final int HEALING_2_COOLDOWN = 10 * 20;
	private static final int FLAT_1 = 2;
	private static final int FLAT_2 = 4;
	private static final double PERCENT_1 = 0.1;
	private static final double PERCENT_2 = 0.2;
	private static final int DAMAGE_PER_1 = 2;
	private static final int DAMAGE_PER_2 = 3;
	private static final int DAMAGE_MAX_1 = 8;
	private static final int DAMAGE_MAX_2 = 9;
	private static final double ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED = 0.025;
	private static final double ENHANCEMENT_COOLDOWN_REDUCTION_MAX = 0.5;
	private static final int ENHANCEMENT_UNDEAD_STUN_DURATION = 10;

	public static final String CHARM_DAMAGE = "Hand of Light Damage";
	public static final String CHARM_COOLDOWN = "Hand of Light Cooldown";
	public static final String CHARM_RANGE = "Hand of Light Range";
	public static final String CHARM_HEALING = "Hand of Light Healing";

	private final double mRange;
	private final int mFlat;
	private final double mPercent;
	private final double mDamagePer;
	private final double mDamageMax;

	private @Nullable Crusade mCrusade;
	private boolean mHasCleansingRain;
	private boolean mHasLuminousInfusion;

	private final HandOfLightCS mCosmetic;

	public HandOfLight(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Hand of Light");
		mInfo.mLinkedSpell = ClassAbility.HAND_OF_LIGHT;
		mInfo.mScoreboardId = "Healing";
		mInfo.mShorthandName = "HoL";
		mInfo.mDescriptions.add("Right click while holding a weapon or tool to heal all other players in a 12 block cone in front of you or within 2 blocks of you " +
			                        "for 2 hearts + 10% of their max health and gives them regen 2 for 4 seconds. " +
			                        "Additionally, damage all mobs in the area with magic damage equal to 2 times the number of undead mobs in the range, up to 8 damage. " +
			                        "If holding a shield, the trigger is changed to sneak + right click. Cooldown: 14s.");
		mInfo.mDescriptions.add("The healing is improved to 4 hearts + 20% of their max health. Damage increases to 3 damage per undead mob, up to 9. Cooldown: 10s.");
		mInfo.mDescriptions.add(
			String.format("The cone is changed to a sphere of equal range, centered on the Cleric." +
				              " The cooldown is reduced by %s%% for each 4 health healed, capped at %s%% cooldown." +
				              " All Undead caught in the radius are stunned for %ss.",
				(int) (ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED * 100),
				(int) (ENHANCEMENT_COOLDOWN_REDUCTION_MAX * 100),
				ENHANCEMENT_UNDEAD_STUN_DURATION / 20.0
			));
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, isLevelOne() ? HEALING_1_COOLDOWN : HEALING_2_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.PINK_DYE, 1);

		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mFlat = isLevelOne() ? FLAT_1 : FLAT_2;
		mPercent = isLevelOne() ? PERCENT_1 : PERCENT_2;
		mDamagePer = isLevelOne() ? DAMAGE_PER_1 : DAMAGE_PER_2;
		mDamageMax = isLevelOne() ? DAMAGE_MAX_1 : DAMAGE_MAX_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HandOfLightCS(), HandOfLightCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);

			mHasCleansingRain = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, CleansingRain.class) != null;
			mHasLuminousInfusion = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, LuminousInfusion.class) != null;
		});
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}

		// If holding a shield, must be sneaking to activate
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offhand = mPlayer.getInventory().getItemInOffHand();
		if (!mPlayer.isSneaking() && (offhand.getType() == Material.SHIELD || mainhand.getType() == Material.SHIELD)) {
			return;
		}

		// Must not match conditions for cleansing rain
		if (mHasCleansingRain && mPlayer.getLocation().getPitch() <= -50) {
			return;
		}

		// Must not match conditions for luminous infusion
		if (mHasLuminousInfusion && mPlayer.getLocation().getPitch() >= 50) {
			return;
		}

		//Cannot be cast with multitool.
		if (mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.MULTITOOL) > 0) {
			return;
		}

		World world = mPlayer.getWorld();
		Location userLoc = mPlayer.getLocation();

		Hitbox hitbox;
		if (!isEnhanced()) {
			hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mRange, Math.toRadians(HEALING_ANGLE))
				         .union(new Hitbox.SphereHitbox(mPlayer.getLocation(), 2));
		} else {
			hitbox = new Hitbox.SphereHitbox(mPlayer.getEyeLocation(), mRange);
		}
		List<LivingEntity> nearbyMobs = hitbox.getHitMobs();
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		List<LivingEntity> undeadMobs = new ArrayList<>(nearbyMobs);
		undeadMobs.removeIf(mob -> !Crusade.enemyTriggersAbilities(mob, mCrusade));
		if (isEnhanced()) {
			undeadMobs.forEach(mob -> EntityUtils.applyStun(mPlugin, ENHANCEMENT_UNDEAD_STUN_DURATION, mob));
		}
		double damage = Math.min(undeadMobs.size() * mDamagePer, mDamageMax);
		damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		double cooldown = getModifiedCooldown();

		if (damage > 0) {
			for (LivingEntity mob : nearbyMobs) {
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, true);
				if (Crusade.applyCrusadeToSlayer(mob, mCrusade)) {
					mPlugin.mEffectManager.addEffect(mob, "CrusadeSlayerTag", new CrusadeEnhancementTag(mCrusade.getEnhancementDuration()));
				}

				Location loc = mob.getLocation();
				mCosmetic.lightDamageEffect(mPlayer, loc, mob);
			}

			mCosmetic.lightDamageCastEffect(world, userLoc, mPlugin, mPlayer, (float) mRange, !isEnhanced() ? HEALING_DOT_ANGLE : -1);
		}

		List<Player> nearbyPlayers = hitbox.getHitPlayers(mPlayer, true);
		nearbyPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		if (!nearbyPlayers.isEmpty()) {
			double healthHealed = 0;
			for (Player p : nearbyPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				double healthBeforeHeal = p.getHealth();
				PlayerUtils.healPlayer(mPlugin, p, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, mFlat + mPercent * maxHealth), mPlayer);
				healthHealed += p.getHealth() - healthBeforeHeal;

				Location loc = p.getLocation();
				mPlugin.mPotionManager.addPotion(p, PotionManager.PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.REGENERATION, 20 * 4, 1, true, true));
				mCosmetic.lightHealEffect(mPlayer, loc, p);
			}

			mCosmetic.lightHealCastEffect(world, userLoc, mPlugin, mPlayer, (float) mRange, !isEnhanced() ? HEALING_DOT_ANGLE : -1);

			if (isEnhanced()) {
				cooldown *= 1 - Math.min((healthHealed / 4) * ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED, ENHANCEMENT_COOLDOWN_REDUCTION_MAX);
			}
		}

		if (damage > 0 || !nearbyPlayers.isEmpty()) {
			putOnCooldown((int) cooldown);
		}
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();

		//Must be holding weapon, tool, or shield
		if (ItemUtils.isShootableItem(mainhand, false) || ItemUtils.isSomePotion(mainhand) || mainhand.getType().isBlock()
			    || mainhand.getType().isEdible() || mainhand.getType() == Material.COMPASS) {
			return false;
		}

		return true;
	}

}

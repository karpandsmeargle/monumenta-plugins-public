package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrismaticShield extends Ability {

	private static final float RADIUS = 4.0f;
	private static final int TRIGGER_HEALTH = 6;
	private static final int ABSORPTION_HEALTH_1 = 8;
	private static final int ABSORPTION_HEALTH_2 = 12;
	private static final int DURATION = 12 * 20;
	private static final int COOLDOWN_1 = 90 * 20;
	private static final int COOLDOWN_2 = 70 * 20;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int STUN_DURATION = 20;

	private final int mAbsorptionHealth;

	public PrismaticShield(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Prismatic Shield");
		mInfo.mLinkedSpell = ClassAbility.PRISMATIC_SHIELD;
		mInfo.mScoreboardId = "Prismatic";
		mInfo.mShorthandName = "PS";
		mInfo.mDescriptions.add("When your health drops below 3 hearts (including if the attack would've killed you), you receive 4 Absorption hearts which lasts up to 12 s. In addition enemies within four blocks are knocked back. Cooldown: 90s.");
		mInfo.mDescriptions.add("The shield is improved to 6 Absorption hearts. Enemies within four blocks are knocked back and stunned for 1 s. Cooldown: 70s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mAbsorptionHealth = getAbilityScore() == 1 ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2;
		mDisplayItem = new ItemStack(Material.SHIELD, 1);
	}

	@Override
	public double getPriorityAmount() {
		return 10000;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlocked() && mPlayer != null) {
			// Calculate whether this effect should not be run based on player health.
			// It is intentional that Prismatic Shield saves you from death if you take a buttload of damage somehow.
			double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

			// Health is less than 0 but does not penetrate the absorption shield
			boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -4 * (mAbsorptionHealth + 1);


			if (healthRemaining > TRIGGER_HEALTH) {
				return;
			} else if (dealDamageLater) {
				// The player has taken fatal damage BUT will be saved by the absorption, so set damage to 0 and compensate later
				event.setCancelled(true);
			}

			// Put on cooldown before processing results to prevent infinite recursion
			putOnCooldown();

			// Conditions match - prismatic shield
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
				MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
				if (getAbilityScore() == 2) {
					EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
				}
			}

			AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionHealth, mAbsorptionHealth, DURATION);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1);
			world.playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
			MessagingUtils.sendActionBarMessage(mPlayer, "Prismatic Shield has been activated");

			if (dealDamageLater) {
				mPlayer.setHealth(1);
				AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
			}
		}
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}
}

package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

public class DepthsVolley extends DepthsAbility {

	public static final String ABILITY_NAME = "Volley";
	private static final int COOLDOWN = 12 * 20;
	public static final int[] ARROWS = {7, 10, 12, 15, 18, 24};
	private static final double[] DAMAGE_MULTIPLIER = {1.4, 1.5, 1.6, 1.7, 1.8, 2.0};

	public static final DepthsAbilityInfo<DepthsVolley> INFO =
		new DepthsAbilityInfo<>(DepthsVolley.class, ABILITY_NAME, DepthsVolley::new, DepthsTree.STEELSAGE, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.VOLLEY_DEPTHS)
			.cooldown(COOLDOWN)
			.displayItem(Material.ARROW)
			.descriptions(DepthsVolley::getDescription)
			.priorityAmount(900); // cancels damage events of volley arrows, so needs to run before other abilities
	public Set<Projectile> mDepthsVolley;
	public Map<LivingEntity, Integer> mDepthsVolleyHitMap;

	public DepthsVolley(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDepthsVolley = new HashSet<>();
		mDepthsVolleyHitMap = new HashMap<>();
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!mPlayer.isSneaking()
			    || isOnCooldown()
			    || !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}

		// Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.33f);
		mDepthsVolley.clear();
		mDepthsVolleyHitMap.clear();

		float projSpeed = (float) projectile.getVelocity().length();
		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				// Store PotionData from the original arrow only if it is weakness or slowness
				PotionData tArrowData = null;
				int fireticks = 0;

				if (projectile instanceof Arrow regularArrow) {
					fireticks = regularArrow.getFireTicks();
					if (regularArrow.hasCustomEffects()) {
						tArrowData = regularArrow.getBasePotionData();
						if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
							// This arrow isn't weakness or slowness - don't store the potion data
							tArrowData = null;
						}
					}
				}

				List<Projectile> projectiles = EntityUtils.spawnVolley(mPlayer, ARROWS[mRarity - 1], projSpeed, 5, projectile.getClass());
				for (Projectile proj : projectiles) {
					mDepthsVolley.add(proj);

					if (proj instanceof AbstractArrow arrow) {
						arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
						if (fireticks > 0) {
							arrow.setFireTicks(fireticks);
						}

						arrow.setCritical(projectile instanceof AbstractArrow projectileArrow && projectileArrow.isCritical());

						// If the base arrow's potion data is still stored, apply it to the new arrows
						if (tArrowData != null) {
							((Arrow) proj).setBasePotionData(tArrowData);
						}
					}

					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);

					ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
					Bukkit.getPluginManager().callEvent(event);
				}

				// We can't just use arrow.remove() because that cancels the event and refunds the arrow
				Location jankWorkAround = mPlayer.getLocation();
				jankWorkAround.setY(-15);
				projectile.teleport(jankWorkAround);
			}
		}.runTaskLater(mPlugin, 0);

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager instanceof Projectile proj && EntityUtils.isAbilityTriggeringProjectile(proj, false) && mDepthsVolley.contains(proj)) {
			if (notBeenHit(enemy)) {
				double damageMultiplier = DAMAGE_MULTIPLIER[mRarity - 1];
				event.setDamage(event.getDamage() * damageMultiplier);
			} else {
				// Only let one Volley arrow hit a given mob
				event.setCancelled(true);
			}
		}
		return false; // only changes event damage
	}

	private boolean notBeenHit(LivingEntity enemy) {
		// Basically the same logic as with MetadataUtils.happenedThisTick but with a hashmap in its stead
		if (mDepthsVolleyHitMap.get(enemy) != null && mDepthsVolleyHitMap.get(enemy) == enemy.getTicksLived()) {
			return false;
		}
		mDepthsVolleyHitMap.put(enemy, enemy.getTicksLived());
		return true;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Shooting a projectile while sneaking shoots a volley consisting of ")
			.append(Component.text(ARROWS[rarity - 1], color))
			.append(Component.text(" projectiles instead. Only one arrow is consumed, and each projectile's damage is multiplied by "))
			.append(Component.text(StringUtils.to2DP(DAMAGE_MULTIPLIER[rarity - 1]), color))
			.append(Component.text(". Cooldown: " + COOLDOWN / 20 + "s."));

	}


}

package com.playmonumenta.plugins.attributes;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

/* Thorns Damage Attribute:
 * +X Thorns Damage
 * Deals X damage to attacking mob when hit by melee or ranged attack.
 * Abilities do not trigger.
*/
public class AttributeThornsDamage implements BaseAttribute {
	private static final String PROPERTY_NAME = "Thorns Damage";
	private static final String STASIS = "Stasis";
	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, double value, EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		if (plugin.mEffectManager.hasEffect(player, STASIS)) {
			return;
		}
		//Set damager to shooter of arrow instead of the actual arrow if hit by projectile damage.
		if (damager instanceof AbstractArrow) {
			final AbstractArrow arrow = (AbstractArrow) event.getDamager();
			final ProjectileSource shooter = arrow.getShooter();
			if (shooter instanceof Entity) {
				damager = (Entity) shooter;
			}
		}
		//Only deal damage if damager is alive and damage is not from an ability.
		//Damage will be 0.0 if used in the wrong slot, but attribute will still be called. Cancel the damage effect if this is the case as well.
		//getFinalDamage check is to prevent thorns from triggering on blocked attack
		if ((damager instanceof LivingEntity) && (value != 0.0) && (event.getCause() != DamageCause.CUSTOM) && (event.getFinalDamage() != 0)) {
			((LivingEntity) damager).damage(value);
		}
	}



}

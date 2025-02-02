package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageDealtSingle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CloakOfShadows extends DepthsAbility {

	public static final String ABILITY_NAME = "Cloak of Shadows";
	public static final int COOLDOWN = 20 * 15;
	public static final int WEAKEN_DURATION = 20 * 6;
	public static final int[] STEALTH_DURATION = {30, 35, 40, 45, 50, 60};
	public static final double[] WEAKEN_AMPLIFIER = {0.2, 0.25, 0.3, 0.35, 0.4, 0.5};
	public static final double[] DAMAGE = {0.4, 0.5, 0.6, 0.7, 0.8, 1};
	public static final int DAMAGE_DURATION = 4 * 20;
	private static final double VELOCITY = 0.7;
	private static final int RADIUS = 5;

	public static final DepthsAbilityInfo<CloakOfShadows> INFO =
		new DepthsAbilityInfo<>(CloakOfShadows.class, ABILITY_NAME, CloakOfShadows::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.CLOAK_OF_SHADOWS)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CloakOfShadows::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.BLACK_CONCRETE)
			.descriptions(CloakOfShadows::getDescription);

	public CloakOfShadows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		Location loc = mPlayer.getEyeLocation();
		ItemStack itemTincture = new ItemStack(Material.BLACK_CONCRETE);
		ItemUtils.setPlainName(itemTincture, "Shadow Bomb");
		ItemMeta tinctureMeta = itemTincture.getItemMeta();
		tinctureMeta.displayName(Component.text("Shadow Bomb", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		itemTincture.setItemMeta(tinctureMeta);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1, 0.15f);
		Item tincture = world.dropItem(loc, itemTincture);
		tincture.setPickupDelay(Integer.MAX_VALUE);

		Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
		vel.multiply(VELOCITY);

		tincture.setVelocity(vel);
		tincture.setGlowing(true);

		putOnCooldown();
		AbilityUtils.applyStealth(mPlugin, mPlayer, STEALTH_DURATION[mRarity - 1], null);

		mPlugin.mEffectManager.addEffect(mPlayer, "CloakOfShadowsDamageEffect", new PercentDamageDealtSingle(DAMAGE_DURATION, DAMAGE[mRarity - 1], EnumSet.of(DamageEvent.DamageType.MELEE)));

		new BukkitRunnable() {

			int mExpire = 0;

			@Override
			public void run() {
				if (tincture.isOnGround()) {
					new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, tincture.getLocation(), 30, 3, 0, 3).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.EXPLOSION_NORMAL, tincture.getLocation(), 30, 2, 0, 2).spawnAsPlayerActive(mPlayer);
					world.playSound(tincture.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.15f);
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(tincture.getLocation(), RADIUS);
					for (LivingEntity mob : mobs) {
						EntityUtils.applyWeaken(mPlugin, WEAKEN_DURATION, WEAKEN_AMPLIFIER[mRarity - 1], mob);
					}

					tincture.remove();
					this.cancel();
				}
				mExpire++;
				if (mExpire >= 10 * 20) {
					tincture.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Left click while sneaking and holding a weapon to throw a shadow bomb, which explodes on landing, applying ")
			.append(Component.text(StringUtils.multiplierToPercentage(WEAKEN_AMPLIFIER[rarity - 1]) + "%", color))
			.append(Component.text(" weaken for " + WEAKEN_DURATION / 20 + " seconds in a " + RADIUS + " block radius. You enter stealth for "))
			.append(Component.text(StringUtils.to2DP(STEALTH_DURATION[rarity - 1] / 20.0), color))
			.append(Component.text(" seconds upon casting and the next instance of melee damage you deal within " + DAMAGE_DURATION / 20 + " seconds deals "))
			.append(Component.text(StringUtils.multiplierToPercentage(DAMAGE[rarity - 1]) + "%", color))
			.append(Component.text(" more damage. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}


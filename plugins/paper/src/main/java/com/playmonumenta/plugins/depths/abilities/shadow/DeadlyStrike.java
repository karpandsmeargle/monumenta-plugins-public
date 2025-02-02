package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DeadlyStrike extends DepthsAbility {

	public static final String ABILITY_NAME = "Deadly Strike";
	public static final double[] DAMAGE = {1.10, 1.125, 1.15, 1.175, 1.2, 1.25};

	public static final DepthsAbilityInfo<DeadlyStrike> INFO =
		new DepthsAbilityInfo<>(DeadlyStrike.class, ABILITY_NAME, DeadlyStrike::new, DepthsTree.SHADOWDANCER, DepthsTrigger.PASSIVE)
			.displayItem(Material.BLACK_CONCRETE_POWDER)
			.descriptions(DeadlyStrike::getDescription);

	public DeadlyStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH) {
			event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);
		}
		return false; // only changes event damage
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Your melee damage is multiplied by ")
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text("."));
	}


}


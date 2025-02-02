package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DepthsDethroner extends DepthsAbility {

	public static final String ABILITY_NAME = "Dethroner";
	public static final double[] ELITE_DAMAGE = {1.14, 1.175, 1.21, 1.245, 1.28, 1.35};
	public static final double[] BOSS_DAMAGE = {1.1, 1.125, 1.15, 1.175, 1.2, 1.25};

	public static final DepthsAbilityInfo<DepthsDethroner> INFO =
		new DepthsAbilityInfo<>(DepthsDethroner.class, ABILITY_NAME, DepthsDethroner::new, DepthsTree.SHADOWDANCER, DepthsTrigger.PASSIVE)
			.displayItem(Material.DRAGON_HEAD)
			.descriptions(DepthsDethroner::getDescription);

	public DepthsDethroner(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (EntityUtils.isBoss(enemy)) {
			event.setDamage(event.getDamage() * BOSS_DAMAGE[mRarity - 1]);
		} else if (EntityUtils.isElite(enemy)) {
			event.setDamage(event.getDamage() * ELITE_DAMAGE[mRarity - 1]);
		}
		return false; // only changes event damage
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("All damage you deal to elites is multiplied by ")
			.append(Component.text(ELITE_DAMAGE[rarity - 1], color))
			.append(Component.text(". All damage you deal to bosses is multiplied by "))
			.append(Component.text(BOSS_DAMAGE[rarity - 1], color))
			.append(Component.text("."));
	}


}


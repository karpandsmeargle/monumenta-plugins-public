package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.ArrowSaving;
import com.playmonumenta.plugins.effects.BonusSoulThreads;
import com.playmonumenta.plugins.effects.DurabilitySaving;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentExperience;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.Stasis;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Collection;
import java.util.HashMap;
import org.bukkit.entity.Entity;

public class CustomEffect {
	private static final String COMMAND = "customeffect";
	private static final String PERMISSION = "monumenta.commands.customeffect";

	@FunctionalInterface
	private interface ZeroArgument {
		void run(Entity entity, int duration);
	}

	@FunctionalInterface
	private interface SingleArgument {
		void run(Entity entity, int duration, double amount);
	}

	public static void register() {
		HashMap<String, ZeroArgument> zeroArgumentEffects = new HashMap<>();
		zeroArgumentEffects.put("stasis", (Entity entity, int duration) -> Plugin.getInstance().mEffectManager.addEffect(entity, Stasis.GENERIC_NAME, new Stasis(duration)));
		zeroArgumentEffects.put("silence", (Entity entity, int duration) -> Plugin.getInstance().mEffectManager.addEffect(entity, AbilitySilence.GENERIC_NAME, new AbilitySilence(duration)));

		HashMap<String, SingleArgument> singleArgumentEffects = new HashMap<>();
		singleArgumentEffects.put("speed", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, PercentSpeed.GENERIC_NAME, new PercentSpeed(duration, amount, PercentSpeed.GENERIC_NAME)));
		singleArgumentEffects.put("damagedealt", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, PercentDamageDealt.GENERIC_NAME, new PercentDamageDealt(duration, amount)));
		singleArgumentEffects.put("damagereceived", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, PercentDamageReceived.GENERIC_NAME, new PercentDamageReceived(duration, amount)));
		singleArgumentEffects.put("experience", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, PercentExperience.GENERIC_NAME, new PercentExperience(duration, amount)));
		singleArgumentEffects.put("attackspeed", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, PercentAttackSpeed.GENERIC_NAME, new PercentAttackSpeed(duration, amount, PercentAttackSpeed.GENERIC_NAME)));
		singleArgumentEffects.put("knockbackresist", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, PercentKnockbackResist.GENERIC_NAME, new PercentKnockbackResist(duration, amount, PercentKnockbackResist.GENERIC_NAME)));
		singleArgumentEffects.put("arrowsaving", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, ArrowSaving.GENERIC_NAME, new ArrowSaving(duration, amount)));
		singleArgumentEffects.put("durabilitysaving", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, DurabilitySaving.GENERIC_NAME, new DurabilitySaving(duration, amount)));
		singleArgumentEffects.put("soul", (Entity entity, int duration, double amount) -> Plugin.getInstance().mEffectManager.addEffect(entity, BonusSoulThreads.GENERIC_NAME, new BonusSoulThreads(duration, amount)));

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(zeroArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("seconds")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					zeroArgumentEffects.get((String) args[1]).run(entity, (int) (((double) args[2]) * 20));
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("seconds"),
				new DoubleArgument("amount")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					singleArgumentEffects.get((String) args[1]).run(entity, (int) (((double) args[2]) * 20), (double) args[3]);
				}
			}).register();
	}
}

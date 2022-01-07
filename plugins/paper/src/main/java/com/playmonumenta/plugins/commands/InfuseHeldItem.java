package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InfusionUtils.InfusionSelection;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.executors.CommandExecutor;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class InfuseHeldItem extends GenericCommand {
	static final String COMMAND = "infusehelditem";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.infusehelditem");

		Argument selectionArg = new MultiLiteralArgument(InfusionSelection.ACUMEN.getLabel(),
		                                                 InfusionSelection.FOCUS.getLabel(),
		                                                 InfusionSelection.PERSPICACITY.getLabel(),
		                                                 InfusionSelection.TENACITY.getLabel(),
		                                                 InfusionSelection.VIGOR.getLabel(),
		                                                 InfusionSelection.VITALITY.getLabel(),
		                                                 InfusionSelection.REFUND.getLabel(),
		                                                 InfusionSelection.SPEC_REFUND.getLabel());

		List<Argument> arguments = new ArrayList<>();
		arguments.add(selectionArg);
		arguments.add(new IntegerArgument("level", 1));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((CommandExecutor) (sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				InfusionSelection selection = InfusionSelection.getInfusionSelection((String) args[0]);
				if (selection == null) {
					CommandAPI.fail("Invalid infusion selection; how did we get here?");
					throw new RuntimeException();
				}
				for (int i = 0; i < (Integer) args[1]; i++) {
					InfusionUtils.infuseItem(player.getInventory().getItemInMainHand(), selection);
				}
			})
			.register();
	}
}

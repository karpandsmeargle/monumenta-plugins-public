package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LeaveGuild {
	public static void register(Plugin plugin) {

		// leaveguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.leaveguild");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("leaveguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (!ServerProperties.getShardName().contains("build")) {
					run(plugin, (Player) args[0]);
				}
			})
			.register();
	}

	private static void run(Plugin plugin, Player player) throws WrapperCommandSyntaxException {
		// Set scores and permissions
		ScoreboardUtils.setScoreboardValue(player, "Founder", 0);

		Group group = LuckPermsIntegration.getGuild(player);
		if (group == null) {
			String err = ChatColor.RED + "You are not in a guild";
			player.sendMessage(err);
			throw CommandAPI.failWithString(err);
		}

		String guildName = LuckPermsIntegration.getGuildName(group);

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			User user = LuckPermsIntegration.UM.getUser(player.getUniqueId());
			if (user == null) {
				return;
			}
			for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
				if (node.getGroupName().equals(group.getName())) {
					user.data().remove(node);
				}
			}
			LuckPermsIntegration.UM.saveUser(user).whenComplete((unused, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				} else {
					Bukkit.getScheduler().runTask(plugin, () -> {
						MonumentaNetworkChatIntegration.refreshPlayer(player);
					});
				}
			});
			LuckPermsIntegration.pushUserUpdate(user);
		});

		player.sendMessage(ChatColor.GOLD + "You have left the guild '" + guildName + "'");
	}
}

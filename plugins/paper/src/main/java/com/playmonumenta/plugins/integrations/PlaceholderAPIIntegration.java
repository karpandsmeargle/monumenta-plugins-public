package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
	private static final Pattern SHARD_NUMBER_PATTERN = Pattern.compile("(?:-|^dev)(\\d+)$");

	private final Plugin mPlugin;

	public PlaceholderAPIIntegration(Plugin plugin) {
		super();
		plugin.getLogger().info("Enabling PlaceholderAPI integration");
		mPlugin = plugin;
	}

	@Override
	public String getIdentifier() {
		return "monumenta";
	}

	@Override
	public @Nullable String getPlugin() {
		return null;
	}

	@Override
	public String getAuthor() {
		return "Team Epic";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public @Nullable String onPlaceholderRequest(Player player, String identifier) {

		// -------------------------  player-independent placeholders -------------------------

		if (identifier.startsWith("loot_table:")) {
			String lootTable = identifier.substring("loot_table:".length());
			ItemStack item = InventoryUtils.getItemFromLootTable(Bukkit.getWorlds().get(0).getSpawnLocation(), NamespacedKeyUtils.fromString(lootTable));
			if (item == null) {
				return "";
			} else {
				return MiniMessage.miniMessage().serialize(ItemUtils.getDisplayName(item).hoverEvent(item.asHoverEvent()));
			}
		}

		if (identifier.equalsIgnoreCase("shard")) {
			return ServerProperties.getShardName();
		}

		if (identifier.equalsIgnoreCase("shard_number")) {
			String fullShardName = PlaceholderAPI.setPlaceholders(null, "%network-relay_shard%");
			// actual shard numbers except for dev shards (for testing)
			Matcher matcher = SHARD_NUMBER_PATTERN.matcher(fullShardName);
			if (matcher.find()) {
				return matcher.group(1);
			}
			return "1";
		}

		if (identifier.startsWith("shrineicon")) {
			String shrineType = identifier.substring("shrineicon_".length());
			if ((shrineType.equalsIgnoreCase("Speed") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D1Finished").orElse(0) > 1) ||
				    (shrineType.equalsIgnoreCase("Resistance") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D3Finished").orElse(0) > 1) ||
				    (shrineType.equalsIgnoreCase("Strength") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D4Finished").orElse(0) > 1) ||
				    (shrineType.equalsIgnoreCase("Intuitive") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D5Finished").orElse(0) > 1) ||
				    (shrineType.equalsIgnoreCase("Thrift") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D6Finished").orElse(0) > 1) ||
				    (shrineType.equalsIgnoreCase("Harvester") && ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D7Finished").orElse(0) > 1)) {
				return "active";
			}
			return "inactive";
		}

		if (identifier.startsWith("shrine")) {
			String shrineType = identifier.substring("shrine_".length());
			int remainingTime;
			if (shrineType.equalsIgnoreCase("Speed")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D1Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.AQUA + "Speed: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.AQUA + "Speed" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Resistance")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D3Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.GRAY + "Resistance: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.GRAY + "Resistance" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Strength")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D4Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.DARK_RED + "Strength: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.DARK_RED + "Strength" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Intuitive")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D5Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.GOLD + "Intuitive: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.GOLD + "Intuitive" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Thrift")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D6Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.LIGHT_PURPLE + "Thrift: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.LIGHT_PURPLE + "Thrift" + ChatColor.WHITE + " is not active.";
				}
			} else if (shrineType.equalsIgnoreCase("Harvester")) {
				remainingTime = ScoreboardUtils.getScoreboardValue("$PatreonShrine", "D7Finished").orElse(0);
				if (remainingTime > 1) {
					remainingTime = (int) Math.floor(remainingTime / 60.0);
					return ChatColor.DARK_GREEN + "Harvester: " + ChatColor.WHITE + remainingTime + "m";
				} else {
					return ChatColor.DARK_GREEN + "Harvester" + ChatColor.WHITE + " is not active.";
				}
			}
		}

		// -------------------------  player-dependent placeholders -------------------------

		if (player == null) {
			return "";
		}

		// %monumenta_class%
		if (identifier.equalsIgnoreCase("class")) {
			String cls = AbilityUtils.getClass(player);
			if (ServerProperties.getClassSpecializationsEnabled(player)) {
				String spec = AbilityUtils.getSpec(player);
				if (!spec.equals("No Spec")) {
					cls = cls + " (" + spec + ")";
				}
			}
			return cls;
		}

		// %monumenta_level%
		if (identifier.equalsIgnoreCase("level")) {
			int charmPower = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.CHARM_POWER).orElse(0);
			charmPower = (charmPower > 0) ? (charmPower / 3) - 2 : 0;
			return Integer.toString(AbilityUtils.getEffectiveTotalSkillPoints(player) +
				                        AbilityUtils.getEffectiveTotalSpecPoints(player) +
				                        ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_ENHANCE).orElse(0) +
				                        charmPower);
		}

		//Player equipped title
		if (identifier.equalsIgnoreCase("title")) {
			Cosmetic title = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.TITLE);
			if (title != null) {
				return title.getName() + " ";
			} else {
				return "";
			}
		}

		if (identifier.startsWith("effect_")) {
			List<String> effectDisplays = DisplayableEffect.getSortedEffectDisplays(mPlugin, player);

			if (identifier.startsWith("effect_more")) {
				int extra = effectDisplays.size() - 10;
				if (extra == 1) {
					//Show 11th if there are exactly 11
					return effectDisplays.get(10);
				} else if (extra > 0) {
					return ChatColor.GRAY + "... and " + extra + " more effects";
				} else {
					return "";
				}
			} else {
				try {
					int index = Integer.parseInt(identifier.substring("effect_".length())) - 1;
					if (effectDisplays.size() > index) {
						return effectDisplays.get(index);
					} else {
						return "";
					}
				} catch (NumberFormatException numberFormatException) {
					MMLog.warning("Failed to find integer after 'effect_' on tab list");
				}
			}
		}

		return null;
	}
}

package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ItemStatCommands {

	public static void registerInfoCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editinfo");

		ItemStatUtils.Region[] regionsRaw = ItemStatUtils.Region.values();
		String[] regions = new String[regionsRaw.length];
		for (int i = 0; i < regions.length; i++) {
			regions[i] = regionsRaw[i].getName();
		}

		ItemStatUtils.Tier[] tiersRaw = ItemStatUtils.Tier.values();
		String[] tiers = new String[tiersRaw.length];
		for (int i = 0; i < tiers.length; i++) {
			tiers[i] = tiersRaw[i].getName();
		}

		ItemStatUtils.Masterwork[] masterworkRaw = ItemStatUtils.Masterwork.values();
		String[] ms = new String[masterworkRaw.length];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = masterworkRaw[i].getName();
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("region").replaceSuggestions(ArgumentSuggestions.strings(regions)));
		arguments.add(new StringArgument("tier").replaceSuggestions(ArgumentSuggestions.strings(tiers)));
		arguments.add(getLocationArgument());
		arguments.add(new StringArgument("masterwork").replaceSuggestions(ArgumentSuggestions.strings(ms)));

		new CommandAPICommand("editinfo").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			ItemStatUtils.Region region = ItemStatUtils.Region.getRegion((String) args[0]);
			ItemStatUtils.Tier tier = ItemStatUtils.Tier.getTier((String) args[1]);
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[2]);
			ItemStatUtils.Masterwork m = ItemStatUtils.Masterwork.getMasterwork((String) args[3]);

			// For R3 items, set tier to match masterwork level
			if (region == ItemStatUtils.Region.RING) {
				if (m != ItemStatUtils.Masterwork.ERROR && m != ItemStatUtils.Masterwork.NONE) {
					switch (Objects.requireNonNull(m)) {
						case ZERO:
						case I:
						case II:
						case III:
							tier = ItemStatUtils.Tier.RARE;
							break;
						case IV:
						case V:
							tier = ItemStatUtils.Tier.ARTIFACT;
							break;
						case VI:
							tier = ItemStatUtils.Tier.EPIC;
							break;
						case VIIA:
						case VIIB:
						case VIIC:
							tier = ItemStatUtils.Tier.LEGENDARY;
							break;
						default:
							break;
					}
				}
			}

			ItemStatUtils.editItemInfo(item, region, tier, m, location);

			ItemStatUtils.generateItemStats(item);
			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, true);
			}
		}).register();
	}

	public static void registerLoreCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editlore");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("add"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			Integer index = (Integer) args[1];

			ItemStatUtils.addLore(item, index, Component.empty());

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.add(new GreedyStringArgument("lore"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			Integer index = (Integer) args[1];
			String lore = (String) args[2];

			ItemStatUtils.addLore(item, index, MessagingUtils.fromMiniMessage(lore));

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("del"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			Integer index = (Integer) args[1];

			ItemStatUtils.removeLore(item, index);

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("replace"));
		arguments.add(new IntegerArgument("index", 0));
		arguments.add(new GreedyStringArgument("lore"));
		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			Integer index = (Integer) args[1];
			String lore = (String) args[2];

			ItemStatUtils.removeLore(item, index);
			ItemStatUtils.addLore(item, index, MessagingUtils.fromMiniMessage(lore));

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("list"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			List<Component> lore = ItemStatUtils.getLore(item);
			for (int i = 0; i < lore.size(); i++) {
				Component line = lore.get(i);
				player.sendMessage(line.clickEvent(ClickEvent.suggestCommand("/editlore replace " + i + " " + MessagingUtils.toMiniMessage(line))));
			}
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("register"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			List<Component> oldLore = item.lore();
			if (oldLore == null || oldLore.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Item has no lore!");
				return;
			}

			int loreIdx = 0;
			for (Component c : oldLore) {
				ItemStatUtils.addLore(item, loreIdx, c);
				loreIdx++;
			}

			ItemStatUtils.generateItemStats(item);
		}).register();
	}

	public static void registerCharmCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editcharm");

		Argument<String> charmEffectArgument =
			new GreedyStringArgument("effect")
				.replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
					String start = info.currentArg();
					String[] split = start.split(" ", 2);
					if (split.length == 1) {
						return Collections.emptyList();
					}
					return Plugin.getInstance().mCharmManager.mCharmEffectList.stream()
						       .filter(e -> e.startsWith(split[1]))
						       .map(e -> split[0] + " " + e)
						       .toList();
				}));

		new CommandAPICommand("editcharm")
			.withPermission(perms)
			.withSubcommand(
				new CommandAPICommand("add")
					.withArguments(new IntegerArgument("index", 0))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						Integer index = (Integer) args[0];

						ItemStatUtils.addCharmEffect(item, index, Component.empty());

						ItemStatUtils.generateItemStats(item);
					}))
			.withSubcommand(
				new CommandAPICommand("add")
					.withArguments(
						new IntegerArgument("index", 0),
						charmEffectArgument)
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						Integer index = (Integer) args[0];
						String lore = (String) args[1];

						CharmManager.CharmParsedInfo parsedInfo = CharmManager.readCharmLine(lore);
						if (parsedInfo == null) {
							player.sendMessage(Component.text("Invalid charm effect! Make sure it starts with a number, optionally a percent sign, then a space, then an effect name.", NamedTextColor.RED));
							return;
						}
						if (!Plugin.getInstance().mCharmManager.mCharmEffectList.contains(parsedInfo.mEffect)) {
							player.sendMessage(Component.text("WARNING: Unknown effect '" + parsedInfo.mEffect + "'. The charm won't work without plugin changes!", NamedTextColor.YELLOW));
						}

						String hexColor = CharmManager.getCharmEffectColor(parsedInfo.mValue >= 0, parsedInfo.mEffect);

						Component text = Component.text(lore, TextColor.fromHexString(hexColor)).decoration(TextDecoration.ITALIC, false);
						ItemStatUtils.addCharmEffect(item, index, text);

						ItemStatUtils.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("del")
					.withArguments(new IntegerArgument("index", 0))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						Integer index = (Integer) args[0];

						ItemStatUtils.removeCharmEffect(item, index);

						ItemStatUtils.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("power")
					.withArguments(new IntegerArgument("amount", 0))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						Integer power = (Integer) args[0];

						if (power > 0) {
							ItemStatUtils.setCharmPower(item, power);
						} else {
							ItemStatUtils.removeCharmPower(item);
						}

						ItemStatUtils.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("replace")
					.withArguments(
						new IntegerArgument("index", 0),
						charmEffectArgument
					).executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						Integer index = (Integer) args[0];
						String lore = (String) args[1];

						CharmManager.CharmParsedInfo parsedInfo = CharmManager.readCharmLine(lore);
						if (parsedInfo == null) {
							player.sendMessage(Component.text("Invalid charm effect! Make sure it starts with a number, optionally a percent sign, then a space, then an effect name.", NamedTextColor.RED));
							return;
						}
						if (!Plugin.getInstance().mCharmManager.mCharmEffectList.contains(parsedInfo.mEffect)) {
							player.sendMessage(Component.text("WARNING: Unknown effect '" + parsedInfo.mEffect + "'. The charm won't work without plugin changes!", NamedTextColor.YELLOW));
						}

						ItemStatUtils.removeCharmEffect(item, index);
						String hexColor = CharmManager.getCharmEffectColor(parsedInfo.mValue >= 0, parsedInfo.mEffect);
						Component text = Component.text(lore, TextColor.fromHexString(hexColor)).decoration(TextDecoration.ITALIC, false);
						ItemStatUtils.addCharmEffect(item, index, text);

						ItemStatUtils.generateItemStats(item);
					}))
			.register();
	}

	public static void registerFishCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editfish");

		new CommandAPICommand("editfish")
			.withPermission(perms)
			.withSubcommand(
				new CommandAPICommand("quality")
					.withArguments(new IntegerArgument("amount", 0, 5))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						Integer quality = (Integer) args[0];

						if (quality > 0) {
							ItemStatUtils.setFishQuality(item, quality);
						} else {
							ItemStatUtils.removeFishQuality(item);
						}

						ItemStatUtils.generateItemStats(item);
					})
			)
		.register();
	}

	public static void registerNameCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editname");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(getLocationArgument());

		new CommandAPICommand("editname").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[0]);

			ItemMeta itemMeta = item.getItemMeta();
			Component displayName = itemMeta.displayName();
			if (displayName != null) {
				itemMeta.displayName(displayName.color(location.getDisplay().color()));
				item.setItemMeta(itemMeta);
				ItemUtils.setPlainName(item);
			}
		}).register();

		arguments.add(new BooleanArgument("bold"));
		arguments.add(new BooleanArgument("underline"));

		new CommandAPICommand("editname").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[0]);
			Boolean bold = (Boolean) args[1];
			Boolean underline = (Boolean) args[2];

			ItemMeta itemMeta = item.getItemMeta();
			Component displayName = itemMeta.displayName();
			if (displayName != null) {
				itemMeta.displayName(displayName.color(location.getDisplay().color()).decoration(TextDecoration.BOLD, bold).decoration(TextDecoration.UNDERLINED, underline).decoration(TextDecoration.ITALIC, false));
				item.setItemMeta(itemMeta);
				ItemUtils.setPlainName(item);
			}
		}).register();

		arguments.add(new GreedyStringArgument("name"));

		new CommandAPICommand("editname").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[0]);
			Boolean bold = (Boolean) args[1];
			Boolean underline = (Boolean) args[2];
			String name = (String) args[3];

			ItemMeta itemMeta = item.getItemMeta();
			itemMeta.displayName(Component.text(name, location.getDisplay().color()).decoration(TextDecoration.BOLD, bold).decoration(TextDecoration.UNDERLINED, underline).decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(itemMeta);
			ItemUtils.setPlainName(item, name);

		}).register();

		arguments.clear();
		arguments.add(new LiteralArgument("replace"));
		arguments.add(new GreedyStringArgument("name"));

		new CommandAPICommand("editname").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			String name = (String) args[0];

			ItemMeta itemMeta = item.getItemMeta();
			Component displayName = itemMeta.displayName();
			if (displayName != null) {
				itemMeta.displayName(Component.text(name, displayName.color()).decorations(displayName.decorations()));
			} else {
				itemMeta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			item.setItemMeta(itemMeta);
			ItemUtils.setPlainName(item, name);
		}).register();
	}

	public static void registerConsumeCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editconsume");

		String[] effects = new String[EffectType.values().length];
		int i = 0;
		for (EffectType type : EffectType.values()) {
			effects[i++] = type.getType();
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("enchantment").includeSuggestions(ArgumentSuggestions.strings(info -> effects)));
		arguments.add(new TimeArgument("duration"));
		arguments.add(new DoubleArgument("strength", 0));

		new CommandAPICommand("editconsume").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			EffectType type = EffectType.fromType((String) args[0]);
			if (type == null) {
				throw CommandAPI.failWithString("Invalid effect type " + args[0]);
			}
			int duration = (int) args[1];
			double strength = (double) args[2];

			ItemStatUtils.addConsumeEffect(item, type, strength, duration);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("del"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editconsume").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			Integer index = (Integer) args[1];

			ItemStatUtils.removeConsumeEffect(item, index);

			ItemStatUtils.generateItemStats(item);
		}).register();
	}

	public static void registerEnchCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editench");

		String[] enchantments = new String[ItemStatUtils.EnchantmentType.values().length + ItemStatUtils.InfusionType.values().length];
		int i = 0;

		for (ItemStatUtils.EnchantmentType enchantment : ItemStatUtils.EnchantmentType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		for (ItemStatUtils.InfusionType enchantment : ItemStatUtils.InfusionType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		Argument<?> enchantmentArgument = new StringArgument("enchantment").replaceSuggestions(ArgumentSuggestions.strings(info -> enchantments));

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(enchantmentArgument);

		new CommandAPICommand("editench").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String enchantment = (String) args[0];

			addEnchantmentOrInfusion(item, player, enchantment, 1);
		}).register();

		arguments.add(new IntegerArgument("level", 0));

		new CommandAPICommand("editench").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String enchantment = (String) args[0];
			Integer level = (Integer) args[1];

			addEnchantmentOrInfusion(item, player, enchantment, level);
		}).register();

		List<Argument<?>> argumentsOther = new ArrayList<>();
		argumentsOther.add(new EntitySelectorArgument.OnePlayer("player"));
		argumentsOther.add(enchantmentArgument);
		argumentsOther.add(new IntegerArgument("level", 0));

		new CommandAPICommand("editench").withPermission(perms).withArguments(argumentsOther).executes((sender, args) -> {
			Player player = (Player) args[0];
			String enchantment = (String) args[1];
			Integer level = (Integer) args[2];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(Component.text("Must be holding an item!", NamedTextColor.RED));
				return;
			}

			addEnchantmentOrInfusion(item, player, enchantment, level);
		}).register();
	}

	private static void addEnchantmentOrInfusion(ItemStack item, Player player, String enchantment, int level) {
		ItemStatUtils.EnchantmentType type1 = ItemStatUtils.EnchantmentType.getEnchantmentType(enchantment);
		if (type1 != null) {
			if (level > 0) {
				ItemStatUtils.addEnchantment(item, type1, level);
			} else {
				ItemStatUtils.removeEnchantment(item, type1);
			}
		}

		ItemStatUtils.InfusionType type2 = ItemStatUtils.InfusionType.getInfusionType(enchantment);
		if (type2 != null) {
			if (level > 0) {
				ItemStatUtils.addInfusion(item, type2, level, player.getUniqueId(), false);
			} else {
				ItemStatUtils.removeInfusion(item, type2, false);
			}
		}

		ItemStatUtils.generateItemStats(item);
		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
		if (playerItemStats != null) {
			playerItemStats.updateStats(player, true, true);
		}
	}

	public static void registerAttrCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editattr");

		String[] attributes = new String[ItemStatUtils.AttributeType.values().length];
		int i = 0;

		for (ItemStatUtils.AttributeType attribute : ItemStatUtils.AttributeType.values()) {
			attributes[i] = attribute.getCodeName().replace(" ", "");
			i++;
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("attribute").replaceSuggestions(ArgumentSuggestions.strings(info -> attributes)));
		arguments.add(new DoubleArgument("amount"));
		arguments.add(new MultiLiteralArgument(ItemStatUtils.Operation.ADD.getName(), ItemStatUtils.Operation.MULTIPLY.getName()));
		arguments.add(new MultiLiteralArgument(ItemStatUtils.Slot.MAINHAND.getName(), ItemStatUtils.Slot.OFFHAND.getName(), ItemStatUtils.Slot.HEAD.getName(), ItemStatUtils.Slot.CHEST.getName(), ItemStatUtils.Slot.LEGS.getName(), ItemStatUtils.Slot.FEET.getName(), ItemStatUtils.Slot.PROJECTILE.getName()));

		new CommandAPICommand("editattr").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String attribute = (String) args[0];
			Double amount = (Double) args[1];
			ItemStatUtils.Operation operation = ItemStatUtils.Operation.getOperation((String) args[2]);
			if (operation == null) {
				throw CommandAPI.failWithString("Invalid operation " + args[2]);
			}
			ItemStatUtils.Slot slot = ItemStatUtils.Slot.getSlot((String) args[3]);
			if (slot == null) {
				throw CommandAPI.failWithString("Invalid slot " + args[3]);
			}

			if ((args[3] == "add" && attribute.contains("Multiply")) || (args[3] == "multiply" && attribute.contains("Add"))) {
				return;
			}

			if (args[3] == "add" && attribute.contains("ProjectileSpeed")) {
				player.sendMessage("You are using the wrong type of Proj Speed, do multiply");
				return;
			}

			ItemStatUtils.AttributeType type1 = ItemStatUtils.AttributeType.getAttributeType(attribute);
			if (type1 != null) {
				if (amount != 0) {
					ItemStatUtils.addAttribute(item, type1, amount, operation, slot);
				} else {
					ItemStatUtils.removeAttribute(item, type1, operation, slot);
				}
			}

			ItemStatUtils.generateItemStats(item);
			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, true);
			}
		}).register();
	}

	public static void registerRemoveCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.removestats");

		new CommandAPICommand("removestats").withPermission(perms).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			// remove all enchantments and attributes even though we clear everything later because some use vanilla mechanics that persist
			for (ItemStatUtils.EnchantmentType ench : ItemStatUtils.EnchantmentType.values()) {
				ItemStatUtils.removeEnchantment(item, ench);
			}

			for (ItemStatUtils.AttributeType attr : ItemStatUtils.AttributeType.values()) {
				for (ItemStatUtils.Operation op : ItemStatUtils.Operation.values()) {
					for (ItemStatUtils.Slot slot : ItemStatUtils.Slot.values()) {
						ItemStatUtils.removeAttribute(item, attr, op, slot);
					}
				}
			}

			NBTItem nbt = new NBTItem(item);
			nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
			item.setItemMeta(nbt.getItem().getItemMeta());
			item.lore(Collections.emptyList());

			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, true);
			}
		}).register();
	}

	public static void registerColorCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.color");

		new CommandAPICommand("color").withPermission(perms).withArguments(getLocationArgument()).executes((sender, args) -> {
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[0]);
			Component message = Component.empty().append(location.getDisplay()).append(Component.text(" (" + location.getColor().asHexString() + ")")).hoverEvent(HoverEvent.showText(Component.text("Click to copy hex code to clipboard"))).clickEvent(ClickEvent.copyToClipboard(location.getColor().asHexString()));
			sender.sendMessage(message);
		}).register();

		new CommandAPICommand("color").withPermission(perms).withArguments(new LiteralArgument("list")).executes((sender, args) -> {
			Component message = Component.empty();
			for (ItemStatUtils.Location location : ItemStatUtils.Location.values()) {
				message = message.append(Component.text(location.getName(), location.getColor()).hoverEvent(HoverEvent.showText(Component.text("Click to copy hex code to clipboard"))).clickEvent(ClickEvent.copyToClipboard(location.getColor().asHexString())));
				message = message.append(Component.text(" "));
			}
			sender.sendMessage(message);
		}).register();
	}

	private static @Nullable ItemStack getHeldItemAndSendErrors(Player player) {
		if (player.getGameMode() != GameMode.CREATIVE) {
			player.sendMessage(Component.text("Must be in creative mode to use this command!", NamedTextColor.RED));
			return null;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() == Material.AIR) {
			player.sendMessage(Component.text("Must be holding an item!", NamedTextColor.RED));
			return null;
		}
		return item;
	}

	private static Argument<String> getLocationArgument() {
		ItemStatUtils.Location[] locationsRaw = ItemStatUtils.Location.values();
		String[] locations = new String[locationsRaw.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsRaw[i].getName();
		}

		return new StringArgument("location").replaceSuggestions(ArgumentSuggestions.strings(locations));
	}

}

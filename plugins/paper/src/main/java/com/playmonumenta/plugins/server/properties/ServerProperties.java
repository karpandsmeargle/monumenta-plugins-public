package com.playmonumenta.plugins.server.properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DungeonUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class ServerProperties {

	private static final ServerProperties INSTANCE = new ServerProperties();

	private boolean mJoinMessagesEnabled = false;
	private boolean mIsTownWorld = false;
	// Height of plots in Sierhaven so that players under plots stay in adventure
	private int mPlotSurvivalMinHeight = 256;

	private boolean mIsSleepingEnabled = true;
	private boolean mKeepLowTierInventory = false;
	private boolean mClassSpecializationsEnabled = false;
	private boolean mAbilityEnhancementsEnabled = false;
	private boolean mAuditMessagesEnabled = true;
	private boolean mRepairExplosions = false;
	private @Nullable Pattern mRepairExplosionsWorldPattern = null;
	private boolean mPreventDungeonItemTransfer = true;
	private boolean mReplaceSpawnerEntities = true;
	private boolean mInfusionsEnabled = true;
	private boolean mMasterworkRefundEnabled = false;
	private boolean mLootBoxEnabled = true;
	private int mHTTPStatusPort = 8000;

	private String mShardName = "default_settings";

	private boolean mDisableEntityScoresInDefaultWorld = true;
	private List<String> mDisableEntityScoresInWorlds = new ArrayList<>();

	private final EnumSet<Material> mUnbreakableBlocks = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mAlwaysPickupMats = EnumSet.noneOf(Material.class);
	private final EnumSet<Material> mNamedPickupMats = EnumSet.noneOf(Material.class);

	private final List<NamespacedKey> mEggifySpawnEggs = new ArrayList<>();
	private int mLootingLimiterMobKills = 0;
	private int mLootingLimiterSpawners = 0;
	private boolean mLootingLimiterIgnoreBreakingChests = false;
	private boolean mDepthsEnabled = false;
	private boolean mTrickyCreepersEnabled = true;

	private final Map<String, Integer> mShardCounts = new HashMap<>();

	public ServerProperties() {
	}

	public static boolean getJoinMessagesEnabled() {
		return INSTANCE.mJoinMessagesEnabled;
	}

	public static boolean getIsTownWorld() {
		return INSTANCE.mIsTownWorld;
	}

	public static int getPlotSurvivalMinHeight() {
		return INSTANCE.mPlotSurvivalMinHeight;
	}

	public static boolean getIsSleepingEnabled() {
		return INSTANCE.mIsSleepingEnabled;
	}

	public static boolean getKeepLowTierInventory() {
		return INSTANCE.mKeepLowTierInventory;
	}

	public static boolean getClassSpecializationsEnabled(@Nullable Player player) {
		if (player == null) {
			return INSTANCE.mClassSpecializationsEnabled;
		}
		if (INSTANCE.mClassSpecializationsEnabled) {
			return true;
		}
		DungeonUtils.DungeonCommandMapping mapping = DungeonUtils.DungeonCommandMapping.getByShard(getShardName());
		if (mapping == null) {
			return false;
		}
		String typeName = mapping.getTypeName();
		if (typeName == null) {
			return false;
		}
		return ScoreboardUtils.getScoreboardValue(player.getName(), typeName).orElse(0) == 1;
	}

	public static boolean getAbilityEnhancementsEnabled(@Nullable Player player) {
		if (player == null) {
			return INSTANCE.mAbilityEnhancementsEnabled;
		}
		if (INSTANCE.mAbilityEnhancementsEnabled) {
			return true;
		}
		DungeonUtils.DungeonCommandMapping mapping = DungeonUtils.DungeonCommandMapping.getByShard(getShardName());
		if (mapping == null) {
			return false;
		}
		String typeName = mapping.getTypeName();
		if (typeName == null) {
			return false;
		}
		return ScoreboardUtils.getScoreboardValue(player.getName(), typeName).orElse(0) == 1;
	}

	public static boolean getAuditMessagesEnabled() {
		return INSTANCE.mAuditMessagesEnabled;
	}

	public static boolean getRepairExplosions() {
		return INSTANCE.mRepairExplosions;
	}

	public static @Nullable Pattern getRepairExplosionsWorldPattern() {
		return INSTANCE.mRepairExplosionsWorldPattern;
	}

	public static boolean getPreventDungeonItemTransfer() {
		return INSTANCE.mPreventDungeonItemTransfer;
	}

	public static boolean getReplaceSpawnerEntities() {
		return INSTANCE.mReplaceSpawnerEntities;
	}

	public static boolean getInfusionsEnabled() {
		return INSTANCE.mInfusionsEnabled;
	}

	public static boolean getLootBoxEnabled() {
		return INSTANCE.mLootBoxEnabled;
	}

	public static int getHTTPStatusPort() {
		return INSTANCE.mHTTPStatusPort;
	}

	public static String getShardName() {
		return INSTANCE.mShardName;
	}

	public static boolean getEntityScoresDisabled(World world) {
		if (world == null) {
			return true;
		}

		if (INSTANCE.mDisableEntityScoresInDefaultWorld) {
			// TODO Should statically read server.properties instead, or add NMS code
			if (Bukkit.getServer().getWorlds().get(0) == world) {
				return true;
			}
		}

		String worldName = world.getName();
		return INSTANCE.mDisableEntityScoresInWorlds.contains(worldName);
	}

	public static Set<Material> getUnbreakableBlocks() {
		return INSTANCE.mUnbreakableBlocks;
	}

	public static Set<Material> getAlwaysPickupMats() {
		return INSTANCE.mAlwaysPickupMats;
	}

	public static Set<Material> getNamedPickupMats() {
		return INSTANCE.mNamedPickupMats;
	}

	public static List<NamespacedKey> getEggifySpawnEggs() {
		return INSTANCE.mEggifySpawnEggs;
	}

	public static int getLootingLimiterMobKills() {
		return INSTANCE.mLootingLimiterMobKills;
	}

	public static int getLootingLimiterSpawners() {
		return INSTANCE.mLootingLimiterSpawners;
	}

	public static boolean getLootingLimiterIgnoreBreakingChests() {
		return INSTANCE.mLootingLimiterIgnoreBreakingChests;
	}

	public static boolean getDepthsEnabled() {
		return INSTANCE.mDepthsEnabled;
	}

	public static boolean getTrickyCreepersEnabled() {
		return INSTANCE.mTrickyCreepersEnabled;
	}

	public static boolean getMasterworkRefundEnabled() {
		return INSTANCE.mMasterworkRefundEnabled;
	}

	public static int getShardCount(String shard) {
		return INSTANCE.mShardCounts.getOrDefault(shard, 1);
	}

	public static void load(Plugin plugin, @Nullable CommandSender sender) {
		INSTANCE.loadInternal(plugin, sender);
	}

	private void loadInternal(Plugin plugin, @Nullable CommandSender sender) {
		QuestUtils.loadScriptedQuests(plugin, "properties", sender, (object) -> {
			mJoinMessagesEnabled = getPropertyValueBool(object, "joinMessagesEnabled", mJoinMessagesEnabled);
			mIsTownWorld = getPropertyValueBool(object, "isTownWorld", mIsTownWorld);
			mPlotSurvivalMinHeight = getPropertyValueInt(object, "plotSurvivalMinHeight", mPlotSurvivalMinHeight);

			mIsSleepingEnabled = getPropertyValueBool(object, "isSleepingEnabled", mIsSleepingEnabled);
			mKeepLowTierInventory = getPropertyValueBool(object, "keepLowTierInventory", mKeepLowTierInventory);
			mClassSpecializationsEnabled = getPropertyValueBool(object, "classSpecializationsEnabled", mClassSpecializationsEnabled);
			mAbilityEnhancementsEnabled = getPropertyValueBool(object, "abilityEnhancementsEnabled", mAbilityEnhancementsEnabled);
			mAuditMessagesEnabled = getPropertyValueBool(object, "auditMessagesEnabled", mAuditMessagesEnabled);
			mRepairExplosions = getPropertyValueBool(object, "repairExplosions", mRepairExplosions);
			String repairExplosionsWorldPattern = getPropertyValueString(object, "repairExplosionsWorldPattern", null);
			if (repairExplosionsWorldPattern != null) {
				try {
					mRepairExplosionsWorldPattern = Pattern.compile(repairExplosionsWorldPattern);
				} catch (PatternSyntaxException e) {
					String error = "Error in repairExplosionsWorldPattern: " + e.getMessage();
					plugin.getLogger().warning(error);
					if (sender != null) {
						sender.sendMessage(Component.text(error, NamedTextColor.RED));
					}
				}
			}
			mPreventDungeonItemTransfer = getPropertyValueBool(object, "preventDungeonItemTransfer", mPreventDungeonItemTransfer);
			mReplaceSpawnerEntities = getPropertyValueBool(object, "replaceSpawnerEntities", mReplaceSpawnerEntities);
			mInfusionsEnabled = getPropertyValueBool(object, "infusionsEnabled", mInfusionsEnabled);
			mMasterworkRefundEnabled = getPropertyValueBool(object, "masterworkRefundEnabled", mMasterworkRefundEnabled);
			mLootBoxEnabled = getPropertyValueBool(object, "lootBoxEnabled", mLootBoxEnabled);
			mHTTPStatusPort = getPropertyValueInt(object, "httpStatusPort", mHTTPStatusPort);

			mDisableEntityScoresInDefaultWorld = getPropertyValueBool(object,
				"disableEntityScoresInDefaultWorld",
				mDisableEntityScoresInDefaultWorld);
			mDisableEntityScoresInWorlds.clear();
			JsonElement disableEntityScoresInWorldsElement = object.get("disableEntityScoresInWorlds");
			if (disableEntityScoresInWorldsElement instanceof JsonArray disableEntityScoresInWorldsArray) {
				for (JsonElement disabledWorldElement : disableEntityScoresInWorldsArray) {
					if (disabledWorldElement instanceof JsonPrimitive disabledWorld
						&& disabledWorld.isString()) {
						mDisableEntityScoresInWorlds.add(disabledWorld.getAsString());
					}
				}
			}

			mShardName = getPropertyValueString(object, "shardName", mShardName);

			getPropertyValueMaterialList(plugin, object, "unbreakableBlocks", sender, mUnbreakableBlocks);
			getPropertyValueMaterialList(plugin, object, "alwaysPickupMaterials", sender, mAlwaysPickupMats);
			getPropertyValueMaterialList(plugin, object, "namedPickupMaterials", sender, mNamedPickupMats);

			getPropertyValueCollection(plugin, object, "eggifySpawnEggs", sender, NamespacedKeyUtils::fromString, mEggifySpawnEggs);

			mLootingLimiterMobKills = getPropertyValueInt(object, "lootingLimiterMobKills", mLootingLimiterMobKills);
			mLootingLimiterSpawners = getPropertyValueInt(object, "lootingLimiterSpawners", mLootingLimiterSpawners);
			mLootingLimiterIgnoreBreakingChests = getPropertyValueBool(object, "lootingLimiterIgnoreBreakingChests", mLootingLimiterIgnoreBreakingChests);

			mDepthsEnabled = getPropertyValueBool(object, "depthsEnabled", mDepthsEnabled);
			mTrickyCreepersEnabled = getPropertyValueBool(object, "trickyCreepersEnabled", mTrickyCreepersEnabled);

			JsonElement shardCounts = object.get("shardCounts");
			if (shardCounts != null) {
				mShardCounts.clear();
				for (Map.Entry<String, JsonElement> shardCount : shardCounts.getAsJsonObject().entrySet()) {
					mShardCounts.put(shardCount.getKey(), shardCount.getValue().getAsJsonPrimitive().getAsInt());
				}
			}

			return null;
		});

		plugin.getLogger().info("Properties:");
		if (sender != null) {
			sender.sendMessage("Properties:");
		}
		for (String str : toDisplay()) {
			plugin.getLogger().info("  " + str);
			if (sender != null) {
				sender.sendMessage("  " + str);
			}
		}
	}

	private List<String> toDisplay() {
		List<String> out = new ArrayList<>();

		out.add("joinMessagesEnabled = " + mJoinMessagesEnabled);
		out.add("isTownWorld = " + mIsTownWorld);
		out.add("plotSurvivalMinHeight = " + mPlotSurvivalMinHeight);

		out.add("isSleepingEnabled = " + mIsSleepingEnabled);
		out.add("keepLowTierInventory = " + mKeepLowTierInventory);
		out.add("classSpecializationsEnabled = " + mClassSpecializationsEnabled);
		out.add("abilityEnhancementsEnabled = " + mAbilityEnhancementsEnabled);
		out.add("auditMessagesEnabled = " + mAuditMessagesEnabled);
		out.add("repairExplosions = " + mRepairExplosions);
		out.add("repairExplosionsWorldPattern = " + (mRepairExplosionsWorldPattern == null ? null : mRepairExplosionsWorldPattern.pattern()));
		out.add("preventDungeonItemTransfer = " + mPreventDungeonItemTransfer);
		out.add("replaceSpawnerEntities = " + mReplaceSpawnerEntities);
		out.add("infusionsEnabled = " + mInfusionsEnabled);
		out.add("masterworkRefundEnabled = " + mMasterworkRefundEnabled);
		out.add("lootBoxEnabled = " + mLootBoxEnabled);
		out.add("httpStatusPort = " + mHTTPStatusPort);

		out.add("shardName = " + mShardName);

		out.add("disableEntityScoresInDefaultWorld = " + mDisableEntityScoresInDefaultWorld);
		out.add("disableEntityScoresInWorlds = [" + String.join(" ", mDisableEntityScoresInWorlds) + "]");

		out.add("unbreakableBlocks = [" + mUnbreakableBlocks.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("alwaysPickupMaterials = [" + mAlwaysPickupMats.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");
		out.add("namedPickupMaterials = [" + mNamedPickupMats.stream().map(Enum::toString).collect(Collectors.joining("  ")) + "]");

		out.add("eggifySpawnEggs = <set of " + mEggifySpawnEggs.size() + " loot tables>");

		out.add("lootingLimiterMobKills = " + mLootingLimiterMobKills);
		out.add("lootingLimiterSpawners = " + mLootingLimiterSpawners);
		out.add("lootingLimiterIgnoreBreakingChests = " + mLootingLimiterIgnoreBreakingChests);

		out.add("depthsEnabled = " + mDepthsEnabled + " (NB: changing this requires a restart)");

		out.add("trickyCreepersEnabled = " + mTrickyCreepersEnabled);

		out.add("shardCounts = " + mShardCounts);

		return out;
	}

	private boolean getPropertyValueBool(JsonObject object, String propertyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(propertyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		return value;
	}

	private int getPropertyValueInt(JsonObject object, String propertyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(propertyName);
		if (element != null) {
			value = element.getAsInt();
		}

		return value;
	}

	@Contract("_, _, !null -> !null")
	private @Nullable String getPropertyValueString(JsonObject object, String propertyName, @Nullable String defaultVal) {
		String value = defaultVal;

		JsonElement element = object.get(propertyName);
		if (element != null) {
			value = element.getAsString();
		}

		return value;
	}

	private void getPropertyValueMaterialList(Plugin plugin, JsonObject object, String propertyName, @Nullable CommandSender sender, Set<Material> set) {
		getPropertyValueCollection(plugin, object, propertyName, sender, Material::getMaterial, set);
	}

	private <T> void getPropertyValueCollection(Plugin plugin, JsonObject object, String propertyName, @Nullable CommandSender sender,
	                                            Function<String, T> parser, Collection<T> collection) {
		JsonElement element = object.get(propertyName);
		if (element != null) {
			collection.clear();

			for (JsonElement iter : element.getAsJsonArray()) {
				try {
					T value = parser.apply(iter.getAsString());
					if (value != null) {
						collection.add(value);
					}
				} catch (Exception e) {
					plugin.getLogger().severe("Invalid " + propertyName + " element: '" + iter + "'");
					e.printStackTrace();

					if (sender != null) {
						sender.sendMessage(ChatColor.RED + "Invalid " + propertyName + " element: '" + iter + "'");
						MessagingUtils.sendStackTrace(sender, e);
					}
				}
			}
		}
	}

	public static ItemStatUtils.Region getRegion(Player player) {
		return ServerProperties.getAbilityEnhancementsEnabled(player) ? ItemStatUtils.Region.RING : ServerProperties.getClassSpecializationsEnabled(player) ? ItemStatUtils.Region.ISLES : ItemStatUtils.Region.VALLEY;
	}
}

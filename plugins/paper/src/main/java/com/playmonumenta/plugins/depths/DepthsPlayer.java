package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.depths.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class DepthsPlayer {

	//Unique identifier for the player
	public UUID mPlayerId;
	//A map containing all abilities the player has and their current rarity
	public Map<String, Integer> mAbilities = new HashMap<>();
	//Unique identifier, mapping to an active depths party object
	public long mPartyNum;
	//The depths ability trees the player is eligible to select from this run
	public List<DepthsTree> mEligibleTrees;
	//Weapon offering options for the player
	public transient List<WeaponAspectDepthsAbility> mWeaponOfferings;
	//Whether or not they have selected a weapon aspect
	public boolean mHasWeaponAspect;
	//Whether or not they have used chaos room this floor already
	public boolean mUsedChaosThisFloor;
	//Whether or not they have deleted an ability on this floor
	public boolean mUsedAbilityDeletion;
	//Individual treasure score. Copied from the party's treasure score when they die.
	public int mFinalTreasureScore;
	//Reward queue implementation to let the player catch up on reward chests they have missed later
	public Queue<DepthsRewardType> mEarnedRewards;

	public DepthsPlayer(Player p) {
		mPlayerId = p.getUniqueId();
		mAbilities = new HashMap<>();
		p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Initializing Depths System! Press button again to begin.");
		mEligibleTrees = initTrees(p);

		//Randomize order of weapon aspects
		//First 3 elements available by default, others locked behind transaction
		mWeaponOfferings = DepthsManager.getWeaponAspects();
		Collections.shuffle(mWeaponOfferings);

		mUsedChaosThisFloor = false;

		//Dummy value- either replaced on death or on win
		mFinalTreasureScore = -1;

		mEarnedRewards = new ConcurrentLinkedQueue<>();

	}

	public List<DepthsTree> initTrees(Player player) {
		int talismanScore = ScoreboardUtils.getScoreboardValue(player, "DDTalisman");
		if (0 < talismanScore && talismanScore <= DepthsUtils.TREES.length) {
			double chance = 1;
			if (player.getScoreboardTags().contains(DepthsManager.ENDLESS_MODE_STRING)) {
				chance = 0.75;
			}

			double rand = Math.random();
			if (4/7.0 < rand && rand < chance) {
				List<DepthsTree> randomTrees = new ArrayList<DepthsTree>();

				DepthsTree talismanTree = DepthsUtils.TREES[talismanScore - 1];
				randomTrees.add(talismanTree);
				player.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Due to your Talisman, you have become a " + DepthsUtils.TREE_NAMES[talismanScore - 1] + " when you would not have otherwise!");

				List<DepthsTree> allTrees = new ArrayList<>();
				//Get all possible trees and shuffle them
				Collections.addAll(allTrees, DepthsTree.values());
				allTrees.remove(talismanTree);
				Collections.shuffle(allTrees);

				//Select one less tree since we already have one chosen
				for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN - 1; i++) {
					randomTrees.add(allTrees.get(i));
				}

				return randomTrees;
			}
		}
		return initRandomTrees();
	}

	/**
	 * Gets random trees from the system that the player can exclusively select from
	 * @return tree list
	 */
	public List<DepthsTree> initRandomTrees() {
		List<DepthsTree> randomTrees = new ArrayList<>();
		List<DepthsTree> allTrees = new ArrayList<>();
		//Get all possible trees and shuffle them
		Collections.addAll(allTrees, DepthsTree.values());
		Collections.shuffle(allTrees);

		//Select the first x values as valid tree offerings this run
		for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN; i++) {
			randomTrees.add(allTrees.get(i));
		}

		return randomTrees;
	}

}

package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellDelayedAction;
import com.playmonumenta.bossfights.spells.SpellRunAction;

public class CorruptInfestedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_corruptinfested";
	public static final int detectionRange = 30;

	LivingEntity mBoss;
	Plugin mPlugin;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CorruptInfestedBoss(plugin, boss);
	}

	public CorruptInfestedBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		mPlugin = plugin;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> mBoss.getLocation().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation(), 1, 0.2, 0.2, 0.2, 0))
		);

		// Boss effectively does nothing
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void death() {
		// Spell triggered when the boss dies
		new SpellDelayedAction(mPlugin, mBoss.getLocation(), 60,
		                       // Sound effect when boss dies
		                       (Location loc) -> {
		                           loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_DEATH, 1f, 0.65f);
		                       },
		                       // Particles while maggots incubate
		                       (Location loc) -> {
		                           //TODO: Change this to a darker more appropriate particle
		                           loc.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.6, 0.6, 0.6, 0);
		                       },
		                       // Maggots spawn
		                       (Location loc) -> {
		                           loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_DEATH, 1f, 0.1f);
		                           loc.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, -1, 0), 50, 0.6, 0.6, 0.6, 0);
		                           //TODO: Raise location up to avoid spawning in blocks?
		                           for (int i = 0; i < 2; i++) {
		                               Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:zombie_pigman "+ loc.getX() +" "+ loc.getY() +" "+ loc.getZ() +" {Anger:3297s,IsBaby:1b,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:5643800,Name:\"{\\\"text\\\":\\\"§fInfernal Boots\\\"}\"}}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:5643800,Name:\"{\\\"text\\\":\\\"§fInfernal Pants\\\"}\"}}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:5643800,Name:\"{\\\"text\\\":\\\"§fInfernal Tunic\\\"}\"}}},{}],HandItems:[{id:\"minecraft:blaze_powder\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§bBlazing Soul\\\"}\"},Enchantments:[{lvl:3s,id:\"minecraft:fire_aspect\"}],AttributeModifiers:[{UUIDMost:-2899840319788137997L,UUIDLeast:-6474403868551417057L,Amount:5.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:blaze_powder\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§bBlazing Soul\\\"}\"},Enchantments:[{lvl:3s,id:\"minecraft:fire_aspect\"}],AttributeModifiers:[{UUIDMost:-2899840319788137997L,UUIDLeast:-6474403868551417057L,Amount:5.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}}]}");
		                           }
		                       }).run();
	}

}

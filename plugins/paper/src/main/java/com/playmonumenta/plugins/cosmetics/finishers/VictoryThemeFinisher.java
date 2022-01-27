package com.playmonumenta.plugins.cosmetics.finishers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.scriptedquests.Plugin;

public class VictoryThemeFinisher {

	public static final String NAME = "Victory Theme";

	public static void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		loc.add(0, 1.5, 0);
		Location loc2 = loc.clone().add(-1, 0, 0);
		Location loc3 = loc.clone().add(1, 0, 0);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
					case 3:
					case 6:
					case 9:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS21);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 19:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS9);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.FS12);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.B17);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 28:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.CS19);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 37:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS21);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 43:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.CS19);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 46:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS21);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 47:
						this.cancel();
						break;
					default:
						break;
				}
				if (mTicks >= 47) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
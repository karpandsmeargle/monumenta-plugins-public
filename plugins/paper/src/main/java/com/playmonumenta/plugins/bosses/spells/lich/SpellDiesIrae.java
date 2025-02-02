package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 * This isn't inspired by etrian odyssey 4 at all *heavy sarcasm*
 */

public class SpellDiesIrae extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private LivingEntity mKey;
	private Location mCenter;
	private double mRange;
	private int mCeiling;
	private List<Location> mCrystalLoc;
	private Collection<EnderCrystal> mCrystal = new ArrayList<EnderCrystal>();
	private String mCrystalNBT;
	private static double mCrystalDmg;
	private final PartialParticle mCloud;
	private final PartialParticle mExpH;
	private final PartialParticle mSoul;
	private final PartialParticle mBreath1;
	private final PartialParticle mBreath2;
	private final PartialParticle mBreath3;
	private final PartialParticle mExpL1;
	private final PartialParticle mExpL2;
	private final PartialParticle mHeart;

	public SpellDiesIrae(Plugin plugin, LivingEntity boss, LivingEntity key, Location loc, double range, int ceil, List<Location> crystalLoc, String crystalnbt) {
		mPlugin = plugin;
		mBoss = boss;
		mKey = key;
		mCenter = loc;
		mRange = range;
		mCeiling = ceil;
		mCrystalLoc = crystalLoc;
		mCrystalNBT = crystalnbt;
		mCloud = new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 50, 0.1, 0.1, 0.1, 0.1);
		mExpH = new PartialParticle(Particle.EXPLOSION_HUGE, mBoss.getLocation(), 1, 0, 0, 0, 0.1).minimumCount(1);
		mSoul = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 2, 0, 0, 0, 0.03);
		mBreath1 = new PartialParticle(Particle.DRAGON_BREATH, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0);
		mExpL1 = new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 0, 0, 0, 0).minimumCount(1);
		mBreath2 = new PartialParticle(Particle.DRAGON_BREATH, mBoss.getLocation(), 15, 0.4, 0.4, 0.4, 0.01);
		mHeart = new PartialParticle(Particle.HEART, mBoss.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
		mBreath3 = new PartialParticle(Particle.DRAGON_BREATH, mBoss.getLocation(), mCrystal.size() * 1000 + 7000, 42, 0, 42, 0.01);
		mExpL2 = new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), mCrystal.size() * 125 + 1000, 42, 0.75, 42, 0);
	}

	public static double getDmg() {
		return mCrystalDmg;
	}

	public static void initDmg(double n) {
		mCrystalDmg = n;
	}

	@Override
	public boolean canRun() {
		if (Lich.getCD()) {
			return false;
		}
		return true;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		BossBar bar = Bukkit.getServer().createBossBar(null, BarColor.GREEN, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
		bar.setVisible(true);
		//only spawn crystals if auto respawn didn't summon a wave 2 seconds before
		if (!SpellCrystalRespawn.getmSpawned()) {
			Lich.spawnCrystal(mCrystalLoc, 4, mCrystalNBT);
		}

		//tele lich to center + 10 blocks + invuln + no ai
		mBoss.teleport(mCenter.clone().add(0, 10, 0));
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3.0f, 0.5f);
		mCloud.location(mBoss.getLocation()).spawnAsBoss();

		//get all active crystals
		for (Location l : mCrystalLoc) {
			mCrystal.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 10));
		}

		//time limit 6s to break all active crystals
		BukkitRunnable runA = new BukkitRunnable() {
			double mT;
			int mCount = mCrystal.size();

			@Override
			public void run() {
				//keep boss in place in case of tp function
				mBoss.teleport(mCenter.clone().add(0, 10, 0));
				//glowy crystal to tell players to break
				if (mT == 0) {
					ScoreboardUtils.modifyTeamColor("crystal", NamedTextColor.WHITE);
					for (EnderCrystal e : mCrystal) {
						e.setGlowing(true);
						e.setBeamTarget(mBoss.getLocation().add(0, 0, 0));
						ScoreboardUtils.addEntityToTeam(e, "crystal");
					}
				}
				//exit function
				mCrystal.removeIf(en -> !en.isValid());
				if (mCrystal.size() == 0) {
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 4.0f, 0.5f);
					Lich.bossGotHit(true);
					mBoss.setAI(true);
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);
					bar.setVisible(false);
					this.cancel();
					return;
				}
				//warning 1
				if (mT == 20 * 2) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3.0f, 0.75f);
					ScoreboardUtils.modifyTeamColor("crystal", NamedTextColor.YELLOW);
				}
				//warning 2
				if (mT == 20 * 4) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3.0f, 0.75f);
					ScoreboardUtils.modifyTeamColor("crystal", NamedTextColor.RED);
				}
				//execute order 66
				if (mT >= 20 * 6) {
					bar.setVisible(false);
					attack();
					this.cancel();
				}
				mT++;
				//boss bar stuff
				int remain = mCrystal.size();
				double progress = remain * 1.0d / mCount;
				bar.setTitle(ChatColor.YELLOW + "" + remain + " Death Crystals Remaining!");
				bar.setProgress(progress);
				if (progress <= 0.34) {
					bar.setColor(BarColor.RED);
				} else if (progress <= 0.67) {
					bar.setColor(BarColor.YELLOW);
				}
				for (Player player : Lich.playersInRange(mCenter, mRange, true)) {
					if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
						bar.addPlayer(player);
					} else {
						bar.removePlayer(player);
					}
				}
			}
		};
		runA.runTaskTimer(mPlugin, 20 * 1, 1);
		mActiveRunnables.add(runA);
	}

	private void attack() {
		int countCrystal = mCrystal.size();
		World world = mBoss.getWorld();
		mCrystalDmg = Math.min(1.2, mCrystal.size() * 0.2);
		int debuffTicks = mCrystal.size() * 5 * 20;

		//healing and final damage calc
		double heal = mBoss.getHealth() + EntityUtils.getMaxHealth(mBoss) * mCrystal.size() * 0.025;
		double healthFinal = Math.min(heal, EntityUtils.getMaxHealth(mBoss));
		double keyheal = mKey.getHealth() + EntityUtils.getMaxHealth(mKey) * mCrystal.size() * 0.05;
		double keyHealthFinal = Math.min(keyheal, EntityUtils.getMaxHealth(mKey));

		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10.0f, 0.5f);
		//kill ghast shield
		for (Location loc : mCrystalLoc) {
			List<LivingEntity> enList = EntityUtils.getNearbyMobs(loc, 3);
			for (LivingEntity en : enList) {
				en.setHealth(0);
			}
		}
		//kill end crystals
		for (EnderCrystal e : mCrystal) {
			e.remove();
			mExpH.location(e.getLocation()).spawnAsBoss();
			world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3f, 1f);
		}

		//heal boss + damage with delay (only once)
		BukkitRunnable runB = new BukkitRunnable() {
			int mInc = 0;

			@Override
			public void run() {
				mInc++;
				if (mInc < 20 * 2) {
					for (EnderCrystal e : mCrystal) {
						Location mStart = e.getLocation();
						Location mEnd = mBoss.getLocation().add(0, 1.5, 0);
						Vector vec = LocationUtils.getVectorTo(mEnd, mStart);

						Location particleLoc = mStart.add(vec.multiply(mInc / 40d));
						mSoul.location(particleLoc).spawnAsBoss();
						mBreath1.location(particleLoc).spawnAsBoss();
					}
				}
				if (mInc == 20 * 2) {
					mHeart.location(mBoss.getEyeLocation()).spawnAsBoss();
					world.playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 3.0f, 1.5f);
					mBoss.setHealth(healthFinal);
					if (mKey.isValid()) {
						mKey.setHealth(keyHealthFinal);
					}
				}
				if (mInc == 20 * 3) {
					Location pitch = mBoss.getLocation();
					pitch.setPitch(90);
					mBoss.teleport(pitch);
				}
				if (mInc == 20 * 4) {
					world.playSound(mBoss.getLocation().clone(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10.0f, 0.5f);
				}
				if (mInc >= 20 * 4 && mInc < 20 * (4 + 1)) {
					Vector vec = LocationUtils.getVectorTo(mCenter, mBoss.getLocation()).multiply((mInc - 4 * 20) / 20d);
					Location pLoc = mBoss.getLocation();
					mExpL1.location(pLoc.add(vec)).spawnAsBoss();
					mBreath2.location(pLoc.add(vec)).spawnAsBoss();
				}
				if (mInc == 20 * (4 + 1)) {
					mExpL2.location(mCenter.clone().add(0, 0.5, 0)).spawnAsBoss();
					mBreath3.location(mCenter.clone()).spawnAsBoss();
					List<Player> players = Lich.playersInRange(mCenter, mRange, true);
					players.removeIf(pl -> SpellDimensionDoor.getShadowed().contains(pl) || pl.getLocation().getY() >= mCenter.getY() + mCeiling);
					for (Player p : players) {
						BossUtils.bossDamagePercent(mBoss, p, mCrystalDmg, "Dies Irae");
						AbilityUtils.increaseHealingPlayer(p, debuffTicks, -1.0, "Lich");
						PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.SLOW, debuffTicks, 0));
						PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.WEAKNESS, debuffTicks, 1));
						world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 0.5f);

						// If the attack was larger than 5, mark the player with a scoreboard tag for DayOfWrath
						if (countCrystal >= 5) {
							p.addScoreboardTag("LichDayOfWrath");
						}
					}
				}
				if (mInc >= 20 * (4 + 2)) {
					Location reset = mCenter.clone();
					reset.setPitch(0);
					Lich.bossGotHit(true);
					mBoss.teleport(reset);
					mBoss.setAI(true);
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);
					mCrystalDmg = 0;
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3.0f, 0.5f);
					this.cancel();
				}
			}
		};
		runB.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runB);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 16;
	}

}

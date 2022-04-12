package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellArachnopocolypse;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthsWrath;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellGroundSurge;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellKaulsJudgement;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStorm;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStrike;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPutridPlague;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellRaiseJungle;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellVolcanicDemise;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


/* Woah it's Kaul! */

/*
Phase 1 :
Attacks :
Raise Jungle
Arachnopocalypse
Putrid plague
Earth's Wrath


Phase 2 :
Earth's wrath
Putrid Plague
Raise Jungle
Kaul's Judgement

Phase 2.5 (50% health) :
Summons a powerful Primordial Elemental that is invulnerable and immovable until out of the ground. Players will have 15 seconds to prepare for the elemental's arrival. Kaul will not be attacking or casting any abilities (except for his passives) during this time. (512 health)

Elemental's Abilities:
Normal Block break passive
Raise Jungle (Kaul's ability), however the timer for raising them will be 30 seconds instead of 40.
Earthen Rupture: After charging for 2 seconds, the Elemental will cause a large rupture that spans out 5 blocks, knocking back all players, dealing 18 damage, and applying Slowness II for 10 seconds.
Stone Blast: After 1 second, fires at all players a powerful block breaking bolt. Intersecting with a player causes 15 damage and applies Weakness II and Slowness II. Intersecting with a block causes a TNT explosion to happen instead. The bolt will stop travelling if it hits a player or a block.
Once the elemental is dead, Kaul returns to the fight. The elemental will meld into the ground for later return in Phase 3.5

Phase 3:
Earthâ€™s Wrath
Putrid plague
Volcanic demise
Kaul's Judgement

Phase 3.5 (20% health [Let's make this even harder shall we?]) :
The Primordial Elemental from Phase 2.5 returns, however he is completely invulnerable to all attacks, and gains Strength I for the rest of the fight. The elemental will remain active until the end of the fight.
The elemental will lose his "Raise Jungle" ability, but will still possess the others.

 *
 */
/*
 * Base Spells:
 * /
 * Volcanic Demise (Magma cream): Kaul shoots at each player at the
 * same time, casting particle fireballs at each player with a large
 * hit radius. On contact with a player, deals 20 damage and ignites
 * the player for 10 seconds. If the player shields the fireball, the
 * shield takes 50% durability damage and is put on cooldown for 30
 * seconds (The player is still set on fire). On contact with a block,
 * explodes and leaves a fiery aura that deals 10 damage and ignites
 * for 5 seconds (+3 seconds for fire duration if a player is already on
 * fire) to players who stand in it. The aura lasts 5 seconds.
 * (Aka Disco Inferno)
 */

public class Kaul extends BossAbilityGroup {
	public static final int ARENA_WIDTH = 111;
	// Barrier layer is from Y 62.0 to 64.0
	public static final int ARENA_MAX_Y = 62;

	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_1 = 18;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_2 = 12;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_3 = 10;
	private static final int LIGHTNING_STRIKE_COOLDOWN_SECONDS_4 = 6;

	private static final int MAX_HEALTH = 2048;
	private static final double SCALING_X = 0.7;
	private static final double SCALING_Y = 0.65;

	// At the centre of the Kaul shrine,
	// upon the height of most of the arena's surface
	private @Nullable LivingEntity mShrineMarker;

	public static final String identityTag = "boss_kaul";
	public static final int detectionRange = 50;
	private static final String primordial = "PrimordialElemental";
	private static final String immortal = "ImmortalElemental";
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private boolean mDefeated = false;
	private boolean mCooldown = false;
	private boolean mPrimordialPhase = false;
	private int mHits = 0;
	private int mPlayerCount;
	private double mDefenseScaling;

	private static final String LIGHTNING_STORM_TAG = "KaulLightningStormTag";
	private static final String PUTRID_PLAGUE_TAG_RED = "KaulPutridPlagueRed";
	private static final String PUTRID_PLAGUE_TAG_BLUE = "KaulPutridPlagueBlue";
	private static final String PUTRID_PLAGUE_TAG_YELLOW = "KaulPutridPlagueYellow";
	private static final String PUTRID_PLAGUE_TAG_GREEN = "KaulPutridPlagueGreen";
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Kaul(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Kaul(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		for (Entity e : boss.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(LIGHTNING_STORM_TAG) && e instanceof LivingEntity) {
				mShrineMarker = (LivingEntity) e;
				break;
			}
		}
		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
		mBoss.setRemoveWhenFarAway(false);
		World world = boss.getWorld();
		mBoss.addScoreboardTag("Boss");

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
					if (player.isSleeping()) {
						DamageUtils.damage(mBoss, player, DamageType.OTHER, 22);
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 1));
						player.sendMessage(ChatColor.DARK_GREEN + "THE JUNGLE FORBIDS YOU TO DREAM.");
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1, 0.85f);
					}
				}
				if (mDefeated || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);
		SpellManager phase1Spells = new SpellManager(
		    Arrays.asList(new SpellRaiseJungle(mPlugin, mBoss, 10, detectionRange, 20 * 9, 20 * 10, mShrineMarker.getLocation().getY()),
		                  new SpellPutridPlague(mPlugin, mBoss, detectionRange, false, mShrineMarker.getLocation()),
		                  new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
		                  new SpellArachnopocolypse(mPlugin, mBoss, detectionRange, mSpawnLoc)));

		Spell judgement = SpellKaulsJudgement.getInstance(mSpawnLoc);

		SpellManager phase2Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, detectionRange, false, mShrineMarker.getLocation()),
			              new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
			              new SpellRaiseJungle(mPlugin, mBoss, 10, detectionRange, 20 * 8, 20 * 10, mShrineMarker.getLocation().getY()),
			              new SpellArachnopocolypse(mPlugin, mBoss, detectionRange, mSpawnLoc),
			              judgement));

		SpellManager phase3Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, detectionRange, true, mShrineMarker.getLocation()),
			              new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
			              new SpellVolcanicDemise(plugin, mBoss, 40D, mShrineMarker.getLocation()),
			              new SpellGroundSurge(mPlugin, mBoss, detectionRange),
			              judgement));

		SpellManager phase4Spells = new SpellManager(
			Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, detectionRange, true, mShrineMarker.getLocation()),
			              new SpellEarthsWrath(mPlugin, mBoss, mShrineMarker.getLocation().getY()),
			              new SpellVolcanicDemise(plugin, mBoss, 40D, mShrineMarker.getLocation()),
			              new SpellGroundSurge(mPlugin, mBoss, detectionRange)));

		List<UUID> hit = new ArrayList<UUID>();

		List<UUID> cd = new ArrayList<UUID>();
		SpellPlayerAction action = new SpellPlayerAction(mBoss, detectionRange, (Player player) -> {
			Vector loc = player.getLocation().toVector();
			if (player.getLocation().getBlock().isLiquid() || !loc.isInSphere(mShrineMarker.getLocation().toVector(), 42)) {
				if (player.getLocation().getY() >= 61 || cd.contains(player.getUniqueId())) {
					return;
				}
				// Damage has no direction so can't be blocked */
				if (BossUtils.bossDamagePercent(mBoss, player, 0.4, (Location)null)) {
					/* Player survived the damage */
					MovementUtils.knockAway(mSpawnLoc, player, -2.5f, 0.85f);
					world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1, 1.3f);
					world.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 80, 0.25, 0.45, 0.25, 0.15);
					cd.add(player.getUniqueId());
					new BukkitRunnable() {
						@Override
						public void run() {
							cd.remove(player.getUniqueId());
						}
					}.runTaskLater(mPlugin, 10);
				}
				if (player.getLocation().getBlock().isLiquid()) {
					if (!hit.contains(player.getUniqueId())) {
						hit.add(player.getUniqueId());
						player.sendMessage(ChatColor.AQUA + "That hurt! It seems like the water is extremely corrosive. Best to stay out of it.");
					}
				} else if (!loc.isInSphere(mShrineMarker.getLocation().toVector(), 42)) {
					player.sendMessage(ChatColor.AQUA + "You feel a powerful force pull you back in fiercely. It seems there's no escape from this fight.");
				}
			}
		});

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
				                    0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
			}),
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_1, false, mShrineMarker.getLocation()),
			new SpellLightningStorm(boss, detectionRange),
			new SpellShieldStun(30 * 20),
			new SpellConditionalTeleport(mBoss, spawnLoc,
			                             b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().getBlock().getType() == Material.LAVA
				                             || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		List<Spell> phase2PassiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
				                    0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
			}),
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_2, true, mShrineMarker.getLocation()),
			new SpellLightningStorm(boss, detectionRange),
			new SpellShieldStun(30 * 20),
			new SpellConditionalTeleport(mBoss, spawnLoc,
			                             b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().getBlock().getType() == Material.LAVA
				                             || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		List<Spell> phase3PassiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				                    0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
				world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				                    0.35, 0.1);
				world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				                    0.35, RED_COLOR);
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				                    0.45, 0.35, Material.BLUE_WOOL.createBlockData());
			}),
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_3, true, mShrineMarker.getLocation()),
			new SpellLightningStorm(boss, detectionRange),
			new SpellShieldStun(30 * 20),
			new SpellConditionalTeleport(mBoss, spawnLoc,
			                             b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().getBlock().getType() == Material.LAVA
				                             || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		List<Spell> phase4PassiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				                    0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
				world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				                    0.35, 0.1);
				world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				                    0.35, RED_COLOR);
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				                    0.45, 0.35, Material.BLUE_WOOL.createBlockData());
			}),
			new SpellLightningStrike(this, LIGHTNING_STRIKE_COOLDOWN_SECONDS_4, true, mShrineMarker.getLocation()),
			new SpellLightningStorm(boss, detectionRange),
			new SpellShieldStun(30 * 20),
			new SpellConditionalTeleport(mBoss, spawnLoc,
			                             b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				                             || b.getLocation().getBlock().getType() == Material.LAVA
				                             || b.getLocation().getBlock().getType() == Material.WATER), action
		);


		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			if (players.size() == 1) {
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE JUNGLE WILL NOT ALLOW A LONE MORTAL LIKE YOU TO LIVE. PERISH, FOOLISH USURPER!\",\"color\":\"dark_green\"}]");
			} else {
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE JUNGLE WILL TAKE YOUR PRESENCE NO MORE. PERISH, USURPERS.\",\"color\":\"dark_green\"}]");
			}
		});

		events.put(75, mBoss -> {
			forceCastSpell(SpellArachnopocolypse.class);
		});

		// Phase 2
		events.put(66, mBoss -> {
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));

			new BukkitRunnable() {
				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					new BukkitRunnable() {
						Location mLoc = mBoss.getLocation();
						float mJ = 0;
						double mRotation = 0;
						double mRadius = 10;

						@Override
						public void run() {
							mJ++;
							world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 3, 0.5f + (mJ / 25));
							for (int i = 0; i < 5; i++) {
								double radian1 = Math.toRadians(mRotation + (72 * i));
								mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
								world.spawnParticle(Particle.SPELL_WITCH, mLoc, 6, 0.25, 0.25, 0.25, 0);
								world.spawnParticle(Particle.BLOCK_DUST, mLoc, 4, 0.25, 0.25, 0.25, 0.25,
								Material.COARSE_DIRT.createBlockData());
								mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
							}
							world.spawnParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 20, 8, 5, 8,
							                    0);
							mRotation += 8;
							mRadius -= 0.25;

							if (mBoss.isDead() || !mBoss.isValid()) {
								this.cancel();
							}

							if (mRadius <= 0) {
								this.cancel();
								Location loc = mShrineMarker.getLocation().subtract(0, 0.5, 0);
								changePhase(SpellManager.EMPTY, phase2PassiveSpells, null);
								new BukkitRunnable() {
									int mT = 0;
									double mRotation = 0;
									double mRadius = 0;

									@Override
									public void run() {
										mT++;
										mRadius = mT;
										world.spawnParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 20, 8, 5, 8, 0);
										world.spawnParticle(Particle.SMOKE_NORMAL, mShrineMarker.getLocation().add(0, 3, 0), 10, 8, 5, 8, 0);
										for (int i = 0; i < 36; i++) {
											double radian1 = Math.toRadians(mRotation + (10 * i));
											loc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
											world.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.4, 0.4, 0.4, 0);
											world.spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.4, 0.4, 0.4, 0.25,
											                    Material.COARSE_DIRT.createBlockData());
											loc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
										}
										for (Block block : LocationUtils.getEdge(loc.clone().subtract(mT, 0, mT),
										                                 loc.clone().add(mT, 0, mT))) {
											if (FastUtils.RANDOM.nextInt(6) == 1 && block.getType() == Material.SMOOTH_SANDSTONE
											    && block.getLocation().add(0, 1.5, 0).getBlock()
											    .getType() == Material.AIR) {
												block.setType(Material.SMOOTH_RED_SANDSTONE);
											}
										}
										if (mT >= 40) {
											this.cancel();
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
								for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
									player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1,
									                 0.75f);
								}
								new BukkitRunnable() {

									@Override
									public void run() {
										mBoss.setInvulnerable(false);
										mBoss.setAI(true);
										teleport(mSpawnLoc);
										mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
										new BukkitRunnable() {

											@Override
											public void run() {
												changePhase(phase2Spells, phase2PassiveSpells, null);
											}

										}.runTaskLater(mPlugin, 20 * 10);
									}

								}.runTaskLater(mPlugin, 20 * 2);
							}
						}

					}.runTaskTimer(mPlugin, 30, 1);
				}

			}.runTaskLater(mPlugin, 20 * 2);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE JUNGLE WILL DEVOUR YOU. ALL RETURNS TO ROT.\",\"color\":\"dark_green\"}]");
		});

		// Forcecast Raise Jungle
		events.put(60, mBoss -> {
			super.forceCastSpell(SpellRaiseJungle.class);
		});

		// Phase 2.5
		events.put(50, mBoss -> {
			changePhase(SpellManager.EMPTY, passiveSpells, null);
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));
			teleport(mSpawnLoc.clone().add(0, 5, 0));
			mPrimordialPhase = true;
			new BukkitRunnable() {
				Location mLoc = mSpawnLoc;
				double mRotation = 0;
				double mRadius = 10;

				@Override
				public void run() {
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(mRotation + (72 * i));
						mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						world.spawnParticle(Particle.SPELL_WITCH, mLoc, 3, 0.1, 0.1, 0.1, 0);
						world.spawnParticle(Particle.BLOCK_DUST, mLoc, 3, 0.1, 0.1, 0.1, 0.25,
						Material.DIRT.createBlockData());
						mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					}
					mRotation += 8;
					mRadius -= 0.15;
					if (mRadius <= 0) {
						this.cancel();
						world.playSound(mLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0);
						world.playSound(mLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
						world.spawnParticle(Particle.CRIT_MAGIC, mLoc, 50, 0.1, 0.1, 0.1, 1);
						world.spawnParticle(Particle.BLOCK_CRACK, mLoc, 150, 0.1, 0.1, 0.1, 0.5,
						                    Material.DIRT.createBlockData());
						LivingEntity miniboss = spawnPrimordial(mLoc);
						new BukkitRunnable() {

							@Override
							public void run() {
								if (miniboss == null) {
									this.cancel();
								} else if (miniboss.isDead() || !miniboss.isValid()) {
									this.cancel();
									mBoss.setInvulnerable(false);
									mBoss.setAI(true);
									teleport(mSpawnLoc);
									mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
									mPrimordialPhase = false;
									new BukkitRunnable() {

										@Override
										public void run() {
											changePhase(phase2Spells, passiveSpells, null);
										}

									}.runTaskLater(mPlugin, 20 * 10);
								}

								if (mBoss.isDead() || !mBoss.isValid()) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 20);
					}
					if (mBoss.isDead()) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 0, 1);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE EARTH AND JUNGLE ARE ENTWINED. PRIMORDIAL, HEWN FROM SOIL AND STONE, END THEM.\",\"color\":\"dark_green\"}]");
		});

		//Force-cast Kaul's Judgement if it hasn't been casted yet.
		events.put(40, mBoss -> {
			forceCastSpell(SpellKaulsJudgement.class);
		});

		// Phase 3
		events.put(33, mBoss -> {
			changePhase(SpellManager.EMPTY, passiveSpells, null);
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));
			new BukkitRunnable() {

				@Override
				public void run() {
					List<ArmorStand> points = new ArrayList<ArmorStand>();
					for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
						if ((e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)
						    || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)
						    || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)
						    || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) && e instanceof ArmorStand) {
							points.add((ArmorStand) e);
						}
					}

					if (!points.isEmpty()) {
						teleport(mSpawnLoc.clone().add(0, 5, 0));
						for (ArmorStand point : points) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 5, 0.75f);
							new BukkitRunnable() {
								Location mLoc = point.getLocation().add(0, 15, 0);
								Vector mDir = LocationUtils.getDirectionTo(mBoss.getLocation().add(0, 1, 0), mLoc);
								float mT = 0;

								@Override
								public void run() {
									mT++;
									if (mT % 2 == 0) {
										world.spawnParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 10, 8, 5, 9, 0);
									}
									world.spawnParticle(Particle.FLAME, mShrineMarker.getLocation().add(0, 3, 0), 10, 8, 5, 9, 0);
									world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1.25, 0), 16, 0.35, 0.45, 0.35, 0);
									world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.25, 0), 1, 0.35, 0.45, 0.35, 0);
									if (mT == 1) {
										mLoc.getWorld().createExplosion(mLoc, 6, true);
										mLoc.getWorld().createExplosion(mLoc.clone().subtract(0, 4, 0), 6, true);
									}
									mLoc.add(mDir.clone().multiply(0.35));
									if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)) {
										world.spawnParticle(Particle.FALLING_DUST, mLoc, 9, 0.4, 0.4, 0.4, Material.BLUE_WOOL.createBlockData());
										world.spawnParticle(Particle.BLOCK_DUST, mLoc, 5, 0.4, 0.4, 0.4, Material.BLUE_WOOL.createBlockData());
										world.spawnParticle(Particle.EXPLOSION_NORMAL, mLoc, 2, 0.4, 0.4, 0.4, 0.1);
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)) {
										world.spawnParticle(Particle.REDSTONE, mLoc, 15, 0.4, 0.4, 0.4, RED_COLOR);
										world.spawnParticle(Particle.FALLING_DUST, mLoc, 10, 0.4, 0.4, 0.4, Material.RED_WOOL.createBlockData());
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)) {
										world.spawnParticle(Particle.FLAME, mLoc, 10, 0.3, 0.3, 0.3, 0.1);
										world.spawnParticle(Particle.SMOKE_LARGE, mLoc, 3, 0.4, 0.4, 0.4, 0);
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) {
										world.spawnParticle(Particle.FALLING_DUST, mLoc, 9, 0.4, 0.4, 0.4, Material.GREEN_TERRACOTTA.createBlockData());
										world.spawnParticle(Particle.BLOCK_DUST, mLoc, 5, 0.4, 0.4, 0.4, Material.GREEN_TERRACOTTA.createBlockData());
										world.spawnParticle(Particle.EXPLOSION_NORMAL, mLoc, 2, 0.4, 0.4, 0.4, 0.1);
									}
									if (mLoc.distance(mSpawnLoc.clone().add(0, 5, 0)) < 1.25 || mLoc.distance(mBoss.getLocation().add(0, 1, 0)) < 1.25) {
										this.cancel();
										mHits++;
									}

									if (mBoss.isDead() || !mBoss.isValid()) {
										this.cancel();
									}

									if (mHits >= 4) {
										this.cancel();
										world.spawnParticle(Particle.SPELL_WITCH, mShrineMarker.getLocation().add(0, 3, 0), 25, 6, 5, 6, 1);
										world.spawnParticle(Particle.FLAME, mShrineMarker.getLocation().add(0, 3, 0), 40, 6, 5, 6, 0.1);
										mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 0.02);
										mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 9999, 0));
										changePhase(SpellManager.EMPTY, phase3PassiveSpells, null);
										world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 200, 0, 0, 0, 0.175);
										world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25);
										world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25);
										world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_GENERIC_EXPLODE, 5, 0.9f);
										world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_ENDER_DRAGON_GROWL, 5, 0f);

										new BukkitRunnable() {
											Location mLoc = mShrineMarker.getLocation().subtract(0, 0.5, 0);
											double mRotation = 0;
											double mRadius = 0;
											int mT = 0;

											@Override
											public void run() {
												mT++;
												mRadius = mT;
												for (int i = 0; i < 36; i++) {
													double radian1 = Math.toRadians(mRotation + (10 * i));
													mLoc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
													world.spawnParticle(Particle.FLAME, mLoc, 2, 0.25, 0.25, 0.25, 0.1);
													world.spawnParticle(Particle.BLOCK_DUST, mLoc, 2, 0.25, 0.25, 0.25, 0.25, Material.COARSE_DIRT.createBlockData());
													mLoc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
												}
												for (Block block : LocationUtils.getEdge(mLoc.clone().subtract(mT, 0, mT), mLoc.clone().add(mT, 0, mT))) {
													if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
														block.setType(Material.NETHERRACK);
														if (FastUtils.RANDOM.nextInt(3) == 1) {
															block.setType(Material.MAGMA_BLOCK);
														}
													} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
														block.setType(Material.SMOOTH_RED_SANDSTONE);
													}
												}
												if (mT >= 40) {
													this.cancel();
												}
											}

										}.runTaskTimer(mPlugin, 0, 1);
										new BukkitRunnable() {

											@Override
											public void run() {
												mBoss.setInvulnerable(false);
												mBoss.setAI(true);
												teleport(mSpawnLoc);
												mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
												new BukkitRunnable() {

													@Override
													public void run() {
														changePhase(phase3Spells, phase3PassiveSpells, null);
													}

												}.runTaskLater(mPlugin, 20 * 10);
											}

										}.runTaskLater(mPlugin, 20 * 3);
									}
								}

							}.runTaskTimer(mPlugin, 40, 1);
						}
					}
				}

			}.runTaskLater(mPlugin, 20 * 2);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"YOU ARE NOT ANTS, BUT PREDATORS. YET THE JUNGLE'S WILL IS MANIFEST; DEATH COMES TO ALL.\",\"color\":\"dark_green\"}]");
		});


		// Phase 3.25
		//Summons a Immortal Elemental at 30% HP
		events.put(30, mBoss -> {
			summonImmortal(plugin, world);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"PRIMORDIAL, RETURN, NOW AS UNDYING AND EVERLASTING AS THE MOUNTAIN.\",\"color\":\"dark_green\"}]");
		});


		//Force-cast Kaul's Judgement if it hasn't been casted yet.
		events.put(25, mBoss -> {
			forceCastSpell(SpellKaulsJudgement.class);
		});

		events.put(10, mBoss -> {
			changePhase(phase4Spells, phase4PassiveSpells, null);
			forceCastSpell(SpellVolcanicDemise.class);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE VALLEY RUNS RED WITH BLOOD TODAY. LET THIS BLASPHEMY END. PREDATORS, FACE THE FULL WILL OF THE JUNGLE. COME.\",\"color\":\"dark_green\"}]");
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange + 30, BarColor.RED, BarStyle.SEGMENTED_10, events);

		//Construct the boss with a delay to prevent the passives from going off during the dialogue
		new BukkitRunnable() {

			@Override
			public void run() {
				constructBoss(phase1Spells, passiveSpells, detectionRange, bossBar, 20 * 10);
			}

		}.runTaskLater(mPlugin, (20 * 10) + 1);
	}

	private void summonImmortal(Plugin plugin, World world) {
		new BukkitRunnable() {
			Location mLoc = mSpawnLoc;
			double mRotation = 0;
			double mRadius = 5;

			@Override
			public void run() {
				for (int i = 0; i < 5; i++) {
					double radian1 = Math.toRadians(mRotation + (72 * i));
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.SPELL_WITCH, mLoc, 3, 0.1, 0.1, 0.1, 0);
					world.spawnParticle(Particle.BLOCK_DUST, mLoc, 4, 0.2, 0.2, 0.2, 0.25,
					Material.COARSE_DIRT.createBlockData());
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
				}
				mRotation += 8;
				mRadius -= 0.25;
				if (mRadius <= 0) {
					this.cancel();
					world.playSound(mLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0);
					world.playSound(mLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
					world.spawnParticle(Particle.CRIT_MAGIC, mLoc, 150, 0.1, 0.1, 0.1, 1);
					LivingEntity miniboss = spawnImmortal(mLoc);
					if (miniboss != null) {
						EntityUtils.setAttributeBase(miniboss, Attribute.GENERIC_MOVEMENT_SPEED, EntityUtils.getAttributeBaseOrDefault(miniboss, Attribute.GENERIC_MOVEMENT_SPEED, 0) + 0.01);
						miniboss.setInvulnerable(true);
						miniboss.setCustomNameVisible(true);
						new BukkitRunnable() {

							@Override
							public void run() {

								if (mBoss.isDead() || !mBoss.isValid() || mDefeated) {
									this.cancel();
									if (!miniboss.isDead()) {
										miniboss.setHealth(0);
									}
								}
							}

						}.runTaskTimer(mPlugin, 0, 20);
					}
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.55f, false);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
		}
		new BukkitRunnable() {
			double mRotation = 0;
			Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mYminus = 0.35;

			@Override
			public void run() {

				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					mBoss.getWorld().spawnParticle(Particle.BLOCK_DUST, mLoc, 4, 0.2, 0.2, 0.2, 0.25,
					                               Material.COARSE_DIRT.createBlockData());
					world.spawnParticle(Particle.SMOKE_LARGE, mLoc, 3, 0.1, 0.1, 0.1, 0.1);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);

				}
				mY -= mY * mYminus;
				mYminus += 0.02;
				if (mYminus >= 1) {
					mYminus = 1;
				}
				if (mRadius >= r) {
					this.cancel();
				}

			}

		}.runTaskTimer(plugin, 0, 1);
	}

	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0f);
		world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0f);
		world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		/* Boss deals AoE damage when melee'ing a player */
		if (event.getType() == DamageType.MELEE && damagee.getLocation().distance(mBoss.getLocation()) <= 2) {
			if (!mCooldown) {
				mCooldown = true;
				new BukkitRunnable() {
					@Override
					public void run() {
						mCooldown = false;
					}
				}.runTaskLater(mPlugin, 20);
				UUID uuid = damagee.getUniqueId();
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4, true)) {
					if (!player.getUniqueId().equals(uuid)) {
						BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, event.getDamage());
					}
				}
				World world = mBoss.getWorld();
				world.spawnParticle(Particle.DAMAGE_INDICATOR, mBoss.getLocation(), 30, 2, 2, 2, 0.1);
				world.spawnParticle(Particle.SWEEP_ATTACK, mBoss.getLocation(), 10, 2, 2, 2, 0.1);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0);
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(event.getDamage() / mDefenseScaling);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mPlayerCount = getArenaParticipants().size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
	}

	private @Nullable LivingEntity spawnPrimordial(Location loc) {
		LivingEntity entity = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, primordial);
		return entity;
	}

	private @Nullable LivingEntity spawnImmortal(Location loc) {
		LivingEntity entity = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, immortal);
		return entity;
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		Spell spell = event.getSpell();
		if (spell != null && spell.castTicks() > 0) {
			mBoss.setInvulnerable(true);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));
			mBoss.setAI(false);
			new BukkitRunnable() {

				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					new BukkitRunnable() {

						@Override
						public void run() {
							// If the Primordial Elemental is active, don't allow other abilities to turn Kaul's AI back on
							if (!mPrimordialPhase) {
								mBoss.setInvulnerable(false);
								mBoss.setAI(true);
								mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
								teleport(mSpawnLoc);
								List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
								if (players.size() > 0) {
									Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
									((Mob) mBoss).setTarget(newTarget);
								}
							}
						}

					}.runTaskLater(mPlugin, spell.castTicks());
				}

			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() <= 0) {
			return;
		}
		String[] dio = new String[] {
			"AS ALL RETURNS TO ROT, SO TOO HAS THIS ECHO FALLEN.",
			"DO NOT THINK THIS ABSOLVES YOUR BLASPHEMY. RETURN HERE AGAIN, AND YOU WILL PERISH.",
			"NOW... THE JUNGLE... MUST SLEEP..."
		};
		mDefeated = true;
		knockback(mPlugin, 10);

		for (Player player : players) {
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 40, 10));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
		}
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		World world = mBoss.getWorld();
		mBoss.removePotionEffect(PotionEffectType.GLOWING);
		for (Entity ent : mSpawnLoc.getNearbyLivingEntities(detectionRange)) {
			if (!ent.getUniqueId().equals(mBoss.getUniqueId()) && ent instanceof WitherSkeleton && !ent.isDead()) {
				ent.remove();
			}
		}
		new BukkitRunnable() {
			final Location mLoc = mShrineMarker.getLocation().subtract(0, 0.5, 0);
			final double mRotation = 0;
			double mRadius = 0;
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mRadius = mT;
				for (int i = 0; i < 36; i++) {
					double radian1 = Math.toRadians(mRotation + (10 * i));
					mLoc.add(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.CLOUD, mLoc, 3, 0.25, 0.25, 0.25, 0.025, null, true);
					world.spawnParticle(Particle.VILLAGER_HAPPY, mLoc, 5, 0.4, 0.25, 0.4, 0.25, null, true);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 1, FastUtils.sin(radian1) * mRadius);
				}
				for (Block block : LocationUtils.getEdge(mLoc.clone().subtract(mT, 0, mT), mLoc.clone().add(mT, 0, mT))) {
					if (block.getType() == Material.MAGMA_BLOCK) {
						block.setType(Material.OAK_LEAVES);
						if (FastUtils.RANDOM.nextInt(5) == 1) {
							block.setType(Material.GLOWSTONE);
						}
					} else if (block.getType() == Material.SMOOTH_RED_SANDSTONE || block.getType() == Material.NETHERRACK) {
						block.setType(Material.GRASS_BLOCK);
						if (FastUtils.RANDOM.nextInt(3) == 1) {
							Block b = block.getLocation().add(0, 1.5, 0).getBlock();
							if (!b.getType().isSolid()) {
								b.setType(Material.GRASS);
							}
						}
					}
				}
				if (mT >= 40) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio[mT].toUpperCase() + "\",\"color\":\"dark_green\"}]");
				mT++;
				if (mT == dio.length) {
					this.cancel();
					teleport(mSpawnLoc);
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							if (mT <= 0) {
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
							}
							mT++;
							if (mT <= 60) {
								mBoss.teleport(mBoss.getLocation().subtract(0, 0.05, 0));
								mBoss.getWorld().spawnParticle(Particle.BLOCK_DUST, mSpawnLoc, 7, 0.3, 0.1, 0.3, 0.25,
								                               Material.COARSE_DIRT.createBlockData());
							} else {
								mBoss.getEquipment().clear();
								mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 27, 0));
								mBoss.setAI(false);
								mBoss.setSilent(true);
								mBoss.setInvulnerable(true);
								if (mT >= 100) {
									this.cancel();
									PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 100 0.8");
									PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"VICTORY\",\"color\":\"green\",\"bold\":true}]");
									PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Kaul, Soul of the Jungle\",\"color\":\"dark_green\",\"bold\":true}]");
									mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
									mBoss.remove();
								}
							}
						}

					}.runTaskTimer(mPlugin, 30, 1);
				}
			}
		}.runTaskTimer(mPlugin, 0, 20 * 6);
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, MAX_HEALTH);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(MAX_HEALTH);

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
				player.removePotionEffect(PotionEffectType.GLOWING);
			}
		}

		EntityEquipment equips = mBoss.getEquipment();
		ItemStack[] armorc = equips.getArmorContents();
		ItemStack m = equips.getItemInMainHand();
		ItemStack o = equips.getItemInOffHand();
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.getEquipment().clear();
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 100, 0));
				mBoss.setAI(false);
				mBoss.setSilent(true);
				mBoss.setInvulnerable(true);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 100, 10));
				World world = mBoss.getWorld();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 0f);
				world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
				world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
				String[] dio = new String[] {
					"THE JUNGLE'S WILL IS UNASSAILABLE, YET YOU SCURRY ACROSS MY SHRINE LIKE ANTS.",
					"IS THE DEFILEMENT OF THE DREAM NOT ENOUGH!?"
				};

				new BukkitRunnable() {
					int mT = 0;
					int mIndex = 0;

					@Override
					public void run() {
						if (mT == 0) {
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3, 0f);
						}

						if (mT % (20 * 4) == 0) {
							if (mIndex < dio.length) {
								PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio[mIndex].toUpperCase() + "\",\"color\":\"dark_green\"}]");
								mIndex++;
							}
						}
						mT++;

						if (mT >= (20 * 8)) {
							this.cancel();
							mBoss.setAI(true);
							mBoss.setSilent(false);
							mBoss.setInvulnerable(false);
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color white");
							mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
							mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
							mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 99999, 0));
							mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 9999, 0));
							world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 0f);
							world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
							world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
							mBoss.getEquipment().setArmorContents(armorc);
							mBoss.getEquipment().setItemInMainHand(m);
							mBoss.getEquipment().setItemInOffHand(o);

							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Kaul\",\"color\":\"dark_green\",\"bold\":true}]");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Soul of the Jungle\",\"color\":\"green\",\"bold\":true}]");
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 5, 0f);
						}

					}

				}.runTaskTimer(mPlugin, 40, 1);
			}

		}.runTaskLater(mPlugin, 1);
	}

	public Collection<Player> getArenaParticipants() {
		if (mShrineMarker == null) {
			return Collections.emptyList();
		}

		Location arenaCenter = mShrineMarker.getLocation();
		arenaCenter.setY(ARENA_MAX_Y / 2d);

		return PlayerUtils.playersInBox(arenaCenter, ARENA_WIDTH, ARENA_MAX_Y);
	}

	public LivingEntity getBoss() {
		return mBoss;
	}
}

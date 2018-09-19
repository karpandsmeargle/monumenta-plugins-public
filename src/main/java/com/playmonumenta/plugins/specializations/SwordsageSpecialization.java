package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class SwordsageSpecialization extends BaseSpecialization {
	private World mWorld;

	public SwordsageSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	public static final String BLADE_DANCE_ACTIVE_METAKEY = "BladeDanceActive";
	private static final Particle.DustOptions BLADE_SURGE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	@Override
	public void EntityDeathEvent(Player player, EntityDeathEvent event) {
		int snakeHead = ScoreboardUtils.getScoreboardValue(player, "SnakeHead");
		LivingEntity entity = event.getEntity();

		/*
		 * Snake's Head: When you kill an elite mob, all non-elite mobs
		 * within 12 blocks get Slowness 1 for 8s and take 4 damage.
		 * At level 2, they instead take 8 damage and also Weakness I
		 * for 8s.
		 */
		if (snakeHead > 0) {
			if (EntityUtils.isElite(entity)) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.SNAKE_HEAD)) {

					int damage = snakeHead == 1 ? 4 : 8;
					for (Entity e : entity.getNearbyEntities(12, 12, 12)) {
						if (EntityUtils.isHostileMob(e)) {
							LivingEntity le = (LivingEntity) e;
							EntityUtils.damageEntity(mPlugin, le, damage, player);
							le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 8, 0, true, false));
							if (snakeHead > 1) {
								le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 8, 0, true, false));
							}
						}
					}

					mWorld.spawnParticle(Particle.CRIT, entity.getLocation(), 2000, 12, 12, 12, 0.45);
					mWorld.spawnParticle(Particle.TOTEM, entity.getLocation(), 2000, 12, 12, 12, 0.1);
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PARROT_IMITATE_CREEPER, 1.25f, 1);
					entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 1);

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SNAKE_HEAD, 10 * 20);
				}
			}
		}
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {

			if (InventoryUtils.isSwordItem(itemInHand)) {
				if (player.isSprinting()) {
					int bladeSurge = ScoreboardUtils.getScoreboardValue(player, "BladeSurge");
					/*
					 * Blade Surge: When Sprinting then Right Clicking, the user slashes
					 * forth, dealing 12 damage to enemies in front of them, and knocking
					 * them away, while casting out a fast moving projectile that deals
					 * 8 damage, and pierces through targets. (Cooldown: 18s)
					 */
					if (bladeSurge > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BLADE_SURGE)) {

							int damage = bladeSurge == 1 ? 10 : 14;

							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.2f);
							mWorld.spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.2, 0.25, 0.2, 0.15);

							new BukkitRunnable() {
								Location loc = player.getEyeLocation();
								Vector dir = loc.getDirection();
								int i = 0;
								@Override
								public void run() {
									i++;
									loc.add(dir.clone().multiply(1.4));
									player.getLocation().getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 2);
									mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
									mWorld.spawnParticle(Particle.CLOUD, loc, 3, 0.05, 0.05, 0.05, 0.03);
									player.getWorld().spawnParticle(Particle.REDSTONE, loc, 50, 0.25, 0.25, 0.25, 1, BLADE_SURGE_COLOR);

									for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.65, 0.65, 0.65)) {
										if (EntityUtils.isHostileMob(e)) {
											LivingEntity le = (LivingEntity) e;
											EntityUtils.damageEntity(mPlugin, le, damage, player);
										}
									}

									if (loc.getBlock().getType().isSolid()) {
										this.cancel();
										mWorld.spawnParticle(Particle.CLOUD, loc, 15, 0.05, 0.05, 0.05, 0.2);
									}

									if (i >= 14) {
										this.cancel();
										mWorld.spawnParticle(Particle.CLOUD, loc, 15, 0.05, 0.05, 0.05, 0.2);
									}

								}

							}.runTaskTimer(mPlugin, 0, 1);

							//If the level of the ability is 2 or greater, sweep and knockback in front

							if (bladeSurge > 1) {
								Location loc = player.getEyeLocation();
								loc.add(loc.getDirection());
								mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 10, 1.5, 0.75, 1.5, 1);

								for (Entity e : loc.getWorld().getNearbyEntities(loc, 2, 1, 2)) {
									if (EntityUtils.isHostileMob(e)) {
										LivingEntity le = (LivingEntity) e;
										EntityUtils.damageEntity(mPlugin, le, 8, player);
										MovementUtils.KnockAway(player, le, 1.1f);
										mWorld.spawnParticle(Particle.CLOUD, e.getLocation(), 15, 0, 0, 0, 0.25);
										mWorld.spawnParticle(Particle.SWEEP_ATTACK, e.getLocation(), 1, 0, 0, 0, 1);
									}
								}
							}

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BLADE_SURGE, 12 * 20);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(BLADE_DANCE_ACTIVE_METAKEY)) {
			double damage = player.getMetadata(BLADE_DANCE_ACTIVE_METAKEY).get(0).asDouble();
			damage += event.getFinalDamage();
			player.setMetadata(BLADE_DANCE_ACTIVE_METAKEY, new FixedMetadataValue(mPlugin, damage));
			double newDealt = event.getDamage() * 0.7;
			event.setDamage(newDealt);
		}
		if (InventoryUtils.isSwordItem(player.getInventory().getItemInMainHand())) {
			if (player.isSneaking() && (player.getHealth() - event.getFinalDamage() > 0)) {
				/*
				 * Blade Dance: Taking damage while sneaking begins a blade dance.
				 * Over the next 3 seconds, any damage you take is reduced by 30%.
				 * At the end of the dance, the damage taken over the duration +5
				 * is shot back in an energy burst facing your direction. At level 2,
				 * the damage burst is 10+ damage
				 * taken. The burst does double damage to elites. (Cooldown: 30s)
				 */
				int bladeDance = ScoreboardUtils.getScoreboardValue(player, "BladeDance");
				if (bladeDance > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BLADE_DANCE)) {
						player.setMetadata(BLADE_DANCE_ACTIVE_METAKEY, new FixedMetadataValue(mPlugin, 0D));
						double extraDamage = bladeDance == 1 ? 5 : 10;
						player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.25f, 2f);
						mWorld.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 250, 6, 6, 6, 0);
						new BukkitRunnable() {
							int i = 0;
							float pitch = 0;
							@Override
							public void run() {
								i += 2;
								player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, pitch);
								pitch += 0.1;
								new BukkitRunnable() {
									Location loc1 = player.getLocation().add(6, 6, 6);
									Location loc2 = player.getLocation().add(-6, -1, -6);

									double x1 = ThreadLocalRandom.current().nextDouble(loc2.getX(), loc1.getX());
									double y1 = ThreadLocalRandom.current().nextDouble(loc2.getY(), loc1.getY());
									double z1 = ThreadLocalRandom.current().nextDouble(loc2.getZ(), loc1.getZ());
									Location l1 = new Location(player.getWorld(), x1, y1, z1);

									double x2 = ThreadLocalRandom.current().nextDouble(loc2.getX(), loc1.getX());
									double y2 = ThreadLocalRandom.current().nextDouble(loc2.getY(), loc1.getY());
									double z2 = ThreadLocalRandom.current().nextDouble(loc2.getZ(), loc1.getZ());
									Location l2 = new Location(player.getWorld(), x2, y2, z2);

									Vector dir = LocationUtils.getDirectionTo(l2, l1);

									int t = 0;
									@Override
									public void run() {
										t++;
										l1.add(dir.clone().multiply(1.15));
										mWorld.spawnParticle(Particle.CRIT_MAGIC, l1, 4, 0, 0, 0, 0.35);
										mWorld.spawnParticle(Particle.CLOUD, l1, 1, 0, 0, 0, 0);
										mWorld.spawnParticle(Particle.SWEEP_ATTACK, l1, 1, 0, 0, 0, 0);
										if (t >= 10) {
											this.cancel();
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);

								if (i >= 20 * 3 || player.isDead()) {
									this.cancel();
									ParticleUtils.explodingConeEffect(mPlugin, player, 6, Particle.EXPLOSION_NORMAL, 0.75f, Particle.SWEEP_ATTACK, 0.25f, 0.33);
									Location loc = player.getLocation();
									loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.25f, 1.1f);
									loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1.25f, 2f);
									int minimum = -6; // Easier changing the minimum
									int maximum = 6; // Easier changing the maximum

									for (int i = 0; i < 75; i++) {
										double x = ThreadLocalRandom.current().nextDouble(minimum, maximum);
										double y = ThreadLocalRandom.current().nextDouble(0, maximum);
										double z = ThreadLocalRandom.current().nextDouble(minimum, maximum);
										Location l = new Location(loc.getWorld(), x, y, z);
										Location locclone = loc.clone();
										locclone.add(l);
										Vector dir = player.getLocation().getDirection();
										double dx = dir.getX();
										double dy = dir.getY();
										double dz = dir.getZ();
										mWorld.spawnParticle(Particle.FLAME, locclone, 0, (float)dx, (float)dy, (float)dz, 0.5);
									}

									for (int i = 0; i < 20; i++) {
										double x = ThreadLocalRandom.current().nextDouble(minimum, maximum);
										double y = ThreadLocalRandom.current().nextDouble(0, maximum);
										double z = ThreadLocalRandom.current().nextDouble(minimum, maximum);
										Location l = new Location(loc.getWorld(), x, y, z);
										Location locclone = loc.clone();
										locclone.add(l);
										Vector dir = player.getLocation().getDirection();
										double dx = dir.getX();
										double dy = dir.getY();
										double dz = dir.getZ();
										mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, locclone, 0, (float)dx, (float)dy, (float)dz, 0.5);
									}

									for (Entity e : player.getNearbyEntities(6, 6, 6)) {
										if (EntityUtils.isHostileMob(e)) {
											LivingEntity mob = (LivingEntity) e;
											Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
											if (player.getLocation().getDirection().dot(toMobVector) > 0.33) {
												double damage = player.getMetadata(BLADE_DANCE_ACTIVE_METAKEY).get(0).asDouble() + extraDamage;
												if (EntityUtils.isElite(e)) {
													damage *= 2;
												}
												EntityUtils.damageEntity(mPlugin, mob, damage, player);
											}
										}
									}

									player.removeMetadata(BLADE_DANCE_ACTIVE_METAKEY, mPlugin);
								}

							}

						}.runTaskTimer(mPlugin, 0, 2);

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BLADE_DANCE, 30 * 20);
					}
				}
			}
		}
		return true;
	}

}

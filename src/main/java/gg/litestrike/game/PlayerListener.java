package gg.litestrike.game;

import gg.crystalized.lobby.App;

import gg.crystalized.lobby.Ranks;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import gg.litestrike.game.GameController.RoundState;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.projectiles.ProjectileSource;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

import java.util.List;

public class PlayerListener implements Listener {
	private LSChatRenderer chat_renderer = new LSChatRenderer();

	private static final int ANTI_HEAL_DURATION = 5 * 20;
	private static final int SUPPORTIVE_AREA_DURATION = 10 * 15;
	private static final int SUPPORTIVE_HEAL_COOLDOWN = 10 * 20;


	@EventHandler
	public void onPlayerLogin(PlayerConnectionValidateLoginEvent event) {
		if (Bukkit.getOnlinePlayers().size() > Litestrike.getInstance().gameConfig.playerCap) {
			event.kickMessage(text("The server is full.\n"));
		}
	}

	@EventHandler
	public void onPlayerJump(PlayerJumpEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			return;
		}
		gc.playerDataManager.get(e.getPlayer()).jumps += 1;
	}

	@EventHandler
	public void onPLayerQuit(PlayerQuitEvent e) {
		e.getPlayer().getPersistentDataContainer().remove(LSItem.NEGATIVE_EFFECT_IMMUNITY);
		e.quitMessage(text(""));
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.teams.get_team(e.getPlayer()) != Team.Placer) {
			return;
		}
		gc.playerDataManager.get(e.getPlayer()).did_leave = true;
		if (gc.bomb != null && gc.bomb instanceof InvItemBomb) {
			InvItemBomb bomb = (InvItemBomb) gc.bomb;
			if (bomb.player.equals(e.getPlayer())) {
				Item i = Bukkit.getWorld("world").dropItem(e.getPlayer().getLocation(), Bomb.bomb_item());
				bomb.drop_bomb(i);
			}
		}
	}

	@EventHandler
	public void onArmorStand(PlayerArmorStandManipulateEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.joinMessage(text(""));
		event.getPlayer().getPersistentDataContainer().remove(LSItem.NEGATIVE_EFFECT_IMMUNITY);
		Player p = event.getPlayer();
		GameController gc = Litestrike.getInstance().game_controller;

		p.teleport(Litestrike.getInstance().mapdata.get_queue_spawn(p.getWorld()));
		p.getInventory().clear();
		try {
			// InventoryManager.giveLobbyItems(p); //why - Callum
			Ranks.passiveNames(p, WHITE, null, null);
			p.playerListName(Ranks.getName(p));
			p.getInventory().setItem(App.BackToHub.slot, App.BackToHub.build());
		} catch (NoClassDefFoundError e) {
		}
		p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
		p.setFoodLevel(20);
		p.lookAt(Litestrike.getInstance().mapdata.get_placer_spawn(p.getWorld()), LookAnchor.EYES);
		p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1, false, false, true));

		if (gc == null) {
			p.setGameMode(GameMode.SURVIVAL);
			QueueSystem.qsb.show_queue_scoreboard(p);
			//No game is going on so removes any potential teams
			p.getPersistentDataContainer().remove(Teams.TEAM_KEY);

		} else {
			// if we are here, it means the player is rejoining
			p.setGameMode(GameMode.SPECTATOR);
			Shop s = gc.getShop(p);

			if (s != null) {
				s.player = p.getName();
			}

			// give player the scoreboard and bossbar again
			ScoreboardController.give_player_scoreboard(p, gc.teams, gc.game_reference);
			Litestrike.getInstance().bbd.showBossBar();
			//if the player rejoined updates the teams acordingly
			gc.teams.updateTeamPDCindividual(p);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onHangingBreak(HangingBreakByEntityEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getClickedBlock() instanceof Hanging) {
			e.setCancelled(true);
			return;
		}
		GameController gc = Litestrike.getInstance().game_controller;
		Player player = e.getPlayer();
		//Makes sure that potions and apples are not ussable while player is anti healed
		//Don't remove: Prevents pottion consumption.
		if (gc != null) {
			PlayerData pd = gc.playerDataManager.get(player);
			if (pd != null && pd.antiHealTicks > 0 && e.getItem() != null && (e.getItem().getType() == Material.GOLDEN_APPLE
					|| e.getItem().getType() == Material.POTION)) {

				e.setCancelled(true);
				return;
			}
		}

		if (e.getItem() != null && e.getItem().getType() == Material.POTION
				&& Litestrike.getInstance().game_controller.round_state == GameController.RoundState.PreRound) {
			e.setCancelled(true);
			return;
		}

		if (e.getItem() != null && e.getItem().getType() == Material.POTION) {
			PotionMeta pm = (PotionMeta) e.getItem().getItemMeta();
			e.getPlayer().addPotionEffects(pm.getCustomEffects());
			SoundEffects.potion_drink(e.getPlayer().getLocation());
			if (e.getHand() == EquipmentSlot.HAND) {
				e.getPlayer().getInventory().setItemInMainHand(null);
			} else {
				e.getPlayer().getInventory().setItemInOffHand(null);
			}
		}

		// prevent stripping with axes
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getMaterial().name().contains("AXE")) {
			String clicked_name = e.getClickedBlock().getType().name();
			if (clicked_name.contains("LOG") || clicked_name.contains("COPPER")) {
				e.setCancelled(true);
			}
		}

	}

	@EventHandler
	public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Hanging) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onChatEvent(AsyncChatEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.round_state == RoundState.GameFinished) {
			return;
		}

		String msg_text = PlainTextComponentSerializer.plainText().serialize(e.message());
		if (!msg_text.startsWith("@a") && !msg_text.startsWith("@A")) {
			List<Player> enemy_team = gc.teams.get_enemy_team_of(e.getPlayer());
			if (enemy_team == null) {
				// if enemy team is null, it means we got a spectator message, so all teamed
				// players are removed
				e.viewers().removeAll(gc.teams.get_all_players());
			} else {
				e.viewers().removeAll(enemy_team);
			}
		}

		e.renderer(ChatRenderer.viewerUnaware(chat_renderer));
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.round_state != RoundState.Running) {
			e.setCancelled(true);
			return;
		}
		if (e.getEntity() instanceof Player player) {
			PlayerData pd = gc.playerDataManager.get(player);
			if (pd == null) return;

			//Wither is used purely for visuals when anti healed
			if (e.getCause() == DamageCause.WITHER) {
				if (pd.antiHealTicks > 0) {
					e.setCancelled(true);
					return;
				}
			}

			//Posion is also only used for the visuals to show when you are on healing cooldown from healing arrows
			//It is like if you heal more you will overdoes so healing stops lmao.
			if (e.getCause() == DamageCause.POISON) {
				if (pd.supportiveHealCooldownTicks > 0) {
					e.setCancelled(true);
					return;
				}
			}
		}

		// healing arrows deal no damage, they only heal allies via proximity
		if (e instanceof EntityDamageByEntityEvent ebe
				&& ebe.getDamager() instanceof Arrow arrow
				&& arrow.getCustomEffects().stream().anyMatch(effect -> effect.getType() == PotionEffectType.REGENERATION)) {
			e.setDamage(0.0);
			return;
		}

		// reduce explosion damage
		if (e.getCause() == DamageCause.ENTITY_EXPLOSION) {
			e.setDamage(e.getDamage() / 3);
		}

		if (e.getEntity() instanceof Hanging) {
			e.setCancelled(true);
			return;
		}

		Entity source = e.getDamageSource().getCausingEntity();
		if (e.getEntity() instanceof ArmorStand) {
			e.setCancelled(true);
			return;
		}

		if (!(source instanceof Player) || !(e.getEntity() instanceof Player)) {
			return;
		}
		Team attacker_team = gc.teams.get_team(source.getUniqueId());
		Team attacked_team = gc.teams.get_team(e.getEntity().getUniqueId());
		if (attacker_team == null || attacked_team == null || attacked_team == attacker_team) {
			e.setCancelled(true);
			return;
		}

		//Supportive arrow direct hit applies anti heal to the enemies
		if (e instanceof EntityDamageByEntityEvent ebe && ebe.getDamager() instanceof Arrow arrow) {
			ItemStack arrowItem = arrow.getItemStack();
			if (arrowItem.hasItemMeta() && arrowItem.getItemMeta().hasItemModel() && arrowItem.getItemMeta().getItemModel().equals(
					new NamespacedKey("crystalized", "supportive_arrow"))) {
				Player target = (Player) e.getEntity();
				PlayerData targetData = gc.playerDataManager.get(target);
				if (targetData != null) {
					targetData.antiHealTicks = ANTI_HEAL_DURATION;
					//Moved so it prevents the visual glitch and doesn't overwrite the bomb usage, but will clean heal arrow
					if (!Litestrike.getInstance().bombListener.isUsingBomb(target)) {
						target.sendActionBar(Component.text(("")));
					}
					targetData.supportiveHealCooldownTicks = 0;
					//Anti heal resets the healing cool down, as it totaly overwrites it and removes the posion cool down indicator
					//sets it to nothing. To reset on anti heal
					//target.removePotionEffect(PotionEffectType.POISON);
					target.setCooldown(Material.GOLDEN_APPLE, ANTI_HEAL_DURATION);
					target.setCooldown(Material.POTION, ANTI_HEAL_DURATION);
					//Wither is just used to display black hearts
					target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ANTI_HEAL_DURATION, 0, false, true, true));
					target.playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.6F, 1.3F);
				}
			}
		}

		PlayerData pd = Litestrike.getInstance().game_controller.playerDataManager.get((Player) source);
		double health = ((Player) e.getEntity()).getHealth();
		double absorption_damage_done = -e.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
		if (health - e.getFinalDamage() <= 0) {
			pd.total_damage += health + absorption_damage_done;
		} else {
			pd.total_damage += e.getFinalDamage() + absorption_damage_done;
		}
	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getSlotType() == InventoryType.SlotType.CRAFTING) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		//will not run if it didn't hit a block
		//In the future we want we can make so the locator arrow works even if it hit the player by removing this
		if (event.getHitBlock() == null)
			return;

		ProjectileSource shootingEntity = event.getEntity().getShooter();
		if (shootingEntity == null)
			return;

		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null)
			return;
		if (!(event.getEntity() instanceof SpectralArrow locatingArrow)) {
			return;
		}
		// only locating arrows (PDC-marked shop item, see LSItem) run the tracer
		// scan; normal spectral arrows keep vanilla glow behavior
		ItemStack arrowItem = locatingArrow.getItemStack();
		if (arrowItem == null || !arrowItem.hasItemMeta()) {
			return;
		}
		Integer locatingMarker = arrowItem.getItemMeta().getPersistentDataContainer().get(LSItem.LOCATING_ARROW_KEY, PersistentDataType.INTEGER);
		if (!Integer.valueOf(1).equals(locatingMarker)) {
			return;
		}
		if (!(shootingEntity instanceof Player shooter)) {
			return;
		}
		Location locatingArrowsLocation = locatingArrow.getLocation().clone();
		if (event.getHitBlockFace() != null) {
			//moves slightly out, so ray traces dont insta hit a block
			locatingArrowsLocation.add(event.getHitBlockFace().getDirection().multiply(0.15));
		}
		Team shooterTeam = gc.teams.get_team(shooter);
		if (shooterTeam == null) {
			return;
		}

		//The repeating task for scaning and locating the enemies
		new BukkitRunnable() {
			//will repeat 3 times
			int repeats = 0;
			@Override
			public void run() {
				if (repeats >= 3 || !locatingArrow.isValid()) {
					locatingArrow.remove();
					cancel();
					return;
				}
				double scanRadius = 20.0;
				for (Player enemy : locatingArrowsLocation.getNearbyPlayers(scanRadius)) {
					Team enemysTeam = gc.teams.get_team(enemy);
					if (enemysTeam == null || enemysTeam == shooterTeam) {
						continue;
					}
					Location enemyLocation = enemy.getEyeLocation();
					Vector direction = enemyLocation.toVector().subtract(locatingArrowsLocation.toVector());
					double distance = direction.length();
					if (distance <= 0.0) {
						continue;
					}
					//Does a ray trace, to enssure that the enemy is not behind a wall
					//direction is being normalizied to keep only direction, though it is not nesseray for this method it is safer
					RayTraceResult blocked = locatingArrow.getWorld().rayTraceBlocks(locatingArrowsLocation, direction.normalize(), distance);
					if (blocked != null) {
						continue;
					}
					enemy.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, true));

					/*
					* Particle locator logic:
					* Shoots out a particle path from the arrow in the direction of the player, thought of making kinda scan area particles,
					* for perfromanse and practicaly prefered this version a lot better.
					* It guides the players attention towards the enemy rather than distracting with cool scaning effect
					* */

					//This adds a little height so it doesn't point to their feet, but not the head either as it would be annoying for vision
					Location enemyParticleLocation = enemy.getLocation().clone().add(0, 1.0, 0);
					Vector particlesToEnmeyPath = enemyParticleLocation.toVector().subtract(locatingArrowsLocation.toVector());
					double distanseToEnemy = particlesToEnmeyPath.length();
					double particle_spacing = 0.6;

					//This step will be added each time in the loop to particle location as it creates a 0.6 block step in the direction the enemy
					//How it works is it takes the particlesToEnemy path normalizing it keeping direction,meaning it would be lenght 1 ,
					//so that would be 1 block.Then multiplies by spacing to make it 0.6 blocks, to make particles look closer together
					//That is more of a comment for myself cause later I might forget lol.
					Vector step = particlesToEnmeyPath.normalize().multiply(particle_spacing);
					Location particleLocation = locatingArrowsLocation.clone();
					Particle.DustOptions options = new Particle.DustOptions(Color.YELLOW, 1.0F);
					for (double travelled = 0; travelled < distanseToEnemy; travelled += particle_spacing) {
						particleLocation.add(step);
						locatingArrowsLocation.getWorld().spawnParticle(Particle.DUST, particleLocation,
								1,
								0.0,
								0.0,
								0.0,
								0.0,
								options
						);
					}
				}
				repeats++;
			}
		}.runTaskTimer(Litestrike.getInstance(), 0L, 20L);
	}

	@EventHandler
	public void onSupportiveArrowHit(ProjectileHitEvent event) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) return;
		if (event.getHitBlock() == null) return;
		if (!(event.getEntity() instanceof Arrow arrow)) return;

		ItemStack arrowItem = arrow.getItemStack();
		if (!arrowItem.hasItemMeta() || !arrowItem.getItemMeta().hasItemModel() || !arrowItem.getItemMeta().getItemModel().equals(
				new NamespacedKey("crystalized", "supportive_arrow"))) {
			return;
		}
		Location supportiveLocation = arrow.getLocation().clone();
		if (event.getHitBlockFace() != null) {
			supportiveLocation.add(event.getHitBlockFace().getDirection().multiply(0.15));
		}
		if (!(arrow.getShooter() instanceof Player shooter)) return;

		Team shooterTeam = gc.teams.get_team(shooter);
		if (shooterTeam == null) return;

		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED); //Just changes

		GameController.SupportiveCircle supportiveCircle = new GameController.SupportiveCircle(supportiveLocation, shooterTeam);
		gc.supportiveCircles.add(supportiveCircle);
		supportiveLocation.getWorld().playSound(supportiveLocation, Sound.BLOCK_BEACON_ACTIVATE, 0.6F, 1.4F);

		new BukkitRunnable() {
			int repeats = 0;
			@Override
			public void run() {
				if (repeats >= 10 || !arrow.isValid()) {
					gc.supportiveCircles.remove(supportiveCircle);
					arrow.remove();
					cancel();
					return;
				}
				//Same as dragon breath three ring particles
				double[] ringRadius = {1.0, 1.5, 2.0};
				int particlePoints = 20;
				//The same logic as in dragon breath.
				for (double radius : ringRadius) {
					for (int i = 0; i < particlePoints; i++) {
						double angle = (Math.PI * 2.0 * i) / particlePoints;
						double x = Math.cos(angle) * radius;
						double z = Math.sin(angle) * radius;
						Location particleLocation = supportiveLocation.clone().add(x, 0.15, z);
						//supportiveLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0.0, 0.0, 0.0, 0.0, supportiveParticle);
						//This was added to so that players can see diffrent particles, no package events needed
						//goes through the players in the world which will view the particles
						for (Player viewer : supportiveLocation.getWorld().getPlayers()) {
							//gets the viewers teamer
							Team viewerTeam = gc.teams.get_team(viewer);
							//this determines how the arrow should look like based on the shooters team, viewers team and the size
							//specattorers also handled
							Particle.DustOptions particle = LSItem.getSupportiveParticle(shooterTeam, viewerTeam, 1.0F);
							//this spawns the particles for the individual players with the correct color
							viewer.spawnParticle(Particle.DUST, particleLocation, 1, 0.0, 0.0, 0.0, 0.0, particle);
						}
					}
				}

				for (Player player : supportiveLocation.getNearbyPlayers(2.0)) {
					Team playerTeam = gc.teams.get_team(player);
					if (playerTeam == null || playerTeam != shooterTeam)continue;
					PlayerData pd = gc.playerDataManager.get(player);
					if (pd == null) continue;

					//Anti heal cancels the supportive area pretty much
					if (pd.antiHealTicks > 0) {
						continue;
					}

					if (pd.supportiveHealCooldownTicks <= 0) {
						// TODO document less heal for shooter in item description
						if(player.equals(shooter)){
							player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 0, false, false, true));
						}
						else {
							player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false, false, true));
						}
						player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.2F);
						player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.0, 0), 4, 0.35, 0.5, 0.35, 0.0);
						//This is similiar logic for when you get healed the particles for individual players looks diffrent depended on the team
						Location healingParticleLocation = player.getLocation().add(0, 1.0, 0);
						for (Player viewer : player.getWorld().getPlayers()) {
							Team viewerTeam = gc.teams.get_team(viewer);
							Particle.DustOptions particle = LSItem.getSupportiveParticle(shooterTeam, viewerTeam, 1.0F);
							//Healing burst of particles
							viewer.spawnParticle(Particle.DUST, healingParticleLocation, 8, 0.4, 0.5, 0.4, 0.0, particle);
						}
						//player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.0, 0), 8, 0.4, 0.5, 0.4, 0.0, supportiveParticle);
						pd.supportiveHealCooldownTicks = SUPPORTIVE_HEAL_COOLDOWN;
					}
				}
				repeats++;
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 15);
	}

	private static boolean isHealingArrow(ItemStack arrowItem) {
		if (arrowItem == null || arrowItem.getType() != Material.TIPPED_ARROW
				|| !(arrowItem.getItemMeta() instanceof PotionMeta potionMeta)
				|| potionMeta.hasItemModel()) {
			return false;
		}
		return potionMeta.getCustomEffects().stream().anyMatch(effect -> effect.getType() == PotionEffectType.REGENERATION);
	}

	@EventHandler
	public void onPotionEffect(EntityPotionEffectEvent event) {
		if (event.getCause() != EntityPotionEffectEvent.Cause.ARROW
				|| !(event.getEntity() instanceof Player target)
				|| !(event.getSource() instanceof Arrow arrow)) {
			return;
		}
		boolean healing = arrow.getCustomEffects().stream().anyMatch(effect -> effect.getType() == PotionEffectType.REGENERATION);
		if (!healing) {
			return;
		}
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			return;
		}
		if (!(arrow.getShooter() instanceof Player shooter) || shooter.equals(target)) {
			event.setCancelled(true);
			return;
		}
		Team shooterTeam = gc.teams.get_team(shooter);
		Team targetTeam = gc.teams.get_team(target);
		if (shooterTeam == null || targetTeam == null || shooterTeam != targetTeam) {
			event.setCancelled(true);
		}
	}

	private static void trackHealingArrow(Arrow arrow, Player shooter) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			return;
		}
		Team shooterTeam = gc.teams.get_team(shooter);
		if (shooterTeam == null) {
			return;
		}
		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if (!arrow.isValid() || ticks++ >= 300) {
					cancel();
					return;
				}
				boolean healed = false;
				for (Player ally : arrow.getLocation().getNearbyPlayers(2.0)) {
					if (ally.equals(shooter) || shooterTeam != gc.teams.get_team(ally)) {
						continue;
					}
					arrow.getCustomEffects().stream().filter(effect -> effect.getType() == PotionEffectType.REGENERATION)
							.forEach(ally::addPotionEffect);
					healed = true;
				}
				if (!healed) {
					return;
				}
				Location loc = arrow.getLocation();
				loc.getWorld().spawnParticle(Particle.HEART, loc, 8, 0.5, 0.5, 0.5);
				loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.5, 0.5, 0.5);
				loc.getWorld().playSound(loc, Sound.ENTITY_SPLASH_POTION_BREAK, 1.0F, 1.0F);
				arrow.remove();
				cancel();
			}
		}.runTaskTimer(Litestrike.getInstance(), 0L, 1L);
	}

	@EventHandler
	public void onBowShot(EntityShootBowEvent event) {
		// count one shot per trigger pull (bow or crossbow) during rounds
		if (Litestrike.getInstance().game_controller.round_state != RoundState.PreRound) {
			if (event.getEntity() instanceof Player p) {
				Litestrike.getInstance().game_controller.playerDataManager.get(p).bow_shots += 1;
				if (event.getProjectile() instanceof Arrow arrow
						&& isHealingArrow(event.getConsumable())) {
					trackHealingArrow(arrow, p);
				}
			}
			return;
		}
		event.setCancelled(true);
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		ItemStack weapon = event.getBow();
		if (weapon != null && weapon.getItemMeta() instanceof CrossbowMeta) {
			CrossbowMeta crossbowMeta = (CrossbowMeta) weapon.getItemMeta();
			crossbowMeta.setChargedProjectiles(null);
			weapon.setItemMeta(crossbowMeta);
		}
		if (event.getProjectile() instanceof Arrow) {
			((Player) event.getEntity()).getInventory().addItem(((Arrow) event.getProjectile()).getItemStack());
		} else if (event.getProjectile() instanceof SpectralArrow) {
			((Player) event.getEntity()).getInventory().addItem(((SpectralArrow) event.getProjectile()).getItemStack());
		}

	}

	@EventHandler
	public void loadCrossbow(EntityLoadCrossbowEvent event) {
		if (Litestrike.getInstance().game_controller.round_state == RoundState.PreRound) {
			event.setCancelled(true);
		}
	}

	// prevents healing if player is under anti heal
	@EventHandler
	public void onRegainHealth(EntityRegainHealthEvent e) {
		if (!(e.getEntity() instanceof Player player)) return;
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) return;
		PlayerData pd = gc.playerDataManager.get(player);
		if (pd != null && pd.antiHealTicks > 0) {
			e.setCancelled(true);
		}
	}
}

class LSChatRenderer implements ChatRenderer.ViewerUnaware {
	@Override
	public Component render(Player source, Component sourceDisplayName, Component message) {
		Team t = Litestrike.getInstance().game_controller.teams.get_team(source);
		TextColor color;
		if (t == null) {
			color = Teams.SPECTATOR_GREY;
		} else if (t == Team.Breaker) {
			color = TextColor.color(0x22fb30);
		} else {
			color = TextColor.color(0xfb3922);
		}
		return text("<").color(color).append((sourceDisplayName)).append(text("> ")).color(color).append(message)
				.color(color);
	}
}

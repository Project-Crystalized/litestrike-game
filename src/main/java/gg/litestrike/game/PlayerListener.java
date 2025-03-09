package gg.litestrike.game;

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.projectiles.ProjectileSource;

import static net.kyori.adventure.text.Component.text;

public class PlayerListener implements Listener {
	private LSChatRenderer chat_renderer = new LSChatRenderer();

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (Bukkit.getOnlinePlayers().size() > Litestrike.PLAYER_CAP) {
			event.disallow(PlayerLoginEvent.Result.KICK_FULL, text("The server is full.\n"));
		}
		if (gc == null) {
			return;
		}

		// the if condition checks if the player is rejoining
		if (gc.teams.wasInitialPlayer(event.getPlayer().getName()) == null) {
			// player isnt rejoining, so we dont allow join
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, text("A game is already in Progress.\n")
					.append(text("If you see this message, it is likely a bug, pls report it to the admins")));
		}
	}

	@EventHandler
	public void onPLayerQuit(PlayerQuitEvent e) {
		e.quitMessage(text(""));
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.teams.get_team(e.getPlayer()) == Team.Breaker) {
			return;
		}
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
		Player p = event.getPlayer();
		GameController gc = Litestrike.getInstance().game_controller;

		p.teleport(Litestrike.getInstance().mapdata.get_queue_spawn(p.getWorld()));
		p.getInventory().clear();
		p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
		p.setFoodLevel(20);
		p.lookAt(Litestrike.getInstance().mapdata.get_placer_spawn(p.getWorld()), LookAnchor.EYES);
		p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1, false, false, true));

		if (gc == null) {
			p.setGameMode(GameMode.SURVIVAL);
			QueueSystem.qsb.show_queue_scoreboard(p);

		} else {
			// if we are here, it means the player is rejoining
			p.setGameMode(GameMode.SPECTATOR);

			if (Shop.getShop(p) != null) {
				Shop.getShop(p).player = p.getName();
			}

			Team should_be_team = gc.teams.wasInitialPlayer(event.getPlayer().getName());

			// give player the scoreboard and bossbar again
			ScoreboardController.give_player_scoreboard(p, should_be_team, gc.teams, gc.game_reference);
			Litestrike.getInstance().bbd.showBossBar();

			if (should_be_team == null) {
				p.kick(text("a fatal logic error occured, pls report this as a bug"));
				Bukkit.getLogger().severe("fatal logic error occured:" +
						"a player was allowed to join during a game, but wasnt in any team previously. That should not be possible");
			}
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
		if (e.getItem() != null && e.getItem().getType() == Material.POTION) {
			PotionMeta pm = (PotionMeta) e.getItem().getItemMeta();
			e.getPlayer().addPotionEffects(pm.getCustomEffects());
			if (e.getHand() == EquipmentSlot.HAND) {
				e.getPlayer().getInventory().setItemInMainHand(null);
			} else {
				e.getPlayer().getInventory().setItemInOffHand(null);
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
		if (!msg_text.startsWith("@all") && !msg_text.startsWith("@a")) {
			e.viewers().removeAll(gc.teams.get_enemy_team_of(e.getPlayer()));
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

		// reduce explosion damage
		if (e.getCause() == DamageCause.ENTITY_EXPLOSION) {
			e.setDamage(e.getDamage() / 3);
		}

		if (e.getEntity() instanceof Hanging) {
			e.setCancelled(true);
			return;
		}

		Entity source = e.getDamageSource().getCausingEntity();
		if (!(source instanceof Player) || !(e.getEntity() instanceof Player)) {
			return;
		}
		if (Teams.get_team(source.getUniqueId()) == Teams.get_team(e.getEntity().getUniqueId())) {
			e.setCancelled(true);
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
		if (event.getHitBlock() == null)
			return;

		ProjectileSource shooter = event.getEntity().getShooter();
		Location loc = event.getEntity().getLocation();

		if (shooter == null)
			return;

		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null)
			return;

		if (event.getEntity().getType() == EntityType.SPECTRAL_ARROW) {
			for (LivingEntity e : loc.getNearbyPlayers(3)) {
				if (gc.teams.get_team((Player) e) == gc.teams.get_team((Player) shooter)) {
					e.removePotionEffect(PotionEffectType.GLOWING);
				}
			}
		}
	}

	@EventHandler
	public void onBowShot(EntityShootBowEvent event) {
		if (Litestrike.getInstance().game_controller.round_state == RoundState.PreRound) {
			event.setCancelled(true);
			if (event.getProjectile() instanceof Arrow) {
				((Player) event.getEntity()).getInventory().addItem(((Arrow) event.getProjectile()).getItemStack());
			} else if (event.getProjectile() instanceof SpectralArrow) {
				((Player) event.getEntity()).getInventory().addItem(((SpectralArrow) event.getProjectile()).getItemStack());
			}
		}
	}

	@EventHandler
	public void loadCrossbow(EntityLoadCrossbowEvent event) {
		if (Litestrike.getInstance().game_controller.round_state == RoundState.PreRound) {
			event.setCancelled(true);
		}
	}

}

class LSChatRenderer implements ChatRenderer.ViewerUnaware {
	@Override
	public Component render(Player source, Component sourceDisplayName, Component message) {
		Team t = Litestrike.getInstance().game_controller.teams.get_team(source);
		TextColor color;
		if (t == Team.Breaker) {
			color = TextColor.color(0x22fb30);
		} else {
			color = TextColor.color(0xfb3922);
		}
		return (text("<").append(sourceDisplayName).append(text("> ")).append(message)).color(color);
	}
}

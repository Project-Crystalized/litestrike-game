package gg.litestrike.game;

import gg.crystalized.lobby.App;
import gg.crystalized.lobby.InventoryManager;
import gg.crystalized.lobby.Ranks;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.PotionMeta;
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

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

import java.util.List;

public class PlayerListener implements Listener {
	private LSChatRenderer chat_renderer = new LSChatRenderer();

	@EventHandler
	public void onPlayerLogin(PlayerConnectionValidateLoginEvent event) {
		if (Bukkit.getOnlinePlayers().size() > Litestrike.PLAYER_CAP) {
			event.kickMessage(text("The server is full.\n"));
		}
	}

	@EventHandler
	public void onPLayerQuit(PlayerQuitEvent e) {
		e.quitMessage(text(""));
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.teams.get_team(e.getPlayer()) != Team.Placer) {
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
		try {
			InventoryManager.giveLobbyItems(p);
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

		} else {
			// if we are here, it means the player is rejoining
			p.setGameMode(GameMode.SPECTATOR);

			if (gc.getShop(p) != null) {
				gc.getShop(p).player = p.getName();
			}

			Team should_be_team = gc.teams.wasInitialPlayer(event.getPlayer().getName());

			// give player the scoreboard and bossbar again
			// ScoreboardController.setup_scoreboard(gc.teams, gc.game_reference);
			ScoreboardController.give_player_scoreboard(p, gc.teams, gc.game_reference);
			Litestrike.getInstance().bbd.showBossBar();
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
		Team attacker_team = Teams.get_team(source.getUniqueId());
		Team attacked_team = Teams.get_team(e.getEntity().getUniqueId());
		if (attacker_team == null || attacked_team == null || attacked_team == attacker_team) {
			e.setCancelled(true);
			return;
		}
		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData((Player) source);
		double health = ((Player) e.getEntity()).getHealth();
		if (health - e.getFinalDamage() <= 0) {
			pd.total_damage += health;
		} else {
			pd.total_damage += e.getFinalDamage();
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

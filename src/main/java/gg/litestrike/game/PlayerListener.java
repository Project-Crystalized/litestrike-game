package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.Component.text;

public class PlayerListener implements Listener {

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		GameController gc = Litestrike.getInstance().game_controller;
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
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		GameController gc = Litestrike.getInstance().game_controller;

		p.teleport(Litestrike.getInstance().mapdata.get_que_spawn(p.getWorld()));
		p.getInventory().clear();
		p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		p.setFoodLevel(20);
		p.lookAt(Litestrike.getInstance().mapdata.get_placer_spawn(p.getWorld()), LookAnchor.EYES);
		p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

		if (gc == null) {
			p.setGameMode(GameMode.SURVIVAL);
		} else {
			// if we are here, it means the player is rejoining
			p.setGameMode(GameMode.SPECTATOR);

			Team should_be_team = gc.teams.wasInitialPlayer(event.getPlayer().getName());
			if (should_be_team == null) {
				p.kick(text("a fatal logic error occured, pls report this as a bug"));
				Bukkit.getLogger().severe("fatal logic error occured:" +
						"a player was allowed to join during a game, but wasnt in any team previously. That should not be possible");
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		// prevent blocks from getting broken
		event.setCancelled(true);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			e.setCancelled(true);
			return;
		}
		if (!(e.getDamager() instanceof Player)) {
			return;
		}
		if (!(e.getEntity() instanceof Player)) {
			return;
		}

		Player damager = (Player) e.getDamager();
		Player damage_receiver = (Player) e.getEntity();

		// if both players arent in same team, cancel damage
		if (gc.teams.get_team(damager) == gc.teams.get_team(damage_receiver)) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		// if game isnt going, cancel event
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			e.setCancelled(true);
			return;
		}

		// if round isnt running, cancel event
		if (gc.round_state != RoundState.Running) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		e.setCancelled(true);
		e.deathMessage(null);
		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		Player p = e.getPlayer();
		// i have no idea if this cast is safe, we will see through playtesting
		Player killer = (Player) e.getDamageSource().getCausingEntity();
		GameController gc = Litestrike.getInstance().game_controller;

		// reset killed player
		p.setGameMode(GameMode.SPECTATOR);
		p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

		// give death/kill and money
		gc.getPlayerData(p).deaths += 1;
		if (killer != null) {
			gc.getPlayerData(killer).kills += 1;
			gc.getPlayerData(killer).addMoney(500, "ғᴏʀ ᴋɪʟʟɪɴɢ " + p.getName());
		}

		Team killed_team = gc.teams.get_team(p);

		// send message
		Component death_message = text(p.getName()).color(Teams.get_team_color(gc.teams.get_team(p)))
			.append(text(" ᴡᴀꜱ ᴋɪʟʟᴇᴅ ").color(Litestrike.YELLOW));
		if (killer != null) {
			death_message = death_message.append(text("ʙʏ ").color(Litestrike.YELLOW))
				.append(text(killer.getName()).color(Teams.get_team_color(gc.teams.get_team(killer))));
		}
		Bukkit.getServer().sendMessage(death_message);

		// play sound
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (gc.teams.get_team(player) == killed_team) {
				SoundEffects.ally_death(player);
			} else {
				SoundEffects.enemy_death(player);
			}
		}

	}
}

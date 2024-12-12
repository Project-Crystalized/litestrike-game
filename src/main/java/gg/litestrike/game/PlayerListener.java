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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import static net.kyori.adventure.text.Component.text;

import java.util.List;

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
		p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
		p.setFoodLevel(20);
		p.lookAt(Litestrike.getInstance().mapdata.get_placer_spawn(p.getWorld()), LookAnchor.EYES);
		p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1, false, false, true));

		if (gc == null) {
			p.setGameMode(GameMode.SURVIVAL);
			Litestrike.getInstance().qsb.show_que_scoreboard(p);

		} else {
			// if we are here, it means the player is rejoining
			p.setGameMode(GameMode.SPECTATOR);

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
		// prevent blocks from getting broken
		event.setCancelled(true);
	}

	@EventHandler
	public void onChatEvent(AsyncChatEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			return;
		}

		List<Player> enemy_team;
		if (gc.teams.get_team(e.getPlayer()) == Team.Breaker) {
			enemy_team = gc.teams.get_placers();
		} else {
			enemy_team = gc.teams.get_breakers();
		}

		e.viewers().removeAll(enemy_team);
		e.renderer(ChatRenderer.viewerUnaware(new LSChatRenderer()));
	}

	// @EventHandler
	// public void onChatDecorate(AsyncChatDecorateEvent e) {
	// GameController gc = Litestrike.getInstance().game_controller;
	// if (e.player() == null || gc == null) {
	// return;
	// }
	//
	// Team t = gc.teams.get_team(e.player());
	// TextColor color;
	// if (t == Team.Breaker) {
	// color = Teams.BREAKER_GREEN;
	// } else {
	// color = Teams.PLACER_RED;
	// }
	// e.result(e.result().color(color));
	// }

	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			e.setCancelled(true);
			return;
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
		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		Player p = e.getPlayer();
		// i have no idea if this cast is safe, we will see through playtesting
		Player killer = (Player) e.getDamageSource().getCausingEntity();
		GameController gc = Litestrike.getInstance().game_controller;

		// reset killed player
		p.setGameMode(GameMode.SPECTATOR);
		p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
		p.getInventory().clear();

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
		String log_msg = p.getName() + "(" + gc.teams.get_team(p) + ") was killed by ";
		if (killer != null) {
			death_message = death_message.append(text("ʙʏ ").color(Litestrike.YELLOW))
					.append(text(killer.getName()).color(Teams.get_team_color(gc.teams.get_team(killer))));
			log_msg = log_msg + killer.getName() + "(" + gc.teams.get_team(killer) + ")";
		}
		Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(death_message);
		Bukkit.getLogger().info(log_msg);

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

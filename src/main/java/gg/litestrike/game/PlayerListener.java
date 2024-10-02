package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

public class PlayerListener implements Listener {

  @EventHandler
  public void onPlayerJoin(PlayerLoginEvent event) {
		// if a game is already running kick the player
		// TODO implement /rejoin
		if (Litestrike.getInstance().game_controller != null) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("A game is already on Progress.\n") 
				.append(Component.text("If you see this message, it is likely a bug, pls report it to the admins")));
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		e.setCancelled(true);
		Player p = e.getPlayer();
		// i have no idea if this cast is safe, well see through playtesting
		Player killer = (Player) e.getDamageSource().getCausingEntity();
		GameController gc = Litestrike.getInstance().game_controller;

		// reset killed player
		p.setGameMode(GameMode.SPECTATOR);
		p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

		// give death/kill and money
		gc.getPlayerData(p).deaths += 1;
		gc.getPlayerData(killer).kills += 1;
		gc.getPlayerData(killer).addMoney(500, "For killing " + p.getName());

		// play sound
		Team killed_team = gc.teams.get_team(p);
		for (Player player : Bukkit.getOnlinePlayers()) {
			// TODO replace sounds and color the text
			player.sendMessage(Component.text(p.getName() + " was killed by " + killer.getName()));
			if (gc.teams.get_team(player) == killed_team) {
				// play sound of own teams killed
				player.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 0.2f, 1f));
			} else {
				// play soundof enemy team killed
				player.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 0.5f, 1f));
			}
		}

	}
}

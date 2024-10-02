package gg.litestrike.game;


import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;

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
		p.setGameMode(GameMode.SPECTATOR);
		p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		// TODO give death, and give killer kill and money
	}
}

package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.lang.Exception;

public class Teams {
	// these are the names of the players that where in the game when it started.
	private List<String> placers;
	private List<String> breakers;

	public Teams() {
		List<String> list = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			list.add(p.getName());
		}
		Collections.shuffle(list);
		int middle = list.size() / 2;

		// if odd, breakers get more
		placers = list.subList(0, middle);
		breakers = list.subList(middle, list.size());
	}

	public List<Player> get_placers() {
		List<Player> placer_list = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (placers.contains(p.getName())) {
				placer_list.add(p);
			}
		}
		return placer_list;
	}
	public List<Player> get_breakers() {
		List<Player> breaker_list = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (breakers.contains(p.getName())) {
				breaker_list.add(p);
			}
		}
		return breaker_list;
	}

	// returns null if the player wasnt a initial player
	// otherwise returns the team that the player should be in
	public Team wasInitialPlayer(String name) {
		if (breakers.contains(name)) {
			return Team.Breaker;
		}
		if (placers.contains(name)) {
			return Team.Placer;
		}
		return null;
	}

	public Team get_team(Player p) {
		if (placers.contains(p.getName())) {
			return Team.Placer;
		}

		if (breakers.contains(p.getName())) {
			return Team.Breaker;
		}

		Bukkit.getLogger().log(Level.SEVERE, "A player that wasnt in any Team was found");
		Bukkit.getLogger().log(Level.SEVERE, "The Plugin will be disabled!");
		// disable plugin when failure
		Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());

		throw new RuntimeException(new Exception("player is in no team"));
	}

}

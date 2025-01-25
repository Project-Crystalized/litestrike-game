package gg.litestrike.game;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.TextColor;

import java.util.UUID;
import java.util.logging.Level;
import java.lang.Exception;

public class Teams {
	// these are the names of the players that where in the game when it started.
	private static List<String> placers;
	private static List<String> breakers;

	public static final TextColor PLACER_RED = TextColor.color(0xe31724);
	public static final TextColor BREAKER_GREEN = TextColor.color(0x0f9415);

	public Teams() {
		List<String> list = Litestrike.getInstance().party_manager.generate_teams();
		int middle = list.size() / 2;

		// if odd, breakers get more
		breakers = list.subList(0, middle);
		placers = list.subList(middle, list.size());
	}

	public void switch_teams() {
		List<String> temporary = placers;
		placers = breakers;
		breakers = temporary;
	}

	public List<Player> get_placers() {
		List<Player> placer_list = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (placers.contains(p.getName()) && p.isConnected()) {
				placer_list.add(p);
			}
		}
		return placer_list;
	}

	public List<Player> get_breakers() {
		List<Player> breaker_list = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (breakers.contains(p.getName()) && p.isConnected()) {
				breaker_list.add(p);
			}
		}
		return breaker_list;
	}

	public List<Player> get_enemy_team_of(Player p) {
		if (breakers.contains(p.getName())) {
			return get_placers();
		} else {
			return get_breakers();
		}
	}

	public List<Player> get_team_of(Team t) {
		if (t == Team.Breaker) {
			return get_breakers();
		} else {
			return get_placers();
		}
	}

	public List<String> get_initial_placers() {
		return placers;
	}

	public List<String> get_initial_breakers() {
		return breakers;
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

		return null;
	}

	public static Team get_team(UUID uuid){
		Player p = Bukkit.getPlayer(uuid);
		if (placers.contains(p.getName())) {
			return Team.Placer;
		}

		if (breakers.contains(p.getName())) {
			return Team.Breaker;
		}
		return null;
	}

	public Team get_team(String name) {
		if (placers.contains(name)) {
			return Team.Placer;
		}

		if (breakers.contains(name)) {
			return Team.Breaker;
		}

		Bukkit.getLogger().log(Level.SEVERE, "A player that wasnt in any Team was found");
		Bukkit.getLogger().log(Level.SEVERE, "The Plugin will be disabled!");
		// disable plugin when failure
		Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());

		return null;
	}

	public static TextColor get_team_color(Team t) {
		if (t == Team.Breaker) {
			return BREAKER_GREEN;
		} else {
			return PLACER_RED;
		}
	}
}

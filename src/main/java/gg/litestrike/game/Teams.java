package gg.litestrike.game;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.TextColor;

import java.util.UUID;
import java.util.logging.Level;

public class Teams {
	// these are the names of the players that where in the game when it started.
	private static List<String> placers;
	private static List<String> breakers;

	public static final TextColor PLACER_RED = TextColor.color(0xe31724);
	public static final TextColor BREAKER_GREEN = TextColor.color(0x0f9415);

	public Teams() {
		List<String> list = generate_fair_teams();
		int middle = list.size() / 2;

		// if odd, breakers get more
		breakers = list.subList(0, middle);
		placers = list.subList(middle, list.size());
	}

	private List<String> generate_fair_teams() {
		List<String> best_team = Litestrike.getInstance().party_manager.generate_teams();
		List<PlayerRankedData> player_ranks = PlayerRankedData.load_player_data();

		if (Litestrike.getInstance().mapdata.ranked) {
			int best_diff_score = get_diff_score(best_team, player_ranks);
			for (int i = 0; i < 3; i++) {
				List<String> new_team = Litestrike.getInstance().party_manager.generate_teams();
				int new_diff_score = get_diff_score(new_team, player_ranks);

				if (new_diff_score < best_diff_score) {
					best_team = new_team;
					best_diff_score = new_diff_score;
				}
			}
		}

		return best_team;
	}

	// get the difference in rp between the two teams
	private int get_diff_score(List<String> teams, List<PlayerRankedData> player_ranks) {
		int middle = teams.size() / 2;

		List<String> tmp_breakers = teams.subList(0, middle);
		List<String> tmp_placers = teams.subList(middle, teams.size());
		int breaker_score = Ranking.get_total_rp_team(tmp_breakers, player_ranks);
		int placer_score = Ranking.get_total_rp_team(tmp_placers, player_ranks);

		return Math.abs(breaker_score - placer_score);
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

	public static Team get_team(UUID uuid) {
		Player p = Bukkit.getPlayer(uuid);
		if (placers.contains(p.getName())) {
			return Team.Placer;
		}

		if (breakers.contains(p.getName())) {
			return Team.Breaker;
		}
		return null;
	}

	public static Team get_team(String name) {
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

	public static int get_team_breaks(Team t) {
		int breaks = 0;
		List<String> team;
		if (t == Team.Breaker) {
			team = breakers;
		} else {
			team = placers;
		}
		for (String name : team) {
			breaks += Litestrike.getInstance().game_controller.getPlayerData(name).getBroken();
		}
		return breaks;
	}

	public static int get_team_plants(Team t) {
		int breaks = 0;
		List<String> team;
		if (t == Team.Breaker) {
			team = breakers;
		} else {
			team = placers;
		}
		for (String name : team) {
			breaks += Litestrike.getInstance().game_controller.getPlayerData(name).getPlaced();
		}
		return breaks;
	}
}

package gg.litestrike.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.persistence.PersistentDataType;

public class Teams {
	// these are the names of the players that where in the game when it started.
	private List<String> placers;
	private List<String> breakers;

	public static final TextColor PLACER_RED = TextColor.color(0xe31724);
	public static final TextColor BREAKER_GREEN = TextColor.color(0x0f9415);
	public static final TextColor SPECTATOR_GREY = TextColor.color(0xb0a2a2);
	//The team pdc for the cry_essentials plugin to tell which team is on which sided
	public static final NamespacedKey TEAM_KEY = new NamespacedKey("crystalized", "team");

	// there are basically 3 ways to generate partys:
	// skillbased: generate_fair_teams
	// random: generate_random_teams
	// manual: both skillbased and random will take partys into account
	public Teams() {
		GameConfig game_conf = Litestrike.getInstance().gameConfig;
		if (game_conf.manualTeamsEnabled) {
			create_manual_teams(game_conf);
			if (breakers.size() == 0 || placers.size() == 0) {
				Bukkit.getLogger().severe("One of the two teams was empty, pls check the manual team selection!");
			}
			log_teams("manual", breakers, placers, null);
			return;
		}

		List<String> list;
		boolean is_ranked = game_conf.ranked;
		if (is_ranked) {
			 list = generate_fair_teams();
		} else {
			 list = generate_random_teams();
		}
		int middle = list.size() / 2;

		// if odd, breakers get more
		breakers = list.subList(0, middle);
		placers = list.subList(middle, list.size());
		if (!is_ranked) {
			log_teams("random", breakers, placers, null);
		}
	}

	private void create_manual_teams(GameConfig gc) {
		Bukkit.getLogger().info("creating teams from manual selection");
		placers = new ArrayList<>(gc.placers);
		breakers = new ArrayList<>(gc.breakers);

		placers.removeIf(placer -> Bukkit.getPlayer(placer) == null);
		breakers.removeIf(breaker -> Bukkit.getPlayer(breaker) == null);
	}

	private List<String> generate_random_teams() {
		return Litestrike.getInstance().party_manager.generate_teams();
	}

	private List<String> generate_fair_teams() {
		List<String> best_team = Litestrike.getInstance().party_manager.generate_teams();
		List<PlayerRankedData> player_ranks = PlayerRankedData.load_player_data();

		if (Litestrike.getInstance().gameConfig.ranked) {
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

		// same split as in Teams(): if odd, breakers get more
		int middle = best_team.size() / 2;
		log_teams("fair (ranked)", best_team.subList(0, middle), best_team.subList(middle, best_team.size()), player_ranks);
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

	private void log_teams(String variant, List<String> breakers, List<String> placers, List<PlayerRankedData> ranks) {
		Bukkit.getLogger().info("[Teams] variant: " + variant);
		log_team("breakers", breakers, ranks);
		log_team("placers", placers, ranks);
	}

	private void log_team(String team_name, List<String> team, List<PlayerRankedData> ranks) {
		HashMap<String, Integer> rp = new HashMap<>();
		if (ranks != null) {
			for (PlayerRankedData prd : ranks) {
				String name = Bukkit.getOfflinePlayer(prd.uuid).getName();
				if (name != null) {
					rp.put(name, prd.rp);
				}
			}
		}
		long total = 0;
		int counted = 0;
		ArrayList<String> parts = new ArrayList<>();
		for (String name : team) {
			int party_size = Litestrike.getInstance().party_manager.get_party_of(name).size();
			String party = party_size == 0 ? "solo" : "party of " + party_size;
			Integer r = rp.get(name);
			if (r == null) {
				parts.add(name + " [unranked, " + party + "]");
			} else {
				total += r;
				counted++;
				parts.add(name + " [" + r + " rp, " + party + "]");
			}
		}
		String avg = counted > 0 ? "" + (total / counted) : "n/a";
		Bukkit.getLogger().info("[Teams] " + team_name + " (" + team.size() + " players, avg " + avg + " rp): " + String.join(", ", parts));
	}

	public List<Player> get_placers() {
		return get_players(placers, false);
	}

	public List<Player> get_alive_placers() {
		return get_players(placers, true);
	}

	public List<Player> get_breakers() {
		return get_players(breakers, false);
	}

	public List<Player> get_alive_breakers() {
		return get_players(breakers, true);
	}

	private List<Player> get_players(List<String> team, boolean alive) {
		List<Player> player_list = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (team.contains(p.getName()) && p.isConnected() && (!alive || !p.getGameMode().equals(GameMode.SPECTATOR) || !p.getGameMode().equals(GameMode.ADVENTURE))) {
				player_list.add(p);
			}
		}
		return player_list;
	}

	public List<Player> get_enemy_team_of(Player p) {
		if (breakers.contains(p.getName())) {
			return get_placers();
		} else if (placers.contains(p.getName())) {
			return get_breakers();
		} else {
			return null;
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

	// returns all players that are not spectators
	public List<Player> get_all_players() {
		List<Player> new_list = new ArrayList<>();
		new_list.addAll(this.get_breakers());
		new_list.addAll(this.get_placers());
		return new_list;
	}

	public Team get_team(Player p) {
		return get_team(p.getName());
	}

	public Team get_team(UUID uuid) {
		Player p = Bukkit.getPlayer(uuid);
		if (p == null) {
			return null;
		}
		return get_team(p.getName());
	}

	public Team get_team(String name) {
		if (placers.contains(name)) {
			return Team.Placer;
		}

		if (breakers.contains(name)) {
			return Team.Breaker;
		}
		return null;
	}

	public static TextColor get_team_color(Team t) {
		if (t == Team.Breaker) {
			return BREAKER_GREEN;
		} else if (t == Team.Placer) {
			return PLACER_RED;
		} else {
			return SPECTATOR_GREY;
		}
	}
	//This puts the players team in to PDC so esentials can tell which players are on which team
	//The names of teams can be different in each game, the essentials only need to compare if the name are called the same

	//The update for all players on team swap etc
	public void updateTeamPDC() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			updateTeamPDCindividual(player);
		}
	}
	//Update for the individual on rejoin
	public void updateTeamPDCindividual(Player player) {
		//Gets the team and depending on it sets the PDC
		Team team = get_team(player);
		if (team == Team.Breaker) {
			player.getPersistentDataContainer().set(TEAM_KEY, PersistentDataType.STRING, "breaker");
		} else if (team == Team.Placer) {
			player.getPersistentDataContainer().set(TEAM_KEY, PersistentDataType.STRING, "placer");
		} else {
			//If it is null makes sure that the key is removed, specators team must be null for proper arrow particles
			player.getPersistentDataContainer().remove(TEAM_KEY);
		}
	}


	public int getTeamBreaksAndPlants(Team t) {
		int sum = 0;
		List<String> team = (t == Team.Breaker ? breakers : placers);
		for (String name : team) {
			PlayerData pd = Litestrike.getInstance().game_controller.playerDataManager.get(name);
			sum += pd.plants + pd.breaks;
		}
		return sum;
	}
}

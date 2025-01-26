package gg.litestrike.game;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.Component.text;

public class Ranking {

	public static void do_ranking(Team winner_team) {
		List<PlayerRankedData> player_ranks = new ArrayList<>();
		GameController gc = Litestrike.getInstance().game_controller;

		List<String> player_names = new ArrayList<>(gc.teams.get_initial_breakers());
		player_names.addAll(gc.teams.get_initial_placers());
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			for (String placer_name : player_names) {
				UUID uuid = Bukkit.getOfflinePlayer(placer_name).getUniqueId();
				String query = "SELECT * FROM LsRanks WHERE player_uuid = ?";

				PreparedStatement ps = conn.prepareStatement(query);
				ps.setBytes(1, uuid_to_bytes(uuid));
				ResultSet rs = ps.executeQuery();
				player_ranks.add(new PlayerRankedData(rs, uuid));
			}
		} catch (SQLException e) {
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("didnt write data to database");
		}

		// player ranks are now loaded

		int placer_avarage_elo = PlayerRankedData.get_elo_of_team(player_ranks, Team.Placer);
		int breaker_avarage_elo = PlayerRankedData.get_elo_of_team(player_ranks, Team.Breaker);
		int avarage_game_score = get_avarage_score();

		for (PlayerRankedData prd : player_ranks) {
			Team players_team = get_current_team(prd.uuid);
			int enemy_elo = breaker_avarage_elo;
			if (players_team == Team.Breaker) {
				enemy_elo = placer_avarage_elo;
			}

			Player p = Bukkit.getPlayer(prd.uuid);
			double won = players_team == winner_team ? 1 : 0;
			// update elo
			prd.elo = (int)Math.round(((double) prd.elo) + 30.0 * (won - elo_probability(enemy_elo, prd.elo)));

			double elo_factor;
			if (players_team == winner_team) {
				elo_factor = (double)enemy_elo / (double)prd.elo;
			} else {
				elo_factor = (double)prd.elo / (double)enemy_elo;
			}

			double score_factor = avarage_game_score == 0 ? 1 : p == null ? 1 : (double)gc.getPlayerData(p).calc_player_score() / (double)avarage_game_score;
			double lp_diff = (players_team == winner_team ? 10 : -10) * ((elo_factor + score_factor) / 2);

			if (lp_diff >= 0) {
				lp_diff = Math.clamp(lp_diff, 1, 20);
			} else if (lp_diff < 0) {
				lp_diff = Math.clamp(lp_diff, -20, -1);
			}

			// update the values
			if (prd.lp >= 100 && players_team == winner_team) {
				prd.promos += 1;
				if (prd.promos >= 2) {
					prd.lp = 0;
					prd.promos = 0;
					prd.rank += 1;
					if (p != null) 
						p.sendMessage(text("You have gained a rank!!"));
				} else {
					if (p != null) 
						p.sendMessage(text("Your promotion has advanced or started. Win " + (2-prd.promos) + " more games to rank up!"));
				}
			} else if (prd.lp + lp_diff < 0) {
				prd.lp = 30;
				prd.promos = 0;
				prd.rank -= 1;
				if (p != null) 
					p.sendMessage(text("Your lp was negative and you have lost a rank :("));
			} else {
				prd.lp += lp_diff;
				if (p != null) 
					p.sendMessage(text("Your lp changed by " + (int)lp_diff + ". You are now at " + prd.lp + " lp."));
			}
		}

		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			for (PlayerRankedData prd : player_ranks) {
				String update = "INSERT INTO LsRanks (elo, rank, lp, promos, player_uuid) VALUES (?,?,?,?, ?) ON CONFLICT(player_uuid) DO UPDATE SET elo=excluded.elo, rank=excluded.rank, lp=excluded.lp, promos=excluded.promos;";

				PreparedStatement ps = conn.prepareStatement(update);
				ps.setInt(1, prd.elo);
				ps.setInt(2, prd.rank);
				ps.setInt(3, prd.lp);
				ps.setInt(4, prd.promos);
				ps.setBytes(5, uuid_to_bytes(prd.uuid));
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("didnt write data to database");
		}
	}

	private static double elo_probability(int rating1, int rating2) {
		return 1.0 / (1 + Math.pow(10, (rating1 - rating2) / 400.0));
	}

	// gets the team, the player would be in, if they would still be online
	private static Team get_current_team(UUID uuid) {
		GameController gc = Litestrike.getInstance().game_controller;
		OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
		Team online_team = gc.teams.get_team(p.getName());
		if (online_team != null) 
			return online_team;
		
		Team initial_team = gc.teams.get_initial_breakers().contains(p.getName()) ? Team.Breaker : Team.Placer;
		Team other_team = gc.teams.get_initial_breakers().contains(p.getName()) ? Team.Placer : Team.Breaker;

		if (gc.round_number > GameController.SWITCH_ROUND) {
			return other_team;
		} else {
			return initial_team;
		}
	}

	private static byte[] uuid_to_bytes(UUID uuid) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	private static int get_avarage_score() {
		int total_score = 0;
		for (Player p : Bukkit.getOnlinePlayers()) {
			total_score += Litestrike.getInstance().game_controller.getPlayerData(p).calc_player_score();
		}
		return total_score / Bukkit.getOnlinePlayers().size();
	}
}

class PlayerRankedData {
	public int elo;
	public int rank;
	public int lp;
	public int promos;
	public UUID uuid;

	public PlayerRankedData(ResultSet rs, UUID uuid) throws SQLException {
		this.uuid = uuid;
		rs.next();
		this.elo = rs.getInt("elo");
		this.rank = rs.getInt("rank");
		this.lp = rs.getInt("lp");
		this.promos = rs.getInt("promos");
		if (elo == 0 && rank == 0 && lp == 0 && promos == 0) {
			elo = 1000;
		}
	}

	public static int get_elo_of_team(List<PlayerRankedData> player_ranks, Team t) {
		GameController gc = Litestrike.getInstance().game_controller;
		int num_of_placers = 0;
		int total_elo_points = 0;
		for (PlayerRankedData prd : player_ranks) {
			if (gc.teams.get_team(Bukkit.getOfflinePlayer(prd.uuid).getName()) == t) {
				num_of_placers += 1;
				total_elo_points += prd.elo;
			}
		}
		return total_elo_points / num_of_placers;
	}
}

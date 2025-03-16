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
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Ranking {

	public static void do_ranking(Team winner_team) {
		List<PlayerRankedData> player_ranks = PlayerRankedData.load_player_data();

		for (PlayerRankedData prd : player_ranks) {
			Team players_team = get_current_team(prd.uuid);
			Player p = Bukkit.getPlayer(prd.uuid);
			if (p == null) {
				Bukkit.getLogger().info("a player was offline, and therefore lost rp");
				prd.rp -= 5;
			}
			OfflinePlayer offline_p = Bukkit.getOfflinePlayer(prd.uuid);

			boolean did_win = players_team == winner_team;
			int point_change = get_win_loss_points(did_win, prd.rank);

			point_change += Litestrike.getInstance().game_controller.getPlayerData(offline_p.getName()).calc_player_score();
			prd.rp += point_change;
			if (p != null) {
				p.sendMessage("You have gained or lost " + point_change + " rp.");
				p.sendMessage("Your rp is now " + prd.rp + " rp.");
			}

			// do rankup
			if (did_win && prd.rp > get_rank_min_rp(prd.rank + 1) + 20) {
				// if the player won, AND he has 20 more rp than the next higher rank
				prd.rank += 1;
				if (p != null) {
					p.sendMessage("You have gained a rank!");
				}
			} else if (!did_win && prd.rp < get_rank_min_rp(prd.rank) - 20) {
				prd.rank -= 1;
				if (prd.rank < 0) {
					prd.rank = 0;
				} else if (p != null) {
					p.sendMessage("You have lost a rank. :(");
				}
			}
		}

		PlayerRankedData.save_players(player_ranks);
	}

	// gets the team, the player would be in, if they would still be online
	private static Team get_current_team(UUID uuid) {
		GameController gc = Litestrike.getInstance().game_controller;
		OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
		Team online_team = Teams.get_team(p.getName());
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

	public static int get_total_rp_team(List<String> team, List<PlayerRankedData> player_ranks) {
		int total = 0;

		for (PlayerRankedData prd : player_ranks) {
			if (!team.contains(Bukkit.getPlayer(prd.uuid).getName())) {
				continue;
			}
			if (prd.rp < 0) {
				total += prd.rp;
				// negative numbers mess up the calculation
				continue;
			}

			int win_loss = prd.recent_wins - prd.recent_losses;
			if (win_loss > 0) {
			total += prd.rp * Math.pow(1.12, win_loss);
			} else if (win_loss < 0) {
			total += prd.rp * Math.pow(0.88, -win_loss);
			}
		}
		return total;
	}

	private static int get_win_loss_points(boolean did_win, int rank) {
		if (!did_win) {
			if (rank == 10) {
				return -7;
			} else {
				return -6;
			}
		} else {
			switch (rank) {
				case 1, 2, 3:
					return 7;
				case 4, 5:
					return 6;
				case 6, 7:
					return 5;
				case 8, 9:
					return 4;
				case 10:
					return 3;
				default:
					Bukkit.getLogger().severe("ERROR ranking, rank outside bounds?");
					return 0;
			}
		}
	}

	private static int get_rank_min_rp(int rank) {
		switch (rank) {
			case 2:
				return 0;
			case 3:
				return 250;
			case 4:
				return 500;
			case 5:
				return 750;
			case 6:
				return 1000;
			case 7:
				return 1250;
			case 8:
				return 1500;
			case 9:
				return 1750;
			case 10:
				return 2000;
			default:
				return 0;
		}
	}
}

class PlayerRankedData {
	public int rank;
	public int rp;
	public UUID uuid;
	public int recent_wins = 0;
	public int recent_losses = 0;

	private PlayerRankedData(ResultSet rs, ResultSet rs_last_games, UUID uuid) throws SQLException {
		this.uuid = uuid;
		rs.next();
		this.rank = rs.getInt("rank");
		this.rp = rs.getInt("rp");
		if (rank == 0 && rp == 0) {
			Bukkit.getLogger().warning("initialized ranks for a new player");
			rank = 2;
			rp = 100;
		}

		while (rs_last_games.next()) {
			if (rs_last_games.getInt("was_winner") == 1) {
				recent_wins += 1;
			} else if (rs_last_games.getInt("was_winner") == 0) {
				recent_losses += 1;
			} else {
				Bukkit.getLogger().severe("super weird error?!?");
			}
		}
	}

	public static List<PlayerRankedData> load_player_data() {
		List<PlayerRankedData> player_ranks = new ArrayList<>();
		GameController gc = Litestrike.getInstance().game_controller;
		List<String> player_names = new ArrayList<>();
		if (gc == null) {
			// this is called at the beginning before gamecontroller is created, to make
			// teams
			player_names = Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).collect(Collectors.toList());
		} else {
			// this is used at the end, to write data back to database
			player_names.addAll(gc.teams.get_initial_breakers());
			player_names.addAll(gc.teams.get_initial_placers());
		}

		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			String query = "SELECT * FROM LsRanks WHERE player_uuid = ?";
			String query_last_games = "SELECT was_winner "
					+ "FROM LsGamesPlayers lgp INNER JOIN LitestrikeGames lsg ON lgp.game = lsg.game_id "
					+ "WHERE player_uuid = ? "
					+ "ORDER BY timestamp DESC "
					+ "LIMIT 10;";
			PreparedStatement ps = conn.prepareStatement(query);
			PreparedStatement ps_last_games = conn.prepareStatement(query_last_games);
			for (String player_name : player_names) {
				UUID uuid = Bukkit.getOfflinePlayer(player_name).getUniqueId();

				ps.setBytes(1, uuid_to_bytes(uuid));
				ps_last_games.setBytes(1, uuid_to_bytes(uuid));
				ResultSet rs = ps.executeQuery();
				ResultSet rs_last_games = ps_last_games.executeQuery();
				player_ranks.add(new PlayerRankedData(rs, rs_last_games, uuid));
			}
		} catch (SQLException e) {
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("didnt load data from database error");
		}
		return player_ranks;
	}

	public static byte[] uuid_to_bytes(UUID uuid) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	public static void save_players(List<PlayerRankedData> player_ranks) {
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			for (PlayerRankedData prd : player_ranks) {
				String update = "INSERT INTO LsRanks (rank, rp, player_uuid) VALUES (?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET rank=excluded.rank, rp=excluded.rp;";

				PreparedStatement ps = conn.prepareStatement(update);
				ps.setInt(1, prd.rank);
				ps.setInt(2, prd.rp);
				ps.setBytes(3, uuid_to_bytes(prd.uuid));
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("didnt write data to database");
		}
	}
}

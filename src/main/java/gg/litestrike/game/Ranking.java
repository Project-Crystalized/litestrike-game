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

public class Ranking {

	public static void do_ranking(Team winner_team) {
		List<PlayerRankedData> player_ranks = PlayerRankedData.load_player_data();

		for (PlayerRankedData prd : player_ranks) {
			Team players_team = get_current_team(prd.uuid);
			Player p = Bukkit.getPlayer(prd.uuid);

			boolean did_win = players_team == winner_team;
			int point_change = get_win_loss_points(did_win, prd.rank);

			int score = Litestrike.getInstance().game_controller.getPlayerData(p).calc_player_score();

			point_change += score / 2;
			prd.rp += point_change;
			p.sendMessage("You have gained or lost " + point_change + " rp.");
			p.sendMessage("Your rp is now " + prd.rp + " rp.");

			// do rankup
			if (did_win && prd.rp > get_rank_min_rp(prd.rank + 1) + 20) {
				// if the player won, AND he has 20 more rp than the next higher rank
				prd.rank += 1;
				p.sendMessage("You have gained a rank!");
			} else if (!did_win && prd.rp < get_rank_min_rp(prd.rank) - 20) {
				prd.rank -= 1;
				p.sendMessage("You have lost a rank. :(");
			}
		}

		PlayerRankedData.save_players(player_ranks);
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

	private static int get_win_loss_points(boolean did_win, int rank) {
		if (!did_win) {
			if (rank == 10) {
				return -8;
			} else {
				return -7;
			}
		} else {
			switch (rank) {
				case 1, 2, 3:
					return 8;
				case 4, 5:
					return 7;
				case 6, 7:
					return 6;
				case 8, 9:
					return 5;
				case 10:
					return 4;
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

	private PlayerRankedData(ResultSet rs, UUID uuid) throws SQLException {
		this.uuid = uuid;
		rs.next();
		this.rank = rs.getInt("rank");
		this.rp = rs.getInt("rp");
		if (rank == 0 && rp == 0) {
			Bukkit.getLogger().warning("initialized ranks for a new player");
			rank = 3;
			rp = 300;
		}
	}

	public static List<PlayerRankedData> load_player_data() {
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
		return player_ranks;
	}

	private static byte[] uuid_to_bytes(UUID uuid) {
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

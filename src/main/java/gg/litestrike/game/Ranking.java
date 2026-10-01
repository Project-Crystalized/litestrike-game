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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Ranking {

	public static void do_ranking(Team winner_team, GameController gc) {
		List<PlayerRankedData> player_ranks = PlayerRankedData.load_player_data();

		for (PlayerRankedData prd : player_ranks) {
			OfflinePlayer offline_p = Bukkit.getOfflinePlayer(prd.uuid);
			Team players_team = gc.teams.get_team(offline_p.getName());

			boolean did_win = players_team == winner_team;
			double rawChange = get_win_loss_points(did_win, prd.rp);

			PlayerData pd = gc.playerDataManager.get(offline_p.getName());
			double perfBonus = 0;
			if (pd == null) {
				Bukkit.getLogger().severe("ranking: no player data for '" + offline_p.getName() + "', skipping the performance bonus");
			} else {
				perfBonus = pd.calc_player_score();
				rawChange += perfBonus;
				if (pd.did_leave) {
					Bukkit.getLogger().info(offline_p.getName() + " player was offline, and therefore lost rp");
					prd.rp -= 20;
				}
			}

			int point_change = (int) Math.round(rawChange);
			prd.rp += point_change;

			doRankupAndChat(prd, point_change, perfBonus);
		}

		PlayerRankedData.save_players(player_ranks);
	}

	private static void doRankupAndChat(PlayerRankedData prd, int point_change, double perfBonus) {
		Player p = Bukkit.getPlayer(prd.uuid);
		if (p == null)
			return;

		String gain = "You have gained ";
		NamedTextColor c = GREEN;
		if (point_change < 0) {
			gain = "You have lost ";
			c = RED;
		}
		p.sendMessage(Component.text(gain + Math.abs(point_change) + " rp. (+" + Math.round(perfBonus) + "rp personal score)").color(c).decoration(BOLD, true));
		p.sendMessage(Component.text("Your rp is now " + prd.rp + " rp.").color(c).decoration(BOLD, true));

		int oldRank = prd.rank;
		prd.rank = rankForRp(prd.rp);
		if (prd.rank > oldRank) {
			p.sendMessage("You have gained a rank!");
		} else if (prd.rank < oldRank) {
			p.sendMessage("You have lost a rank. :(");
		}
	}

	public static int get_total_rp_team(List<String> team, List<PlayerRankedData> player_ranks) {
		int total = 0;

		for (PlayerRankedData prd : player_ranks) {
			String name = Bukkit.getOfflinePlayer(prd.uuid).getName();
			if (name == null) {
				Bukkit.getLogger().warning("ranking: could not resolve the name of ranked player uuid " + prd.uuid);
				continue;
			}
			if (!team.contains(name)) {
				continue;
			}
			total += prd.rp;
		}
		return total;
	}

	static int rankForRp(int rp) {
		for (int rank = 10; rank > 2; rank--) {
			if (rp >= get_rank_min_rp(rank)) {
				return rank;
			}
		}
		return 2;
	}

	static int get_win_loss_points(boolean did_win, int rp) {
		double fromCenter = (rp - 1100) / 1100.0;
		if (did_win) {
			return (int) (16 - 8 * fromCenter);
		} else {
			return (int) -(16 + 8 * fromCenter);
		}
	}

	static int get_rank_min_rp(int rank) {
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
		if (rs.next()) {
			this.rank = rs.getInt("rank");
			this.rp = rs.getInt("rp");
		}
		// a missing row or a zeroed entry means this is a new player
		if (rank == 0 && rp == 0) {
			Bukkit.getLogger().warning("initialized ranks for a new player");
			rank = 2;
			rp = 100;
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
			PreparedStatement ps = conn.prepareStatement(query);
			for (String player_name : player_names) {
				UUID uuid = Bukkit.getOfflinePlayer(player_name).getUniqueId();

				ps.setBytes(1, uuid_to_bytes(uuid));
				ResultSet rs = ps.executeQuery();
				player_ranks.add(new PlayerRankedData(rs, uuid));
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

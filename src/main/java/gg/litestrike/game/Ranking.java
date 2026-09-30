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

	static final int START_RATING = 1000; // system center, and the assumed strength of unknown teams
	static final int NEW_PLAYER_RATING = 300; // new players start low and climb fast to their true level
	static final int K = 44; // even game: +-22
	static final int PERF_WEIGHT = 25; // solo carry can flip a loss to +3
	static final int MAX_SWING = 40; // nothing moves more than this, except leavers
	static final int MIN_RATING = 100;
	static final int LEAVER_PENALTY = -40;

	static int ratingChange(int mine, int enemyAverage, boolean won, double perf) {
		// chess formula: how likely you were to win, from 0.0 to 1.0.
		// being 400 points stronger means winning is ten times as likely.
		double ratingGap = (enemyAverage - mine) / 400.0;
		double expected = 1.0 / (1.0 + Math.pow(10, ratingGap));
		double actual = 0.0;
		if (won) {
			actual = 1.0;
		}
		// you gain points for beating expectations, lose for falling short.
		// great personal play adds a bonus on top.
		double change = K * (actual - expected) + PERF_WEIGHT * perf;
		if (change > MAX_SWING) {
			change = MAX_SWING;
		}
		if (change < -MAX_SWING) {
			change = -MAX_SWING;
		}
		return (int) Math.round(change);
	}

	static double performanceScore(float myDamage, float avgDamage, int plants, int breaks, float avgObjectives) {
		// damage compared to the lobby average: double the average is +1.0.
		double perf = 0;
		if (avgDamage > 0) {
			perf = (myDamage - avgDamage) / avgDamage;
		}
		// each plant or break is worth a tenth.
		perf = perf + 0.1 * (plants + breaks);
		if (perf > 1.0) {
			perf = 1.0;
		}
		if (perf < -1.0) {
			perf = -1.0;
		}
		return perf;
	}

	static int rankForRating(int rating) {
		if (rating >= 2500) return 10;
		if (rating >= 2200) return 9;
		if (rating >= 1900) return 8;
		if (rating >= 1600) return 7;
		if (rating >= 1300) return 6;
		if (rating >= 1000) return 5;
		if (rating >= 700) return 4;
		if (rating >= 400) return 3;
		return 2;
	}

	public static void do_ranking(Team winner_team) {
		List<PlayerRankedData> player_ranks = PlayerRankedData.load_player_data();
		GameController gc = Litestrike.getInstance().game_controller;

		int placerAvg = get_average_rp_team(gc.teams.get_initial_placers(), player_ranks);
		int breakerAvg = get_average_rp_team(gc.teams.get_initial_breakers(), player_ranks);

		float totalDamage = 0;
		float totalObjectives = 0;
		for (PlayerData pd : gc.playerDataManager.getAll()) {
			totalDamage += pd.total_damage;
			totalObjectives += pd.plants + pd.breaks;
		}
		float avgDamage = totalDamage / gc.playerDataManager.getAll().size();
		float avgObjectives = totalObjectives / gc.playerDataManager.getAll().size();

		for (PlayerRankedData prd : player_ranks) {
			OfflinePlayer offline_p = Bukkit.getOfflinePlayer(prd.uuid);
			Team players_team = gc.teams.get_team(offline_p.getName());

			PlayerData pd = gc.playerDataManager.get(offline_p.getName());
			if (pd == null) {
				Bukkit.getLogger().severe("RANKING: a player had no PlayerData???");
				continue;
			}
			boolean didLeave = pd != null && pd.did_leave;
			double perf = 0;
			perf = performanceScore(pd.total_damage, avgDamage, pd.plants, pd.breaks, avgObjectives);

			boolean did_win = players_team == winner_team;
			int enemyAvg = (placerAvg + breakerAvg) / 2;
			if (players_team == Team.Placer) {
				enemyAvg = breakerAvg;
			} else if (players_team == Team.Breaker) {
				enemyAvg = placerAvg;
			}

			int point_change = ratingChange(prd.rp, enemyAvg, did_win, perf);
			if (didLeave) {
				point_change = LEAVER_PENALTY;
			}
			prd.rp = Math.max(MIN_RATING, prd.rp + point_change);

			int oldRank = prd.rank;
			prd.rank = rankForRating(prd.rp);

			Player p = Bukkit.getPlayer(prd.uuid);
			if (p != null) {
				String gain = "You have gained ";
				NamedTextColor c = GREEN;
				if (point_change >= 0) {
				}
				if (point_change < 0) {
					gain = "You have lost ";
					c = RED;
				}
				p.sendMessage(Component.text(gain + Math.abs(point_change) + " rp.").color(c).decoration(BOLD, true));
				p.sendMessage(Component.text("Your rp is now " + prd.rp + " rp.").color(c).decoration(BOLD, true));
			}

			if (p != null) {
				if (prd.rank > oldRank) {
					p.sendMessage("You have gained a rank!");
				} else if (prd.rank < oldRank) {
					p.sendMessage("You have lost a rank. :(");
				}
			}
		}

		PlayerRankedData.save_players(player_ranks);
	}

	public static int get_average_rp_team(List<String> team, List<PlayerRankedData> player_ranks) {
		int total = 0;
		int count = 0;
		for (PlayerRankedData prd : player_ranks) {
			String name = Bukkit.getOfflinePlayer(prd.uuid).getName();
			if (name == null || !team.contains(name)) {
				continue;
			}
			total += prd.rp;
			count++;
		}
		if (count == 0) {
			return START_RATING;
		}
		return total / count;
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
			rp = Ranking.NEW_PLAYER_RATING;
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

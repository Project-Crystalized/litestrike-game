package gg.litestrike.game;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class LsDatabase {

	private static String dbDir() {
		String d = System.getenv("CRYSTALIZED_DB_DIR");
		if (d == null || d.isBlank())
			d = System.getProperty("user.home") + "/databases/test_dbs";
		try {
			java.nio.file.Files.createDirectories(java.nio.file.Path.of(d));
		} catch (java.io.IOException e) {
			throw new IllegalStateException("Could not create database directory: " + d, e);
		}
		return d;
	}

	public static String URL = "jdbc:sqlite:" + dbDir() + "/litestrike_db.sql";

	static void setDatabaseUrlForTests(String url) {
		URL = url;
	}

	// this is run on server startup
	public static void setup_databases() {
		// we query the games by timestamp, so creating an index for it can improve
		// perf.
		String create_ls_games = "CREATE TABLE IF NOT EXISTS LiteStrikeGames ("
				+ "game_id 			INTEGER PRIMARY KEY,"
				+ "placer_wins 	INTEGER,"
				+ "breaker_wins INTEGER,"
				+ "timestamp 		INTEGER,"
				+ "map 					STRING,"
				+ "winner 			INTEGER,"
				+ "game_ref			INTEGER"
				+ ");";
		String create_ls_players = "CREATE TABLE IF NOT EXISTS LsGamesPlayers ("
				+ "player_uuid 		BLOB,"
				+ "game 					INTEGER REFERENCES LiteStrikeGames(game_id),"
				+ "placed_bombs 	INTEGER,"
				+ "broken_bombs 	INTEGER,"
				+ "kills 					INTEGER,"
				+ "assists 				INTEGER,"
				+ "gained_money 	INTEGER,"
				+ "spent_money 		INTEGER,"
				+ "bought_items 	BLOB,"
				+ "was_winner			INTEGER,"
				+ "damage_dealt		REAL,"
				+ "deaths					INTEGER,"
				+ "did_leave			INTEGER,"
				+ "jumps					INTEGER,"
				+ "hits_dealt			INTEGER,"
				+ "bow_shots			INTEGER"
				+ ");";

		String create_ls_ranks = "CREATE TABLE IF NOT EXISTS LsRanks ("
				+ "player_uuid 		BLOB UNIQUE,"
				+ "rank						INTEGER,"
				+ "rp							INTEGER,"
				+ "last_ranked			INTEGER NOT NULL DEFAULT 0"
				+ ");";

		try (Connection conn = DriverManager.getConnection(URL)) {
			Statement stmt = conn.createStatement();
			stmt.execute(create_ls_games);
			stmt.execute(create_ls_players);
			stmt.execute(create_ls_ranks);
		} catch (SQLException e) {
			Bukkit.getLogger().severe("Failed to set up the Litestrike database at " + URL + ".");
			Bukkit.getLogger().severe("The plugin will keep running, but no game data will be recorded until this is fixed.");
			Bukkit.getLogger().severe("Error: " + e.getMessage());
		}

		// HINT this can be removed at some point, its only here cause damage_dealt
		// didnt exist originaly so it is added if missing
		// it also not adds the deaths column which didnt exist before
		create_damage_deaths_column();
		ensure_ranks_last_ranked_column();
		initialize_rank_timestamps();
	}

	// the LsGamesPlayers table has grown over time, so databases created by older
	// versions of the plugin are missing some columns. this checks if a column
	// exists and adds it if it doesnt.
	private static void create_damage_deaths_column() {
		String[][] columns = {
				{ "damage_dealt", "damage_dealt REAL" },
				{ "deaths", "deaths INTEGER" },
				{ "did_leave", "did_leave INTEGER" },
				{ "jumps", "jumps INTEGER" },
				{ "hits_dealt", "hits_dealt INTEGER" },
				{ "bow_shots", "bow_shots INTEGER" },
		};

		for (String[] column : columns) {
			ensure_column("LsGamesPlayers", column[0], column[1]);
		}
	}

	private static void ensure_ranks_last_ranked_column() {
		ensure_column("LsRanks", "last_ranked", "last_ranked INTEGER NOT NULL DEFAULT 0");
	}

	private static void initialize_rank_timestamps() {
		String update = "UPDATE LsRanks SET last_ranked = unixepoch() WHERE last_ranked = 0;";
		try (Connection conn = DriverManager.getConnection(URL)) {
			int rows = conn.createStatement().executeUpdate(update);
			if (rows > 0) {
				Bukkit.getLogger().info("Initialized last_ranked for " + rows + " ranked players.");
			}
		} catch (SQLException e) {
			Bukkit.getLogger().severe("Could not initialize last_ranked in the LsRanks table.");
			Bukkit.getLogger().severe("Error: " + e.getMessage());
		}
	}

	private static void ensure_column(String table, String column_name, String definition) {
		String check_column = "SELECT " + column_name + " FROM " + table + " LIMIT 1;";
		String create_column = "ALTER TABLE " + table + " ADD COLUMN " + definition + ";";

		try (Connection conn = DriverManager.getConnection(URL)) {
			conn.createStatement().execute(check_column);
		} catch (SQLException e) {
			// the query failed, which usually means the column is missing, so we add it
			try (Connection conn = DriverManager.getConnection(URL)) {
				conn.createStatement().execute(create_column);
				Bukkit.getLogger().info("Added missing column '" + column_name + "' to the " + table + " table.");
			} catch (SQLException ex) {
				Bukkit.getLogger().severe("Could not add missing column '" + column_name
						+ "' to the " + table + " table. The database is either corrupted or missing entirely.");
				Bukkit.getLogger().severe("Backup or delete the file at " + URL + " and restart the server.");
				Bukkit.getLogger().severe("Error: " + ex.getMessage());
			}
		}
	}

	public static void save_game(Team winner) {
		GameController gc = Litestrike.getInstance().game_controller;

		List<Component> rp_lines = List.of();
		if (Litestrike.getInstance().gameConfig.ranked) {
			rp_lines = Ranking.do_ranking(winner, gc);
		}

		String save_game = "INSERT INTO LiteStrikeGames(placer_wins, breaker_wins, timestamp, map, winner, game_ref) VALUES(?, ?, unixepoch(), ?, ?, ?)";

		int winner_int = winner == Team.Placer ? 1 : 0;

		try (Connection conn = DriverManager.getConnection(URL)) {
			PreparedStatement game_stmt = conn.prepareStatement(save_game);
			game_stmt.setInt(1, gc.placer_wins_amt);
			game_stmt.setInt(2, gc.breaker_wins_amt);
			game_stmt.setString(3, Litestrike.getInstance().mapdata.map_name);
			game_stmt.setInt(4, winner_int);
			game_stmt.setInt(5, gc.game_reference);
			game_stmt.executeUpdate();

			int game_id = conn.prepareStatement("SELECT last_insert_rowid();").executeQuery().getInt("last_insert_rowid()");

			String save_player = "INSERT INTO LsGamesPlayers(player_uuid, game, placed_bombs, broken_bombs, "
					+ "kills, assists, gained_money, spent_money, bought_items, was_winner, damage_dealt, deaths, did_leave, jumps, hits_dealt, bow_shots)"
					+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement player_stmt = conn.prepareStatement(save_player);
			for (PlayerData pd : gc.playerDataManager.getAll()) {
				int is_winner = gc.teams.get_team(pd.player) == winner ? 1 : 0;
				OfflinePlayer oplayer = Bukkit.getOfflinePlayer(pd.player);

				int did_leave_int = pd.did_leave ? 1 : 0;

				player_stmt.setBytes(1, uuid_to_bytes(oplayer));
				player_stmt.setInt(2, game_id);
				player_stmt.setInt(3, pd.plants);
				player_stmt.setInt(4, pd.breaks);
				player_stmt.setInt(5, pd.kills);
				player_stmt.setInt(6, pd.assists);
				player_stmt.setInt(7, pd.getTotalMoneyGained());
				player_stmt.setInt(8, pd.getTotalMoneySpent());
				player_stmt.setBytes(9, get_bought_items(oplayer));
				player_stmt.setInt(10, is_winner);
				player_stmt.setFloat(11, pd.total_damage);
				player_stmt.setInt(12, pd.deaths);
				player_stmt.setInt(13, did_leave_int);
				player_stmt.setInt(14, pd.jumps);
				player_stmt.setInt(15, pd.hits_dealt);
				player_stmt.setInt(16, pd.bow_shots);
				player_stmt.executeUpdate();
			}

			Bukkit.getLogger().info("Successfully wrote data to LsDatabase");
		} catch (SQLException e) {
			Bukkit.getLogger().severe("Failed to save this game to the Litestrike database. "
					+ "The stats of this game will not be recorded.");
			Bukkit.getLogger().severe("Error: " + e.getMessage());
		}

		send_spectator_summary(gc, winner, rp_lines);
	}

	private static void send_spectator_summary(GameController gc, Team winner, List<Component> rp_lines) {
		List<Player> spectators = new ArrayList<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (gc.teams.get_team(p) == null) {
				spectators.add(p);
			}
		}
		if (spectators.isEmpty()) {
			return;
		}

		float total_damage = 0;
		int total_jumps = 0;
		int total_kills = 0;
		int total_deaths = 0;
		int total_assists = 0;
		int total_plants = 0;
		int total_breaks = 0;
		int total_hits = 0;
		int total_bow_shots = 0;
		int total_money_gained = 0;
		int total_money_spent = 0;
		for (PlayerData pd : gc.playerDataManager.getAll()) {
			total_damage += pd.total_damage;
			total_jumps += pd.jumps;
			total_kills += pd.kills;
			total_deaths += pd.deaths;
			total_assists += pd.assists;
			total_plants += pd.plants;
			total_breaks += pd.breaks;
			total_hits += pd.hits_dealt;
			total_bow_shots += pd.bow_shots;
			total_money_gained += pd.getTotalMoneyGained();
			total_money_spent += pd.getTotalMoneySpent();
		}

		Component stat_lines = stat_line("Total damage", "%.1f".formatted(total_damage))
				.append(stat_line("Total jumps", String.valueOf(total_jumps)))
				.append(stat_line("Kills / deaths / assists",
						total_kills + " / " + total_deaths + " / " + total_assists))
				.append(stat_line("Plants / breaks", total_plants + " / " + total_breaks))
				.append(stat_line("Hits / bow shots", total_hits + " / " + total_bow_shots))
				.append(stat_line("Money gained / spent", total_money_gained + " / " + total_money_spent));

		Component winner_text = winner == Team.Placer ? Litestrike.PLACER_TEXT : Litestrike.BREAKER_TEXT;
		Component message = build_spectator_summary(winner_text, rp_lines,
				Litestrike.getInstance().gameConfig.ranked, stat_lines);
		Audience.audience(spectators).sendMessage(message);
	}

	static Component build_spectator_summary(Component winner_text, List<Component> rp_lines, boolean ranked,
			Component stat_lines) {
		Component message = Component.text("-----------------------------\n").color(NamedTextColor.GOLD)
				.append(Component.text(" ɢᴀᴍᴇ sᴜᴍᴍᴀʀʏ\n").color(Litestrike.YELLOW))
				.append(Component.text(" Winner: ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
				.append(winner_text)
				.append(Component.text("\n"))
				.append(stat_lines);

		if (ranked && !rp_lines.isEmpty()) {
			message = message.append(Component.text(" ʀᴘ ᴄʜᴀɴɢᴇs:\n").color(Litestrike.YELLOW));
			for (Component line : rp_lines) {
				message = message.append(line).append(Component.text("\n"));
			}
		}

		return message.append(Component.text("-----------------------------").color(NamedTextColor.GOLD));
	}

	static Component stat_line(String label, String value) {
		return Component.text(" " + label + ": ").color(Litestrike.YELLOW)
				.append(Component.text(value).color(NamedTextColor.WHITE))
				.append(Component.text("\n"));
	}

	private static byte[] get_bought_items(OfflinePlayer p) {
		Shop s = Litestrike.getInstance().game_controller.getShop(p);
		ByteBuffer bb = ByteBuffer.allocate(s.shopLog.size() * 2);
		for (LSItem lsi : s.shopLog) {
			if (lsi == null) {
				bb.putShort((short) 0);
			} else {
				bb.putShort(lsi.id);
			}
		}
		return bb.array();
	}

	private static byte[] uuid_to_bytes(OfflinePlayer p) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		UUID uuid = p.getUniqueId();
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
}

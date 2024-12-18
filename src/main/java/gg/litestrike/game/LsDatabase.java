package gg.litestrike.game;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LsDatabase {

	public static final String URL = "jdbc:sqlite:"+ System.getProperty("user.home")+"/databases/litestrike_db.sql";

	public static void setup_databases() {
		String create_ls_games = "CREATE TABLE IF NOT EXISTS LiteStrikeGames ("
				+ "game_id 			INTEGER PRIMARY KEY,"
				+ "placer_wins 	INTEGER,"
				+ "breaker_wins INTEGER,"
				+ "timestamp 		INTEGER,"
				+ "map 					INTEGER,"
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
				+ "was_winner			INTEGER"
				+ ");";

		try (Connection conn = DriverManager.getConnection(URL)) {
			Statement stmt = conn.createStatement();
			stmt.execute(create_ls_games);
			stmt.execute(create_ls_players);
		} catch (SQLException e) {
			Bukkit.getLogger().severe(e.getMessage());
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				Bukkit.getLogger().severe(ste.toString());
			}
		}
	}

	public static void save_game(Team winner) {
		String save_game = "INSERT INTO LiteStrikeGames(placer_wins, breaker_wins, timestamp, map, winner, game_ref) VALUES(?, ?, unixepoch(), ?, ?, ?)";
		GameController gc = Litestrike.getInstance().game_controller;

		int placer_wins_amt = 0;
		int breaker_wins_amt = 0;
		for (gg.litestrike.game.Team w : gc.round_results) {
			if (w == gg.litestrike.game.Team.Placer) {
				placer_wins_amt += 1;
			} else {
				breaker_wins_amt += 1;
			}
		}
		int winner_int = winner == Team.Placer ? 1 : 0;

		try (Connection conn = DriverManager.getConnection(URL)) {
			PreparedStatement game_stmt = conn.prepareStatement(save_game);
			game_stmt.setInt(1, placer_wins_amt);
			game_stmt.setInt(2, breaker_wins_amt);
			game_stmt.setInt(3, Litestrike.getInstance().mapdata.map_name.hashCode());
			game_stmt.setInt(4, winner_int);
			game_stmt.setInt(5, gc.game_reference);
			game_stmt.executeUpdate();

			int game_id = conn.prepareStatement("SELECT last_insert_rowid();").executeQuery().getInt("last_insert_rowid()");

			String save_player = "INSERT INTO LsGamesPlayers(player_uuid, game, placed_bombs, broken_bombs, "
					+ "kills, assists, gained_money, spent_money, bought_items, was_winner)"
					+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement player_stmt = conn.prepareStatement(save_player);
			for (Player p : Bukkit.getOnlinePlayers()) {
				PlayerData pd = gc.getPlayerData(p);
				int is_winner = gc.teams.get_team(p) == winner ? 1 : 0;

				player_stmt.setBytes(1, uuid_to_bytes(p));
				player_stmt.setInt(2, game_id);
				player_stmt.setInt(3, pd.getPlaced());
				player_stmt.setInt(4, pd.getBroken());
				player_stmt.setInt(5, pd.kills);
				player_stmt.setInt(6, pd.assists);
				player_stmt.setInt(7, pd.getTotalMoneyGained());
				player_stmt.setInt(8, pd.getTotalMoneySpent());
				player_stmt.setBytes(9, get_bought_items(p));
				player_stmt.setInt(10, is_winner);
				player_stmt.executeUpdate();
			}

		} catch (SQLException e) {
			Bukkit.getLogger().severe(e.getMessage());
		}
		Bukkit.getLogger().info("Successfully worte data to LsDatabase");
	}

	private static byte[] get_bought_items(Player p) {
		Shop s = Shop.getShop(p);
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

	private static byte[] uuid_to_bytes(Player p) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		UUID uuid = p.getUniqueId();
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
}

package gg.litestrike.game;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LsDatabase {

	public static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";
	public static final String TEMPORARY_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/lobby_db.sql";

	public static void setup_databases() {
		// we query the games by timestamp, so creating a index for it can improve perf.
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
				+ "was_winner			INTEGER"
				+ ");";

		String create_ls_ranks = "CREATE TABLE IF NOT EXISTS LsRanks ("
				+ "player_uuid 		BLOB UNIQUE,"
				+ "rank						INTEGER,"
				+ "rp							INTEGER"
				+ ");";

		try (Connection conn = DriverManager.getConnection(URL)) {
			Statement stmt = conn.createStatement();
			stmt.execute(create_ls_games);
			stmt.execute(create_ls_players);
			stmt.execute(create_ls_ranks);
		} catch (SQLException e) {
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("continueing without database");
		}
	}

	public static void save_game(Team winner) {
		GameController gc = Litestrike.getInstance().game_controller;

		if (Litestrike.getInstance().mapdata.ranked) {
			Ranking.do_ranking(winner);
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

			Bukkit.getLogger().info("Successfully worte data to LsDatabase");
		} catch (SQLException e) {
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("didnt write data to database");
		}
	}

	public static void writeTemporaryData(Player p, int xp, int money){
		try(Connection conn = DriverManager.getConnection(TEMPORARY_URL)){
			PreparedStatement prepared = conn.prepareStatement("SELECT COUNT(*) AS count FROM TemporaryData WHERE player_uuid = ?;");
			prepared.setBytes(1,uuid_to_bytes(p));
			String query;

			if(prepared.executeQuery().getInt("count") <= 0){
				query = "INSERT INTO TemporaryData(player_uuid, xp_amount, money_amount) VALUES (?, ?, ?);";
				PreparedStatement prep = conn.prepareStatement(query);
				prep.setBytes(1, uuid_to_bytes(p));
				prep.setInt(2, xp);
				prep.setInt(3, money);
				prep.executeUpdate();
			}else{
				query = "UPDATE TemporaryData SET xp_amount = ?, money_amount = ? WHERE player_uuid = ?";
				PreparedStatement prep_state = conn.prepareStatement("SELECT * FROM TemporaryData WHERE player_uuid = ?;");
				prep_state.setBytes(1,uuid_to_bytes(p));
				ResultSet set = prep_state.executeQuery();
				PreparedStatement prep = conn.prepareStatement(query);
				prep.setBytes(1, uuid_to_bytes(p));
				prep.setInt(2, xp + set.getInt("xp_amount"));
				prep.setInt(3, money + set.getInt("money_amount"));
				prep.executeUpdate();
			}
		}catch(SQLException e){
			Bukkit.getLogger().warning(e.getMessage());
			Bukkit.getLogger().warning("didnt write data to temporary database");
		}
	}

	private static byte[] get_bought_items(Player p) {
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

	private static byte[] uuid_to_bytes(Player p) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		UUID uuid = p.getUniqueId();
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
}

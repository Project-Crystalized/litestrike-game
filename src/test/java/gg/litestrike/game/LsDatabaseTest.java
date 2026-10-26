package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

public class LsDatabaseTest {

	private static final List<String> NEW_COLUMNS = List.of("damage_dealt", "deaths", "did_leave", "jumps", "hits_dealt",
			"bow_shots");

	private static final UUID PLAYER_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		LsDatabase.setDatabaseUrlForTests("jdbc:sqlite:" + tempDir.resolve("litestrike_test.sql").toString());
		MockBukkit.mock();
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void setup_creates_all_tables_with_current_columns() throws SQLException {
		LsDatabase.setup_databases();
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			assertTrue(tableExists(conn, "LiteStrikeGames"));
			assertTrue(tableExists(conn, "LsGamesPlayers"));
			assertTrue(tableExists(conn, "LsRanks"));
			assertTrue(getColumns(conn, "LsGamesPlayers").containsAll(NEW_COLUMNS));
			assertTrue(getColumns(conn, "LsRanks").contains("last_ranked"));
			assertTrue(isNotNull(conn, "LsRanks", "last_ranked"));
		}
	}

	@Test
	void setup_is_idempotent() throws SQLException {
		LsDatabase.setup_databases();
		LsDatabase.setup_databases();
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			assertTrue(tableExists(conn, "LiteStrikeGames"));
			assertTrue(getColumns(conn, "LsGamesPlayers").containsAll(NEW_COLUMNS));
		}
	}

	@Test
	void old_databases_get_missing_columns_added() throws SQLException {
		createOldSchema();
		LsDatabase.setup_databases();
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			assertTrue(tableExists(conn, "LiteStrikeGames"));
			assertTrue(tableExists(conn, "LsRanks"));
			assertTrue(getColumns(conn, "LsGamesPlayers").containsAll(NEW_COLUMNS));
			assertTrue(getColumns(conn, "LsRanks").contains("last_ranked"));
			assertTrue(isNotNull(conn, "LsRanks", "last_ranked"));
		}
	}

	@Test
	void old_rank_rows_get_last_ranked_initialized() throws SQLException {
		createOldSchema();
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO LsRanks (player_uuid, rank, rp) VALUES (?, ?, ?);");
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(PLAYER_UUID));
			ps.setInt(2, 6);
			ps.setInt(3, 1200);
			ps.executeUpdate();
		}

		LsDatabase.setup_databases();

		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			long lastRanked = getLastRanked(conn, PLAYER_UUID);
			assertTrue(lastRanked > 0);
			assertEquals(1200, getRp(conn, PLAYER_UUID));
		}
	}

	private void createOldSchema() throws SQLException {
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL)) {
			Statement stmt = conn.createStatement();
			stmt.execute("CREATE TABLE LiteStrikeGames ("
					+ "game_id INTEGER PRIMARY KEY,"
					+ "placer_wins INTEGER,"
					+ "breaker_wins INTEGER,"
					+ "timestamp INTEGER,"
					+ "map STRING,"
					+ "winner INTEGER,"
					+ "game_ref INTEGER);");
			stmt.execute("CREATE TABLE LsGamesPlayers ("
					+ "player_uuid BLOB,"
					+ "game INTEGER,"
					+ "placed_bombs INTEGER,"
					+ "broken_bombs INTEGER,"
					+ "kills INTEGER,"
					+ "assists INTEGER,"
					+ "gained_money INTEGER,"
					+ "spent_money INTEGER,"
					+ "bought_items BLOB,"
					+ "was_winner INTEGER);");
			stmt.execute("CREATE TABLE LsRanks (player_uuid BLOB UNIQUE, rank INTEGER, rp INTEGER);");
		}
	}

	private static boolean tableExists(Connection conn, String table) throws SQLException {
		try (ResultSet rs = conn.createStatement().executeQuery(
				"SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "';")) {
			return rs.next();
		}
	}

	private static Set<String> getColumns(Connection conn, String table) throws SQLException {
		Set<String> columns = new HashSet<>();
		try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(" + table + ");")) {
			while (rs.next()) {
				columns.add(rs.getString("name"));
			}
		}
		return columns;
	}

	private static boolean isNotNull(Connection conn, String table, String column) throws SQLException {
		try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(" + table + ");")) {
			while (rs.next()) {
				if (rs.getString("name").equals(column)) {
					return rs.getInt("notnull") == 1;
				}
			}
		}
		return false;
	}

	private static long getLastRanked(Connection conn, UUID uuid) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("SELECT last_ranked FROM LsRanks WHERE player_uuid = ?;")) {
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(uuid));
			try (ResultSet rs = ps.executeQuery()) {
				assertTrue(rs.next());
				return rs.getLong("last_ranked");
			}
		}
	}

	private static int getRp(Connection conn, UUID uuid) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("SELECT rp FROM LsRanks WHERE player_uuid = ?;")) {
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(uuid));
			try (ResultSet rs = ps.executeQuery()) {
				assertTrue(rs.next());
				return rs.getInt("rp");
			}
		}
	}
}
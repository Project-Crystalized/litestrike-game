package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

public class LsDatabaseTest {

	private static final List<String> NEW_COLUMNS = List.of("damage_dealt", "deaths", "did_leave", "jumps", "hits_dealt",
			"bow_shots");

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
}
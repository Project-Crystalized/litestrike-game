package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

public class RankDecayTest {

	private static final long NOW = 1_800_000_000L;
	private static final long DAY = 24 * 60 * 60;

	private static final UUID PLAYER = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID OTHER_PLAYER = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		LsDatabase.setDatabaseUrlForTests("jdbc:sqlite:" + tempDir.resolve("rank_decay_test.sql").toString());
		MockBukkit.mock();
		LsDatabase.setup_databases();
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void recent_player_is_not_decayed() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 20 * DAY);

		assertEquals(0, Ranking.runDecaySweep(NOW, false));
		assertEquals(1200, getRp(PLAYER));
		assertEquals(NOW - 20 * DAY, getLastRanked(PLAYER));
	}

	@Test
	void exactly_at_grace_is_decayed() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 21 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(1140, getRp(PLAYER));
		assertEquals(NOW, getLastRanked(PLAYER));
	}

	@Test
	void decay_amount_and_timestamp_are_updated() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 22 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(1140, getRp(PLAYER));
		assertEquals(Ranking.rankForRp(1140), getRank(PLAYER));
		assertEquals(NOW, getLastRanked(PLAYER));
	}

	@Test
	void floor_clamps_rp() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(950), 950, NOW - 22 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(900, getRp(PLAYER));
		assertEquals(Ranking.rankForRp(900), getRank(PLAYER));
	}

	@Test
	void player_at_floor_is_not_decayed() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(900), 900, NOW - 30 * DAY);

		assertEquals(0, Ranking.runDecaySweep(NOW, false));
		assertEquals(900, getRp(PLAYER));
		assertEquals(NOW - 30 * DAY, getLastRanked(PLAYER));
	}

	@Test
	void player_below_floor_is_not_decayed() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(890), 890, NOW - 30 * DAY);

		assertEquals(0, Ranking.runDecaySweep(NOW, false));
		assertEquals(890, getRp(PLAYER));
	}

	@Test
	void zero_timestamp_is_not_decayed() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1500), 1500, 0);

		assertEquals(0, Ranking.runDecaySweep(NOW, false));
		assertEquals(1500, getRp(PLAYER));
	}

	@Test
	void only_inactive_players_are_decayed() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 22 * DAY);
		insertRank(OTHER_PLAYER, Ranking.rankForRp(1300), 1300, NOW - 2 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(1140, getRp(PLAYER));
		assertEquals(1300, getRp(OTHER_PLAYER));
	}

	@Test
	void running_the_sweep_twice_does_not_decay_twice() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 22 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(1140, getRp(PLAYER));

		assertEquals(0, Ranking.runDecaySweep(NOW, false));
		assertEquals(1140, getRp(PLAYER));
	}

	@Test
	void a_later_sweep_decays_again() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 22 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(1140, getRp(PLAYER));

		assertEquals(1, Ranking.runDecaySweep(NOW + 21 * DAY, false));
		assertEquals(1080, getRp(PLAYER));
	}

	@Test
	void dry_run_does_not_write() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1200), 1200, NOW - 22 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, true));
		assertEquals(1200, getRp(PLAYER));
		assertEquals(NOW - 22 * DAY, getLastRanked(PLAYER));
	}

	@Test
	void sweep_repairs_a_stale_rank() throws SQLException {
		insertRank(PLAYER, 2, 1200, NOW - 2 * DAY);

		assertEquals(0, Ranking.runDecaySweep(NOW, false));
		assertEquals(1200, getRp(PLAYER));
		assertEquals(Ranking.rankForRp(1200), getRank(PLAYER));
	}

	@Test
	void decay_can_demote_a_player() throws SQLException {
		insertRank(PLAYER, Ranking.rankForRp(1004), 1004, NOW - 22 * DAY);

		assertEquals(1, Ranking.runDecaySweep(NOW, false));
		assertEquals(944, getRp(PLAYER));
		assertEquals(5, getRank(PLAYER));
	}

	private void insertRank(UUID uuid, int rank, int rp, long lastRanked) throws SQLException {
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL);
				PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO LsRanks (player_uuid, rank, rp, last_ranked) VALUES (?, ?, ?, ?);")) {
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(uuid));
			ps.setInt(2, rank);
			ps.setInt(3, rp);
			ps.setLong(4, lastRanked);
			ps.executeUpdate();
		}
	}

	private int getRp(UUID uuid) throws SQLException {
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL);
				PreparedStatement ps = conn.prepareStatement("SELECT rp FROM LsRanks WHERE player_uuid = ?;")) {
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(uuid));
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt("rp") : -1;
			}
		}
	}

	private int getRank(UUID uuid) throws SQLException {
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL);
				PreparedStatement ps = conn.prepareStatement("SELECT rank FROM LsRanks WHERE player_uuid = ?;")) {
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(uuid));
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt("rank") : -1;
			}
		}
	}

	private long getLastRanked(UUID uuid) throws SQLException {
		try (Connection conn = DriverManager.getConnection(LsDatabase.URL);
				PreparedStatement ps = conn.prepareStatement("SELECT last_ranked FROM LsRanks WHERE player_uuid = ?;")) {
			ps.setBytes(1, PlayerRankedData.uuid_to_bytes(uuid));
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getLong("last_ranked") : -1;
			}
		}
	}
}

package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class RankingTest {

	@Test
	void even_game_swings_22() {
		assertEquals(22, Ranking.ratingChange(1000, 1000, true, 0.0));
		assertEquals(-22, Ranking.ratingChange(1000, 1000, false, 0.0));
	}

	@Test
	void upset_win_swings_hard() {
		// 400 points underdog: raw change is +40
		assertEquals(40, Ranking.ratingChange(1000, 1400, true, 0.0));
		// losing as the underdog barely stings
		assertEquals(-4, Ranking.ratingChange(1000, 1400, false, 0.0));
		// carry bonus on top of an upset still clamps to MAX_SWING
		assertEquals(40, Ranking.ratingChange(1000, 1400, true, 1.0));
	}

	@Test
	void solo_carry_flips_a_loss() {
		assertEquals(3, Ranking.ratingChange(1000, 1000, false, 1.0));
		assertEquals(-3, Ranking.ratingChange(1000, 1000, true, -1.0));
	}

	@Test
	void performance_is_damage_share_plus_objectives() {
		assertEquals(1.0, Ranking.performanceScore(2000, 1000, 0, 0, 0), 1e-9);
		assertEquals(-1.0, Ranking.performanceScore(0, 1000, 0, 0, 0), 1e-9);
		assertEquals(0.0, Ranking.performanceScore(1000, 1000, 0, 0, 0), 1e-9);
		assertEquals(0.4, Ranking.performanceScore(1000, 1000, 2, 2, 2), 1e-9);
		assertEquals(0.2, Ranking.performanceScore(1000, 1000, 2, 0, 0.5f), 1e-9);
		assertEquals(1.0, Ranking.performanceScore(5000, 1000, 5, 5, 0), 1e-9);
	}

	@Test
	void rank_bands() {
		assertEquals(10, Ranking.rankForRating(3000));
		assertEquals(10, Ranking.rankForRating(2500));
		assertEquals(9, Ranking.rankForRating(2499));
		assertEquals(9, Ranking.rankForRating(2200));
		assertEquals(5, Ranking.rankForRating(1000));
		assertEquals(4, Ranking.rankForRating(999));
		assertEquals(4, Ranking.rankForRating(700));
		assertEquals(3, Ranking.rankForRating(699));
		assertEquals(3, Ranking.rankForRating(400));
		assertEquals(2, Ranking.rankForRating(399));
		assertEquals(2, Ranking.rankForRating(100));
	}

	@Test
	void uuid_to_bytes_is_sixteen_bytes_and_roundtrips() {
		UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		byte[] bytes = PlayerRankedData.uuid_to_bytes(uuid);
		assertEquals(16, bytes.length);
		assertEquals(uuid, uuidFromBytes(bytes));
	}

	@Test
	void uuid_to_bytes_splits_msb_lsb_correctly() {
		UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		byte[] bytes = PlayerRankedData.uuid_to_bytes(uuid);
		byte[] msb = new byte[8];
		byte[] lsb = new byte[8];
		System.arraycopy(bytes, 0, msb, 0, 8);
		System.arraycopy(bytes, 8, lsb, 0, 8);
		assertArrayEquals(longToBytes(uuid.getMostSignificantBits()), msb);
		assertArrayEquals(longToBytes(uuid.getLeastSignificantBits()), lsb);
	}

	private static UUID uuidFromBytes(byte[] bytes) {
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(16);
		bb.put(bytes);
		bb.flip();
		return new UUID(bb.getLong(), bb.getLong());
	}

	private static byte[] longToBytes(long value) {
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(8);
		bb.putLong(value);
		return bb.array();
	}
}

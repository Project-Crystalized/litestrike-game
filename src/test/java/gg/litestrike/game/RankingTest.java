package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class RankingTest {

	@Test
	void win_points_by_rank() {
		assertEquals(7, Ranking.get_win_loss_points(true, 1));
		assertEquals(7, Ranking.get_win_loss_points(true, 2));
		assertEquals(7, Ranking.get_win_loss_points(true, 3));
		assertEquals(6, Ranking.get_win_loss_points(true, 4));
		assertEquals(6, Ranking.get_win_loss_points(true, 5));
		assertEquals(5, Ranking.get_win_loss_points(true, 6));
		assertEquals(5, Ranking.get_win_loss_points(true, 7));
		assertEquals(4, Ranking.get_win_loss_points(true, 8));
		assertEquals(4, Ranking.get_win_loss_points(true, 9));
		assertEquals(3, Ranking.get_win_loss_points(true, 10));
	}

	@Test
	void loss_points_by_rank() {
		assertEquals(-6, Ranking.get_win_loss_points(false, 1));
		assertEquals(-6, Ranking.get_win_loss_points(false, 9));
		assertEquals(-7, Ranking.get_win_loss_points(false, 10));
	}

	@Test
	void rank_min_rp_by_rank() {
		assertEquals(0, Ranking.get_rank_min_rp(2));
		assertEquals(250, Ranking.get_rank_min_rp(3));
		assertEquals(500, Ranking.get_rank_min_rp(4));
		assertEquals(750, Ranking.get_rank_min_rp(5));
		assertEquals(1000, Ranking.get_rank_min_rp(6));
		assertEquals(1250, Ranking.get_rank_min_rp(7));
		assertEquals(1500, Ranking.get_rank_min_rp(8));
		assertEquals(1750, Ranking.get_rank_min_rp(9));
		assertEquals(2000, Ranking.get_rank_min_rp(10));
		assertEquals(0, Ranking.get_rank_min_rp(1));
		assertEquals(0, Ranking.get_rank_min_rp(99));
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
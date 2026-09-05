package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PlayerDataTest {

	@Test
	void score_is_weighted_sum() {
		// kills * 0.34 + assists * 0.16 + team_objectives * 0.24
		assertEquals(4.92, PlayerData.calculateScore(10, 5, 3), 1e-9);
		assertEquals(0.0, PlayerData.calculateScore(0, 0, 0), 1e-9);
		assertEquals(3.4, PlayerData.calculateScore(10, 0, 0), 1e-9);
		assertEquals(2.4, PlayerData.calculateScore(0, 0, 10), 1e-9);
		assertEquals(1.6, PlayerData.calculateScore(0, 10, 0), 1e-9);
	}
}
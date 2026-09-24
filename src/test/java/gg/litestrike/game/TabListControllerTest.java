package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import gg.crystalized.lobby.Leaderboards;

public class TabListControllerTest {

	@Test
	void make_two_digits_left_pads_with_dots() {
		assertEquals("1000", TabListController.makeTwoDigits(1000, 4));
		assertEquals(".....".repeat(1) + "999", TabListController.makeTwoDigits(999, 4));
		assertEquals(".....".repeat(5) + "42", TabListController.makeTwoDigits(42, 7));
		assertEquals(".....".repeat(5) + "123", TabListController.makeTwoDigits(123, 8));
		assertEquals("5", TabListController.makeTwoDigits(5, 1));
	}

	@Test
	void make_two_digits_does_not_trim_overflow() {
		assertEquals("123456", TabListController.makeTwoDigits(123456, 4));
		assertEquals("1234", TabListController.makeTwoDigits(1234, 4));
	}

	@Test
	void balance_pairs_of_known_glyph_widths() {
		// 'a' and 'Z' both have glyph width 5, plus 1 for the gap between the two chars
		assertEquals(2, Leaderboards.balance("a"));
		assertEquals(5, Leaderboards.balance("aZ"));
		assertEquals(2, Leaderboards.balance("Z"));
	}
}
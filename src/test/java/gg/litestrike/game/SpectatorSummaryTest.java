package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class SpectatorSummaryTest {

	private static String plain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	@Test
	void rp_line_shows_gained_points() {
		Component line = Ranking.format_rp_line("Alice", Team.Placer, 500, 518, false);
		assertEquals(" Alice: +18 (500 -> 518)", plain(line));
	}

	@Test
	void rp_line_shows_lost_points() {
		Component line = Ranking.format_rp_line("Bob", Team.Breaker, 1200, 1188, false);
		assertEquals(" Bob: -12 (1200 -> 1188)", plain(line));
	}

	@Test
	void rp_line_marks_players_that_left() {
		Component line = Ranking.format_rp_line("Carol", Team.Placer, 500, 480, true);
		assertEquals(" Carol: -20 (500 -> 480) (left)", plain(line));
	}

	@Test
	void summary_includes_rp_changes_when_ranked() {
		List<Component> rp_lines = List.of(
				Ranking.format_rp_line("Alice", Team.Placer, 500, 518, false),
				Ranking.format_rp_line("Bob", Team.Breaker, 1200, 1188, false));
		Component stat_lines = LsDatabase.stat_line("Total damage", "1234.5")
				.append(LsDatabase.stat_line("Total jumps", "87"));

		String summary = plain(LsDatabase.build_spectator_summary(
				Component.text("Placers"), rp_lines, true, stat_lines));

		assertTrue(summary.contains("ɢᴀᴍᴇ sᴜᴍᴍᴀʀʏ"));
		assertTrue(summary.contains("Winner: Placers"));
		assertTrue(summary.contains("Total damage: 1234.5"));
		assertTrue(summary.contains("Total jumps: 87"));
		assertTrue(summary.contains("ʀᴘ ᴄʜᴀɴɢᴇs:"));
		assertTrue(summary.contains("Alice: +18 (500 -> 518)"));
		assertTrue(summary.contains("Bob: -12 (1200 -> 1188)"));
	}

	@Test
	void summary_omits_rp_changes_when_unranked() {
		List<Component> rp_lines = List.of(
				Ranking.format_rp_line("Alice", Team.Placer, 500, 518, false));
		Component stat_lines = LsDatabase.stat_line("Total damage", "1234.5");

		String summary = plain(LsDatabase.build_spectator_summary(
				Component.text("Breakers"), rp_lines, false, stat_lines));

		assertTrue(summary.contains("Total damage: 1234.5"));
		assertFalse(summary.contains("ʀᴘ ᴄʜᴀɴɢᴇs:"));
		assertFalse(summary.contains("Alice"));
	}

	@Test
	void summary_omits_rp_changes_when_there_are_no_ranked_players() {
		Component stat_lines = LsDatabase.stat_line("Total jumps", "87");

		String summary = plain(LsDatabase.build_spectator_summary(
				Component.text("Placers"), List.of(), true, stat_lines));

		assertTrue(summary.contains("Total jumps: 87"));
		assertFalse(summary.contains("ʀᴘ ᴄʜᴀɴɢᴇs:"));
	}
}

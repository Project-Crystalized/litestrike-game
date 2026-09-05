package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

public class GameConfigTest {

	private static GameConfig fresh() {
		return new GameConfig(new YamlConfiguration());
	}

	@Test
	void fresh_config_has_default_values() {
		GameConfig gc = fresh();
		assertEquals(28, GameConfig.Setting.values().length);
		assertEquals(6, gc.playersToStart);
		assertEquals(8, gc.playerCap);
		assertEquals(4, gc.switchRound);
		assertEquals(20 * 23, gc.preRoundTime);
		assertEquals(180 * 20, gc.runningTime);
		assertEquals(5 * 20, gc.postRoundTime);
		assertEquals(20 * 12, gc.finishTime);
		assertEquals(20 * 5, gc.plantTime);
		assertEquals(20 * 7, gc.breakTime);
		assertEquals(400, gc.killMoney);
		assertEquals(50, gc.assistMoney);
		assertEquals(700, gc.winRoundMoney);
		assertEquals(400, gc.loseRoundMoney);
		assertEquals(1000, gc.nextRoundMoney);
		assertEquals(5000, gc.lastRoundMoney);
		assertEquals(0, gc.plantMoney);
		assertEquals(0, gc.breakMoney);
		assertEquals(20 * 2, gc.launchPadProtection);
		assertEquals(20 * 4, gc.launchPadHighPingProtection);
		assertEquals(20 * 6, gc.leviPadProtection);
		assertEquals(20 * 7, gc.autoJumpPadProtection);
		assertEquals(20 * 7, gc.jumpPadProtection);
		assertFalse(gc.freeShop);
		assertFalse(gc.fastGame);
		assertFalse(gc.manualTeamsEnabled);
		assertFalse(gc.ranked);
		assertTrue(gc.placers.isEmpty());
		assertTrue(gc.breakers.isEmpty());
	}

	@Test
	void defaults_restores_original_values() {
		GameConfig gc = fresh();
		gc.set("killMoney", "9999");
		gc.set("fastGame", "true");
		gc.set("placers", "alice,bob");
		gc.defaults();
		assertEquals(400, gc.killMoney);
		assertFalse(gc.fastGame);
		assertEquals(20 * 23, gc.preRoundTime);
		assertTrue(gc.placers.isEmpty());
	}

	@Test
	void fast_game_enable_sets_fast_round_times() {
		GameConfig gc = fresh();
		assertTrue(gc.set("fastGame", "true"));
		assertTrue(gc.fastGame);
		assertEquals(20 * 5, gc.preRoundTime);
		assertEquals(1 * 20, gc.postRoundTime);
	}

	@Test
	void fast_game_disable_restores_round_times() {
		GameConfig gc = fresh();
		gc.set("fastGame", "true");
		assertTrue(gc.set("fastGame", "false"));
		assertFalse(gc.fastGame);
		assertEquals(20 * 23, gc.preRoundTime);
		assertEquals(5 * 20, gc.postRoundTime);
	}

	@Test
	void constructor_applies_fast_game_from_config() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("fast-game", true);
		GameConfig gc = new GameConfig(config);
		assertTrue(gc.fastGame);
		assertEquals(20 * 5, gc.preRoundTime);
	}

	@Test
	void constructor_reads_team_config() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("teams.enable", true);
		config.set("teams.placers", List.of("alice", "bob"));
		config.set("teams.breakers", List.of("carol"));
		GameConfig gc = new GameConfig(config);
		assertTrue(gc.manualTeamsEnabled);
		assertEquals(List.of("alice", "bob"), gc.placers);
		assertEquals(List.of("carol"), gc.breakers);
	}

	@Test
	void set_get_roundtrip_for_int_settings() {
		GameConfig gc = fresh();
		for (GameConfig.Setting setting : GameConfig.Setting.values()) {
			if (setting.isBoolean() || setting == GameConfig.Setting.placers || setting == GameConfig.Setting.breakers) {
				continue;
			}
			assertTrue(gc.set(setting.toString(), "1234"), "failed to set " + setting);
			assertEquals("1234", gc.get(setting.toString()), "get mismatch for " + setting);
		}
	}

	@Test
	void set_get_roundtrip_for_boolean_settings() {
		GameConfig gc = fresh();
		for (GameConfig.Setting setting : GameConfig.Setting.values()) {
			if (!setting.isBoolean()) {
				continue;
			}
			assertTrue(gc.set(setting.toString(), "true"), "failed to set " + setting);
			assertEquals("true", gc.get(setting.toString()), "get mismatch for " + setting);
		}
	}

	@Test
	void set_get_roundtrip_for_player_lists() {
		GameConfig gc = fresh();
		assertTrue(gc.set("placers", "alice, bob ,, carol"));
		assertTrue(gc.set("breakers", "dave"));
		assertEquals("alice,bob,carol", gc.get("placers"));
		assertEquals("dave", gc.get("breakers"));
		assertEquals(List.of("alice", "bob", "carol"), gc.placers);
	}

	@Test
	void unknown_key_is_rejected() {
		GameConfig gc = fresh();
		assertFalse(gc.set("notASetting", "1"));
		assertNull(gc.get("notASetting"));
	}

	@Test
	void parse_player_list_trims_and_drops_empties() {
		assertEquals(List.of("a", "b", "c"), GameConfig.parsePlayerList("  a , b ,, c  "));
		assertEquals(List.of(), GameConfig.parsePlayerList(""));
		assertEquals(List.of(), GameConfig.parsePlayerList(" , , "));
	}

	@Test
	void is_boolean_flags_only_the_four_boolean_settings() {
		assertTrue(GameConfig.Setting.fastGame.isBoolean());
		assertTrue(GameConfig.Setting.freeShop.isBoolean());
		assertTrue(GameConfig.Setting.manualTeamsEnabled.isBoolean());
		assertTrue(GameConfig.Setting.ranked.isBoolean());
		assertFalse(GameConfig.Setting.killMoney.isBoolean());
		assertFalse(GameConfig.Setting.placers.isBoolean());
	}
}
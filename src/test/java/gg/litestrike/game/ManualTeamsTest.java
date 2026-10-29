package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ManualTeamsTest {
	private GameConfig gameConfig;
	private ManualTeams manualTeams;

	@BeforeEach
	void setUp() {
		gameConfig = new GameConfig(new YamlConfiguration());
		manualTeams = new ManualTeams(gameConfig);
	}

	@Test
	void adding_to_breakers_removes_from_placers() {
		gameConfig.placers.add("alice");

		assertTrue(manualTeams.addToTeam("breakers", "alice"));

		assertTrue(gameConfig.placers.isEmpty());
		assertEquals(List.of("alice"), gameConfig.breakers);
		assertTrue(gameConfig.manualTeamsEnabled);
	}

	@Test
	void adding_to_placers_removes_from_breakers() {
		gameConfig.breakers.add("alice");

		assertTrue(manualTeams.addToTeam("placers", "alice"));

		assertTrue(gameConfig.breakers.isEmpty());
		assertEquals(List.of("alice"), gameConfig.placers);
		assertTrue(gameConfig.manualTeamsEnabled);
	}

	@Test
	void adding_same_player_twice_does_not_duplicate() {
		assertTrue(manualTeams.addToTeam("breakers", "alice"));
		assertTrue(manualTeams.addToTeam("breakers", "alice"));

		assertEquals(List.of("alice"), gameConfig.breakers);
	}

	@Test
	void unknown_team_is_rejected() {
		assertFalse(manualTeams.addToTeam("spectators", "alice"));

		assertFalse(gameConfig.manualTeamsEnabled);
		assertTrue(gameConfig.placers.isEmpty());
		assertTrue(gameConfig.breakers.isEmpty());
	}
}

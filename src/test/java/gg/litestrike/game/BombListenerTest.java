package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockbukkit.mockbukkit.MockBukkit;

public class BombListenerTest {

	@BeforeEach
	void setUp() {
		MockBukkit.mock();
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void iron_trapdoor_stays_usable_for_bow_charging() {
		assertFalse(BombListener.isInteractable(Material.IRON_TRAPDOOR));
	}

	@Test
	void wooden_trapdoors_are_blocked() {
		assertTrue(BombListener.isInteractable(Material.OAK_TRAPDOOR));
		assertTrue(BombListener.isInteractable(Material.SPRUCE_TRAPDOOR));
		assertTrue(BombListener.isInteractable(Material.COPPER_TRAPDOOR));
	}

	@Test
	void fence_gates_are_blocked() {
		assertTrue(BombListener.isInteractable(Material.OAK_FENCE_GATE));
	}

	@Test
	void doors_and_containers_are_blocked() {
		assertTrue(BombListener.isInteractable(Material.OAK_DOOR));
		assertTrue(BombListener.isInteractable(Material.CHEST));
		assertTrue(BombListener.isInteractable(Material.OAK_BUTTON));
	}

	@Test
	void plain_blocks_and_stairs_stay_usable() {
		assertFalse(BombListener.isInteractable(Material.STONE));
		assertFalse(BombListener.isInteractable(Material.OAK_STAIRS));
		assertFalse(BombListener.isInteractable(Material.OAK_FENCE));
	}
}

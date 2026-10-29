package gg.litestrike.game.mapfeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class FallProtectionTrackerTest {

	private final UUID player = UUID.randomUUID();

	@Test
	void protection_expires_after_duration() {
		FallProtectionTracker tracker = new FallProtectionTracker();
		tracker.protect(player, 100, 40);

		assertTrue(tracker.isProtected(player, 139));
		assertFalse(tracker.isProtected(player, 140));
	}

	@Test
	void later_protection_extends_instead_of_being_cut_short() {
		FallProtectionTracker tracker = new FallProtectionTracker();
		tracker.protect(player, 100, 40); // until 140
		tracker.protect(player, 120, 140); // until 260

		assertTrue(tracker.isProtected(player, 200));
		assertTrue(tracker.isProtected(player, 259));
		assertFalse(tracker.isProtected(player, 260));
	}

	@Test
	void shorter_later_protection_does_not_reduce_existing_expiry() {
		FallProtectionTracker tracker = new FallProtectionTracker();
		tracker.protect(player, 100, 140); // until 240
		tracker.protect(player, 120, 40); // would be until 160

		assertTrue(tracker.isProtected(player, 200));
		assertFalse(tracker.isProtected(player, 240));
	}

	@Test
	void consume_removes_active_protection() {
		FallProtectionTracker tracker = new FallProtectionTracker();
		tracker.protect(player, 100, 40);

		assertTrue(tracker.consume(player, 120));
		assertFalse(tracker.isProtected(player, 120));
		assertFalse(tracker.consume(player, 120));
	}

	@Test
	void consume_does_not_cancel_expired_protection() {
		FallProtectionTracker tracker = new FallProtectionTracker();
		tracker.protect(player, 100, 40);

		assertFalse(tracker.consume(player, 140));
		assertEquals(0, tracker.size());
	}

	@Test
	void remove_expired_drops_old_entries() {
		FallProtectionTracker tracker = new FallProtectionTracker();
		tracker.protect(player, 100, 40);

		tracker.removeExpired(140);

		assertEquals(0, tracker.size());
		assertFalse(tracker.isProtected(player, 140));
	}
}

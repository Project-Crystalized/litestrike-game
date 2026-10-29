package gg.litestrike.game.mapfeatures;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks until when a player is protected from fall damage.
 * <p>
 * Multiple pads can boost the same player before they land. Keeping the latest
 * expiry (instead of one timer per boost) means a later boost extends the
 * protection rather than being cut short by an earlier boost's timer.
 */
final class FallProtectionTracker {
	private final Map<UUID, Integer> protected_until = new HashMap<>();

	void protect(UUID uuid, int now, int duration) {
		int until = now + Math.max(0, duration);
		protected_until.merge(uuid, until, Math::max);
	}

	boolean isProtected(UUID uuid, int now) {
		Integer until = protected_until.get(uuid);
		return until != null && until > now;
	}

	/**
	 * Removes the player's protection and reports whether it was still active.
	 * This is used when fall damage happens: the first landing consumes the
	 * protection, even if the stored expiry has already passed.
	 */
	boolean consume(UUID uuid, int now) {
		Integer until = protected_until.remove(uuid);
		return until != null && until > now;
	}

	void removeExpired(int now) {
		protected_until.entrySet().removeIf(entry -> entry.getValue() <= now);
	}

	int size() {
		return protected_until.size();
	}
}

package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SoundEffectsTest {

	@Test
	void kill_pitch_follows_streak_ladder() {
		assertEquals(1.0f, SoundEffects.killPitch(1), 1e-6f);
		assertEquals(1.1225f, SoundEffects.killPitch(2), 1e-6f);
		assertEquals(1.2599f, SoundEffects.killPitch(3), 1e-6f);
		assertEquals(1.4983f, SoundEffects.killPitch(4), 1e-6f);
		assertEquals(1.6818f, SoundEffects.killPitch(5), 1e-6f);
	}

	@Test
	void kill_pitch_clamps_out_of_range_streaks() {
		assertEquals(1.0f, SoundEffects.killPitch(0), 1e-6f);
		assertEquals(1.0f, SoundEffects.killPitch(-3), 1e-6f);
		assertEquals(1.6818f, SoundEffects.killPitch(6), 1e-6f);
		assertEquals(1.6818f, SoundEffects.killPitch(99), 1e-6f);
	}
}

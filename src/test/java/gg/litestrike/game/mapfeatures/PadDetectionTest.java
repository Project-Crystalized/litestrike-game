package gg.litestrike.game.mapfeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class PadDetectionTest {

	private static Set<String> columns(double x, double z) {
		Set<String> out = new HashSet<>();
		for (int[] c : PadDetection.footprintColumns(x, z)) {
			out.add(c[0] + "," + c[1]);
		}
		return out;
	}

	@Test
	void centeredPlayerOverlapsOneColumn() {
		assertEquals(Set.of("0,0"), columns(0.5, 0.5));
	}

	@Test
	void playerStraddlingEastEdgeIncludesNeighborColumn() {
		// hitbox corner reaches past x = 1.0 while feet are still in column 0
		Set<String> cols = columns(0.75, 0.5);
		assertEquals(Set.of("0,0", "1,0"), cols);
	}

	@Test
	void playerStraddlingWestEdgeIncludesNegativeColumn() {
		Set<String> cols = columns(0.25, 0.5);
		assertEquals(Set.of("-1,0", "0,0"), cols);
	}

	@Test
	void playerStraddlingCornerOverlapsFourColumns() {
		assertEquals(Set.of("0,0", "1,0", "0,1", "1,1"), columns(0.75, 0.75));
	}

	@Test
	void hitboxEdgeExactlyOnBlockBoundaryStaysInOneColumn() {
		// feet at x = 0.3 -> corner exactly on the boundary at 0/1
		assertEquals(Set.of("0,0"), columns(0.3, 0.5));
		// corner exactly on the boundary from the other side
		assertEquals(Set.of("0,0", "1,0"), columns(0.7, 0.5));
	}

	@Test
	void negativeCoordinatesFloorCorrectly() {
		assertEquals(Set.of("-1,0", "0,0"), columns(-0.2, 0.5));
		assertEquals(Set.of("-1,-1", "0,-1", "-1,0", "0,0"), columns(-0.2, -0.2));
	}

	@Test
	void sampleYMatchesOldGetRelativeDownBehaviour() {
		// standing on top of a block at y=64 (feet at 65.0) -> pad check at 64
		assertEquals(64, PadDetection.sampleY(65.0));
		// mid-air within the block above the pad -> still 64 (same as getRelative(DOWN))
		assertEquals(64, PadDetection.sampleY(65.5));
		// one block higher -> pad not detected
		assertEquals(65, PadDetection.sampleY(66.0));
		// world floor at y 0 -> -1
		assertEquals(-1, PadDetection.sampleY(0.0));
		// negative y floors down, never truncates up
		assertEquals(-65, PadDetection.sampleY(-64.0));
	}

	@Test
	void footprintAlwaysCoversFeetBlockColumn() {
		// the column the feet coordinate is in must always be among the sampled ones
		for (double[] pos : new double[][] {{0.5, 0.5}, {0.75, 0.5}, {0.25, 0.75}, {0.999, 0.001}}) {
			int fx = (int) Math.floor(pos[0]);
			int fz = (int) Math.floor(pos[1]);
			assertTrue(columns(pos[0], pos[1]).contains(fx + "," + fz),
					"feet column missing for " + pos[0] + "," + pos[1]);
		}
	}
}

package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

public class MapDataTest {

	@Test
	void parse_coords_reads_three_floats() {
		assertArrayEquals(new double[] { 1.0, 2.0, 3.0 },
				MapData.parseCoords(JsonParser.parseString("[1, 2, 3]")));
	}

	@Test
	void parse_coords_handles_negatives_and_decimals() {
		assertArrayEquals(new double[] { -12.5, 0.0, 64.25 },
				MapData.parseCoords(JsonParser.parseString("[-12.5, 0, 64.25]")));
	}
}
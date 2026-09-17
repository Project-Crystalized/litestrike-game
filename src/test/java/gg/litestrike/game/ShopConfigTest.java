package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShopConfigTest {

	private static List<JsonObject> entries(String json) {
		List<JsonObject> list = new ArrayList<>();
		for (var element : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("items")) {
			list.add(element.getAsJsonObject());
		}
		return list;
	}

	@Test
	void valid_config_has_no_problems() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a", "b"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": 100, \"slot\": 0}, {\"name\": \"b\", \"price\": null, \"slot\": null}]}"));
		assertTrue(problems.isEmpty(), problems::toString);
	}

	@Test
	void missing_entry_is_a_problem() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a", "b"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": 100, \"slot\": 0}]}"));
		assertEquals(1, problems.size());
	}

	@Test
	void unknown_and_nameless_entries_are_problems() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": null, \"slot\": null}, {\"name\": \"nope\", \"price\": 1, \"slot\": 1}, {\"price\": 1, \"slot\": 2}]}"));
		assertEquals(2, problems.size());
	}

	@Test
	void slot_without_price_is_a_problem() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": null, \"slot\": 0}]}"));
		assertEquals(1, problems.size());
	}

	@Test
	void negative_price_and_bad_slot_are_problems() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a", "b"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": -5, \"slot\": 0}, {\"name\": \"b\", \"price\": 10, \"slot\": 99}]}"));
		assertEquals(2, problems.size());
	}

	@Test
	void duplicate_slots_are_a_problem() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a", "b"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": 100, \"slot\": 0}, {\"name\": \"b\", \"price\": 200, \"slot\": 0}]}"));
		assertEquals(1, problems.size());
	}

	@Test
	void free_price_is_ok() {
		var problems = LSItem.ShopValidator.validateShopEntries(List.of("a"),
				entries("{\"items\": [{\"name\": \"a\", \"price\": 0, \"slot\": 0}]}"));
		assertTrue(problems.isEmpty(), problems::toString);
	}
}

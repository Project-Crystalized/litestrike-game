package gg.litestrike.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static Map<String, int[]> layoutOf(List<JsonObject> entries) {
		Map<String, int[]> layout = new HashMap<>();
		for (JsonObject o : entries) {
			layout.put(o.get("name").getAsString(), new int[] {
					o.get("price").isJsonNull() ? -1 : o.get("price").getAsInt(),
					o.get("slot").isJsonNull() ? -1 : o.get("slot").getAsInt() });
		}
		return layout;
	}

	private static Map<String, int[]> defaultSignatureLayout() {
		Map<String, int[]> layout = new HashMap<>();
		layout.put("iron_axe", new int[] { 1750, 0 });
		layout.put("slime_sword", new int[] { 1000, 18 });
		layout.put("ricochet_bow", new int[] { 1000, 8 });
		layout.put("explosive_arrow", new int[] { 350, 49 });
		layout.put("broadsword", new int[] { 1000, -1 });
		layout.put("explosive_bow", new int[] { 1750, -1 });
		return layout;
	}

	private static String resource(String name) throws Exception {
		try (var in = ShopConfigTest.class.getResourceAsStream(name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void default_signature_matches() {
		assertTrue(LSItem.ShopValidator.matchesDefaultLayout(defaultSignatureLayout()));
	}

	@Test
	void price_deviation_breaks_signature() {
		var layout = defaultSignatureLayout();
		layout.put("iron_axe", new int[] { 2000, 0 });
		assertFalse(LSItem.ShopValidator.matchesDefaultLayout(layout));
	}

	@Test
	void slot_deviation_breaks_signature() {
		var layout = defaultSignatureLayout();
		layout.put("iron_axe", new int[] { 1750, 2 });
		assertFalse(LSItem.ShopValidator.matchesDefaultLayout(layout));
	}

	@Test
	void null_swap_breaks_signature() {
		var layout = defaultSignatureLayout();
		layout.put("broadsword", new int[] { 1000, 0 });
		assertFalse(LSItem.ShopValidator.matchesDefaultLayout(layout));
	}

	@Test
	void missing_entry_breaks_signature() {
		var layout = defaultSignatureLayout();
		layout.remove("ricochet_bow");
		assertFalse(LSItem.ShopValidator.matchesDefaultLayout(layout));
	}

	@Test
	void extra_entries_keep_signature() {
		var layout = defaultSignatureLayout();
		layout.put("brand_new_gun", new int[] { 9999, 53 });
		assertTrue(LSItem.ShopValidator.matchesDefaultLayout(layout));
	}

	@Test
	void shipped_default_file_matches_signature() throws Exception {
		assertTrue(LSItem.ShopValidator.matchesDefaultLayout(layoutOf(entries(resource("/items.json")))));
	}

	@Test
	void tubnet_beta_file_breaks_signature() throws Exception {
		assertFalse(LSItem.ShopValidator.matchesDefaultLayout(layoutOf(entries(resource("/items_tubnet_beta.json")))));
	}
}

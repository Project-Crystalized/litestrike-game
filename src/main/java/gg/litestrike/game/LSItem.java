package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.bukkit.inventory.ItemFlag.*;
import static org.bukkit.enchantments.Enchantment.*;
import static org.bukkit.Material.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.text.Component.translatable;

public class LSItem {
	public final ItemStack item;
	public final List<Component> description;
	public final Integer price;
	public final Integer slot;
	public final Component name;
	public final Integer modelData;
	public final String key;
	private static short creation_number = 1;
	public final Short id;

	public static final NamespacedKey BREEZE_DAGGER_STATE_KEY = new NamespacedKey("crystalized", "breeze_dagger_state");
	public static final NamespacedKey LOCATING_ARROW_KEY = new NamespacedKey("crystalized", "locating_arrow");
	public static final NamespacedKey NEGATIVE_EFFECT_IMMUNITY = new NamespacedKey("litestrike", "negative_effect_immunity");

	public static List<LSItem> shopItems = createItems();

	public enum ItemCategory {
		Melee,
		Range,
		Armor,
		Ammunition,
		Consumable,
		Defuser
	}

	public final ItemCategory categ;

	public LSItem(ItemStack item, Integer price, List<Component> description, ItemCategory cate, Integer slot,
			Component name, Integer modelData, String key) {
		this.price = price;
		this.description = description;
		this.categ = cate;
		this.item = item;
		this.slot = slot;
		this.name = name;
		this.modelData = modelData;
		this.key = key;
		this.id = creation_number;
		creation_number++;

		ItemMeta meta = item.getItemMeta();
		if (item.getType().getMaxDurability() > 0) {
			meta.setUnbreakable(true);
			meta.addItemFlags(HIDE_UNBREAKABLE);
		}

		if (price != null && item.getType() != Material.ARROW) {
			Component item_name;
			if (meta.displayName() != null) {
				item_name = meta.displayName();
			} else {
				item_name = translatable(item.translationKey()).decoration(ITALIC, false);
			}
			if (price <= 500) {
				meta.displayName(item_name.color(WHITE));
			} else if (price <= 1000) {
				meta.displayName(item_name.color(TextColor.color(0x8f5805)));
			} else if (price <= 1500) {
				meta.displayName(item_name.color(TextColor.color(0x4DA4E5)));
			} else if (price <= 2000) {
				meta.displayName(item_name.color(TextColor.color(0xfbc522)));
			} else {
				meta.displayName(item_name.color(TextColor.color(0xb02ae2)));
			}
		}

		item.setItemMeta(meta);
	}

	private static List<LSItem> createItems() {
		// IMPORTANT: the order in which the items are created must be preserved
		// because it is used as a id in the database, also better not remove items from
		// the list
		List<Builder> builders = default_builders();
		List<JsonObject> entries = load_shop_entries();
		List<String> keys = new ArrayList<>();
		for (Builder builder : builders) {
			keys.add(builder.key);
		}
		List<String> problems = ShopValidator.validateShopEntries(keys, entries);
		for (String problem : problems) {
			Bukkit.getLogger().severe("[Litestrike] items.json: " + problem);
		}
		if (!problems.isEmpty()) {
			throw new IllegalStateException("[Litestrike] items.json has " + problems.size() + " problem(s), see above");
		}

		Map<String, JsonObject> byKey = new HashMap<>();
		for (JsonObject o : entries) {
			byKey.put(o.get("name").getAsString(), o);
		}
		List<LSItem> lsItems = new ArrayList<>();
		for (Builder builder : builders) {
			builder.applyOverrides(byKey.get(builder.key));
			lsItems.add(builder.build());
		}

		creation_number = 1; // reset id

		return lsItems;
	}

	static Map<String, int[]> currentShopLayout() {
		Map<String, int[]> layout = new HashMap<>();
		for (LSItem item : shopItems) {
			layout.put(item.key, new int[] { item.price == null ? -1 : item.price, item.slot == null ? -1 : item.slot });
		}
		return layout;
	}

	static String defaultBackground() {
		return ShopValidator.matchesDefaultLayout(currentShopLayout()) ? Shop.SHOP_BACKGROUND_DEFAULT
				: Shop.SHOP_BACKGROUND_GENERIC;
	}

	// items.json in the plugin data folder is the single source of truth for shop
	// merchandising: every known item needs an entry with "price" and "slot", both
	// null hides the item. entries match by "name". missing entries, nameless or
	// unknown entries, unpriced slots, negative prices, out of range or duplicate
	// slots fail startup loudly instead of selling the wrong shop.
	private static List<JsonObject> load_shop_entries() {
		Path path = Litestrike.getInstance().getDataFolder().toPath().resolve("items.json");
		if (Files.notExists(path)) {
			throw new IllegalStateException("[Litestrike] items.json missing in " + Litestrike.getInstance().getDataFolder());
		}
		try {
			List<JsonObject> entries = new ArrayList<>();
			JsonArray items = JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray("items");
			for (JsonElement element : items) {
				entries.add(element.getAsJsonObject());
			}
			return entries;
		} catch (Exception e) {
			throw new IllegalStateException("[Litestrike] items.json invalid: " + e.getMessage(), e);
		}
	}

	static class ShopValidator {

		static List<String> validateShopEntries(List<String> keys, List<JsonObject> entries) {
			List<String> problems = new ArrayList<>();
			Map<String, JsonObject> byKey = new HashMap<>();
			for (JsonObject o : entries) {
				if (!o.has("name") || o.get("name").isJsonNull()) {
					problems.add("entry without name");
					continue;
				}
				String name;
				try {
					name = o.get("name").getAsString();
				} catch (Exception e) {
					problems.add("entry with non-string name");
					continue;
				}
				if (!keys.contains(name)) {
					problems.add("unknown item '" + name + "'");
					continue;
				}
				if (byKey.putIfAbsent(name, o) != null) {
					problems.add("duplicate entry for '" + name + "'");
				}
			}
			for (String key : keys) {
				if (!byKey.containsKey(key)) {
					problems.add("missing shop entry for '" + key + "'");
				}
			}
			Map<Integer, String> slotUse = new HashMap<>();
			for (Map.Entry<String, JsonObject> entry : byKey.entrySet()) {
				String key = entry.getKey();
				JsonObject o = entry.getValue();
				Integer price = readIntField(o, "price", key, problems);
				Integer slot = readIntField(o, "slot", key, problems);
				if (price != null && price < 0) {
					problems.add("'" + key + "': negative price " + price);
				}
				if (slot != null && (slot < 0 || slot > 53)) {
					problems.add("'" + key + "': slot out of range " + slot + ", the shop inventory has slots 0-53");
					slot = null;
				}
				if (slot != null && price == null) {
					problems.add("'" + key + "': slot without price");
				}
				if (slot != null) {
					String prev = slotUse.putIfAbsent(slot, key);
					if (prev != null) {
						problems.add("slot " + slot + " used by both '" + prev + "' and '" + key + "'");
					}
				}
			}
			return problems;
		}

		private static Integer readIntField(JsonObject o, String field, String key, List<String> problems) {
			if (!o.has(field) || o.get(field).isJsonNull()) {
				return null;
			}
			try {
				return o.get(field).getAsInt();
			} catch (Exception e) {
				problems.add("'" + key + "': " + field + " must be an integer");
				return null;
			}
		}

		static boolean matchesDefaultLayout(Map<String, int[]> shop) {
			return is(shop, "iron_axe", 1750, 0)
					&& is(shop, "slime_sword", 1000, 18)
					&& is(shop, "ricochet_bow", 1000, 8)
					&& is(shop, "explosive_arrow", 350, 49)
					&& is(shop, "broadsword", 1000, -1)
					&& is(shop, "explosive_bow", 1750, -1);
		}

		private static boolean is(Map<String, int[]> shop, String name, int price, int slot) {
			int[] actual = shop.get(name);
			return actual != null && actual[0] == price && actual[1] == slot;
		}
	}

	private static List<Builder> default_builders() {
		List<Builder> builders = new ArrayList<>();

		builders.add(Builder.of(DIAMOND_CHESTPLATE)
				.key("diamond_chestplate")
				.enchantment(PROTECTION, 1)
				.category(ItemCategory.Armor));

		builders.add(Builder.of(IRON_SWORD)
				.key("iron_sword")
				.description("crystalized.sword.iron.desc")
				.category(ItemCategory.Melee));

		builders.add(Builder.of(STONE_SWORD)
				.key("stone_sword")
				.category(ItemCategory.Melee));

		builders.add(Builder.of(IRON_AXE)
				.key("iron_axe")
				.category(ItemCategory.Melee));

		builders.add(Builder.of(BOW)
				.key("bow")
				.category(ItemCategory.Range));

		builders.add(Builder.of(ARROW, 6)
				.key("arrow")
				.category(ItemCategory.Ammunition));

		builders.add(Builder.of(LEATHER_CHESTPLATE)
				.key("breaker_armor")
				.leatherColor(Color.fromRGB(0x0f9415), 1)
				.category(ItemCategory.Armor));

		builders.add(Builder.of(LEATHER_CHESTPLATE)
				.key("placer_armor")
				.leatherColor(Color.fromRGB(0xe31724), 1)
				.category(ItemCategory.Armor));

		builders.add(Builder.of(IRON_PICKAXE)
				.key("defuser")
				.name("crystalized.item.defuser.name")
				.attributeModifier(Attribute.ATTACK_DAMAGE, 0d, AttributeModifier.Operation.MULTIPLY_SCALAR_1,
						EquipmentSlotGroup.ANY)
				.hideAttributes()
				.description("crystalized.item.defuser.desc1")
				.description("crystalized.item.defuser.desc2")
				.category(ItemCategory.Defuser));

		builders.add(Builder.of(GOLDEN_APPLE)
				.key("golden_apple")
				.description("crystalized.item.gapple.desc1")
				.description("crystalized.item.gapple.desc2")
				.category(ItemCategory.Consumable));

		builders.add(Builder.of(IRON_CHESTPLATE)
				.key("iron_chestplate")
				.enchantment(PROTECTION, 1)
				.category(ItemCategory.Armor));

		builders.add(Builder.of(CROSSBOW)
				.key("quickdraw")
				.enchantment(QUICK_CHARGE, 1)
				.model("quick_charge_crossbow")
				.name("crystalized.crossbow.quickcharge.name")
				.description("crystalized.crossbow.quickcharge.desc")
				.category(ItemCategory.Range).modelData(2));

		builders.add(Builder.of(STONE_SWORD)
				.key("pufferfish_sword")
				.model("pufferfish_sword")
				.name("crystalized.sword.pufferfish.name")
				.description("crystalized.sword.pufferfish.desc")
				.category(ItemCategory.Melee).modelData(2));

		builders.add(Builder.of(STONE_SWORD)
				.key("slime_sword")
				.enchantment(KNOCKBACK, 1)
				.model("slime_sword")
				.name("crystalized.sword.slime.name")
				.description("crystalized.sword.slime.desc1")
				.description("crystalized.sword.slime.desc2")
				.category(ItemCategory.Melee).modelData(1));

		builders.add(Builder.of(BOW)
				.key("marksman_bow")
				.model("marksman_bow")
				.name("crystalized.bow.marksman.name")
				.description("crystalized.bow.marksman.desc")
				.category(ItemCategory.Range).modelData(1));

		builders.add(Builder.of(BOW)
				.key("ricochet_bow")
				.enchantment(PUNCH, 1)
				.model("ricochet_bow")
				.name("crystalized.bow.ricochet.name")
				.description("crystalized.bow.ricochet.desc")
				.category(ItemCategory.Range).modelData(3));

		builders.add(Builder.of(CROSSBOW)
				.key("multishot_crossbow")
				.enchantment(MULTISHOT, 1)
				.model("multishot_crossbow")
				.name("crystalized.crossbow.multi.name")
				.description("crystalized.crossbow.multi.desc")
				.category(ItemCategory.Range).modelData(1));

		builders.add(Builder.of(CROSSBOW)
				.key("charged_crossbow")
				.model("charged_crossbow")
				.enchantable(100)
				.metaEnchant(UNBREAKING, 1)
				// Added the enchanting glint to the charged crosbow.
				.nameRaw("crystalized.crossbow.charged.name")
				.description("crystalized.crossbow.charged.desc")
				.category(ItemCategory.Range).modelData(3));

		builders.add(Builder.of(POTION)
				.key("speed2_potion")
				.potionEffect(PotionEffectType.SPEED, 20 * 10, 1)
				.name(Component.text("Potion of Swiftness").color(WHITE).decoration(ITALIC, false))
				.nameField(Component.text("Potion of Swiftness"))
				.category(ItemCategory.Consumable));

		builders.add(Builder.of(POTION)
				.key("speed1_potion")
				.potionEffect(PotionEffectType.SPEED, 20 * 25, 0)
				.name(Component.text("Potion of Swiftness").color(WHITE).decoration(ITALIC, false))
				.nameField(Component.text("Potion of Swiftness"))
				.category(ItemCategory.Consumable));

		builders.add(Builder.of(POTION)
				.key("resistance_potion")
				.potionEffect(PotionEffectType.RESISTANCE, 20 * 25, 0)
				.name(Component.text("Potion of Resistance").color(WHITE).decoration(ITALIC, false))
				.nameField(Component.text("Potion of Resistance"))
				.category(ItemCategory.Consumable));

		builders.add(Builder.of(SPECTRAL_ARROW, 3)
				.key("locating_arrow")
				.name(Component.text("Locating Arrow").decoration(ITALIC, false))
				.persistentData(LOCATING_ARROW_KEY, 1)
				.category(ItemCategory.Ammunition));

		builders.add(Builder.of(ARROW, 3)
				.key("dragon_arrow")
				.model("dragon_arrow")
				.name("crystalized.item.dragonarrow.name")
				.description("crystalized.item.dragonarrow.desc")
				.loreOnItem()
				.category(ItemCategory.Ammunition).modelData(1));

		builders.add(Builder.of(ARROW, 3)
				.key("explosive_arrow")
				.model("explosive_arrow")
				.name("crystalized.item.explosivearrow.name")
				.description("crystalized.item.explosivearrow.desc")
				.loreOnItem()
				.category(ItemCategory.Ammunition).modelData(2));

		builders.add(Builder.of(STONE_SWORD)
				.key("underdog_sword")
				.model("underdog_sword")
				.name("crystalized.sword.underdog.name")
				.description("crystalized.sword.underdog.desc")
				.loreOnItem()
				.nameField(Component.text("Underdog Sword").decoration(ITALIC, false))
				.category(ItemCategory.Melee).modelData(3));

		builders.add(Builder.of(STONE_PICKAXE)
				.key("stone_pickaxe")
				.category(ItemCategory.Defuser));

		// ItemStack angled = new ItemStack(BOW);
		// ItemMeta angled_meta = angled.getItemMeta();
		// angled_meta.setItemModel(new NamespacedKey("crystalized", "angled_bow"));
		// angled_meta.displayName(translatable("crystalized.bow.angled.name").decoration(ITALIC,
		// false));
		// angled.setItemMeta(angled_meta);
		// List<Component> angled_lore = new ArrayList<>();
		// marksman_lore.add(translatable("crystalized.bow.angled.desc").color(WHITE).decoration(ITALIC,
		// false));
		// lsItems.add(new LSItem(angled, 500, angled_lore, ItemCategory.Range, 44,
		// translatable("crystalized.bow.angled.name").decoration(ITALIC, false), 1));

		builders.add(Builder.of(CROSSBOW)
				.key("crossbow")
				.nameField(translatable("crystalized.bow.angled.name").decoration(ITALIC, false))
				.category(ItemCategory.Range).modelData(1));

		// ItemStack shield = new ItemStack(ENDER_PEARL);
		// lsItems.add(new LSItem(shield, 500, null, ItemCategory.Range, 4, null, 1));
		// ItemStack wooden_axe = new ItemStack(GOAT_HORN);
		// lsItems.add(new LSItem(wooden_axe, 100, null, ItemCategory.Range, 13, null,
		// 1));

		builders.add(Builder.of(STONE_SWORD)
				.key("breeze_dagger")
				.model("breeze_dagger")
				.name("crystalized.sword.wind.name")
				.description("crystalized.sword.wind.desc")
				.persistentData(BREEZE_DAGGER_STATE_KEY, 0)
				.category(ItemCategory.Melee).modelData(2));

		builders.add(Builder.of(CROSSBOW)
				.key("precise_crossbow")
				.model("precise_crossbow")
				.name("crystalized.crossbow.precise.name")
				.description("crystalized.crossbow.precise.desc")
				.category(ItemCategory.Range).modelData(3));

		// normal spectral arrow (vanilla glow, no tracer scan).
		builders.add(Builder.of(SPECTRAL_ARROW, 3)
				.key("spectral_arrow")
				.category(ItemCategory.Ammunition));

		builders.add(Builder.of(IRON_SWORD)
				.key("broadsword")
				.enchantment(SHARPNESS, 1)
				.name(Component.text("Broadsword").decoration(ITALIC, false))
				.category(ItemCategory.Melee));

		builders.add(Builder.of(BOW)
				.key("explosive_bow")
				.model("explosive_bow")
				.name("crystalized.bow.explosive.name")
				.description("crystalized.bow.explosive.desc1")
				.description("crystalized.bow.explosive.desc2")
				.category(ItemCategory.Range).modelData(2));

		// healing arrow does 0 damage to enemys TODO mention it in lore
		builders.add(Builder.of(TIPPED_ARROW, 4)
				.key("healing_arrow")
				.name(Component.text("Healing Arrow").decoration(ITALIC, false))
				.potionEffect(PotionEffectType.REGENERATION, 80, 1)
				.category(ItemCategory.Ammunition));

		builders.add(Builder.of(ARROW, 8)
				.key("arrow_8")
				.category(ItemCategory.Ammunition));

		builders.add(Builder.of(SPECTRAL_ARROW, 4)
				.key("spectral_arrow_4")
				.category(ItemCategory.Ammunition));

		//Supportive Arrow
		//TODO: Lore, when hits an enemy makes them unhelable for a short period of time, when hits the floor creates area of healing, and protection
		builders.add(Builder.of(ARROW, 3)
				.key("supportive_arrow")
				.model("supportive_arrow")
				.name(Component.text("Supportive Arrow").decoration(ITALIC, false))
				.category(ItemCategory.Ammunition));

		return builders;
	}

	// a small builder to avoid repeating the getItemMeta/setItemModel/displayName/
	// setItemMeta/lore boilerplate for every item
	public static class Builder {
		private final ItemStack item;
		private final List<Component> description = new ArrayList<>();
		private Integer price;
		private Integer slot;
		private ItemCategory categ;
		private Component name;
		private Integer modelData;
		private boolean lore_on_item;
		private String key;

		private Builder(ItemStack item) {
			this.item = item;
		}

		public static Builder of(Material material) {
			return of(material, 1);
		}

		public static Builder of(Material material, int amount) {
			return new Builder(new ItemStack(material, amount));
		}

		// stable internal name, used to reference this item in items.json
		public Builder key(String key) {
			this.key = key;
			return this;
		}

		public Builder price(int price) {
			this.price = price;
			return this;
		}

		public Builder slot(int slot) {
			this.slot = slot;
			return this;
		}

		public Builder category(ItemCategory categ) {
			this.categ = categ;
			return this;
		}

		public Builder modelData(int modelData) {
			this.modelData = modelData;
			return this;
		}

		public Builder enchantment(Enchantment enchantment, int level) {
			item.addEnchantment(enchantment, level);
			return this;
		}

		// adds an enchant to the item meta, keeping its level restriction check
		public Builder metaEnchant(Enchantment enchantment, int level) {
			ItemMeta meta = item.getItemMeta();
			meta.addEnchant(enchantment, level, false);
			item.setItemMeta(meta);
			return this;
		}

		public Builder enchantable(int value) {
			ItemMeta meta = item.getItemMeta();
			meta.setEnchantable(value);
			item.setItemMeta(meta);
			return this;
		}

		public Builder model(String key) {
			ItemMeta meta = item.getItemMeta();
			meta.setItemModel(new NamespacedKey("crystalized", key));
			item.setItemMeta(meta);
			return this;
		}

		public Builder name(String translationKey) {
			return name(translatable(translationKey).decoration(ITALIC, false));
		}

		// name that keeps the default translatable style (no italic:false),
		// used by the charged crossbow
		public Builder nameRaw(String translationKey) {
			setDisplayName(translatable(translationKey));
			this.name = translatable(translationKey).decoration(ITALIC, false);
			return this;
		}

		public Builder name(Component component) {
			setDisplayName(component);
			this.name = component;
			return this;
		}

		// sets only the LSItem name field, not the item display name
		public Builder nameField(Component component) {
			this.name = component;
			return this;
		}

		public Builder description(String translationKey) {
			description.add(translatable(translationKey).color(WHITE).decoration(ITALIC, false));
			return this;
		}

		// also writes the description to the item meta lore
		public Builder loreOnItem() {
			this.lore_on_item = true;
			return this;
		}

		public Builder leatherColor(Color color, int enchantLevel) {
			Shop.colorArmor(color, item, enchantLevel);
			return this;
		}

		public Builder hideAttributes() {
			item.addItemFlags(HIDE_ATTRIBUTES);
			return this;
		}

		public Builder attributeModifier(Attribute attribute, double amount, AttributeModifier.Operation operation,
				EquipmentSlotGroup slotGroup) {
			ItemMeta meta = item.getItemMeta();
			meta.addAttributeModifier(attribute,
					new AttributeModifier(NamespacedKey.minecraft("foo"), amount, operation, slotGroup));
			item.setItemMeta(meta);
			return this;
		}

		public Builder potionEffect(PotionEffectType type, int durationTicks, int amplifier) {
			PotionMeta meta = (PotionMeta) item.getItemMeta();
			meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier, true, true, true), true);
			item.setItemMeta(meta);
			return this;
		}

		public Builder persistentData(NamespacedKey key, int value) {
			ItemMeta meta = item.getItemMeta();
			meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER,
					value);
			item.setItemMeta(meta);
			return this;
		}

		private void setDisplayName(Component component) {
			ItemMeta meta = item.getItemMeta();
			meta.displayName(component);
			item.setItemMeta(meta);
		}

		private void applyOverrides(JsonObject json) {
			if (json.has("price") && !json.get("price").isJsonNull()) {
				this.price = json.get("price").getAsInt();
			}
			if (json.has("slot") && !json.get("slot").isJsonNull()) {
				int slot = json.get("slot").getAsInt();
				if (slot < 0 || slot > 53) {
					Bukkit.getLogger().warning("[Litestrike] items.json: ignoring out of range slot " + slot
							+ " for item '" + key + "', the shop inventory has slots 0-53");
					return;
				}
				this.slot = slot;
			}
		}

		public LSItem build() {
			if (lore_on_item && !description.isEmpty()) {
				ItemMeta meta = item.getItemMeta();
				meta.lore(description);
				item.setItemMeta(meta);
			}
			return new LSItem(item, price, description.isEmpty() ? null : description, categ, slot, name, modelData,
					key);
		}
	}

	// this can handle null being passed in
	public ItemStack buildDisplayItem(String p_name) {
		if (price == null) {
			return null;
		}
		List<Component> lore;
		if (description == null) {
			lore = new ArrayList<>();
		} else {
			lore = new ArrayList<>(description);
		}
		if ((item.getType() == ARROW || item.getType() == SPECTRAL_ARROW) && modelData == null) {
			lore.add(Component.text("\uE12C \uE12D").color(WHITE).decoration(ITALIC, false));
		} else if (item.getType() == ARROW && modelData == 1) {
			lore.add(Component.text("\uE12C").color(WHITE).decoration(ITALIC, false));
		} else if (item.getType() == ARROW && modelData == 2) {
			lore.add(Component.text("\uE12C \uE12C").color(WHITE).decoration(ITALIC, false));
		}
		Player p = Bukkit.getPlayer(p_name);
		lore.add(Component.text("")); // add a newline so that the price is seperated
		if (Litestrike.getInstance().gameConfig.freeShop) {
			lore.add(Component.text("FREE" + "\uE104").color(WHITE).decoration(TextDecoration.ITALIC, false));
		} else {
			if (p != null && (Litestrike.getInstance().game_controller.playerDataManager.get(p_name).getMoney() - price) >= 0) {
				lore.add(Component.text(price + "\uE104").color(WHITE).decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text(price + "\uE104").color(RED).decoration(TextDecoration.ITALIC, false));
			}
		}

		ItemStack displayItem = item.clone();
		ItemMeta meta = displayItem.getItemMeta();
		meta.lore(lore);
		displayItem.setItemMeta(meta);

		if (is_underdog_sword(item)) {
			displayItem = do_underdog_sword(Litestrike.getInstance().game_controller.teams.get_team(p_name));
			ItemMeta dog_meta = displayItem.getItemMeta();
			var dog_lore = dog_meta.lore();
			dog_lore.addAll(lore);
			dog_meta.lore(dog_lore);
			displayItem.setItemMeta(dog_meta);
		}

		return displayItem;
	}

	public static boolean is_same_ls_item(ItemStack item, ItemStack ls_item) {
		if (LSItem.is_underdog_sword(item) && LSItem.is_underdog_sword(ls_item)) {
			return true;
		}

		if (item.getType() == ls_item.getType()
				&& Objects.equals(ShopListener.identifyItemModel(item), ShopListener.identifyItemModel(ls_item))) {
			if (!item.getEnchantments().equals(ls_item.getEnchantments())) {
				return false;
			}
			if (item.getItemMeta() instanceof PotionMeta && ls_item.getItemMeta() instanceof PotionMeta) {
				PotionMeta item_meta = (PotionMeta) item.getItemMeta();
				PotionMeta ls_item_meta = (PotionMeta) ls_item.getItemMeta();
				if (item_meta.getCustomEffects().equals(ls_item_meta.getCustomEffects())) {
					return true;
				} else {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static ItemStack do_underdog_sword(Team t) {
		GameController gc = Litestrike.getInstance().game_controller;
		int rounds_down = 0;
		if (t == Team.Breaker) {
			rounds_down = gc.placer_wins_amt - gc.breaker_wins_amt;
		} else {
			rounds_down = gc.breaker_wins_amt - gc.placer_wins_amt;
		}
		if (rounds_down <= 0) {
			rounds_down = 0;
		}
		ItemStack underDog = new ItemStack(STONE_SWORD);
		ItemMeta underDog_meta = underDog.getItemMeta();
		underDog_meta.setItemModel(new NamespacedKey("crystalized", "underdog_sword"));
		CustomModelDataComponent cmdc = underDog_meta.getCustomModelDataComponent();
		cmdc.setFloats(List.of((float) rounds_down));
		underDog_meta.setCustomModelDataComponent(cmdc);
		underDog_meta.displayName(Component.translatable("crystalized.sword.underdog.name").decoration(ITALIC, false)
				.color(TextColor.color(0x8f5805)));
		List<Component> underDog_lore = new ArrayList<>();
		underDog_lore.add(Component.translatable("crystalized.sword.underdog.desc").color(WHITE).decoration(ITALIC, false));
		underDog_lore.add(Component.text(""));
		underDog_lore
				.add(Component.text("Current bonus: " + ((double) rounds_down / 2) + " damage.").color(WHITE).decoration(ITALIC,
						false));
		underDog_meta.lore(underDog_lore);
		underDog_meta.setUnbreakable(true);

		underDog.setItemMeta(underDog_meta);
		return underDog;
	}

	public static boolean is_underdog_sword(ItemStack item) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasItemModel()) {
			return false;
		}
		return item.getItemMeta().getItemModel().equals(new NamespacedKey("crystalized", "underdog_sword"));
	}

	public static boolean isBreezeDagger(ItemStack item) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasItemModel()) {
			return false;
		}
		return item.getItemMeta().getItemModel().equals(new NamespacedKey("crystalized", "breeze_dagger"));
	}

	public static ItemCategory getItemCategory(ItemStack i) {
		for (LSItem lsi : LSItem.shopItems) {
			if (i.getType() == lsi.item.getType()) {
				return lsi.categ;
			}
		}

		String name = i.getType().name();
		// hardcoding default equipment because it isn't in LSITEM
		if (name.contains("SWORD")) {
			return ItemCategory.Melee;
		}
		if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS")) {
			return ItemCategory.Armor;
		}
		if (name.contains("PICKAXE")) {
			return ItemCategory.Defuser;
		}
		if (name.contains("POTION")) {
			return ItemCategory.Consumable;
		}

		return null;
	}

	public static void doSupportingArrow(GameController gc) {
		for (PlayerData pd : gc.playerDataManager.getAll()) {
			Player player = Bukkit.getPlayerExact(pd.player);
			if (player == null || !player.isOnline()) {
				continue;
			}

			// antiheal particles
			if (pd.antiHealTicks > 0) {
				pd.antiHealTicks--;
				player.getWorld().spawnParticle(Particle.DUST, player.getLocation() .add(0, 1.0, 0), 2, 0.35, 0.5, 0.35, 0.0, new Particle.DustOptions(Color.BLACK, 0.8F));
			}

			// send cooldown actionbar
			if (pd.supportiveHealCooldownTicks > 0) {
				pd.supportiveHealCooldownTicks--;
				//if the player is using the bomb will not overwrite. And then stops and on cool down should show up
				if (!Litestrike.getInstance().bombListener.isUsingBomb(player)) {
					double secondsRemaining = pd.supportiveHealCooldownTicks / 20.0;
					player.sendActionBar(Component.text(String.format("Supportive Arrow healing available in %.1fs", secondsRemaining), NamedTextColor.AQUA));
				}
			}

			if (isInCircle(player, gc) && pd.antiHealTicks <= 0) {
				player.getPersistentDataContainer().set(NEGATIVE_EFFECT_IMMUNITY, PersistentDataType.BYTE, (byte) 1);
				//This the protective particles which appere around the player, while in the circle team based as well
				Location protectionParticleLocation = player.getLocation().add(0, 1.0, 0);
				Team protectedPlayerTeam = gc.teams.get_team(player);
				for (Player viewer : player.getWorld().getPlayers()) {
					Team viewerTeam = gc.teams.get_team(viewer);
					Particle.DustOptions particle = getSupportiveParticle(protectedPlayerTeam, viewerTeam, 0.8F);
					viewer.spawnParticle(Particle.DUST, protectionParticleLocation, 2, 0.35, 0.5, 0.35, 0.0, particle);
				}


				// Clense negative effects
				for (PotionEffect effect : player.getActivePotionEffects()) {
					PotionEffectType type = effect.getType();
						// Skips the posion as it the cool down indicator before player can use the arrow again
						// if (type == PotionEffectType.POISON && pd.supportiveHealCooldownTicks > 0) {
						// 	continue;
						// }
					if (isNegativeEffect(type)) {
						player.removePotionEffect(type);
					}
				}

			} else {
				player.getPersistentDataContainer().remove(NEGATIVE_EFFECT_IMMUNITY);
			}
		}
	}
	//This method exists to get the the supportive particle based on the players team
	//The circler owner team is the team which owns the supportive circl, the team of the viewer is the player observing it.
	public static Particle.DustOptions getSupportiveParticle(Team theCircleOwnerTeam, Team theTeamOfTheViewer, float size) {
		//This is the colour that the particle will be
		Color colour;
		//This is for the spectator players with no teams
		if (theTeamOfTheViewer == null) {
			//Spectators allways sees breakers as aqua, and placers as dark blue
			if (theCircleOwnerTeam == Team.Breaker) {
				colour = Color.AQUA;
			} else {
				//dark blue
				colour = Color.fromRGB(0, 70, 180);
			}
		} else if (theTeamOfTheViewer == theCircleOwnerTeam) {
			//This is what the same team players see
			colour = Color.AQUA;
		} else {
			//This is what the enemy sees
			colour = Color.fromRGB(0, 70, 180);
		}
		//returns how the particle must look, and it's size
		return new Particle.DustOptions(colour, size);
	}

	private static boolean isNegativeEffect(PotionEffectType type) {
		return type == PotionEffectType.SLOWNESS
				|| type == PotionEffectType.MINING_FATIGUE
				|| type == PotionEffectType.INSTANT_DAMAGE
				|| type == PotionEffectType.NAUSEA
				|| type == PotionEffectType.BLINDNESS
				|| type == PotionEffectType.HUNGER
				|| type == PotionEffectType.WEAKNESS
				|| type == PotionEffectType.LEVITATION
				|| type == PotionEffectType.UNLUCK
				|| type == PotionEffectType.DARKNESS
				|| type == PotionEffectType.POISON;
	}

	private static boolean isInCircle(Player player, GameController gc) {
		for (GameController.SupportiveCircle circle : gc.supportiveCircles) {
			if (player.getWorld() != circle.location().getWorld()) {
				continue;
			}
			if (gc.teams.get_team(player) != circle.team()) {
				continue;
			}
			double x = player.getLocation().getX() - circle.location().getX();
			double z = player.getLocation().getZ() - circle.location().getZ();
			double horizontalDistanceSquared = (x * x) + (z * z);

			if (horizontalDistanceSquared <= 4.0 && Math.abs(player.getLocation().getY() - circle.location().getY()) <= 2.0) {
				return true;
			}
		}
		return false;
	}

}

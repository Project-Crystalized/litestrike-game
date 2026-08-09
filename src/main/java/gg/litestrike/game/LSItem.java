package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import java.util.logging.Level;

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
	public static HashMap<String, LSItem> importantEquip = new HashMap<>();

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
		Map<Object, JsonObject> overrides = load_item_overrides();

		List<LSItem> lsItems = new ArrayList<>();
		for (int i = 0; i < builders.size(); i++) {
			Builder builder = builders.get(i);
			JsonObject o = overrides.get(builder.key);
			if (o == null) {
				o = overrides.get(i + 1);
			}
			if (o != null) {
				builder.applyOverrides(o);
			}
			lsItems.add(builder.build());
		}

		creation_number = 1; // reset id

		return lsItems;
	}

	// reads items.json from the plugin data folder, which can override the price
	// and shop slot of items. entries are matched by their internal item name (the
	// "name" field); a numeric "id" (= 1-based creation order) is still accepted.
	// any missing or invalid file just falls back to the default items.
	private static Map<Object, JsonObject> load_item_overrides() {
		Map<Object, JsonObject> overrides = new HashMap<>();
		try {
			Path path = Litestrike.getInstance().getDataFolder().toPath().resolve("items.json");
			if (Files.notExists(path)) {
				return overrides;
			}
			JsonArray items = JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray("items");
			for (JsonElement element : items) {
				JsonObject o = element.getAsJsonObject();
				if (o.has("name")) {
					overrides.put(o.get("name").getAsString(), o);
				} else if (o.has("id")) {
					overrides.put(o.get("id").getAsInt(), o);
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "[Litestrike] Could not load items.json, using the default item list: " + e);
		}
		return overrides;
	}

	private static List<Builder> default_builders() {
		List<Builder> builders = new ArrayList<>();

		builders.add(Builder.of(DIAMOND_CHESTPLATE)
				.key("diamond_chestplate")
				.enchantment(PROTECTION, 1)
				.price(500).slot(31).category(ItemCategory.Armor));

		builders.add(Builder.of(IRON_SWORD)
				.key("iron_sword")
				.description("crystalized.sword.iron.desc")
				.price(750).slot(0).category(ItemCategory.Melee));

		builders.add(Builder.of(STONE_SWORD)
				.key("stone_sword")
				.category(ItemCategory.Melee));

		builders.add(Builder.of(IRON_AXE)
				.key("iron_axe")
				.price(1750).slot(2).category(ItemCategory.Melee));

		builders.add(Builder.of(BOW)
				.key("bow")
				.category(ItemCategory.Range));

		builders.add(Builder.of(ARROW, 6)
				.key("arrow")
				.price(150).slot(50).category(ItemCategory.Ammunition));

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
				.price(500).slot(Shop.DEFUSER_SLOT).category(ItemCategory.Defuser));

		builders.add(Builder.of(GOLDEN_APPLE)
				.key("golden_apple")
				.description("crystalized.item.gapple.desc1")
				.description("crystalized.item.gapple.desc2")
				.price(750).slot(48).category(ItemCategory.Consumable));

		builders.add(Builder.of(IRON_CHESTPLATE)
				.key("iron_chestplate")
				.enchantment(PROTECTION, 1)
				.price(250).slot(40).category(ItemCategory.Armor));

		builders.add(Builder.of(CROSSBOW)
				.key("quickdraw")
				.enchantment(QUICK_CHARGE, 1)
				.model("quick_charge_crossbow")
				.name("crystalized.crossbow.quickcharge.name")
				.description("crystalized.crossbow.quickcharge.desc")
				.price(2000).slot(24).category(ItemCategory.Range).modelData(2));

		builders.add(Builder.of(STONE_SWORD)
				.key("pufferfish_sword")
				.model("pufferfish_sword")
				.name("crystalized.sword.pufferfish.name")
				.description("crystalized.sword.pufferfish.desc")
				.price(1000).slot(18).category(ItemCategory.Melee).modelData(2));

		builders.add(Builder.of(STONE_SWORD)
				.key("slime_sword")
				.enchantment(KNOCKBACK, 1)
				.model("slime_sword")
				.name("crystalized.sword.slime.name")
				.description("crystalized.sword.slime.desc1")
				.description("crystalized.sword.slime.desc2")
				.price(1000).slot(20).category(ItemCategory.Melee).modelData(1));

		builders.add(Builder.of(BOW)
				.key("marksman_bow")
				.model("marksman_bow")
				.name("crystalized.bow.marksman.name")
				.description("crystalized.bow.marksman.desc")
				.price(750).slot(6).category(ItemCategory.Range).modelData(1));

		builders.add(Builder.of(BOW)
				.key("ricochet_bow")
				.enchantment(PUNCH, 1)
				.model("ricochet_bow")
				.name("crystalized.bow.ricochet.name")
				.description("crystalized.bow.ricochet.desc")
				.price(1500).slot(8).category(ItemCategory.Range).modelData(3));

		builders.add(Builder.of(CROSSBOW)
				.key("multishot_crossbow")
				.enchantment(MULTISHOT, 1)
				.model("multishot_crossbow")
				.name("crystalized.crossbow.multi.name")
				.description("crystalized.crossbow.multi.desc")
				.price(2000).slot(26).category(ItemCategory.Range).modelData(1));

		builders.add(Builder.of(CROSSBOW)
				.key("charged_crossbow")
				.model("charged_crossbow")
				.enchantable(100)
				.metaEnchant(UNBREAKING, 1)
				// Added the enchanting glint to the charged crosbow.
				.nameRaw("crystalized.crossbow.charged.name")
				.description("crystalized.crossbow.charged.desc")
				.price(2500).slot(25).category(ItemCategory.Range).modelData(3));

		builders.add(Builder.of(POTION)
				.key("speed2_potion")
				.potionEffect(PotionEffectType.SPEED, 20 * 10, 1)
				.name(Component.text("Potion of Swiftness").color(WHITE).decoration(ITALIC, false))
				.nameField(Component.text("Potion of Swiftness"))
				.price(1000).slot(46).category(ItemCategory.Consumable));

		builders.add(Builder.of(POTION)
				.key("speed1_potion")
				.potionEffect(PotionEffectType.SPEED, 20 * 25, 0)
				.name(Component.text("Potion of Swiftness").color(WHITE).decoration(ITALIC, false))
				.nameField(Component.text("Potion of Swiftness"))
				.price(750).slot(47).category(ItemCategory.Consumable));

		builders.add(Builder.of(POTION)
				.key("resistance_potion")
				.potionEffect(PotionEffectType.RESISTANCE, 20 * 25, 0)
				.name(Component.text("Potion of Resistance").color(WHITE).decoration(ITALIC, false))
				.nameField(Component.text("Potion of Resistance"))
				.price(750).slot(45).category(ItemCategory.Consumable));

		builders.add(Builder.of(SPECTRAL_ARROW, 3)
				.key("spectral_arrow")
				.price(150).slot(51).category(ItemCategory.Ammunition));

		builders.add(Builder.of(ARROW, 3)
				.key("dragon_arrow")
				.model("dragon_arrow")
				.name("crystalized.item.dragonarrow.name")
				.description("crystalized.item.dragonarrow.desc")
				.loreOnItem()
				.price(350).slot(52).category(ItemCategory.Ammunition).modelData(1));

		builders.add(Builder.of(ARROW, 3)
				.key("explosive_arrow")
				.model("explosive_arrow")
				.name("crystalized.item.explosivearrow.name")
				.description("crystalized.item.explosivearrow.desc")
				.loreOnItem()
				.price(350).slot(53).category(ItemCategory.Ammunition).modelData(2));

		builders.add(Builder.of(STONE_SWORD)
				.key("underdog_sword")
				.model("underdog_sword")
				.name("crystalized.sword.underdog.name")
				.description("crystalized.sword.underdog.desc")
				.loreOnItem()
				.nameField(Component.text("Underdog Sword").decoration(ITALIC, false))
				.price(750).slot(36).category(ItemCategory.Melee).modelData(3));

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
				.price(1250).slot(44).category(ItemCategory.Range).modelData(1));

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
				.persistentData(0)
				.price(800).category(ItemCategory.Melee).modelData(2));

		// I tried to add here the Presies CrossBow for testing purposes
		builders.add(Builder.of(CROSSBOW)
				.key("precise_crossbow")
				.model("precise_crossbow")
				.name("crystalized.crossbow.precise.name")
				.description("crystalized.crossbow.precise.desc")
				// Adjusted the price so it is worth buying it over charged crosbow
				.price(1750).slot(43).category(ItemCategory.Range).modelData(3));

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

		public Builder persistentData(int value) {
			ItemMeta meta = item.getItemMeta();
			meta.getPersistentDataContainer().set(new NamespacedKey("namespace", "key"), PersistentDataType.INTEGER,
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
				this.slot = json.get("slot").getAsInt();
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
		if (Litestrike.getInstance().getConfig().getBoolean("free-shop")) {
			lore.add(Component.text("FREE" + "\uE104").color(WHITE).decoration(TextDecoration.ITALIC, false));
		} else {
			if (p != null && (Litestrike.getInstance().game_controller.getPlayerData(p_name).getMoney() - price) >= 0) {
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
		if (item.getItemMeta().getItemModel().equals(new NamespacedKey("crystalized", "underdog_sword"))) {
			return true;
		}
		return false;

		/*
		 * if (item == null || !item.hasItemMeta() ||
		 * !item.getItemMeta().hasCustomModelData()) {
		 * return false;
		 * }
		 * return (item.getType() == Material.STONE_SWORD
		 * && (item.getItemMeta().getCustomModelData() >= 3 &&
		 * item.getItemMeta().getCustomModelData() <= 7));
		 */
	}

	public static boolean isBreezeDagger(ItemStack item) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasItemModel()) {
			return false;
		}
		if (item.getItemMeta().getItemModel().equals(new NamespacedKey("crystalized", "breeze_dagger"))) {
			return true;
		}
		return false;
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
}

package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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

	private static short creation_number = 1;
	public final Short id;

	public enum ItemCategory {
		Melee,
		Range,
		Armor,
		Ammunition,
		Consumable,
		Defuser
	}

	public final ItemCategory categ;

	private LSItem(ItemStack item, Integer price, List<Component> description, ItemCategory cate, Integer slot) {
		this.price = price;
		this.description = description;
		this.categ = cate;
		this.item = item;
		this.slot = slot;
		this.id = creation_number;
		creation_number++;

		ItemMeta meta = item.getItemMeta();
		if (item.getType().getMaxDurability() > 0) {
			meta.setUnbreakable(true);
		}

		if (price != null) {
			Component item_name;
			if (meta.displayName() != null) {
				item_name = meta.displayName();
			} else {
				item_name = translatable(item.translationKey()).decoration(ITALIC, false);
			}
			if (price <= 500) {
				meta.displayName(item_name.color(WHITE));
			} else if (price <= 1000) {
				meta.displayName(item_name.color(TextColor.color(0x4DA4E5)));
			} else if (price <= 2000) {
				meta.displayName(item_name.color(TextColor.color(0xfbc522)));
			} else {
				meta.displayName(item_name.color(TextColor.color(0xb02ae2)));
			}
		}

		item.setItemMeta(meta);
		item.addItemFlags(HIDE_UNBREAKABLE);
	}

	public static List<LSItem> createItems() {
		List<LSItem> lsItems = new ArrayList<>();

		// IMPORTANT: the order in which the items are created must be preserved
		// because it is used as a id in the database, also better not remove items from
		// the list
		ItemStack diamondChestplate = new ItemStack(DIAMOND_CHESTPLATE);
		diamondChestplate.addEnchantment(PROTECTION, 1);
		lsItems.add(new LSItem(diamondChestplate, 500, null, ItemCategory.Armor, 31));

		ItemStack ironSword = new ItemStack(IRON_SWORD);
		List<Component> ironSword_lore = new ArrayList<>();
		ironSword_lore.add(translatable("crystalized.sword.iron.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(ironSword, 750, ironSword_lore, ItemCategory.Melee, 0));

		ItemStack stoneSword = new ItemStack(STONE_SWORD);
		lsItems.add(new LSItem(stoneSword, null, null, ItemCategory.Melee, null));

		ItemStack ironAxe = new ItemStack(IRON_AXE);
		lsItems.add(new LSItem(ironAxe, 1750, null, ItemCategory.Melee, 2));

		ItemStack bow = new ItemStack(BOW);
		lsItems.add(new LSItem(bow, null, null, ItemCategory.Range, null));

		ItemStack arrow = get_arrows();
		lsItems.add(new LSItem(arrow, 150, null, ItemCategory.Ammunition, 50));

		ItemStack defuser = new ItemStack(IRON_PICKAXE);
		ItemMeta defuser_meta = defuser.getItemMeta();
		defuser_meta.displayName(translatable("crystalized.item.defuser.name").decoration(ITALIC, false));
		defuser_meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
				NamespacedKey.minecraft("foo"), 0d, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
		defuser.setItemMeta(defuser_meta);
		defuser.addItemFlags(HIDE_ATTRIBUTES);
		List<Component> defuser_lore = new ArrayList<>();
		defuser_lore.add(translatable("crystalized.item.defuser.desc1").color(WHITE).decoration(ITALIC, false));
		defuser_lore.add(translatable("crystalized.item.defuser.desc2").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(defuser, 100, defuser_lore, ItemCategory.Defuser, Shop.DEFUSER_SLOT));

		ItemStack gapple = new ItemStack(GOLDEN_APPLE);
		List<Component> gapple_lore = new ArrayList<>();
		gapple_lore.add(translatable("crystalized.item.gapple.desc1").color(WHITE).decoration(ITALIC, false));
		gapple_lore.add(translatable("crystalized.item.gapple.desc2").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(gapple, 750, gapple_lore, ItemCategory.Consumable, 49));

		ItemStack ironChestplate = new ItemStack(IRON_CHESTPLATE);
		ironChestplate.addEnchantment(PROTECTION, 1);
		lsItems.add(new LSItem(ironChestplate, 250, null, ItemCategory.Armor, 40));

		ItemStack quickdraw = new ItemStack(CROSSBOW);
		quickdraw.addEnchantment(QUICK_CHARGE, 1);
		ItemMeta quickdraw_meta = quickdraw.getItemMeta();
		quickdraw_meta.setCustomModelData(2);
		quickdraw_meta.displayName(translatable("crystalized.crossbow.quickcharge.name").decoration(ITALIC, false));
		quickdraw.setItemMeta(quickdraw_meta);
		List<Component> quickdraw_lore = new ArrayList<>();
		quickdraw_lore.add(translatable("crystalized.crossbow.quickcharge.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(quickdraw, 2000, quickdraw_lore, ItemCategory.Range, 24));

		ItemStack pufferFish = new ItemStack(STONE_SWORD);
		ItemMeta pufferFish_meta = pufferFish.getItemMeta();
		pufferFish_meta.setCustomModelData(2);
		pufferFish_meta.displayName(translatable("crystalized.sword.pufferfish.name").decoration(ITALIC, false));
		pufferFish.setItemMeta(pufferFish_meta);
		List<Component> pufferFish_lore = new ArrayList<>();
		pufferFish_lore.add(translatable("crystalized.sword.pufferfish.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(pufferFish, 1250, pufferFish_lore, ItemCategory.Melee, 18));

		ItemStack slimeSword = new ItemStack(STONE_SWORD);
		slimeSword.addEnchantment(KNOCKBACK, 1);
		ItemMeta slimeSword_meta = slimeSword.getItemMeta();
		slimeSword_meta.setCustomModelData(1);
		slimeSword_meta.displayName(translatable("crystalized.sword.slime.name").decoration(ITALIC, false));
		slimeSword.setItemMeta(slimeSword_meta);
		List<Component> slimeSword_lore = new ArrayList<>();
		slimeSword_lore.add(translatable("crystalized.sword.slime.desc1").color(WHITE).decoration(ITALIC, false));
		slimeSword_lore.add(translatable("crystalized.sword.slime.desc2").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(slimeSword, 1000, slimeSword_lore, ItemCategory.Melee, 20));

		ItemStack marksman = new ItemStack(BOW);
		ItemMeta marksman_meta = marksman.getItemMeta();
		marksman_meta.setCustomModelData(1);
		marksman_meta.displayName(translatable("crystalized.bow.marksman.name").decoration(ITALIC, false));
		marksman.setItemMeta(marksman_meta);
		List<Component> marksman_lore = new ArrayList<>();
		marksman_lore.add(translatable("crystalized.bow.marksman.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(marksman, 750, marksman_lore, ItemCategory.Range, 6));

		ItemStack ricochet = new ItemStack(BOW);
		ricochet.addEnchantment(PUNCH, 1);
		ItemMeta ricochet_meta = ricochet.getItemMeta();
		ricochet_meta.setCustomModelData(2);
		ricochet_meta.displayName(translatable("crystalized.bow.ricochet.name").decoration(ITALIC, false));
		ricochet.setItemMeta(ricochet_meta);
		List<Component> ricochet_lore = new ArrayList<>();
		ricochet_lore.add(translatable("crystalized.bow.ricochet.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(ricochet, 1500, ricochet_lore, ItemCategory.Range, 8));

		ItemStack multishot = new ItemStack(CROSSBOW);
		multishot.addEnchantment(MULTISHOT, 1);
		ItemMeta multishot_meta = multishot.getItemMeta();
		multishot_meta.setCustomModelData(1);
		multishot_meta.displayName(translatable("crystalized.crossbow.multi.name").decoration(ITALIC, false));
		multishot.setItemMeta(multishot_meta);
		List<Component> multishot_lore = new ArrayList<>();
		multishot_lore.add(translatable("crystalized.crossbow.multi.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(multishot, 2000, multishot_lore, ItemCategory.Range, 26));

		creation_number = 1; // reset id

		return lsItems;
	}

	public static ItemStack get_arrows() {
		ItemStack arrow = new ItemStack(ARROW, 6);
		ItemMeta arrow_meta = arrow.getItemMeta();
		arrow_meta.displayName(translatable("crystalized.item.arrows.name").color(WHITE).decoration(ITALIC, false));
		arrow.setItemMeta(arrow_meta);
		return arrow;
	}

	// this can handle null being passed in
	public ItemStack buildDisplayItem(Player p) {
		List<Component> lore;
		if (description == null) {
			lore = new ArrayList<>();
		} else {
			lore = new ArrayList<>(description);
		}
		lore.add(Component.text("")); // add a newline so that the price is seperated
		if (p != null && (Litestrike.getInstance().game_controller.getPlayerData(p).getMoney() - price) >= 0) {
			lore.add(Component.text("" + price + "\uE104").color(WHITE).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("" + price + "\uE104").color(RED).decoration(TextDecoration.ITALIC, false));
		}

		ItemStack displayItem = item.clone();
		ItemMeta meta = displayItem.getItemMeta();
		meta.lore(lore);
		displayItem.setItemMeta(meta);
		return displayItem;
	}

	public static ItemCategory getItemCategory(ItemStack i) {
		for (LSItem lsi : LSItem.createItems()) {
			if (i.getType() == lsi.item.getType()) {
				return lsi.categ;
			}
		}

		String name = i.getType().name();
		// hardcoding default equipment because it isnt in LSITEM
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

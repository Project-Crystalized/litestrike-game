package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static org.bukkit.enchantments.Enchantment.*;

public class LSItem {
	public final ItemStack item;
	public final ItemStack displayItem;
	public final String description;
	public final int price;
	public final String name;
	public final Integer model;
	public final Integer slot;

	public final Enchantment enchant;
	private final Integer pow;

	public enum ItemCategory {
		Melee,
		Range,
		Armor,
		Ammunition,
		Consumable,
		Defuser
	}

	public final ItemCategory categ;

	public LSItem(ItemStack item, ItemStack displayItem, int price, String description, ItemCategory cate, String name,
			Enchantment enchant, Integer pow, Integer model, Integer slot) {
		this.item = item;
		this.displayItem = displayItem;
		this.price = price;
		this.description = description;
		this.categ = cate;
		this.name = name;
		this.enchant = enchant;
		this.pow = pow;
		this.model = model;
		this.slot = slot;
	}

	public static List<LSItem> createItems() {
		/*
		 * To-Do List for hard coding new items:
		 * 1. create new LSItem object
		 * 2. add the new LSItem object to the lsItems list
		 * 3. code a new branch to the switch in getItemCategory()
		 */

		LSItem diamondChestplate = new LSItem(new ItemStack(Material.DIAMOND_CHESTPLATE),
				new ItemStack(Material.DIAMOND_CHESTPLATE), 500, null, ItemCategory.Armor, null, PROTECTION, 1, null, 31);
		LSItem ironSword = new LSItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SWORD), 750,
				"stab stab", ItemCategory.Melee, null, null, null, null, 0);
		LSItem stoneSword = new LSItem(new ItemStack(Material.STONE_SWORD), new ItemStack(Material.IRON_SWORD), 0, null,
				ItemCategory.Melee, null, null, null, null, null);
		LSItem ironAxe = new LSItem(new ItemStack(Material.IRON_AXE), new ItemStack(Material.IRON_AXE), 1750, null,
				ItemCategory.Melee, null, null, null, null, 2);
		LSItem bow = new LSItem(new ItemStack(Material.BOW), new ItemStack(Material.BOW), 0, null, ItemCategory.Range, null,
				null, null, null, null);
		LSItem arrow = new LSItem(new ItemStack(Material.ARROW, 6), new ItemStack(Material.ARROW, 6), 100, null,
				ItemCategory.Ammunition, null, null, null, null, 50);
		LSItem defuser = new LSItem(new ItemStack(Material.IRON_PICKAXE), new ItemStack(Material.IRON_PICKAXE), 100,
				"Don't be a loser buy a defuser -Tubbo", ItemCategory.Defuser, "Defuser", null, null, null, null);
		LSItem pickaxe = new LSItem(new ItemStack(Material.STONE_PICKAXE), new ItemStack(Material.STONE_PICKAXE), 0, null,
				ItemCategory.Defuser, null, null, null, null, null);
		LSItem gapple = new LSItem(new ItemStack(Material.GOLDEN_APPLE), new ItemStack(Material.GOLDEN_APPLE), 500, null,
				ItemCategory.Consumable, null, null, null, null, 49);
		LSItem ironChestplate = new LSItem(new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_CHESTPLATE),
				250, null, ItemCategory.Armor, null, PROTECTION, 1, null, 40);
		LSItem quickdraw = new LSItem(new ItemStack(Material.CROSSBOW), new ItemStack(Material.CROSSBOW), 2000,
				"A crossbow that draws lightning fast.", ItemCategory.Range, "Quickdraw Crossbow", QUICK_CHARGE, 1, 2, 24);
		LSItem pufferFish = new LSItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SWORD), 1250,
				"Adds poison 1 to the player when hit.", ItemCategory.Melee, "Pufferfish Sword", null, null, 2, 18);
		LSItem slimeSword = new LSItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SWORD), 1000,
				"Adds slowness 1 to the player when hit.", ItemCategory.Melee, "Slime Sword", KNOCKBACK, 1, 1, 20);
		LSItem marksman = new LSItem(new ItemStack(Material.BOW), new ItemStack(Material.BOW), 750, null,
				ItemCategory.Range, "Marksman Bow", POWER, 1, 1, 6);
		LSItem ricochet = new LSItem(new ItemStack(Material.BOW), new ItemStack(Material.BOW), 1500,
				"A bouncy bow with bouncy arrows.", ItemCategory.Range, "Ricochet Bow", PUNCH, 1, 2, 8);
		LSItem multishot = new LSItem(new ItemStack(Material.CROSSBOW), new ItemStack(Material.CROSSBOW), 2000,
				"A crossbow that shoots multiple arrows.", ItemCategory.Range, "Multishot Crossbow", MULTISHOT, 1, 1, 26);

		List<LSItem> lsItems = new ArrayList<>();

		lsItems.add(diamondChestplate);// 0
		lsItems.add(ironSword); // 1
		lsItems.add(stoneSword);// 2
		lsItems.add(ironAxe);// 3
		lsItems.add(bow);// 4
		lsItems.add(arrow);// 5
		lsItems.add(defuser);// 6
		lsItems.add(pickaxe);// 7
		lsItems.add(gapple);// 8
		lsItems.add(ironChestplate);// 19
		lsItems.add(quickdraw);// 10
		lsItems.add(pufferFish);// 11
		lsItems.add(slimeSword);// 12
		lsItems.add(marksman); // 13
		lsItems.add(ricochet);// 14
		lsItems.add(multishot);// 15

		for (LSItem ls : lsItems) {
			nameItem(ls);
			enchantItem(ls);
			setModel(ls);
		}

		return lsItems;
	}

	public static void updateDescription(Player p, LSItem display) {

		if ((Litestrike.getInstance().game_controller.getPlayerData(p).getMoney() - display.price) >= 0) {
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("" + display.price + "\n").color(WHITE).decoration(TextDecoration.ITALIC, false));
			if (display.description != null) {
				lore.add(Component.text(display.description).color(WHITE).decoration(TextDecoration.ITALIC, false));
			}
			ItemMeta meta = display.displayItem.getItemMeta();
			meta.lore(lore);
			display.displayItem.setItemMeta(meta);
		} else {
			List<Component> lore = new ArrayList<>();
			ItemMeta meta = display.displayItem.getItemMeta();
			lore.add(Component.text("" + display.price).color(RED).decoration(TextDecoration.ITALIC, false));
			if (display.description != null) {
				lore.add(Component.text(display.description).decoration(TextDecoration.ITALIC, false));
			}
			meta.lore(lore);
			display.displayItem.setItemMeta(meta);
		}

	}

	public static String getItemCategory(ItemStack i) {
		switch (i.getType()) {
			case Material.IRON_SWORD: {
				return "Melee";
			}
			case Material.IRON_AXE: {
				return "Melee";
			}
			case Material.STONE_SWORD: {
				return "Melee";
			}
			case Material.BOW: {
				return "Range";
			}
			case Material.ARROW: {
				return "Ammunition";
			}
			case Material.IRON_PICKAXE: {
				return "Defuser";
			}
			case Material.STONE_PICKAXE: {
				return "Defuser";
			}
			case Material.GOLDEN_APPLE: {
				return "Consumable";
			}
			case Material.IRON_CHESTPLATE: {
				return "Armor";
			}
			case Material.CROSSBOW: {
				return "Range";
			}
		}
		return "no_value";
	}

	public static void nameItem(LSItem i) {
		if (i.name == null) {
			return;
		}
		ItemMeta meta = i.item.getItemMeta();
		ItemMeta metadis = i.displayItem.getItemMeta();
		meta.itemName(Component.text(i.name));
		metadis.itemName(Component.text(i.name));
		i.item.setItemMeta(meta);
		i.displayItem.setItemMeta(metadis);
	}

	public static void enchantItem(LSItem i) {
		if (i.enchant == null || i.pow == null) {
			return;
		}
		ItemMeta meta = i.item.getItemMeta();
		ItemMeta metadis = i.displayItem.getItemMeta();
		meta.addEnchant(i.enchant, i.pow, true);
		metadis.addEnchant(i.enchant, i.pow, true);
		i.item.setItemMeta(meta);
		i.displayItem.setItemMeta(metadis);
	}

	public static void setModel(LSItem i) {
		if (i.model == null) {
			return;
		}
		ItemMeta meta = i.item.getItemMeta();
		ItemMeta metadis = i.displayItem.getItemMeta();
		meta.setCustomModelData(i.model);
		metadis.setCustomModelData(i.model);
		i.item.setItemMeta(meta);
		i.displayItem.setItemMeta(metadis);
	}
}

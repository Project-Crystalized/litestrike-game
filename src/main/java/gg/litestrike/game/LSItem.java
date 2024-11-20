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
import static org.bukkit.Material.*;

public class LSItem {
	public final ItemStack item;
	public final String description;
	public final Integer price;
	public final Integer slot;

	public enum ItemCategory {
		Melee,
		Range,
		Armor,
		Ammunition,
		Consumable,
		Defuser
	}

	public final ItemCategory categ;

	private LSItem(Material material, Integer price, String description, ItemCategory cate, String name,
			Enchantment enchant, Integer pow, Integer model, Integer slot) {
		this.price = price;
		this.description = description;
		this.categ = cate;
		this.slot = slot;

		this.item = new ItemStack(material);
		if (material == ARROW) {
			// this is a special case for arrows
			item.setAmount(6);
		}

		this.enchantItem(enchant, pow);
		this.nameItem(name);
		this.setModel(model);
	}

	public static List<LSItem> createItems() {
		/*
		 * To-Do List for hard coding new items:
		 * 1. create new LSItem object
		 * 2. add the new LSItem object to the lsItems list
		 * 3. code a new branch to the switch in getItemCategory()
		 */

		LSItem diamondChestplate = new LSItem(DIAMOND_CHESTPLATE, 500, null, ItemCategory.Armor, null, PROTECTION, 1, null, 31);
		LSItem ironSword = new LSItem(IRON_SWORD, 750, "stab stab", ItemCategory.Melee, null, null, null, null, 0);
		LSItem stoneSword = new LSItem(STONE_SWORD, null, null, ItemCategory.Melee, null, null, null, null, null);
		LSItem ironAxe = new LSItem(IRON_AXE, 1750, null, ItemCategory.Melee, null, null, null, null, 2);
		LSItem bow = new LSItem(BOW, null, null, ItemCategory.Range, null, null, null, null, null);
		LSItem arrow = new LSItem(ARROW, 100, null, ItemCategory.Ammunition, null, null, null, null, 50);
		LSItem defuser = new LSItem(IRON_PICKAXE, 100, "Don't be a loser buy a defuser -Tubbo", ItemCategory.Defuser, "Defuser", null, null, null, null);
		LSItem pickaxe = new LSItem(STONE_PICKAXE, null, null, ItemCategory.Defuser, null, null, null, null, null);
		LSItem gapple = new LSItem(GOLDEN_APPLE, 500, null, ItemCategory.Consumable, null, null, null, null, 49);
		LSItem ironChestplate = new LSItem(IRON_CHESTPLATE, 250, null, ItemCategory.Armor, null, PROTECTION, 1, null, 40);
		LSItem quickdraw = new LSItem(CROSSBOW, 2000, "A crossbow that draws lightning fast.", ItemCategory.Range, "Quickdraw Crossbow", QUICK_CHARGE, 1, 2, 24);
		LSItem pufferFish = new LSItem(IRON_SWORD, 1250, "Adds poison 1 to the player when hit.", ItemCategory.Melee, "Pufferfish Sword", null, null, 2, 18);
		LSItem slimeSword = new LSItem(IRON_SWORD, 1000, "Adds slowness 1 to the player when hit.", ItemCategory.Melee, "Slime Sword", KNOCKBACK, 1, 1, 20);
		LSItem marksman = new LSItem(BOW, 750, null, ItemCategory.Range, "Marksman Bow", POWER, 1, 1, 6);
		LSItem ricochet = new LSItem(BOW, 1500, "A bouncy bow with bouncy arrows.", ItemCategory.Range, "Ricochet Bow", PUNCH, 1, 2, 8);
		LSItem multishot = new LSItem(CROSSBOW, 2000, "A crossbow that shoots multiple arrows.", ItemCategory.Range, "Multishot Crossbow", MULTISHOT, 1, 1, 26);

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

		return lsItems;
	}

	// this can handle null being passed in
	public ItemStack buildDisplayItem(Player p) {
		List<Component> lore = new ArrayList<>();
		if (p != null && (Litestrike.getInstance().game_controller.getPlayerData(p).getMoney() - price) >= 0) {
			lore.add(Component.text("" + price + "\n").color(WHITE).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("" + price).color(RED).decoration(TextDecoration.ITALIC, false));
		}

		if (description != null) {
			lore.add(Component.text(description).color(WHITE).decoration(TextDecoration.ITALIC, false));
		}
		ItemStack displayItem = item.clone();
		ItemMeta meta = displayItem.getItemMeta();
		meta.lore(lore);
		displayItem.setItemMeta(meta);
		return displayItem;
	}

	public void nameItem(String name) {
		if (name == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		meta.itemName(Component.text(name));
		item.setItemMeta(meta);
	}

	public void enchantItem(Enchantment enchant, Integer pow) {
		if (enchant == null || pow == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(enchant, pow, true);
		item.setItemMeta(meta);
	}

	public void setModel(Integer model) {
		if (model == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		meta.setCustomModelData(model);
		item.setItemMeta(meta);
	}

	public static ItemCategory getItemCategory(ItemStack i) {
		switch (i.getType()) {
			case IRON_SWORD:
			case IRON_AXE: 
			case STONE_SWORD: 
				return ItemCategory.Melee;
			case CROSSBOW:
			case BOW:
				return ItemCategory.Range;
			case ARROW: {
				return ItemCategory.Ammunition;
			}
			case IRON_PICKAXE:
			case STONE_PICKAXE:
				return ItemCategory.Defuser;
			case GOLDEN_APPLE: {
				return ItemCategory.Consumable;
			}
			case IRON_CHESTPLATE: {
				return ItemCategory.Armor;
			}
			default:
				return null;
		}
	}
}


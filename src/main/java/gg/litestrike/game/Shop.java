package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import gg.litestrike.game.LSItem.ItemCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.EMERALD;
import static org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE;
import static org.bukkit.enchantments.Enchantment.*;

public class Shop {
	public Inventory currentView;
	public String player;
	public HashMap<LSItem.ItemCategory, LSItem> currentEquip = new HashMap<>();
	public HashMap<LSItem.ItemCategory, LSItem> previousEquip = new HashMap<>();
	public HashMap<LSItem, Integer> consAndAmmoCount = new HashMap<>();
	public List<LSItem> shopLog;
	public static HashMap<String, Shop> shopList = new HashMap<>();

	public static final int DEFUSER_SLOT = 22;

	public Shop(Player p) {
		if (p == null) {
			return;
		}

		shopList.put(p.getName(), this);
		player = p.getName();
		currentView = Bukkit.getServer().createInventory(null, 54, title(p.getName()));
		shopLog = new ArrayList<>();
		// TODO make the Shop its own item
	}

	public static Shop getShop(Player p) {
		return shopList.get(p.getName());
	}

	private void setItems() {
		for (LSItem item : LSItem.shopItems) {
			if (item == null || item.slot == null) {
				continue;
			}
			if (item.categ == ItemCategory.Defuser
					&& Teams.get_team(player) != Team.Breaker) {
				continue;
			}
			currentView.setItem(item.slot, item.buildDisplayItem(player));
		}
	}

	private static Component title(String p) {
		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
		return Component.text("\uA000" + "\uA001" + "\uE104" + pd.getMoney()).color(WHITE);
	}

	public void update_shop() {
		currentView = Bukkit.getServer().createInventory(null, 54, title(player));
		setItems();
	}

	public void open_shop() {
		update_shop();
		Bukkit.getPlayer(player).openInventory(currentView);
	}

	// this is called once in next_round()
	public static void giveShop_and_update() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			Shop s = getShop(p);
			s.shopLog.add(null);
			s.update_shop();

			ItemStack shop = new ItemStack(EMERALD, 1);
			ItemMeta meta = shop.getItemMeta();
			meta.displayName(Component.text("Shop").color(WHITE).decoration(ITALIC, false));
			List<TextComponent> list = new ArrayList<>();
			list.add(Component.text("right click").color(GRAY).decoration(ITALIC, false));
			meta.lore(list);
			shop.setItemMeta(meta);
			if (p.getInventory().getItem(8) == null || p.getInventory().getItem(8).isEmpty()) {
				p.getInventory().setItem(8, shop);
			} else {
				p.getInventory().addItem(shop);
			}
		}
	}

	public static void giveDefaultArmor(Player p) {
		GameController gc = Litestrike.getInstance().game_controller;
		PlayerInventory inv = p.getInventory();
		ItemStack arrows_item = new ItemStack(Material.ARROW, 6);

		// give 6 arrows back
		int arrows = 0;
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType() == Material.ARROW) {
				arrows += is.getAmount();
			}
		}
		if (arrows < 6) {
			arrows_item.setAmount(6 - arrows);
			inv.addItem(arrows_item);
		}

		if (!(p.getGameMode() == GameMode.SPECTATOR || gc.round_number == 1
				|| gc.round_number == GameController.SWITCH_ROUND + 1
				|| gc.round_number == (GameController.SWITCH_ROUND * 2) + 1)) {
			// no need to give equipment
			return;
		}

		Team player_team = gc.teams.get_team(p);
		inv.clear();
		p.setItemOnCursor(null);
		inv.setItem(0, new ItemStack(Material.STONE_SWORD));
		inv.setItem(1, new ItemStack(Material.BOW));
		arrows_item.setAmount(6);
		inv.setItem(2, arrows_item);

		Color c = null;
		Color boot_color = null;
		if (player_team == Team.Placer) {
			c = Color.fromRGB(0xe31724);
			boot_color = Color.fromRGB(0xe88b28);
		} else if (player_team == Team.Breaker) {
			c = Color.fromRGB(0x0f9415);
			boot_color = Color.fromRGB(0x8119c9);
			inv.addItem(new ItemStack(Material.STONE_PICKAXE));
		}
		inv.setHelmet(colorArmor(c, new ItemStack(Material.LEATHER_HELMET), 1));
		inv.setChestplate(colorArmor(c, new ItemStack(Material.LEATHER_CHESTPLATE), 1));
		inv.setLeggings(colorArmor(c, new ItemStack(Material.LEATHER_LEGGINGS), 1));
		inv.setBoots(colorArmor(boot_color, new ItemStack(Material.LEATHER_BOOTS), 2));

		// give unbreakable to all items
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType().getMaxDurability() > 0) {
				ItemMeta im = is.getItemMeta();
				im.setUnbreakable(true);
				is.setItemMeta(im);
				is.addItemFlags(HIDE_UNBREAKABLE);
			}
		}
	}

	public static ItemStack colorArmor(Color c, ItemStack i, int ench_level) {
		LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
		lam.setColor(c);
		i.setItemMeta(lam);
		i.addEnchantment(PROTECTION, ench_level);
		return i;
	}

	public boolean alreadyHasThis(ItemStack item) {
		for (int i = 0; i <= 40; i++) {
			if (Bukkit.getPlayer(player).getInventory().getItem(i) == null) {
				continue;
			}
			ItemStack it = Bukkit.getPlayer(player).getInventory().getItem(i);
			if (it == null) {
				continue;
			}
			if (LSItem.is_underdog_sword(item) && LSItem.is_underdog_sword(it)) {
				return true;
			}

			if (it.getType() == item.getType()
					&& Objects.equals(ShopListener.identifyCustomModelData(it), ShopListener.identifyCustomModelData(item))) {
				return true;
			}
		}
		return false;
	}

	public int findInvIndex(ItemStack item) {
		for (int i = 0; i <= 40; i++) {
			if (Bukkit.getPlayer(player).getInventory().getItem(i) == null) {
				continue;
			}
			ItemStack it = Bukkit.getPlayer(player).getInventory().getItem(i);
			if (it == null) {
				continue;
			}

			if (LSItem.is_underdog_sword(item) && LSItem.is_underdog_sword(it)) {
				return i;
			}

			if (it.getType() == item.getType()
					&& Objects.equals(ShopListener.identifyCustomModelData(it), ShopListener.identifyCustomModelData(item))) {
				return i;
			}
		}
		return -1;
	}

	public int findInvIndex(LSItem.ItemCategory categ) {
		for (int i = 0; i <= 40; i++) {
			if (Bukkit.getPlayer(player).getInventory().getItem(i) == null) {
				continue;
			}
			ItemStack it = Bukkit.getPlayer(player).getInventory().getItem(i);
			if (it == null) {
				continue;
			}

			if (LSItem.getItemCategory(it) == categ) {
				return i;
			}
		}
		return -1;
	}

	public static void removeShop(Player p) {
		Inventory inv = p.getInventory();
		Shop s = getShop(p);
		for (int i = 0; i <= 40; i++) {
			if (inv.getItem(i) == null) {
				continue;
			}
			if (inv.getItem(i).getType() == EMERALD) {
				inv.clear(i);
			}
		}
		s.currentView.close();
		if (p.getItemOnCursor().getType() == EMERALD) {
			p.setItemOnCursor(null);
		}
	}

	public void resetEquip() {
		previousEquip.clear();
		currentEquip.clear();
		currentEquip.put(LSItem.ItemCategory.Melee, LSItem.shopItems.get(2));
		currentEquip.put(LSItem.ItemCategory.Range, LSItem.shopItems.get(4));
		if (Teams.get_team(player) == Team.Placer) {
			currentEquip.put(LSItem.ItemCategory.Armor, LSItem.shopItems.get(7));
		} else {
			currentEquip.put(LSItem.ItemCategory.Armor, LSItem.shopItems.get(6));
			currentEquip.put(LSItem.ItemCategory.Defuser, LSItem.shopItems.get(25));
		}
	}

	public void resetEquipCounters() {
		consAndAmmoCount.clear();
		for (LSItem item : LSItem.shopItems) {
			if (item.categ == LSItem.ItemCategory.Ammunition || item.categ == LSItem.ItemCategory.Consumable) {
				consAndAmmoCount.put(item, 0);
			}
		}
	}

	public void add_item(ItemStack item) {}

	// public void remove_item(ItemStack item) {}
}

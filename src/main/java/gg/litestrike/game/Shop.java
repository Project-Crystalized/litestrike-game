package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

import gg.litestrike.game.LSItem.ItemCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE;
import static org.bukkit.enchantments.Enchantment.*;

public class Shop implements InventoryHolder {
	public List<LSItem> shopItems;
	public Inventory currentView;
	public Player player;
	public List<LSItem> buyHistory;
	public List<LSItem> shopLog;
	public static HashMap<String, Shop> shopList = new HashMap<>();

	public static final int DEFUSER_SLOT = 22;

	public Shop(Player p) {
		if (p == null) {
			return;
		}

		shopList.put(p.getName(), this);
		shopItems = LSItem.createItems();
		player = p;
		currentView = Bukkit.getServer().createInventory(this, 54, title(p));
		buyHistory = new ArrayList<>();
		shopLog = new ArrayList<>();
		// TODO make the Shop its own item
		// TODO i think its fine, no need to make shop its own item
	}

	public static Shop getShop(Player p) {
		return shopList.get(p.getName());
	}

	public void setItems(List<LSItem> ware) {
		for (LSItem item : ware) {
			if (item == null || item.slot == null) {
				continue;
			}
			if (item.categ == ItemCategory.Defuser
					&& Litestrike.getInstance().game_controller.teams.get_team(player) != Team.Breaker) {
				continue;
			}
			currentView.setItem(item.slot, item.buildDisplayItem(player));
		}
	}

	public void setDefuser() {
		if (Litestrike.getInstance().game_controller.teams.get_team(player) == Team.Breaker) {
			currentView.setItem(DEFUSER_SLOT, shopItems.get(6).buildDisplayItem(player));
		}
	}

	@Override
	public @NotNull Inventory getInventory() {
		return Bukkit.getServer().createInventory(this, 1);
	}

	public static Component title(Player p) {
		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
		return Component.text("\uA000" + "\uA001" + "\uE104" + pd.getMoney()).color(WHITE);
	}

	public void updateTitle(boolean openAgain) {
		Inventory inv = Bukkit.getServer().createInventory(this, 54, title(player));
		if (openAgain) {
			player.openInventory(inv);
			currentView = inv;
			setItems(shopItems);
			setDefuser();
		} else {
			currentView.close();
		}
	}

	public static void giveShop() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			Shop s = getShop(p);
			s.updateTitle(false);
			p.getInventory().addItem(new ItemStack(Material.EMERALD, 1));
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

	private static ItemStack colorArmor(Color c, ItemStack i, int ench_level) {
		LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
		lam.setColor(c);
		i.setItemMeta(lam);
		i.addEnchantment(PROTECTION, ench_level);
		return i;
	}

	public boolean alreadyHasThis(ItemStack item) {
		for (int i = 0; i <= 40; i++) {
			if (player.getInventory().getItem(i) == null) {
				continue;
			}
			ItemStack it = player.getInventory().getItem(i);
			if (it == null) {
				continue;
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
			if (player.getInventory().getItem(i) == null) {
				continue;
			}
			ItemStack it = player.getInventory().getItem(i);
			if (it == null) {
				continue;
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
			if (player.getInventory().getItem(i) == null) {
				continue;
			}
			ItemStack it = player.getInventory().getItem(i);
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
			if (inv.getItem(i).getType() == Material.EMERALD) {
				inv.clear(i);
			}
		}
		s.currentView.close();
	}

	public static ItemStack getBasicKid(ItemCategory cate, Player p) {

		GameController gc = Litestrike.getInstance().game_controller;
		if (cate == ItemCategory.Melee) {
			return new ItemStack(Material.STONE_SWORD);
		} else if (cate == ItemCategory.Range) {
			return new ItemStack(Material.BOW);
		} else if (cate == ItemCategory.Defuser) {
			return new ItemStack(Material.STONE_PICKAXE);
		} else if (cate == ItemCategory.Armor) {
			if (gc.teams.get_team(p.getName()) == Team.Placer) {
				return Shop.colorArmor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_CHESTPLATE), 1);
			} else {
				return Shop.colorArmor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_CHESTPLATE), 1);
			}
		} else if (cate == ItemCategory.Ammunition) {
			ItemStack arrow = new ItemStack(Material.ARROW, 6);
			ItemMeta meta = arrow.getItemMeta();
			meta.itemName(translatable("crystalized.item.arrows.name").color(WHITE).decoration(ITALIC, false));
			return arrow;
		}
		return null;
	}

	public void removeFromBuyHistory(LSItem item, LSItem lsitem) {
		ArrayList<Integer> list = new ArrayList<>();
		for (int i = 0; i <= buyHistory.size() - 1; i++) {
			if (buyHistory.get(i) == null) {
				continue;
			}
			if (buyHistory.get(i) == item) {
				list.add(i);
			}
			if (lsitem != null) {
				if (buyHistory.get(i) == lsitem) {
					boolean b = true;
					for (Integer j : list) {
						if (j == i) {
							b = false;
						}
					}

					if (b) {
						list.add(i);
					}
				}
			}
		}
		for (Integer i : list) {
			buyHistory.remove(i.intValue());
		}
	}
}

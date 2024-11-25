package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class Shop implements InventoryHolder {
	public List<LSItem> shopItems;
	public Inventory currentView;
	public Player player;
	public List<LSItem> buyHistory;
	public static HashMap<String, Shop> shopList = new HashMap<>();

	public Shop(Player p) {
		if (p == null) {
			return;
		}

		shopList.put(p.getName(), this);
		shopItems = LSItem.createItems();
		player = p;
		currentView = Bukkit.getServer().createInventory(this, 54, title(p));
		buyHistory = new ArrayList<>();
		// TODO make the Shop its own item
		// TODO i think its fine, no need to make shop its own item
	}

	public static Shop getShop(Player p) {
		return shopList.get(p.getName());
	}

	public void setItems(List<LSItem> ware) {
		Inventory i = currentView;
		for (LSItem item : ware) {
			if (item == null) {
				continue;
			}
			if (item.slot != null) {
				i.setItem(item.slot, item.buildDisplayItem(player));
			}
		}
	}

	public void setDefuser() {
		if (Litestrike.getInstance().game_controller.teams.get_team(player) == Team.Breaker) {
			currentView.setItem(22, shopItems.get(6).buildDisplayItem(player));
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

	public void updateTitle() {
		currentView.close();
		currentView = Bukkit.getServer().createInventory(this, 54, title(player));
		player.openInventory(currentView);
		setItems(shopItems);
		setDefuser();
	}

	public static void giveShop() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.getInventory().addItem(new ItemStack(Material.EMERALD, 1));
		}
	}

	public static void giveDefaultArmor(Player p) {
		GameController gc = Litestrike.getInstance().game_controller;
		PlayerInventory inv = p.getInventory();

		// give 6 arrows back
    int arrows = 0;
    for(ItemStack is : inv.getContents()) {
        if(is != null && is.getType() == Material.ARROW) {
            arrows += is.getAmount();
        }
    }
		if (arrows < 6) {
			inv.addItem(new ItemStack(Material.ARROW, 6-arrows));
		}

		if (!(p.getGameMode() == GameMode.SPECTATOR || gc.round_number == 1
				|| gc.round_number == GameController.SWITCH_ROUND + 1)) {
			// no need to give equipment
			return;
		}

		Team player_team = gc.teams.get_team(p);
		inv.clear();
		inv.setItem(0, new ItemStack(Material.STONE_SWORD));
		inv.setItem(1, new ItemStack(Material.BOW));
		inv.setItem(2, new ItemStack(Material.ARROW, 6));

		Color c = null;
		if (player_team == Team.Placer) {
			c = Color.fromRGB(0xe31724);
		} else if (player_team == Team.Breaker) {
			c = Color.fromRGB(0x0f9415);
			inv.addItem(new ItemStack(Material.STONE_PICKAXE));
		}
		inv.setHelmet(colorArmor(c, new ItemStack(Material.LEATHER_HELMET)));
		inv.setChestplate(colorArmor(c, new ItemStack(Material.LEATHER_CHESTPLATE)));
		inv.setLeggings(colorArmor(c, new ItemStack(Material.LEATHER_LEGGINGS)));
		inv.setBoots(colorArmor(c, new ItemStack(Material.LEATHER_BOOTS)));

		// give unbreakable to all items
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType().getMaxDurability() > 0) {
				ItemMeta im = is.getItemMeta();
				im.setUnbreakable(true);
				is.setItemMeta(im);
			}
		}
	}

	private static ItemStack colorArmor(Color c, ItemStack i) {
		LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
		lam.setColor(c);
		i.setItemMeta(lam);
		return i;
	}

	public boolean alreadyHasThis(ItemStack item) {
		for (int i = 0; i <= 40; i++) {
			if (player.getInventory().getItem(i) == null) {
				continue;
			}
			Component it = player.getInventory().getItem(i).displayName();
			if (it == null) {
				continue;
			}
			Component ite = item.displayName();
			PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
			String itName = plainSerializer.serialize(it);
			String iteName = plainSerializer.serialize(ite);
			if (itName.equals(iteName)) {
				return true;
			}
		}
		return false;
	}

	public static void removeShop(Player p) {
		Inventory inv = p.getInventory();
		Shop s = getShop(p);
		for (int i = 0; i <= 40; i++) {
			ItemStack it = p.getInventory().getItem(i);
			if (it == null) {
				continue;
			}
			if (it.getType() == Material.EMERALD) {
				inv.clear(i);
				s.currentView.close();
			}
		}
	}
}

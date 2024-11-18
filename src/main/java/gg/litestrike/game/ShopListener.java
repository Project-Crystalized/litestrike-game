package gg.litestrike.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ShopListener implements Listener {

	@EventHandler
	public void openShop(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		Shop s = Shop.getShop(p);
		if (event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK) {
			if (p.getInventory().getItemInMainHand().getType() == Material.EMERALD) {
				p.openInventory(s.currentView);
				s.setItems(s.shopItems);
				s.setDefuser();
			}
		}
	}

	@EventHandler
	public void buyItem(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		Shop s = Shop.getShop(p);
		if (event.getCurrentItem() == null) {
			return;
		}
		if (event.getInventory() != s.currentView) {
			return;
		}
		event.setCancelled(true);
		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);

		for (LSItem lsitem : s.shopItems) {
			if (lsitem.slot == null || lsitem.slot != event.getSlot()) {
				continue;
			}

			// if the item is not ammuntion and also not a consumable and we already have
			// it, then we cant but it
			if (lsitem.categ != LSItem.ItemCategory.Ammunition && lsitem.categ != LSItem.ItemCategory.Consumable
					&& Shop.alreadyHasThis(p, lsitem.item)) {
				p.sendMessage(Component.text("You already have this item").color(RED));
				p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
				return;
			}
			// check that we have enough money
			if (!pd.removeMoney(lsitem.price)) {
				p.sendMessage(Component.text("Cant afford this").color(RED));
				p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
				return;
			}

			if (lsitem.categ == LSItem.ItemCategory.Armor) {
				p.getInventory().setChestplate(lsitem.item);
			} else if (Shop.findInvIndex(p, lsitem) == -1) {
				p.getInventory().addItem(lsitem.item);
			} else {
				p.getInventory().setItem(Shop.findInvIndex(p, lsitem), lsitem.item);
			}
			p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
			s.updateTitle();
			s.buyHistory.add(lsitem);
			return;
		}
	}

	public void undoBuy(ItemStack item, Player p, int slot) {
		String cate = LSItem.getItemCategory(item);
		Shop s = Shop.getShop(p);
		List<LSItem> h = s.buyHistory;
		switch (cate) {
			case "Melee": {
				for (int i = h.size(); i == 0; i--) {
					if (LSItem.getItemCategory(h.get(i).item).equals("Melee")) {
						if (item.getType() == Material.STONE_SWORD) {
							return;
						}
						p.getInventory().setItem(slot, h.get(i).item);
						Litestrike.getInstance().game_controller.getPlayerData(p).giveMoneyBack(h.get(i).price);
						s.updateTitle();
						h.remove(i);
						return;
					}
				}
			}
			case "Range": {
				for (int i = h.size(); i == 0; i--) {
					if (LSItem.getItemCategory(h.get(i).item).equals("Range")) {
						if (item.getType() == Material.BOW && item.getEnchantments().isEmpty()) {
							return;
						}
						p.getInventory().setItem(slot, h.get(i).item);
						Litestrike.getInstance().game_controller.getPlayerData(p).giveMoneyBack(h.get(i).price);
						s.updateTitle();
						h.remove(i);
						return;
					}
				}
			}
			case "Ammunition": {
				for (int i = h.size(); i == 0; i--) {
					if (LSItem.getItemCategory(h.get(i).item).equals("Ammunition")) {
						if (item.getType() == Material.ARROW && !(item.hasItemMeta()) && item.getAmount() == 6) {
							return;
						}
						ItemStack n = new ItemStack(h.get(i).item.getType(),
								p.getInventory().getItem(slot).getAmount() - h.get(i).item.getAmount());
						p.getInventory().setItem(slot, n);
						Litestrike.getInstance().game_controller.getPlayerData(p).giveMoneyBack(h.get(i).price);
						s.updateTitle();
						return;
					}
				}
			}
		}
	}
}

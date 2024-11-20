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

import gg.litestrike.game.LSItem.ItemCategory;

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
					&& s.alreadyHasThis(lsitem.item)) {
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

			// remove items of same categ from inv
			if (lsitem.categ != ItemCategory.Ammunition && lsitem.categ != ItemCategory.Consumable) {
				for (int i = 0; i <= 40; i++) {
					ItemStack it = p.getInventory().getItem(i);
					if (it == null) {
						continue;
					}
					if (LSItem.getItemCategory(it) == lsitem.categ) {
						p.getInventory().clear(i);
					}
				}
			}

			if (lsitem.categ == ItemCategory.Armor) {
				p.getInventory().setChestplate(lsitem.item);
			} else {
				p.getInventory().addItem(lsitem.item);
			}
			p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
			s.updateTitle();
			s.buyHistory.add(lsitem);
			return;
		}
	}

	public void undoBuy(LSItem item, Player p, int slot) {
		Shop s = Shop.getShop(p);
		List<LSItem> h = s.buyHistory;
		for (int i = h.size(); i == 0; i--) {
			switch (item.categ) {
				case ItemCategory.Melee: {
					if (h.get(i).categ == ItemCategory.Melee) {
						if (item.item.getType() == Material.STONE_SWORD) {
							return;
						}
						p.getInventory().setItem(slot, h.get(i).item);
						Litestrike.getInstance().game_controller.getPlayerData(p).giveMoneyBack(h.get(i).price);
						s.updateTitle();
						h.remove(i);
						return;
					}
				}
				case ItemCategory.Range: {
					if (h.get(i).categ == ItemCategory.Range) {
						if (item.item.getType() == Material.BOW && item.item.getEnchantments().isEmpty()) {
							return;
						}
						p.getInventory().setItem(slot, h.get(i).item);
						Litestrike.getInstance().game_controller.getPlayerData(p).giveMoneyBack(h.get(i).price);
						s.updateTitle();
						h.remove(i);
						return;
					}
				}
				case ItemCategory.Ammunition: {
					if (h.get(i).categ == ItemCategory.Ammunition) {
						if (item.item.getType() == Material.ARROW && !(item.item.hasItemMeta()) && item.item.getAmount() == 6) {
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

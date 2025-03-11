package gg.litestrike.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import gg.litestrike.game.LSItem.ItemCategory;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ShopListener implements Listener {

	@EventHandler
	public void openShop(PlayerInteractEvent event) {
		if (event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK) {
			Player p = event.getPlayer();
			if (p.getInventory().getItemInMainHand().getType() == Material.EMERALD) {
				Shop s = Shop.getShop(p);
				s.open_shop();
			}
		}
	}

	@EventHandler
	public void buyItem(InventoryClickEvent event) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.round_state != RoundState.PreRound) {
			return;
		}
		Player p = (Player) event.getWhoClicked();
		Shop s = Shop.getShop(p);
		if (event.getCurrentItem() == null) {
			return;
		}
		if (event.getInventory() != s.currentView) {
			return;
		}
		event.setCancelled(true);

		if (event.isRightClick()) {
			undoBuy(event.getCurrentItem(), (Player) event.getWhoClicked(), event.getSlot());
			return;
		}

		LSItem clicked_item = null;
		for (LSItem lsitem : LSItem.shopItems) {
			if (lsitem.slot == null || lsitem.slot != event.getSlot()) {
				continue;
			}
			if (lsitem.slot == Shop.DEFUSER_SLOT && gc.teams.get_team(p) != Team.Breaker) {
				continue;
			}

			clicked_item = lsitem;
			break;
		}
		if (clicked_item == null) {
			return;
		}
		// if the item is not ammunition and also not a consumable, and we already have
		// it, then we cant buy it
		if (clicked_item.categ != ItemCategory.Ammunition && clicked_item.categ != ItemCategory.Consumable
				&& s.alreadyHasThis(clicked_item.item)) {
			p.sendMessage(Component.text("You already have this item").color(RED));
			p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
			return;
		}
		// check that we have enough money
		if (!gc.getPlayerData(p).removeMoney(clicked_item.price)) {
			p.sendMessage(Component.text("Cant afford this").color(RED));
			p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
			return;
		}

		// remove items of same categ from inv
		if (clicked_item.categ != ItemCategory.Ammunition && clicked_item.categ != ItemCategory.Consumable
				&& clicked_item.categ != ItemCategory.Armor) {
			int cont = s.findInvIndex(clicked_item.categ);
			if (cont == -1) {
				Bukkit.getLogger().severe("tried to get a -1 index for item: " + clicked_item.item.getType());
			}
			p.getInventory().clear(cont);
		}

		if (clicked_item.categ != ItemCategory.Ammunition && clicked_item.categ != ItemCategory.Consumable) {
			if (s.previousEquip.get(clicked_item.categ) != null) {
				s.previousEquip.replace(clicked_item.categ, s.currentEquip.get(clicked_item.categ));
			} else {
				s.previousEquip.put(clicked_item.categ, s.currentEquip.get(clicked_item.categ));
			}
			s.currentEquip.replace(clicked_item.categ, clicked_item);
		} else {
			int i = s.consAndAmmoCount.get(clicked_item);
			s.consAndAmmoCount.remove(clicked_item);
			s.consAndAmmoCount.put(clicked_item, i + 1);
		}

		if (clicked_item.categ == ItemCategory.Armor) {
			p.getInventory().setChestplate(clicked_item.item);
		} else {
			// underog
			if (LSItem.is_underdog_sword(clicked_item.item)) {
				p.getInventory().addItem(LSItem.do_underdog_sword(gc.teams.get_team(p)));
			} else {
				p.getInventory().addItem(clicked_item.item);
			}
		}
		p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
		s.open_shop();
		s.shopLog.add(clicked_item);
	}

	public void undoBuy(ItemStack item, Player p, int slot) {
		Shop s = Shop.getShop(p);
		GameController gc = Litestrike.getInstance().game_controller;
		LSItem lsitem = null;

		for (LSItem lsi : LSItem.shopItems) {
			// find corresponding LSItem to the item clicked by slot
			if (lsi.slot == null) {
				continue;
			}

			if (lsi.slot.equals(slot) && (lsi.item.getType() == item.getType())) {
				lsitem = lsi;
				break;
			}
		}

		if (lsitem == null) {
			return;
		}

		int invSlot = s.findInvIndex(lsitem.item);

		// go through the players inv and find the item we want to sell
		if (!(s.alreadyHasThis(lsitem.item))) {
			return;
		}

		if (p.getInventory().getItem(invSlot) == null) {
			return;
		}

		Inventory inv = p.getInventory();

		// check what ItemCategory it is and find the item
		if (lsitem.categ != LSItem.ItemCategory.Consumable && lsitem.categ != LSItem.ItemCategory.Ammunition) {
			if (s.previousEquip.get(lsitem.categ) == null) {
				return;
			}
			s.currentEquip.replace(lsitem.categ, s.previousEquip.get(lsitem.categ));
			inv.setItem(invSlot, s.previousEquip.get(lsitem.categ).item);
			s.previousEquip.remove(lsitem.categ);
		} else {
			if (s.consAndAmmoCount.get(lsitem) <= 0) {
				return;
			}
			int amount = p.getInventory().getItem(invSlot).getAmount() / lsitem.item.getAmount() - 1;
			int count = s.consAndAmmoCount.get(lsitem) - 1;
			if (count < 0) {
				return;
			}
			s.consAndAmmoCount.remove(lsitem);
			s.consAndAmmoCount.put(lsitem, count);
			inv.clear(invSlot);
			for (int i = amount; i > 0; i--) {
				if (i == amount) {
					inv.setItem(invSlot, lsitem.item);
				} else {
					inv.addItem(lsitem.item);
				}
			}

		}
		gc.getPlayerData(p).giveMoneyBack(lsitem.price);
		s.open_shop();
		p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));

		for (int i = 0; i < s.shopLog.size(); i++) {
			if (s.shopLog.get(i) == lsitem) {
				s.shopLog.remove(i);
			}
		}
	}

	public static Integer identifyCustomModelData(ItemStack item) {
		if (item.hasItemMeta()) {
			if (item.getItemMeta().hasCustomModelData()) {
				return item.getItemMeta().getCustomModelData();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}

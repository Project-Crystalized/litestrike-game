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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

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

		for (LSItem lsitem : s.shopItems) {
			if (lsitem.slot == null || (lsitem.slot != event.getSlot() && event.getSlot() != 49)) {
				continue;
			}
			if (lsitem.slot == Shop.DEFUSER_SLOT && gc.teams.get_team(p) != Team.Breaker) {
				continue;
			}

			Integer lsitemData;
			Integer iteData;

			if (lsitem.item.hasItemMeta()) {
				if (lsitem.item.getItemMeta().hasCustomModelData()) {
					lsitemData = lsitem.item.getItemMeta().getCustomModelData();
				} else {
					lsitemData = null;
				}
			} else {
				lsitemData = null;
			}

			if (event.getCurrentItem().hasItemMeta()) {
				if (event.getCurrentItem().getItemMeta().hasCustomModelData()) {
					iteData = event.getCurrentItem().getItemMeta().getCustomModelData();
				} else {
					iteData = null;
				}
			} else {
				iteData = null;
			}

			if (lsitem.item.getType() != event.getCurrentItem().getType() || !Objects.equals(lsitemData, iteData)) {
				continue;
			}
			// if the item is not ammunition and also not a consumable, and we already have
			// it, then we cant but it
			if (lsitem.categ != ItemCategory.Ammunition && lsitem.categ != ItemCategory.Consumable
					&& s.alreadyHasThis(lsitem.item)) {
				p.sendMessage(Component.text("You already have this item").color(RED));
				p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
				return;
			}
			// check that we have enough money
			if (!gc.getPlayerData(p).removeMoney(lsitem.price)) {
				p.sendMessage(Component.text("Cant afford this").color(RED));
				p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
				return;
			}

			// remove items of same categ from inv
			if (lsitem.categ != ItemCategory.Ammunition && lsitem.categ != ItemCategory.Consumable
					&& lsitem.categ != ItemCategory.Armor) {
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
			s.updateTitle(lsitem, true);
			s.shopLog.add(lsitem);
			s.buyHistory.add(lsitem);
			return;
		}
	}

	public void undoBuy(ItemStack item, Player p, int slot) {
		Shop s = Shop.getShop(p);
		GameController gc = Litestrike.getInstance().game_controller;
		ItemStack ite = null;
		LSItem lsitem = null;
		Integer invSlot = null;

		for (LSItem lsi : s.shopItems) {
			// find corresponding LSItem to the item clicked by slot
			if (lsi.slot == null) {
				continue;
			}

			if (lsi.slot.equals(slot) || (lsi.item.getType() == item.getType() && Objects.equals(identifyCustomModelData(lsi.item), identifyCustomModelData(item)))) {
				lsitem = lsi;
				break;
			}

		}

		if (lsitem == null) {
			return;
		}

		// go through the players inv and find the item we want to sell
		for (int i = 0; i <= 40; i++) {
			ite = p.getInventory().getItem(i);

			if (ite == null) {
				continue;
			}

			if (lsitem.item.getType() == ite.getType() && Objects.equals(identifyCustomModelData(ite), identifyCustomModelData(lsitem.item))) {
				invSlot = i;
				break;
			}

			if (i == 40) {
				return;
			}
		}

		if (invSlot == null) {
			return;
		}

		// go through the buyHistory and find and LSItem that has the same category but
		// isn't the same item
		LSItem hisitem = null;

		for (int j = s.buyHistory.size() - 1; j > 0; j--) {
			LSItem hist_item = s.buyHistory.get(j);

			if (hist_item == null) {
				return;
			}

			if (lsitem.categ == ItemCategory.Consumable || lsitem.categ == ItemCategory.Ammunition) {
				if (hist_item.categ == lsitem.categ) {
					hisitem = hist_item;
					break;
				}
			}
			if (hist_item.categ == lsitem.categ && hist_item.item != lsitem.item) {
				hisitem = hist_item;
				break;
			}
		}

		int amount = ite.getAmount() - lsitem.item.getAmount();
		ItemStack stack = null;

		if (hisitem == null && Litestrike.getInstance().game_controller.round_number == 1) {
			// p.sendMessage("giving basic kid...");
			stack = Shop.getBasicKid(lsitem.categ, p);
			// if we don't find any buys in the history we give the player the basic kid
		} else if (hisitem == null) {
			return;
		} else if (lsitem.categ == ItemCategory.Consumable || lsitem.categ == ItemCategory.Ammunition) {
			if (lsitem.item.getType() == Material.ARROW && ite.getAmount() == 6 && lsitem.modelData == null) {
				return;
			}
			if (!(amount <= 0)) {
				stack = new ItemStack(lsitem.item.getType(), amount);
				ItemMeta stack_meta = stack.getItemMeta();
				if(lsitem.name != null){
					stack_meta.displayName(lsitem.name);
				}
				if(lsitem.modelData != null){
					stack_meta.setCustomModelData(lsitem.modelData);
				}
				stack.setItemMeta(stack_meta);
			}

		} else {
			stack = hisitem.item;
		}

		if (stack == null) {
			p.getInventory().clear(invSlot);
		} else {
			p.getInventory().setItem(invSlot, stack);
		}

		gc.getPlayerData(p).giveMoneyBack(lsitem.price);
		s.updateTitle(lsitem, true);
		p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));

		ArrayList<Integer> list = new ArrayList<>();
		for (int i = 0; i <= s.buyHistory.size() - 1; i++) {
			if (s.buyHistory.get(i) == null) {
				continue;
			}
			if (s.buyHistory.get(i) == hisitem) {
				list.add(i);
			}

			if (s.buyHistory.get(i) == lsitem) {
				boolean b = true;
				for(Integer j : list) {
					if(j == i){
						b = false;
					}
				}

				if(b) {
					list.add(i);
				}
			}
		}
		for (Integer i : list) {
			s.buyHistory.remove(i.intValue());
		}

		for (int i = 0; i < s.shopLog.size(); i++) {
			if (s.shopLog.get(i) == lsitem) {
				s.shopLog.remove(i);
			}
		}
	}

	public Integer identifyCustomModelData(ItemStack item){
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

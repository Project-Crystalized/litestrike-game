package gg.litestrike.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import gg.litestrike.game.LSItem.ItemCategory;

import static java.util.Arrays.stream;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ShopListener implements Listener {

	@EventHandler
	public void openShop(PlayerInteractEvent event) {
		if (event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK) {
			Player p = event.getPlayer();
			if (p.getInventory().getItemInMainHand().getType() == Material.EMERALD) {
				Shop s = Litestrike.getInstance().game_controller.getShop(p);
				s.open_shop();
			}
		}
	}

	@EventHandler
	public void buyItem(InventoryClickEvent event) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null || gc.round_state != GameController.RoundState.PreRound) {
			return;
		}
		Player p = (Player) event.getWhoClicked();
		Shop s = gc.getShop(p);
		if (s == null) {
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

		/*
		Old rule
		int numberOfGapsInInventory = 0;
		for(ItemStack it : p.getInventory().getContents()){
			if(it == null || it.getType() != Material.GOLDEN_APPLE) continue;
			numberOfGapsInInventory = it.getAmount() + numberOfGapsInInventory;
		}*/

		//This fixes the bug with the apples, that it stole the money of the user while cancleing the buy
		//As well fixes the issue where the player throughs apples on the floor and buys more.
		//Here it will get the amount of apples bought, if no apples bought in this round buying sesion then returns 0
		//Warning: It will reset per round so technicly you can buy more apples if you saved them up, but that is fair.
		int applesBought = s.consAndAmmoCount.getOrDefault(clicked_item, 0);
		//Cheks if it is a golden apple and the amount is less or or equal to three
		//So per round the player can buy max 3 apples. Resets each round.
		if(clicked_item.item.getType() == Material.GOLDEN_APPLE && applesBought >= 3){
			//Let's the player know that only apples allowed per the sesion.
			p.sendMessage(Component.text("We won't sell you more, you got more than you need.", RED));
			//Plays the villager noice
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
			// underdog
			if (LSItem.is_underdog_sword(clicked_item.item)) {
				p.getInventory().addItem(LSItem.do_underdog_sword(gc.teams.get_team(p)));
			} else {
				//Code for apples was here before, moved up before money deduction to ensure it ain't stolen.

				//Made sure this is set to clone, to prevent the arrow ocasionaly being re written to be 1 in the shop
				//Cloning is safter than direcly putting it into the inventory
				p.getInventory().addItem(clicked_item.item.clone());
			}
		}
		p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
		s.open_shop();
		s.shopLog.add(clicked_item);

		//achievement shit, not giving but setup for ls_onlyweapons
		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
		switch (clicked_item.item.getType()) {
			case GOLDEN_APPLE,
				 POTION, SPLASH_POTION, LINGERING_POTION,
				 IRON_CHESTPLATE, DIAMOND_CHESTPLATE
					-> {
				pd.eligibleForOnlyWeaponsAchievement = false;
			}
		}
	}

	@EventHandler
	public void onItemPickup(EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player p) {
			//achievement shit, not giving but setup for ls_onlyweapons
			PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
			switch (e.getItem().getItemStack().getType()) {
				case GOLDEN_APPLE,
					 POTION, SPLASH_POTION, LINGERING_POTION
						-> {
					pd.eligibleForOnlyWeaponsAchievement = false;
				}
			}
		}
	}

	public void undoBuy(ItemStack item, Player p, int slot) {
		Shop s = Litestrike.getInstance().game_controller.getShop(p);
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
			if (LSItem.is_underdog_sword(s.previousEquip.get(lsitem.categ).item)) {
				inv.setItem(invSlot, LSItem.do_underdog_sword(Teams.get_team(p.getName())));
			} else {
				//Also made sure this is cloning as well, to prevent potential arrow over writting due to this
				//Again cloning is safer than directly putting stuff in
				inv.setItem(invSlot, s.previousEquip.get(lsitem.categ).item.clone());
			}
			s.previousEquip.remove(lsitem.categ);
		} else {
			if (s.consAndAmmoCount.get(lsitem) <= 0) {
				return;
			}
			// int amount = p.getInventory().getItem(invSlot).getAmount() /
			// lsitem.item.getAmount() - 1;
			int count = s.consAndAmmoCount.get(lsitem) - 1;
			if (count < 0) {
				return;
			}
			s.consAndAmmoCount.remove(lsitem);
			s.consAndAmmoCount.put(lsitem, count);

			ItemStack item_in_slot = inv.getItem(invSlot);
			if (item_in_slot.getAmount() - lsitem.item.getAmount() < 0) {
				return;
			}
			item_in_slot.setAmount(item_in_slot.getAmount() - lsitem.item.getAmount());
			inv.setItem(invSlot, item_in_slot);
			// inv.clear(invSlot);
			// for (int i = amount; i > 0; i--) {
			// if (i == amount) {
			// inv.setItem(invSlot, lsitem.item);
			// } else {
			// inv.addItem(lsitem.item);
			// }
			// }
		}
		if (!Litestrike.getInstance().getConfig().getBoolean("free-shop")) {
			gc.getPlayerData(p).giveMoneyBack(lsitem.price);
		}
		s.open_shop();
		p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));

		for (int i = 0; i < s.shopLog.size(); i++) {
			if (s.shopLog.get(i) == lsitem) {
				s.shopLog.remove(i);
			}
		}
	}

	public static String identifyItemModel(ItemStack item) {
		if (item.hasItemMeta()) {
			if (item.getItemMeta().hasItemModel()) {
				return item.getItemMeta().getItemModel().toString();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}

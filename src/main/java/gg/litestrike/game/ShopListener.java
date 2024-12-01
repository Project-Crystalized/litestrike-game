package gg.litestrike.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Color;
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
		GameController gc = Litestrike.getInstance().game_controller;

		if (event.isRightClick()) {
			undoBuy(event.getCurrentItem(), (Player) event.getWhoClicked(), event.getSlot());
			return;
		}

		PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);

		for (LSItem lsitem : s.shopItems) {
			if (lsitem.slot == null || lsitem.slot != event.getSlot()) {
				continue;
			}
			if (lsitem.slot == Shop.DEFUSER_SLOT && gc.teams.get_team(p) != Team.Breaker) {
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
			s.updateTitle(lsitem);
			s.buyHistory.add(lsitem);
			return;
		}
	}

	public void undoBuy(ItemStack item, Player p, int slot) {

		Shop s = Shop.getShop(p);
		GameController gc = Litestrike.getInstance().game_controller;

		LSItem lsitem = null;
		for (LSItem lsi : s.shopItems) {
			// find corresponding LSItem to the item clicked by slot
			if (lsi.item.getType() != Material.IRON_PICKAXE && lsi.slot != null) {
				if (lsi.slot != slot) {
					lsitem = lsi;
				}
			}
		}
		if (lsitem == null) {
			return;
		}

		// go through the players inv and find the item we want to sell
		for (int i = 0; i <= 40; i++) {
			ItemStack ite = p.getInventory().getItem(i);

			Component lsitemcom = lsitem.item.displayName();
			Component itecom = ite.displayName();
			PlainTextComponentSerializer ptcs = PlainTextComponentSerializer.plainText();
			String lsitemName = ptcs.serialize(lsitemcom);
			String iteName = ptcs.serialize(itecom);

			if (lsitemName.equals(iteName)) {
				// check what category the item is in order to sell properly
				if (lsitem.categ == LSItem.ItemCategory.Melee || lsitem.categ == LSItem.ItemCategory.Range
						|| lsitem.categ == LSItem.ItemCategory.Defuser) {
					// go through the buyHistory and find and LSItem that has the same category but
					// isn't the same item
					for (int j = s.buyHistory.size(); j > 1; j--) {
						LSItem hisitem = s.buyHistory.get(j);
						if (hisitem.categ == lsitem.categ && hisitem.item != lsitem.item) {
							p.getInventory().setItem(i, hisitem.item);
							gc.getPlayerData(p).addMoney(lsitem.price, "For selling an Item!");
							s.updateTitle(lsitem);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));
							return;
						}
					}
					// if we don't find any buys in the history we give the player the basic kit
					if (lsitem.categ == LSItem.ItemCategory.Melee) {
						p.getInventory().setItem(i, new ItemStack(Material.STONE_SWORD));
					} else if (lsitem.categ == LSItem.ItemCategory.Range) {
						p.getInventory().setItem(i, new ItemStack(Material.BOW));
					} else {
						p.getInventory().setItem(i, new ItemStack(Material.STONE_PICKAXE));
					}
					p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));
					gc.getPlayerData(p).addMoney(lsitem.price, "For selling an Item!");
					s.updateTitle(lsitem);
					return;
				} else if (lsitem.categ == LSItem.ItemCategory.Armor) {
					for (int j = s.buyHistory.size(); j > 1; j--) {
						LSItem hisitem = s.buyHistory.get(j);
						if (hisitem.categ == lsitem.categ && hisitem.item != lsitem.item) {
							p.getInventory().setChestplate(hisitem.item);
							gc.getPlayerData(p).addMoney(lsitem.price, "For selling an Item!");
							s.updateTitle(lsitem);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));
							return;
						}
					}
					if (gc.teams.get_team(p.getName()) == Team.Placer) {
						p.getInventory()
								.setChestplate(Shop.colorArmor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_CHESTPLATE)));
					} else {
						p.getInventory()
								.setChestplate(Shop.colorArmor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_CHESTPLATE)));
					}
					gc.getPlayerData(p).addMoney(lsitem.price, "For selling an Item!");
					s.updateTitle(lsitem);
					p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 3));
					return;
				} else if (lsitem.categ == LSItem.ItemCategory.Consumable || lsitem.categ == LSItem.ItemCategory.Ammunition) {
					if (lsitem.slot == 50 && ite.getAmount() == 6) {
						return;
					}
					for (int j = s.buyHistory.size(); j > 1; j--) {
						LSItem hisitem = s.buyHistory.get(j);
						if (hisitem.categ == lsitem.categ && hisitem.item != lsitem.item) {
							int amt = lsitem.item.getAmount();
							int amount = ite.getAmount();
							p.getInventory().setItem(i, new ItemStack(lsitem.item.getType(), amount - amt));
							gc.getPlayerData(p).addMoney(lsitem.price, "For selling an Item!");
							s.updateTitle(lsitem);
							return;
						}
					}
				}
			}
		}
	}
}

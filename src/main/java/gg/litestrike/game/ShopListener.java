package gg.litestrike.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ShopListener implements Listener {

	@EventHandler
	public void openShop(PlayerInteractEvent event) {
		event.setCancelled(false);
		Player p = event.getPlayer();
		Shop s = Shop.getShop(p);
		if (event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK) {
			if (p.getInventory().getItemInMainHand().getType() == Material.EMERALD) {
				p.openInventory(s.currentView);
				Shop.setItems(p, s.shopItems);
				Shop.setDefuser(p);
			}
		}
	}

	@EventHandler
	public void buyItem(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		Shop s = Shop.getShop(p);
		Inventory v = s.currentView;
		if (event.getCurrentItem() == null) {
			return;
		}
		/*
		 * if(event.getInventory() == v && event.getView().getBottomInventory() ==
		 * p.getInventory()){
		 * event.setCancelled(true);
		 * undoBuy(event.getCurrentItem(), p, event.getSlot());
		 * return;
		 * }
		 *
		 */
		if (event.getInventory() == v) {
			event.setCancelled(true);
			String mess = "Can't afford item";
			String mess2 = "You already have this item";
			PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
			Component itemDisplayName = event.getCurrentItem().displayName();
			PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
			String itemName = plainSerializer.serialize(itemDisplayName);

			switch (itemName) {

				case "[Diamond Chestplate]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(0).item)) {
						if (pd.removeMoney(s.shopItems.get(0).price)) {
							event.getWhoClicked().getInventory().setChestplate(s.shopItems.get(0).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(0));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Iron Chestplate]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(9).item)) {
						if (pd.removeMoney(s.shopItems.get(9).price)) {
							event.getWhoClicked().getInventory().setChestplate(s.shopItems.get(9).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(9));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Iron Sword]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(1).item)) {
						if (pd.removeMoney(s.shopItems.get(1).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(1)) == -1) {
								p.getInventory().addItem(s.shopItems.get(1).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(1)),
									s.shopItems.get(1).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(1));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}

				case "[Iron Axe]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(3).item)) {
						if (pd.removeMoney(s.shopItems.get(3).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(3)) == -1) {
								p.getInventory().addItem(s.shopItems.get(3).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(3)),
									s.shopItems.get(3).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(3));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}

				case "[Arrow]": {
					if (pd.removeMoney(s.shopItems.get(5).price)) {
						event.getWhoClicked().getInventory().addItem(s.shopItems.get(5).item);
						p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
						s.updateTitle(p);
						s.buyHistory.add(s.shopItems.get(5));
					} else {
						p.sendMessage(Component.text(mess).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Iron Pickaxe]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(6).item)) {
						if (pd.removeMoney(s.shopItems.get(6).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(6)) == -1) {
								p.getInventory().addItem(s.shopItems.get(6).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(6)),
									s.shopItems.get(6).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(6));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Golden Apple]": {
					if (pd.removeMoney(s.shopItems.get(8).price)) {
						event.getWhoClicked().getInventory().addItem(s.shopItems.get(8).item);
						p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
						s.updateTitle(p);
						s.buyHistory.add(s.shopItems.get(8));
					} else {
						p.sendMessage(Component.text(mess).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Quickdraw Crossbow]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(10).item)) {
						if (pd.removeMoney(s.shopItems.get(10).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(10)) == -1) {
								p.getInventory().addItem(s.shopItems.get(10).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(10)),
									s.shopItems.get(10).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(10));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Pufferfish Sword]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(11).item)) {
						if (pd.removeMoney(s.shopItems.get(11).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(11)) == -1) {
								p.getInventory().addItem(s.shopItems.get(11).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(11)), s.shopItems.get(11).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(11));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Slime Sword]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(12).item)) {
						if (pd.removeMoney(s.shopItems.get(12).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(12)) == -1) {
								p.getInventory().addItem(s.shopItems.get(12).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(12)), s.shopItems.get(12).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(12));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Multishot Crossbow]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(15).item)) {
						if (pd.removeMoney(s.shopItems.get(15).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(15)) == -1) {
								p.getInventory().addItem(s.shopItems.get(15).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(15)), s.shopItems.get(15).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(15));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Marksman Bow]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(13).item)) {
						if (pd.removeMoney(s.shopItems.get(13).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(13)) == -1) {
								p.getInventory().addItem(s.shopItems.get(13).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(13)), s.shopItems.get(13).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(13));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
				case "[Ricochet Bow]": {
					if (!Shop.alreadyHasThis(p, s.shopItems.get(14).item)) {
						if (pd.removeMoney(s.shopItems.get(14).price)) {
							if (Shop.findInvIndex(p, s.shopItems.get(14)) == -1) {
								p.getInventory().addItem(s.shopItems.get(14).item);
								p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
								return;
							}
							event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, s.shopItems.get(14)), s.shopItems.get(14).item);
							p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
							s.updateTitle(p);
							s.buyHistory.add(s.shopItems.get(14));
						} else {
							p.sendMessage(Component.text(mess).color(RED));
							p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
						}
					} else {
						p.sendMessage(Component.text(mess2).color(RED));
						p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
					}
					return;
				}
			}

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
						PlayerData.addMoney(h.get(i).price, p);
						s.updateTitle(p);
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
						PlayerData.addMoney(h.get(i).price, p);
						s.updateTitle(p);
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
						PlayerData.addMoney(h.get(i).price, p);
						s.updateTitle(p);
						return;
					}
				}
			}
		}
	}
}

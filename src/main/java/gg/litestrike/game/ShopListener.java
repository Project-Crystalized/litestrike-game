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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ShopListener implements Listener {

    public Inventory updateShop(){
        return Shop.shopInv;
    }

    public List<LSItem> getShopItems(){
        return Shop.shopItems;
    }

    @EventHandler
    public void openShop(PlayerInteractEvent event) {
        event.setCancelled(false);
        Player p = event.getPlayer();
        if (event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK) {
            if (p.getInventory().getItemInMainHand().getType() == Material.EMERALD) {
                p.openInventory(updateShop());
                Shop.setItems();
                Shop.setDefuser(p);
            }
        }
    }
    @EventHandler
    public void buyItem(InventoryClickEvent event){
        if(event.getView().title().equals(Shop.title((Player) event.getWhoClicked()))) {
            event.setCancelled(true);

            String mess = "Can't afford item";
            String mess2 = "You already have this item";
            Player p = (Player) event.getWhoClicked();
            PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData((Player) event.getWhoClicked());

            switch (Objects.requireNonNull(event.getCurrentItem()).getType()) {

                case Material.DIAMOND_CHESTPLATE: {
                    if (!Shop.alreadyHasThis(p, getShopItems().get(0).item)) {
                        if (pd.removeMoney(getShopItems().get(0).price)) {
                            event.getWhoClicked().getInventory().setChestplate(getShopItems().get(0).item);
                            p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                            Shop.updateTitle(p);
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
                case Material.IRON_CHESTPLATE: {
                    if (!Shop.alreadyHasThis(p, getShopItems().get(9).item)) {
                        if (pd.removeMoney(getShopItems().get(9).price)) {
                            event.getWhoClicked().getInventory().setChestplate(getShopItems().get(9).item);
                            p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                            Shop.updateTitle(p);
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
                case Material.IRON_SWORD: {
                    if (!Shop.alreadyHasThis(p, getShopItems().get(1).item)) {
                        if (pd.removeMoney(getShopItems().get(1).price)) {
                            if (Shop.findInvIndex(p, getShopItems().get(1)) == -1) {
                                p.getInventory().addItem(getShopItems().get(1).item);
                                p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                                return;
                            }
                            event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, getShopItems().get(1)), getShopItems().get(1).item);
                            p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                            Shop.updateTitle(p);
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

                case Material.IRON_AXE: {
                    if (!Shop.alreadyHasThis(p, getShopItems().get(3).item)) {
                        if (pd.removeMoney(getShopItems().get(3).price)) {
                            if (Shop.findInvIndex(p, getShopItems().get(3)) == -1) {
                                p.getInventory().addItem(getShopItems().get(3).item);
                                p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                                return;
                            }
                            event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, getShopItems().get(3)), getShopItems().get(3).item);
                            p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                            Shop.updateTitle(p);
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

                case Material.ARROW: {
                    if (pd.removeMoney(getShopItems().get(5).price)) {
                        event.getWhoClicked().getInventory().addItem(getShopItems().get(5).item);
                        p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                        Shop.updateTitle(p);
                    } else {
                        p.sendMessage(Component.text(mess).color(RED));
                        p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
                    }
                    return;
                }
                case Material.IRON_PICKAXE: {
                    if(!Shop.alreadyHasThis(p, getShopItems().get(6).item)) {
                        if (pd.removeMoney(getShopItems().get(6).price)) {
                            if (Shop.findInvIndex(p, getShopItems().get(6)) == -1) {
                                p.getInventory().addItem(getShopItems().get(6).item);
                                p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                                return;
                            }
                            event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, getShopItems().get(6)), getShopItems().get(6).item);
                            p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                            Shop.updateTitle(p);
                        } else {
                            p.sendMessage(Component.text(mess).color(RED));
                            p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
                        }
                    }else{
                        p.sendMessage(Component.text(mess2).color(RED));
                        p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
                    }
                    return;
                }
                case Material.GOLDEN_APPLE: {
                    if (pd.removeMoney(getShopItems().get(8).price)) {
                        event.getWhoClicked().getInventory().addItem(getShopItems().get(8).item);
                        p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                        Shop.updateTitle(p);
                    } else {
                        p.sendMessage(Component.text(mess).color(RED));
                        p.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.AMBIENT, 1, 1));
                    }
                    return;
                }
                case Material.CROSSBOW: {
                    if (!Shop.alreadyHasThis(p, getShopItems().get(10).item)) {
                        if (pd.removeMoney(getShopItems().get(10).price)) {
                            if (Shop.findInvIndex(p, getShopItems().get(10)) == -1) {
                                p.getInventory().addItem(getShopItems().get(10).item);
                                p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                                return;
                            }
                            event.getWhoClicked().getInventory().setItem(Shop.findInvIndex(p, getShopItems().get(10)), getShopItems().get(10).item);
                            p.playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 5));
                            Shop.updateTitle(p);
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
    }


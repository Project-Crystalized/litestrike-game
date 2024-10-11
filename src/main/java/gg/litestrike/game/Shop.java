package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static org.bukkit.Material.DIAMOND_CHESTPLATE;
import static org.bukkit.Material.EMERALD;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class Shop implements Listener, InventoryHolder {
    List<LSItem> shopItems;
    private final Inventory shopInv;

    public Shop() {
        shopItems = LSItem.createItems();
        shopInv = Bukkit.getServer().createInventory(this, 54);
        //TODO make the Shop its own item
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().addItem(new ItemStack(EMERALD));
        }
    }


    @EventHandler
    public void openShop(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK) {
            if (p.getInventory().getItemInMainHand().getType() == Material.EMERALD) {
                p.openInventory(this.shopInv);
                this.setItems();
            }
        }
    }

    public void setItems(){
        for(LSItem item : shopItems) {
            switch(item.item.getType()){
                case Material.DIAMOND_CHESTPLATE:{
                    this.shopInv.setItem(31, item.displayItem);
                }
                case Material.IRON_SWORD:{
                    this.shopInv.setItem(0, item.displayItem);
                }
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.shopInv;
    }

    @EventHandler
    public void buyItem(InventoryClickEvent event){

    }
}

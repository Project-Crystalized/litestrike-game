package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static org.bukkit.Material.DIAMOND_CHESTPLATE;
import static org.bukkit.Material.EMERALD;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class Shop implements Listener, InventoryHolder {
    public List<LSItem> shopItems;
    private Inventory shopInv;

    public Shop(Player p) {
        if(p == null){
            return;
        }
            shopItems = LSItem.createItems();
            shopInv = Bukkit.getServer().createInventory(this, 54, this.title(p));
            //TODO make the Shop its own item
    }


    @EventHandler
    public void openShop(PlayerInteractEvent event) {
        event.setCancelled(false);
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
        if(event.getView().title().equals(this.title((Player) event.getWhoClicked()))) {
            event.setCancelled(true);
            String mess = "Can't afford item";
            Player p = (Player) event.getWhoClicked();
            PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData((Player) event.getWhoClicked());
            switch (Objects.requireNonNull(event.getCurrentItem()).getType()) {
                case Material.DIAMOND_CHESTPLATE: {
                    if (pd.removeMoney(shopItems.get(0).price)) {
                        event.getWhoClicked().getInventory().addItem(shopItems.get(0).item);
                        this.updateTitle(p);
                    } else {
                        p.sendMessage(Component.text(mess).color(RED));
                    }
                }
                case Material.IRON_SWORD: {
                    if (pd.removeMoney(shopItems.get(1).price)) {
                        event.getWhoClicked().getInventory().addItem(shopItems.get(1).item);
                        this.updateTitle(p);
                    } else {
                        p.sendMessage(Component.text(mess).color(RED));
                    }
                }
            }
        }
    }

    public Component title(Player p){
            PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
            return Component.text("" + pd.getMoney());
    }

    public void updateTitle(Player p){
        shopInv.close();
        final Shop shop = new Shop(p);
        p.openInventory(this.shopInv);
    }

    public static void giveShop(){
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().addItem(new ItemStack(EMERALD));
            final Shop shop = new Shop(p);
        }
    }
}

package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LSItem {
    public final ItemStack item;
    public final ItemStack displayItem;
    public final String description;
    public final int price;
    public LSItem(ItemStack item, ItemStack displayItem, int price, String description){
        this.item = item;
        this.displayItem = displayItem;
        this.price = price;
        this.description = description;
    }

    public static List<LSItem> createItems(){
        LSItem diamondChestplate = new LSItem(new ItemStack(Material.DIAMOND_CHESTPLATE), new ItemStack(Material.DIAMOND_CHESTPLATE), 300, null);
        LSItem.setDescription(diamondChestplate);
        LSItem ironSword = new LSItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SWORD), 1000, null);
        LSItem.setDescription(ironSword);
        List<LSItem> lsItems = new ArrayList();
        lsItems.add(diamondChestplate);
        lsItems.add(ironSword);
        return lsItems;
    }
    public static void setDescription(LSItem display){
        if(display.description != null){
            List<Component> lore = new ArrayList<>();
            //TODO add the lore!!
            lore.add(Component.text(display.description));
            display.displayItem.getItemMeta().lore(lore);
            display.displayItem.setItemMeta(display.displayItem.getItemMeta());
        }
        String prize = "" + display.price;
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""+ display.price));
        display.displayItem.getItemMeta().lore(lore);
        display.displayItem.setItemMeta(display.displayItem.getItemMeta());

    }
}




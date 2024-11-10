package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class LSItem {
    public final ItemStack item;
    public final ItemStack displayItem;
    public final String description;
    public final int price;
    enum ItemCategory{
        Melee,
        Range,
        Armor,
        Ammunition,
        Consumable,
        Defuser
    }
    public final ItemCategory categ;
    public LSItem(ItemStack item, ItemStack displayItem, int price, String description, ItemCategory cate){
        this.item = item;
        this.displayItem = displayItem;
        this.price = price;
        this.description = description;
        this.categ = cate;
    }

    public static List<LSItem> createItems(){
        /*
        To-Do List for hard coding new items:
        1. create new LSItem object
        2. add the new LSItem object to the lsItems list
        3. code a new branch to the switch in setItems()
        4. code a new branch to the switch in getItemCategory()
        5. code a new branch to the switch in buyItem()
         */
        LSItem diamondChestplate = new LSItem(new ItemStack(Material.DIAMOND_CHESTPLATE), new ItemStack(Material.DIAMOND_CHESTPLATE), 500, null, ItemCategory.Armor);
        LSItem ironSword = new LSItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_SWORD), 750, "stab stab", ItemCategory.Melee);
        LSItem stoneSword = new LSItem(new ItemStack(Material.STONE_SWORD), new ItemStack(Material.IRON_SWORD), 0, null, ItemCategory.Melee);
        LSItem ironAxe = new LSItem(new ItemStack(Material.IRON_AXE), new ItemStack(Material.IRON_AXE), 1750, null, ItemCategory.Melee);
        LSItem bow = new LSItem(new ItemStack(Material.BOW), new ItemStack(Material.BOW), 0, null, ItemCategory.Range);
        LSItem arrow = new LSItem(new ItemStack(Material.ARROW, 6), new ItemStack(Material.ARROW, 6), 100, null, ItemCategory.Ammunition);
        LSItem defuser = new LSItem(new ItemStack(Material.IRON_PICKAXE), new ItemStack(Material.IRON_PICKAXE), 100, "Don't be a loser buy a defuser -Tubbo", ItemCategory.Defuser);
        LSItem pickaxe = new LSItem(new ItemStack(Material.STONE_PICKAXE), new ItemStack(Material.STONE_PICKAXE), 0, null, ItemCategory.Defuser);
        LSItem gapple = new LSItem(new ItemStack(Material.GOLDEN_APPLE), new ItemStack(Material.GOLDEN_APPLE), 500, null, ItemCategory.Consumable);
        LSItem ironChestplate = new LSItem(new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_CHESTPLATE), 250, null, ItemCategory.Armor);
        LSItem crossbow = new LSItem(new ItemStack(Material.CROSSBOW), new ItemStack(Material.CROSSBOW), 1000, null, ItemCategory.Range);

        nameItem(defuser.item);
        nameItem(defuser.displayItem);
        List<LSItem> lsItems = new ArrayList<>();
        lsItems.add(diamondChestplate);
        lsItems.add(ironSword);
        lsItems.add(stoneSword);
        lsItems.add(ironAxe);
        lsItems.add(bow);
        lsItems.add(arrow);
        lsItems.add(defuser);
        lsItems.add(pickaxe);
        lsItems.add(gapple);
        lsItems.add(ironChestplate);
        lsItems.add(crossbow);
        return lsItems;
    }
    public static void updateDescription(Player p, LSItem display){

        if((Litestrike.getInstance().game_controller.getPlayerData(p).getMoney() - display.price) >= 0){
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""+ display.price).color(WHITE).decoration(TextDecoration.ITALIC,false));
            if(display.description != null){
                lore.add(Component.text(display.description).color(WHITE).decoration(TextDecoration.ITALIC,false));
            }
            ItemMeta meta = display.displayItem.getItemMeta();
            meta.lore(lore);
            display.displayItem.setItemMeta(meta);
        }else{
            List<Component> lore = new ArrayList<>();
            ItemMeta meta = display.displayItem.getItemMeta();
            lore.add(Component.text(""+ display.price).color(RED).decoration(TextDecoration.ITALIC,false));
            if(display.description != null){
                lore.add(Component.text(display.description).decoration(TextDecoration.ITALIC,false));
            }
            meta.lore(lore);
            display.displayItem.setItemMeta(meta);
        }


    }


    public static String getItemCategory(ItemStack i){
        switch(i.getType()){
            case Material.IRON_SWORD: {
                return "Melee";
            }
            case Material.IRON_AXE: {
                return "Melee";
            }
            case Material.STONE_SWORD:{
                return "Melee";
            }
            case Material.BOW:{
                return "Range";
            }
            case Material.ARROW:{
                return "Ammunition";
            }
            case Material.IRON_PICKAXE: {
                return "Defuser";
            }
            case Material.STONE_PICKAXE: {
                return "Defuser";
            }
            case Material.GOLDEN_APPLE: {
                return "Consumable";
            }
            case Material.IRON_CHESTPLATE: {
                return "Armor";
            }
            case Material.CROSSBOW: {
                return "Range";
            }
        }
        return "no_value";
    }

    public static void nameItem(ItemStack i){
        ItemMeta meta = i.getItemMeta();
        meta.itemName(Component.text("Defuser"));
        i.setItemMeta(meta);
    }
}




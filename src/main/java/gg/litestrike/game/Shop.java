package gg.litestrike.game;

import com.destroystokyo.paper.MaterialSetTag;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static org.bukkit.Material.EMERALD;

public class Shop implements InventoryHolder {
    public static List<LSItem> shopItems;
    public static Inventory shopInv;

    public Shop(Player p) {
        if(p == null){
            return;
        }
        if(shopItems == null){
            shopItems = LSItem.createItems();
        }
        shopInv = Bukkit.getServer().createInventory(this, 54, title(p));

        for(LSItem item : shopItems) {
            LSItem.updateDescription(p, item);
        }
            //TODO make the Shop its own item
    }


    public static void setItems(){
        for(LSItem item : shopItems) {
            switch(item.item.getType()){
                case Material.DIAMOND_CHESTPLATE:{
                    Shop.shopInv.setItem(31, item.displayItem);
                }
                case Material.IRON_SWORD:{
                    Shop.shopInv.setItem(0, item.displayItem);
                }
                case Material.IRON_AXE:{
                    Shop.shopInv.setItem(2, item.displayItem);
                }
                case Material.ARROW:{
                    Shop.shopInv.setItem(50, item.displayItem);
                }
                case Material.GOLDEN_APPLE:{
                    Shop.shopInv.setItem(49, item.displayItem);
                }
                case Material.IRON_CHESTPLATE:{
                    Shop.shopInv.setItem(40, item.displayItem);
                }
                case Material.CROSSBOW:{
                    Shop.shopInv.setItem(26, item.displayItem);
                }
            }
        }
    }

    public static void setDefuser(Player p){
        if(Teams.get_team(p) == Team.Breaker){
            Shop.shopInv.setItem(42, shopItems.get(6).displayItem);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Shop.shopInv;
    }



    public static Component title(Player p){
            PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
            return Component.text("\uA000" + "\uA001" +"\uE104"+ pd.getMoney()).color(WHITE);
    }

    public static void updateTitle(Player p){
        shopInv.close();
        new Shop(p);
        p.openInventory(Shop.shopInv);
        setItems();
        setDefuser(p);
    }

    public static void giveShop(){
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().addItem(new ItemStack(EMERALD));
            new Shop(p);
        }
    }

    public static int findInvIndex(Player p, LSItem item){
        String cate = item.categ.toString();
        for (int i = 0; i <= 40; i++) {
            ItemStack it = p.getInventory().getItem(i);
            if(it == null){
                continue;
            }
            if(Objects.equals(LSItem.getItemCategory(it), cate)){
                return i;
            }
        }
        return -1;
    }

    public static void giveDefaultArmor(Player p) {
            if(Teams.get_team(p) == Team.Placer) {
                PlayerInventory inv = p.getInventory();
                inv.setHelmet(colorArmor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_HELMET)));
                inv.setChestplate(colorArmor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_CHESTPLATE)));
                inv.setLeggings(colorArmor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_LEGGINGS)));
                inv.setBoots(colorArmor(Color.fromRGB(0xe31724), new ItemStack(Material.LEATHER_BOOTS)));
                inv.setItem(0, new ItemStack(Material.STONE_SWORD));
                inv.setItem(1, new ItemStack(Material.BOW));
                inv.setItem(2, new ItemStack(Material.ARROW, 6));
            }else if(Teams.get_team(p) == Team.Breaker) {
                PlayerInventory inv = p.getInventory();
                inv.setHelmet(colorArmor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_HELMET)));
                inv.setChestplate(colorArmor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_CHESTPLATE)));
                inv.setLeggings(colorArmor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_LEGGINGS)));
                inv.setBoots(colorArmor(Color.fromRGB(0x0f9415), new ItemStack(Material.LEATHER_BOOTS)));
                inv.setItem(0, new ItemStack(Material.STONE_SWORD));
                inv.setItem(1, new ItemStack(Material.BOW));
                inv.setItem(2, new ItemStack(Material.ARROW, 6));
                inv.addItem(new ItemStack(Material.STONE_PICKAXE));
            }
    }

    public static ItemStack colorArmor(Color c, ItemStack i) {
        LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
        lam.setColor(c);
        i.setItemMeta(lam);
        return i;
    }

    public static boolean alreadyHasThis(Player p, ItemStack item){
        for (int i = 0; i <= 40; i++) {
            ItemStack it = p.getInventory().getItem(i);
            if(it == null){
                continue;
            }
            if(it.getType() == item.getType()){
                return true;
            }
        }
        return false;
    }

    public static void removeShop(Player p){
        Inventory inv = p.getInventory();
        for (int i = 0; i <= 40; i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null) {
                continue;
            }
            if (it.getType() == Material.EMERALD){
                inv.clear(i);
                shopInv.close();
            }
        }
    }
}

package gg.litestrike.game;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import gg.litestrike.game.LSItem.ItemCategory;
import gg.crystalized.lobby.LobbyDatabase;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.EMERALD;
import static org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE;
import static org.bukkit.enchantments.Enchantment.*;

public class Shop {
	private static final String SHOP_BACKGROUND_DEFAULT = "\uA001";
	private static final String SHOP_BACKGROUND_GENERIC = "\uA016";

	public Inventory currentView;
	public String player;
	public Player p;
	public String shopBackground;
	public HashMap<LSItem.ItemCategory, LSItem> currentEquip = new HashMap<>();
	public HashMap<LSItem.ItemCategory, LSItem> previousEquip = new HashMap<>();
	public HashMap<LSItem, Integer> consAndAmmoCount = new HashMap<>();
	public List<LSItem> shopLog;

	public Shop(Player p) {
		if (p == null) {
			Bukkit.getLogger().severe("tried to create a shop with a null player, continueing");
			return;
		}

		Litestrike.getInstance().game_controller.shopList.put(p.getName(), this);
		this.p = p;
		player = p.getName();
		shopBackground = getShopBackground(p);
		currentView = Bukkit.getServer().createInventory(null, 54, title(p.getName()));
		shopLog = new ArrayList<>();
	}

	private static String getShopBackground(Player p) {
		try {
			if (!LobbyDatabase.canSeeConfusingTextures(p)) {
				return SHOP_BACKGROUND_GENERIC;
			}
		} catch (NoClassDefFoundError e) {}

		if (!LSItem.ShopValidator.matchesDefaultLayout(LSItem.currentShopLayout())) {
			return SHOP_BACKGROUND_GENERIC;
		}
		return SHOP_BACKGROUND_DEFAULT;
	}

	private Component title(String p) {
		PlayerData pd = Litestrike.getInstance().game_controller.playerDataManager.get(p);
		return Component.text("\uA000" + shopBackground + "\uE104" + pd.getMoney()).color(WHITE);
	}

	public void update_shop() {
		currentView = Bukkit.getServer().createInventory(null, 54, title(player));

		for (LSItem item : LSItem.shopItems) {
			if (item == null || item.slot == null) {
				continue;
			}
			if (item.categ == ItemCategory.Defuser
					&& Litestrike.getInstance().game_controller.teams.get_team(player) != Team.Breaker) {
				continue;
			}
			currentView.setItem(item.slot, item.buildDisplayItem(player));
		}
	}

	private void openBedrockShop(boolean isSelling) {
		FloodgatePlayer fp = FloodgateApi.getInstance().getPlayer(p.getUniqueId());
		PlayerData pd = Litestrike.getInstance().game_controller.playerDataManager.get(p);
		List<LSItem> items = new ArrayList<>();
		for (JsonElement a : LSItem.shopBedrockOrder) {
			LSItem lsitem = LSItem.getLsItem(a.getAsString());
			if (lsitem == null) continue;
			if (lsitem.slot != null) {
				if (Litestrike.getInstance().game_controller.teams.get_team(p) == Team.Placer && lsitem.categ.equals(ItemCategory.Defuser)) {
					continue;
				}
				items.add(lsitem);
			}
		}

		String title = "cry_grid2;§fLitestrike Shop";
		String content = "§qBuy §ritems here, Click the first button to start selling.";
		if (isSelling) {
			title = title + " (Selling)";
			content = "§mSell §ritems here, Click the first button to start buying again.";
		}

		SimpleForm.Builder form = SimpleForm.builder()
				.title(title)
				.content(content + "\n\n§rYou have \uE104§3" + pd.getMoney() + "§r.")
				;

		//2 of the same buttons is needed here, just to make the form look nice with how its layed out
		if (!isSelling) {
			form = form.button("Sell an Item", FormImage.Type.PATH, "textures/blocks/concrete_red");
			form = form.button("Sell an Item", FormImage.Type.PATH, "textures/blocks/concrete_red");
		} else {
			form = form.button("Buy an Item", FormImage.Type.PATH, "textures/blocks/concrete_green");
			form = form.button("Buy an Item", FormImage.Type.PATH, "textures/blocks/concrete_green");
		}

		for (LSItem i : items) {
			String name;
			if (i.name != null) {
				name = i.nameFallback;
			} else {
				name = i.item.getI18NDisplayName(); //This is deprecated, theres no better alternative (we cant use components)
			}

			if (i.bedrockTexture == null) { //should never happen, but just in case
				form = form.button(name + "\nPrice: \uE104" + i.price);
			} else {
				String texture = i.bedrockTexture;
				String amount = "";

				//underdog sword shit
				if (i.key.equals("underdog_sword")) {
					GameController gc = Litestrike.getInstance().game_controller;
					int rounds_down = 0;
					if (gc.teams.get_team(p) == Team.Breaker) {
						rounds_down = gc.placer_wins_amt - gc.breaker_wins_amt;
					} else {
						rounds_down = gc.breaker_wins_amt - gc.placer_wins_amt;
					}
					if (rounds_down <= 0) {
						rounds_down = 0;
					}

					texture = "textures/crystalized/item/underdog_sword_" + (rounds_down + 1);
				}

				if (i.item.getAmount() != 1) amount = " x" +i.item.getAmount();


				//So in form buttons, we cant use components at all, the next best is just working with the internal name
				form = form.button(
						name + amount
								+ "\nPrice: \uE104" + i.price,
						FormImage.Type.PATH, texture
				);
			}
		}

		form = form.validResultHandler(response -> {
			if (response.clickedButtonId() <= 1) {
				openBedrockShop(!isSelling);
			} else {
				LSItem i = items.get(response.clickedButtonId() - 2);
				GameController gc = Litestrike.getInstance().game_controller;
				Bukkit.getLogger().info("Tried to buy" + i.key + "|" + response.clickedButtonId());
				if (isSelling) {
					ShopListener.undoBuy(i.item, p, i.slot);
				} else {
					ShopListener.buyItem(p, i.item, i.slot, gc, this);
				}
			}
		});

		fp.sendForm(form);
	}

	public void open_shop() {
		update_shop();

		if (isBedrock(p)) {
			openBedrockShop(false);
			return;
		}

		Bukkit.getPlayer(player).openInventory(currentView);
	}

	public void close_shop() {
		if (isBedrock(p)) {
			FloodgateApi.getInstance().closeForm(p.getUniqueId());
		} else {
			this.currentView.close();
		}
	}

	// this is called once in next_round()
	public static void giveShop_and_update() {
		GameController gc = Litestrike.getInstance().game_controller;
		for (Player p : gc.teams.get_all_players()) {
			gc.getShop(p).shopLog.add(null);
			gc.getShop(p).update_shop();

			ItemStack shop = new ItemStack(EMERALD, 1);
			ItemMeta meta = shop.getItemMeta();
			meta.displayName(Component.text("Shop").color(WHITE).decoration(ITALIC, false));
			List<TextComponent> list = new ArrayList<>();
			list.add(Component.text("right click").color(GRAY).decoration(ITALIC, false));
			meta.lore(list);
			meta.setItemModel(NamespacedKey.fromString("crystalized:models/ls_shop/ls_shop"));
			shop.setItemMeta(meta);
			if (p.getInventory().getItem(7) == null || p.getInventory().getItem(7).isEmpty()) {
				p.getInventory().setItem(7, shop);
			} else {
				p.getInventory().addItem(shop);
			}
		}
	}

	public static void giveDefaultArmor(Player p) {
		GameController gc = Litestrike.getInstance().game_controller;
		PlayerInventory inv = p.getInventory();
		ItemStack arrows_item = new ItemStack(Material.ARROW, 6);

		// give 6 arrows back
		int arrows = 0;
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType() == Material.ARROW) {
				arrows += is.getAmount();
			}
		}
		if (arrows < 6) {
			arrows_item.setAmount(6 - arrows);
			inv.addItem(arrows_item);
		}

		if (!(p.getGameMode() == GameMode.SPECTATOR || gc.round_number == 1
			|| gc.round_number == Litestrike.getInstance().gameConfig.switchRound + 1
			|| gc.round_number == (Litestrike.getInstance().gameConfig.switchRound * 2) + 1)) {
			// no need to give equipment
			return;
		}

		Team player_team = gc.teams.get_team(p);
		inv.clear();
		p.setItemOnCursor(null);
		inv.setItem(0, new ItemStack(Material.STONE_SWORD));
		inv.setItem(1, new ItemStack(Material.BOW));
		arrows_item.setAmount(6);
		inv.setItem(2, arrows_item);

		Color c = null;
		Color boot_color = null;
		if (player_team == Team.Placer) {
			c = Color.fromRGB(0xe31724);
			boot_color = Color.fromRGB(0xe88b28);
		} else if (player_team == Team.Breaker) {
			c = Color.fromRGB(0x0f9415);
			boot_color = Color.fromRGB(0x8119c9);
			inv.addItem(new ItemStack(Material.STONE_PICKAXE));
		}
		inv.setHelmet(colorArmor(c, new ItemStack(Material.LEATHER_HELMET), 1));
		inv.setChestplate(colorArmor(c, new ItemStack(Material.LEATHER_CHESTPLATE), 1));
		inv.setLeggings(colorArmor(c, new ItemStack(Material.LEATHER_LEGGINGS), 1));
		inv.setBoots(colorArmor(boot_color, new ItemStack(Material.LEATHER_BOOTS), 2));

		// give unbreakable to all items
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType().getMaxDurability() > 0) {
				ItemMeta im = is.getItemMeta();
				im.setUnbreakable(true);
				is.setItemMeta(im);
				is.addItemFlags(HIDE_UNBREAKABLE);
			}
		}
	}

	public static ItemStack colorArmor(Color c, ItemStack i, int ench_level) {
		LeatherArmorMeta lam = (LeatherArmorMeta) i.getItemMeta();
		lam.setColor(c);
		i.setItemMeta(lam);
		i.addEnchantment(PROTECTION, ench_level);
		return i;
	}

	public boolean alreadyHasThis(ItemStack item) {
		return findInInventory(it -> LSItem.is_same_ls_item(it, item)) != -1;
	}

	public int findInvIndex(ItemStack item) {
		return findInInventory(it -> LSItem.is_same_ls_item(it, item));
	}

	public int findInvIndex(LSItem.ItemCategory categ) {
		return findInInventory(it -> LSItem.getItemCategory(it) == categ);
	}

	private int findInInventory(Predicate<ItemStack> matches) {
		PlayerInventory inv = Bukkit.getPlayer(player).getInventory();
		for (int i = 0; i <= 40; i++) {
			ItemStack it = inv.getItem(i);
			if (it != null && matches.test(it)) {
				return i;
			}
		}
		return -1;
	}

	public static void removeShop(Player p) {
		Inventory inv = p.getInventory();
		Shop s = Litestrike.getInstance().game_controller.getShop(p);
		for (int i = 0; i <= 40; i++) {
			if (inv.getItem(i) == null) {
				continue;
			}
			if (inv.getItem(i).getType() == EMERALD) {
				inv.clear(i);
			}
		}
		s.close_shop();
		if (p.getItemOnCursor().getType() == EMERALD) {
			p.setItemOnCursor(null);
		}
	}

	public void resetEquip() {
		previousEquip.clear();
		currentEquip.clear();
		currentEquip.put(LSItem.ItemCategory.Melee, LSItem.shopItems.get(2));
		currentEquip.put(LSItem.ItemCategory.Range, LSItem.shopItems.get(4));
		if (Litestrike.getInstance().game_controller.teams.get_team(player) == Team.Placer) {
			currentEquip.put(LSItem.ItemCategory.Armor, LSItem.shopItems.get(7));
		} else {
			currentEquip.put(LSItem.ItemCategory.Armor, LSItem.shopItems.get(6));
			currentEquip.put(LSItem.ItemCategory.Defuser, LSItem.shopItems.get(25));
		}
	}

	public void resetEquipCounters() {
		consAndAmmoCount.clear();
		for (LSItem item : LSItem.shopItems) {
			if (item.categ == LSItem.ItemCategory.Ammunition || item.categ == LSItem.ItemCategory.Consumable) {
				consAndAmmoCount.put(item, 0);
			}
		}
	}

	public boolean isBedrock(Player p) {
		return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
	}
}

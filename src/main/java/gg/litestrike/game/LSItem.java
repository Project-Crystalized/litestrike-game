package gg.litestrike.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.bukkit.inventory.ItemFlag.*;
import static org.bukkit.enchantments.Enchantment.*;
import static org.bukkit.Material.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.text.Component.translatable;

public class LSItem {
	public final ItemStack item;
	public final List<Component> description;
	public final Integer price;
	public final Integer slot;
	public final Component name;
	public final Integer modelData;
	private static short creation_number = 1;
	public final Short id;
	public static HashMap<String, LSItem> importantEquip = new HashMap<>();

	public static List<LSItem> shopItems = createItems();

	public enum ItemCategory {
		Melee,
		Range,
		Armor,
		Ammunition,
		Consumable,
		Defuser
	}

	public final ItemCategory categ;

	public LSItem(ItemStack item, Integer price, List<Component> description, ItemCategory cate, Integer slot,
			Component name, Integer modelData) {
		this.price = price;
		this.description = description;
		this.categ = cate;
		this.item = item;
		this.slot = slot;
		this.name = name;
		this.modelData = modelData;
		this.id = creation_number;
		creation_number++;

		ItemMeta meta = item.getItemMeta();
		if (item.getType().getMaxDurability() > 0) {
			meta.setUnbreakable(true);
		}

		if (price != null && item.getType() != Material.ARROW) {
			Component item_name;
			if (meta.displayName() != null) {
				item_name = meta.displayName();
			} else {
				item_name = translatable(item.translationKey()).decoration(ITALIC, false);
			}
			if (price <= 500) {
				meta.displayName(item_name.color(WHITE));
			} else if (price <= 1000) {
				meta.displayName(item_name.color(TextColor.color(0x8f5805)));
			} else if (price <= 1500) {
				meta.displayName(item_name.color(TextColor.color(0x4DA4E5)));
			} else if (price <= 2000) {
				meta.displayName(item_name.color(TextColor.color(0xfbc522)));
			} else {
				meta.displayName(item_name.color(TextColor.color(0xb02ae2)));
			}
		}

		item.setItemMeta(meta);
		item.addItemFlags(HIDE_UNBREAKABLE);
	}

	private static List<LSItem> createItems() {
		List<LSItem> lsItems = new ArrayList<>();

		// IMPORTANT: the order in which the items are created must be preserved
		// because it is used as a id in the database, also better not remove items from
		// the list
		ItemStack diamondChestplate = new ItemStack(DIAMOND_CHESTPLATE);
		diamondChestplate.addEnchantment(PROTECTION, 1);
		lsItems.add(new LSItem(diamondChestplate, 500, null, ItemCategory.Armor, 31, null, null));

		ItemStack ironSword = new ItemStack(IRON_SWORD);
		List<Component> ironSword_lore = new ArrayList<>();
		ironSword_lore.add(translatable("crystalized.sword.iron.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(ironSword, 750, ironSword_lore, ItemCategory.Melee, 0, null, null));

		ItemStack stoneSword = new ItemStack(STONE_SWORD);
		lsItems.add(new LSItem(stoneSword, null, null, ItemCategory.Melee, null, null, null));

		ItemStack ironAxe = new ItemStack(IRON_AXE);
		lsItems.add(new LSItem(ironAxe, 1750, null, ItemCategory.Melee, 2, null, null));

		ItemStack bow = new ItemStack(BOW);
		lsItems.add(new LSItem(bow, null, null, ItemCategory.Range, null, null, null));

		ItemStack arrow = new ItemStack(ARROW, 6);
		List<Component> arrowDesc = new ArrayList<>();
		arrowDesc.add(Component.text("| '").color(WHITE).decoration(ITALIC, false));
		ItemMeta meta  = arrow.getItemMeta();
		meta.lore(arrowDesc);
		arrow.setItemMeta(meta);
		lsItems.add(new LSItem(arrow, 150, arrowDesc, ItemCategory.Ammunition, 50, null, null));

		ItemStack breakerAmour = new ItemStack(LEATHER_CHESTPLATE);
		breakerAmour = Shop.colorArmor(Color.fromRGB(0x0f9415), breakerAmour, 1);
		lsItems.add(new LSItem(breakerAmour, null, null, ItemCategory.Armor, null, null, null));

		ItemStack placerAmour = new ItemStack(LEATHER_CHESTPLATE);
		placerAmour = Shop.colorArmor(Color.fromRGB(0xe31724), placerAmour, 1);
		lsItems.add(new LSItem(placerAmour, null, null, ItemCategory.Armor, null, null, null));

		ItemStack defuser = new ItemStack(IRON_PICKAXE);
		ItemMeta defuser_meta = defuser.getItemMeta();
		defuser_meta.displayName(translatable("crystalized.item.defuser.name").decoration(ITALIC, false));
		defuser_meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
				NamespacedKey.minecraft("foo"), 0d, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
		defuser.setItemMeta(defuser_meta);
		defuser.addItemFlags(HIDE_ATTRIBUTES);
		List<Component> defuser_lore = new ArrayList<>();
		defuser_lore.add(translatable("crystalized.item.defuser.desc1").color(WHITE).decoration(ITALIC, false));
		defuser_lore.add(translatable("crystalized.item.defuser.desc2").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(defuser, 500, defuser_lore, ItemCategory.Defuser, Shop.DEFUSER_SLOT,
				translatable("crystalized.item.defuser.name").decoration(ITALIC, false), null));

		ItemStack gapple = new ItemStack(GOLDEN_APPLE);
		List<Component> gapple_lore = new ArrayList<>();
		gapple_lore.add(translatable("crystalized.item.gapple.desc1").color(WHITE).decoration(ITALIC, false));
		gapple_lore.add(translatable("crystalized.item.gapple.desc2").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(gapple, 750, gapple_lore, ItemCategory.Consumable, 48, null, null));

		ItemStack ironChestplate = new ItemStack(IRON_CHESTPLATE);
		ironChestplate.addEnchantment(PROTECTION, 1);
		lsItems.add(new LSItem(ironChestplate, 250, null, ItemCategory.Armor, 40, null, null));

		ItemStack quickdraw = new ItemStack(CROSSBOW);
		quickdraw.addEnchantment(QUICK_CHARGE, 1);
		ItemMeta quickdraw_meta = quickdraw.getItemMeta();
		quickdraw_meta.setCustomModelData(2);
		quickdraw_meta.displayName(translatable("crystalized.crossbow.quickcharge.name").decoration(ITALIC, false));
		quickdraw.setItemMeta(quickdraw_meta);
		List<Component> quickdraw_lore = new ArrayList<>();
		quickdraw_lore.add(translatable("crystalized.crossbow.quickcharge.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(quickdraw, 2000, quickdraw_lore, ItemCategory.Range, 24,
				translatable("crystalized.crossbow.quickcharge.name").decoration(ITALIC, false), 2));

		ItemStack pufferFish = new ItemStack(STONE_SWORD);
		ItemMeta pufferFish_meta = pufferFish.getItemMeta();
		pufferFish_meta.setCustomModelData(2);
		pufferFish_meta.displayName(translatable("crystalized.sword.pufferfish.name").decoration(ITALIC, false));
		pufferFish.setItemMeta(pufferFish_meta);
		List<Component> pufferFish_lore = new ArrayList<>();
		pufferFish_lore.add(translatable("crystalized.sword.pufferfish.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(pufferFish, 1250, pufferFish_lore, ItemCategory.Melee, 18,
				translatable("crystalized.sword.pufferfish.name").decoration(ITALIC, false), 2));

		ItemStack slimeSword = new ItemStack(STONE_SWORD);
		slimeSword.addEnchantment(KNOCKBACK, 1);
		ItemMeta slimeSword_meta = slimeSword.getItemMeta();
		slimeSword_meta.setCustomModelData(1);
		slimeSword_meta.displayName(translatable("crystalized.sword.slime.name").decoration(ITALIC, false));
		slimeSword.setItemMeta(slimeSword_meta);
		List<Component> slimeSword_lore = new ArrayList<>();
		slimeSword_lore.add(translatable("crystalized.sword.slime.desc1").color(WHITE).decoration(ITALIC, false));
		slimeSword_lore.add(translatable("crystalized.sword.slime.desc2").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(slimeSword, 1000, slimeSword_lore, ItemCategory.Melee, 20,
				translatable("crystalized.sword.slime.name").decoration(ITALIC, false), 1));

		ItemStack marksman = new ItemStack(BOW);
		ItemMeta marksman_meta = marksman.getItemMeta();
		marksman_meta.setCustomModelData(1);
		marksman_meta.displayName(translatable("crystalized.bow.marksman.name").decoration(ITALIC, false));
		marksman.setItemMeta(marksman_meta);
		List<Component> marksman_lore = new ArrayList<>();
		marksman_lore.add(translatable("crystalized.bow.marksman.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(marksman, 750, marksman_lore, ItemCategory.Range, 6,
				translatable("crystalized.bow.marksman.name").decoration(ITALIC, false), 1));

		ItemStack ricochet = new ItemStack(BOW);
		ricochet.addEnchantment(PUNCH, 1);
		ItemMeta ricochet_meta = ricochet.getItemMeta();
		ricochet_meta.setCustomModelData(3);
		ricochet_meta.displayName(translatable("crystalized.bow.ricochet.name").decoration(ITALIC, false));
		ricochet.setItemMeta(ricochet_meta);
		List<Component> ricochet_lore = new ArrayList<>();
		ricochet_lore.add(translatable("crystalized.bow.ricochet.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(ricochet, 1500, ricochet_lore, ItemCategory.Range, 8,
				translatable("crystalized.bow.ricochet.name").decoration(ITALIC, false), 3));

		ItemStack multishot = new ItemStack(CROSSBOW);
		multishot.addEnchantment(MULTISHOT, 1);
		ItemMeta multishot_meta = multishot.getItemMeta();
		multishot_meta.setCustomModelData(1);
		multishot_meta.displayName(translatable("crystalized.crossbow.multi.name").decoration(ITALIC, false));
		multishot.setItemMeta(multishot_meta);
		List<Component> multishot_lore = new ArrayList<>();
		multishot_lore.add(translatable("crystalized.crossbow.multi.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(multishot, 2000, multishot_lore, ItemCategory.Range, 26,
				translatable("crystalized.crossbow.multi.name").decoration(ITALIC, false), 1));

		ItemStack charged = new ItemStack(CROSSBOW);
		ItemMeta charged_meta = charged.getItemMeta();
		charged_meta.setCustomModelData(3);
		charged_meta.displayName(translatable("crystalized.crossbow.charged.name"));
		charged.setItemMeta(charged_meta);
		List<Component> charged_lore = new ArrayList<>();
		charged_lore.add(translatable("crystalized.crossbow.charged.desc").color(WHITE).decoration(ITALIC, false));
		lsItems.add(new LSItem(charged, 2500, charged_lore, ItemCategory.Range, 44,
				translatable("crystalized.crossbow.charged.name").decoration(ITALIC, false), 3));

		ItemStack speed2pot = new ItemStack(POTION);
		PotionMeta speed2potMeta = (PotionMeta) speed2pot.getItemMeta();
		speed2potMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 1, true, true, true), true);
		speed2potMeta.displayName(Component.text("Potion of Swiftness").color(WHITE).decoration(ITALIC, false));
		speed2pot.setItemMeta(speed2potMeta);
		lsItems.add(
				new LSItem(speed2pot, 1000, null, ItemCategory.Consumable, 46, Component.text("Potion of Swiftness"), null));

		ItemStack speed1pot = new ItemStack(POTION);
		PotionMeta speed1potMeta = (PotionMeta) speed1pot.getItemMeta();
		speed1potMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 25, 0, true, true, true), true);
		speed1potMeta.displayName(Component.text("Potion of Swiftness").color(WHITE).decoration(ITALIC, false));
		speed1pot.setItemMeta(speed1potMeta);
		lsItems.add(
				new LSItem(speed1pot, 750, null, ItemCategory.Consumable, 47, Component.text("Potion of Swiftness"), null));

		ItemStack respot = new ItemStack(POTION);
		PotionMeta respotMeta = (PotionMeta) respot.getItemMeta();
		respotMeta.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 25, 0, true, true, true), true);
		respotMeta.displayName(Component.text("Potion of Resistance").color(WHITE).decoration(ITALIC, false));
		respot.setItemMeta(respotMeta);
		lsItems.add(
				new LSItem(respot, 750, null, ItemCategory.Consumable, 45, Component.text("Potion of Resistance"), null));

		ItemStack spectralArrow = new ItemStack(SPECTRAL_ARROW, 3);
		List<Component> spectral_lore = new ArrayList<>();
		spectral_lore.add(Component.text("| '").color(WHITE).decoration(ITALIC, false));
		ItemMeta spectral_meta = spectralArrow.getItemMeta();
		spectral_meta.lore(spectral_lore);
		spectralArrow.setItemMeta(spectral_meta);
		lsItems.add(new LSItem(spectralArrow, 125, spectral_lore, ItemCategory.Ammunition, 51, null, null));

		ItemStack dragonArrow = new ItemStack(ARROW, 3);
		ItemMeta dragon_meta = dragonArrow.getItemMeta();
		dragon_meta.setCustomModelData(1);
		dragon_meta.displayName(translatable("crystalized.item.dragonarrow.name").decoration(ITALIC, false));
		List<Component> dragon_lore = new ArrayList<>();
		dragon_lore.add(translatable("crystalized.item.dragonarrow.desc").color(WHITE).decoration(ITALIC, false));
		dragon_lore.add(Component.text("|").color(WHITE).decoration(ITALIC, false));
		dragon_meta.lore(dragon_lore);
		dragonArrow.setItemMeta(dragon_meta);
		lsItems.add(new LSItem(dragonArrow, 350, dragon_lore, ItemCategory.Ammunition, 52,
				translatable("crystalized.item.dragonarrow.name").decoration(ITALIC, false), 1));

		ItemStack exploArrow = new ItemStack(ARROW, 3);
		ItemMeta explo_meta = exploArrow.getItemMeta();
		explo_meta.setCustomModelData(2);
		explo_meta.displayName(translatable("crystalized.item.explosivearrow.name").decoration(ITALIC, false));
		List<Component> explo_lore = new ArrayList<>();
		explo_lore.add(translatable("crystalized.item.explosivearrow.desc").color(WHITE).decoration(ITALIC, false));
		explo_lore.add(Component.text("| |").color(WHITE).decoration(ITALIC, false));
		explo_meta.lore(explo_lore);
		exploArrow.setItemMeta(explo_meta);
		lsItems.add(new LSItem(exploArrow, 350, explo_lore, ItemCategory.Ammunition, 53,
				translatable("crystalized.item.explosivearrow.name").decoration(ITALIC, false), 2));

		ItemStack underDog = new ItemStack(STONE_SWORD);
		ItemMeta underDog_meta = underDog.getItemMeta();
		underDog_meta.setCustomModelData(3);
		underDog_meta.displayName(Component.translatable("crystalized.sword.underdog.name").decoration(ITALIC, false));
		List<Component> underDog_lore = new ArrayList<>();
		underDog_lore.add(Component.translatable("crystalized.sword.underdog.desc").color(WHITE).decoration(ITALIC, false));
		underDog_meta.lore(underDog_lore);
		underDog.setItemMeta(underDog_meta);
		lsItems.add(new LSItem(underDog, 750, underDog_lore, ItemCategory.Melee, 36,
				Component.text("Underdog Sword").decoration(ITALIC, false), 3));

		ItemStack stonePick = new ItemStack(STONE_PICKAXE);
		lsItems.add(new LSItem(stonePick, null, null, ItemCategory.Defuser, null, null, null));

		creation_number = 1; // reset id

		return lsItems;
	}

	// this can handle null being passed in
	public ItemStack buildDisplayItem(Player p) {
		if (price == null) {
			return null;
		}
		List<Component> lore;
		if (description == null) {
			lore = new ArrayList<>();
		} else {
			lore = new ArrayList<>(description);
		}
		lore.add(Component.text("")); // add a newline so that the price is seperated
		if (p != null && (Litestrike.getInstance().game_controller.getPlayerData(p).getMoney() - price) >= 0) {
			lore.add(Component.text("" + price + "\uE104").color(WHITE).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("" + price + "\uE104").color(RED).decoration(TextDecoration.ITALIC, false));
		}

		ItemStack displayItem = item.clone();
		ItemMeta meta = displayItem.getItemMeta();
		meta.lore(lore);
		displayItem.setItemMeta(meta);

		// underdog
		if (item.getType() == Material.STONE_SWORD && item.getItemMeta().getCustomModelData() == 3) {
			displayItem = do_underdog_sword(displayItem, p);
		}
		return displayItem;
	}

	public static ItemStack do_underdog_sword(ItemStack item, Player player) {
		ItemStack cloned_item = item.clone();
		int placer_wins_amt = 0;
		int breaker_wins_amt = 0;
		for (Team w : Litestrike.getInstance().game_controller.round_results) {
			if (w == Team.Placer) {
				placer_wins_amt += 1;
			} else {
				breaker_wins_amt += 1;
			}
		}
		int rounds_down = 0;
		if (Teams.get_team(player.getName()) == Team.Breaker) {
			rounds_down = placer_wins_amt - breaker_wins_amt;
		} else {
			rounds_down = breaker_wins_amt - placer_wins_amt;
		}
		if (rounds_down <= 0) {
			return cloned_item;
		}
		// Bukkit.getLogger().severe("rounds down is: " + rounds_down + " for player: "
		// + player.getName());
		ItemMeta im = cloned_item.getItemMeta();
		im.setCustomModelData(3 + rounds_down);
		im.addEnchant(Enchantment.SHARPNESS, rounds_down, true);
		cloned_item.setItemMeta(im);
		return cloned_item;
	}

	public static ItemCategory getItemCategory(ItemStack i) {
		for (LSItem lsi : LSItem.shopItems) {
			if (i.getType() == lsi.item.getType()) {
				return lsi.categ;
			}
		}

		String name = i.getType().name();
		// hardcoding default equipment because it isn't in LSITEM
		if (name.contains("SWORD")) {
			return ItemCategory.Melee;
		}
		if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS")) {
			return ItemCategory.Armor;
		}
		if (name.contains("PICKAXE")) {
			return ItemCategory.Defuser;
		}
		if (name.contains("POTION")) {
			return ItemCategory.Consumable;
		}

		return null;
	}
}

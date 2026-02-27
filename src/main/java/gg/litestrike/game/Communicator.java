package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

// new: Left Click: go here (arrow)
// new Right Click: Enemy Spotted (arrow)
// Shift Left Click: Lets Go A
// Shift Right Click: Lets Go B
// Swap Hand Item: Retreat
// Shift Swap Hand: Wait here.
// Drop: Help me.!
// Shift Drop: Drop Please

public class Communicator implements Listener {
	public static final int ARROW_DURATION = 120;
	private List<PingArrow> arrows = new ArrayList<>();

	public Communicator() {
		new BukkitRunnable() {
			public void run() {
				arrows.forEach(pa -> {
					pa.Age += 1;

					// do bobbing up and down
					double yOffset = Math.sin(Math.toRadians(pa.Age * 9)) / 2;
					pa.armor_stand.teleport(pa.armor_stand.getLocation().add(0, (yOffset - pa.lastYOffset), 0));
					pa.lastYOffset = yOffset;

				});
				arrows.removeIf(pa -> pa.shouldRemove());
			};
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	private boolean isCommunication(PlayerEvent e, ItemStack held_item) {
		if (held_item == null || held_item.getType() != Material.PITCHER_POD
				|| Litestrike.getInstance().game_controller == null) {
			return false;
		}
		if (e.getPlayer().hasCooldown(Material.PITCHER_POD)) {
			e.getPlayer().playSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.AMBIENT, 1.0f, 0.5f));
			return false;
		} else {
			e.getPlayer().setCooldown(Material.PITCHER_POD, 10);
		}

		return true;
	}

	private void makeArrow(Player p, PingArrow.ArrowType type) {
		arrows.forEach(arrow -> {
			if (arrow.p == p) {
				arrow.armor_stand.remove();
			}
		});
		arrows.removeIf(arrow -> arrow.p == p);
		arrows.add(new PingArrow(p, type));
	}

	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		if (!isCommunication(e, e.getItem())) {
			return;
		}
		e.setCancelled(true);
		Player p = e.getPlayer();

		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (e.getPlayer().isSneaking()) {
				sendCommunication(p, "lets_go_a", 0x545DD6); // marine blue
			} else {
				sendCommunication(p, "go_here", 0x78D941); // green
				makeArrow(p, PingArrow.ArrowType.GoHere);
			}
		} else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (e.getPlayer().isSneaking()) {
				sendCommunication(p, "lets_go_b", 0x545DD6); // marine blue
			} else {
				sendCommunication(p, "enemy_spotted", 0xD4220F); // red
				makeArrow(p, PingArrow.ArrowType.EnemySeen);
			}
		}
	}

	@EventHandler
	public void onSwapHandEvent(PlayerSwapHandItemsEvent e) {
		if (e.getOffHandItem() == null || e.getOffHandItem().getType() == Material.PITCHER_POD) {
			e.setCancelled(true);
		}
		if (!isCommunication(e, e.getOffHandItem())) {
			return;
		}
		e.setCancelled(true);
		Player p = e.getPlayer();

		if (e.getPlayer().isSneaking()) {
			sendCommunication(p, "wait_here", 0x7278CF); // light blue
		} else if (isCarrieing(p)) {
			sendCommunication(p, "get_carried", 0xE32DB1); // purple
		} else {
			sendCommunication(p, "retreat", 0xE3AF34); // orange
		}
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		if (!isCommunication(e, e.getItemDrop().getItemStack())) {
			return;
		}
		e.setCancelled(true);
		Player p = e.getPlayer();

		if (e.getPlayer().isSneaking()) {
			sendCommunication(p, "drop_please", 0xDE83C5); // pink
		} else if (isInMeowSituation(p)) {
			sendCommunication(p, "meow", 0xE32DB1); // purple
		} else if (isCooked(p)) {
			sendCommunication(p, "cooked", 0xC7BC95); // light brown
		} else {
			sendCommunication(p, "help_me", 0xDE83C5); // pink
		}
	}

	private void sendCommunication(Player p, String text, int color) {
		Teams t = Litestrike.getInstance().game_controller.teams;
		List<Player> enemy_team = t.get_enemy_team_of(p);
		List<Player> audience = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!enemy_team.contains(player)) {
				audience.add(player);
			}
		}
		Audience a = Audience.audience(audience);

		TextColor team_color = Teams.get_team_color(Teams.get_team(p.getName()));

		a.sendMessage(Component.text("<" + p.getName() + "> ").color(team_color)
				.append(translatable("crystalized.game.litestrike.communicator." + text).color(TextColor.color(color))));
	}

	private boolean isCooked(Player p) {
		Teams t = Litestrike.getInstance().game_controller.teams;
		boolean someone_alive = false;
		for (Player team_member : t.get_team_of(t.get_team(p))) {
			if (team_member.getGameMode() == GameMode.SURVIVAL && team_member != p) {
				someone_alive = true;
				break;
			}
		}
		return (p.getHealth() <= 1.0 && someone_alive == false);
	}

	private boolean isCarrieing(Player p) {
		GameController gc = Litestrike.getInstance().game_controller;

		float remaining_team_damage = 0;
		for (Player team_member : gc.teams.get_team_of(gc.teams.get_team(p))) {
			if (team_member != p) {
				remaining_team_damage += gc.getPlayerData(team_member).total_damage;
			}
		}
		return remaining_team_damage > 100.0 && gc.getPlayerData(p).total_damage > remaining_team_damage;
	}

	private boolean isInMeowSituation(Player p) {
		boolean catNearby = false;
		for (Player maybe_cat : p.getLocation().getNearbyPlayers(3)) {
			if (maybe_cat == p) {
				continue;
			}
			if (maybe_cat.getName().contains("cat") || maybe_cat.getName().contains("Cat")) {
				catNearby = true;
			}
		}

		return catNearby && p.isJumping();
	}

	public static void giveRadio() {
		for (Player p : Litestrike.getInstance().game_controller.teams.get_all_players()) {
			ItemStack radio = new ItemStack(Material.PITCHER_POD, 1);
			ItemMeta meta = radio.getItemMeta();
			meta.displayName(
					Component.text("Communicator").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
			List<TextComponent> list = new ArrayList<>();
			list.add(Component.text("Right click!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			list.add(Component.text("Swap hands!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			list.add(Component.text("Drop it!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			list.add(Component.text("And do all of these Shifted!").color(NamedTextColor.GOLD)
					.decoration(TextDecoration.ITALIC, false));
			meta.lore(list);
			radio.setItemMeta(meta);

			if (p.getInventory().contains(Material.PITCHER_POD)) {
				continue;
			}

			if (p.getInventory().getItem(7) == null || p.getInventory().getItem(7).isEmpty()) {
				p.getInventory().setItem(7, radio);
			} else {
				p.getInventory().addItem(radio);
			}
		}
	}
}

class PingArrow {
	Player p;
	int Age;
	ArmorStand armor_stand;
	ArrowType aT;
	double lastYOffset;
	Team team;

	public enum ArrowType {
		GoHere,
		EnemySeen,
	}


	public PingArrow(Player p, ArrowType at) {
		Teams t = Litestrike.getInstance().game_controller.teams;
		this.p = p;
		this.Age = 0;
		this.aT = at;
		this.lastYOffset = 0;

		HashSet<Material> transparents = new HashSet<>();
		transparents.add(Material.AIR);
		transparents.add(Material.LIGHT);
		transparents.add(Material.GLASS);
		transparents.add(Material.GLASS_PANE);
		transparents.add(Material.CAVE_AIR);
		transparents.add(Material.HANGING_ROOTS);
		transparents.add(Material.VOID_AIR);
		transparents.add(Material.TINTED_GLASS);
		transparents.add(Material.WATER);

		Location lookingAt = p.getTargetBlock(transparents, 40).getLocation().add(0.5, 2, 0.5);
    Vector direction = p.getLocation().toVector().subtract(lookingAt.toVector()).normalize();

		this.armor_stand = p.getWorld().spawn(lookingAt.add(direction), ArmorStand.class, as -> {
			as.lookAt(p.getLocation(), LookAnchor.EYES);
			as.setInvisible(true);
			as.setInvulnerable(true);
			as.setGravity(false);
			as.setCollidable(false);
			as.setCanPickupItems(false);
			as.setMarker(true);
			as.setGlowing(true);
			as.addScoreboardTag("arrow");
		});

		ItemStack is = new ItemStack(Material.POINTED_DRIPSTONE);
		ItemMeta im = is.getItemMeta();
		im.setItemModel(NamespacedKey.fromString("crystalized:models/ping_arrow"));
		is.setItemMeta(im);
		armor_stand.getEquipment().setHelmet(is);
		for (Player player : t.get_enemy_team_of(p)) {
			player.hideEntity(Litestrike.getInstance(), armor_stand);
		}
	}

	public boolean shouldRemove() {
		if (Age % 4 != 0) {
			return false;
		}

		// check if player of own team is nearby
		Teams t = Litestrike.getInstance().game_controller.teams;
		Collection<Player> nearby_players = armor_stand.getLocation().getNearbyPlayers(2);
		Team own_team = t.get_team(p);
		boolean player_nearby = nearby_players.stream().anyMatch(nearby -> t.get_team(nearby) == own_team);

		if (Age >= Communicator.ARROW_DURATION || player_nearby) {
			armor_stand.remove();
			return true;
		} else {
			return false;
		}
	}
}

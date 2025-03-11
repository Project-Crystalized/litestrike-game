package gg.litestrike.game;

import static org.bukkit.GameMode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

public class DeathHandler implements Listener {

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		e.setCancelled(true);
		if (e.getPlayer().getGameMode() != SURVIVAL) {
			return;
		}
		Player p = e.getPlayer();
		GameController gc = Litestrike.getInstance().game_controller;

		// reset killed player
		p.setGameMode(SPECTATOR);
		p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
		p.getInventory().clear();

		Entity entity = e.getDamageSource().getCausingEntity();
		Player killer = null;
		if (entity != null && entity instanceof Player) {
			killer = (Player) entity;
		}

		send_death_message(p, killer, get_death_icon(e.getDamageSource().getDamageType(), killer));
		log_death_message(p, killer);

		// give death/kill and money
		gc.getPlayerData(p).deaths += 1;
		if (killer != null) {
			gc.getPlayerData(killer).kills += 1;
			gc.getPlayerData(killer).addMoney(400,
					translatable("crystalized.game.litestrike.money.kill")
							.append(text(p.getName()))
							.color(Teams.get_team_color(gc.teams.get_team(killer))));
		}

		List<Player> assisters = get_assisters(p);
		assisters.remove(killer);
		for (Player assister : assisters) {
			gc.getPlayerData(assister).assists += 1;
			gc.getPlayerData(assister).addMoney(50,
					translatable("crystalized.game.litestrike.money.assist")
							.append(text(p.getName()))
							.color(Teams.get_team_color(gc.teams.get_team(p))));
		}

		// Shop.getShop(p).resetEquip();
		// Shop.getShop(p).resetEquipCounters();

		Team killed_team = gc.teams.get_team(p);
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (gc.teams.get_team(player) == killed_team) {
				SoundEffects.ally_death(player);
			} else {
				SoundEffects.enemy_death(player);
			}
		}
	}

	private List<Player> get_assisters(Player p) {
		GameController gc = Litestrike.getInstance().game_controller;
		List<Player> assiters = new ArrayList<>();
		for (PlayerData pd : gc.playerDatas) {
			if (pd.assist_list.get(p) != null && pd.assist_list.get(p) > 3.9) {
				Player player = Bukkit.getPlayer(pd.player);
				if (player != null) {
					assiters.add(player);
				}
			}
		}
		return assiters;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		GameController gc = Litestrike.getInstance().game_controller;
		if (gc == null) {
			e.setCancelled(true);
			return;
		}
		if (!(e.getEntity() instanceof Player) || !(e.getDamageSource().getCausingEntity() instanceof Player)) {
			return;
		}
		Player damager = (Player) e.getDamageSource().getCausingEntity();
		Player damage_receiver = (Player) e.getEntity();

		Bukkit.getLogger().severe("damage dealt: " + e.getFinalDamage());

		if (gc.teams.get_team(damage_receiver) == gc.teams.get_team(damager)) {
			e.setCancelled(true);
		} else {
			Map<Player, Double> assist_list = gc.getPlayerData(damager).assist_list;
			assist_list.put(damage_receiver, assist_list.getOrDefault(damage_receiver, 0.0) + e.getFinalDamage());
		}
	}

	private Component get_death_icon(DamageType dt, Player killer) {
		if (dt == DamageType.ARROW) {
			if (killer.getInventory().getItemInMainHand().getType() == Material.CROSSBOW
					|| killer.getInventory().getItemInOffHand().getType() == Material.CROSSBOW) {
				return text(" \uE11E "); // CROSSBOW death icon
			}
			return text(" \uE102 "); // BOW death icon
		} else if (dt == DamageType.PLAYER_ATTACK) {
			if (killer.getInventory().getItemInMainHand().getType().toString().toLowerCase().contains("pickaxe")
					|| killer.getInventory().getItemInOffHand().getType().toString().toLowerCase().contains("pickaxe")) {
				return text(" \uE11E "); // CROSSBOW death icon
			}
			if (killer.getInventory().getItemInMainHand().getType().toString().toLowerCase().contains("axe")
					|| killer.getInventory().getItemInOffHand().getType().toString().toLowerCase().contains("axe")) {
				return text(" \uE11F "); // PICKAXE death icon
			}
			return text(" \uE101 "); // SWORD death icon
		} else {
			return text(" \uE103 "); // generic death icon
		}
	}

	private void log_death_message(Player p, Player killer) {
		GameController gc = Litestrike.getInstance().game_controller;
		String log_msg = p.getName() + "(" + gc.teams.get_team(p) + ") was killed by ";
		if (killer != null) {
			log_msg = log_msg + killer.getName() + "(" + gc.teams.get_team(killer) + ")";
		}
		Bukkit.getLogger().info(log_msg);
	}

	private static final Component death_prefix = text("[\uE103]");

	private void send_death_message(Player p, Player killer, Component icon) {
		GameController gc = Litestrike.getInstance().game_controller;
		Component player_name = text(p.getName()).color(Teams.get_team_color(gc.teams.get_team(p)));
		Component death_message = text("  ");

		if (killer != null) {
			Component killer_name = text(killer.getName()).color(Teams.get_team_color(gc.teams.get_team(killer)));
			death_message = death_message.append(killer_name);
		}

		death_message = death_message.append(icon).append(player_name);

		Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(death_prefix.append(death_message));
	}
}

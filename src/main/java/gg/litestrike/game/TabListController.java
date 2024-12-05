package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.List;

class TabListController {
	public TabListController() {

		new BukkitRunnable() {
			@Override
			public void run() {
				if (Litestrike.getInstance().game_controller == null) {
					cancel();
					return;
				}
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendPlayerListFooter(text("")
							.append(text("---------------------------------------------------").color(NamedTextColor.GRAY))
							.append(render_player_stat(p))
							.append(text("\n---------------------------------------------------\n\n").color(NamedTextColor.GRAY)));

					p.sendPlayerListHeader(text("LITESTRIKE").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)
							.append(text(" \uE100").color(NamedTextColor.WHITE)));
				}

			}
		}.runTaskTimer(Litestrike.getInstance(), 5, 20);
	}

	private static Component render_player_stat(Player p) {
		GameController gc = Litestrike.getInstance().game_controller;
		Team p_team = gc.teams.get_team(p);

		Component footer = text("");

		List<Component> disc_list = new ArrayList<>();
		List<Component> enemy = new ArrayList<>();
		List<Component> allay = new ArrayList<>();

		for (PlayerData pd : gc.playerDatas) {
			Player player = Bukkit.getPlayer(pd.player);
			Component player_stats = text(" \uE101 ") // sword death icon
					.append(text(pd.kills))
					.append(text(" \uE103 ")) // Death icon
					.append(text(pd.deaths))
					.append(text(" Score: "))
					.append(text(pd.calc_player_score()));

			if (player == null) {
				disc_list.add(text("\n ")
						.append(text("[Disconnected] "))
						.append(text(pd.player).color(NamedTextColor.GRAY))
						.append(player_stats));
				continue;
			}

			Component player_status = text("\n ");
			if (player.getGameMode() == GameMode.SPECTATOR) {
				player_status = player_status.append(text("[Dead] "));
			} else {
				player_status = player_status.append(text("[Alive] "));
			}
			if (gc.teams.get_team(player) == Team.Placer) {
				player_status = player_status.append(text(pd.player).color(Teams.PLACER_RED));
			} else {
				player_status = player_status.append(text(pd.player).color(Teams.BREAKER_GREEN));
			}
			player_status = player_status.append(player_stats);
			if (gc.teams.get_team(player) == p_team) {
				allay.add(player_status);
			} else {
				enemy.add(player_status);
			}
		}

		for (Component c : allay) {
			footer = footer.append(c);
		}
		for (Component c : enemy) {
			footer = footer.append(c);
		}
		for (Component c : disc_list) {
			footer = footer.append(c);
		}

		return footer;
	}

}

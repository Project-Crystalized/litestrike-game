package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
							.append(
									text("-------------------------------\uE101 / \uE103 / A---\uE104-------").color(NamedTextColor.GRAY))
							.append(render_player_stat(p))
							.append(text("\n---------------------------------------------------\n").color(NamedTextColor.GRAY)));

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
			Component player_stats = text(pd.kills)
					.append(text(" / "))
					.append(text(pd.deaths))
					.append(text(" / "))
					.append(text(pd.assists))
					.append(text("    " + pd.getMoney()).color(TextColor.color(0x0ab1c4)));

			Component player_status;
			if (player == null) {
				player_status = text("\n ").append(text("[Disconnected] ")).append(text(pd.player).color(NamedTextColor.GRAY));
			} else if (player.getGameMode() == GameMode.SPECTATOR) {
				player_status = text("\n ").append(text("[Dead] ")).append(text(pd.player).color(NamedTextColor.GRAY));
			} else if (gc.teams.get_team(player) == Team.Placer) {
				player_status = text("\n ").append(text("[Alive] ")).append(text(pd.player).color(Teams.PLACER_RED));
			} else {
				player_status = text("\n ").append(text("[Alive] ")).append(text(pd.player).color(Teams.BREAKER_GREEN));
			}

			int left_size = PlainTextComponentSerializer.plainText().serialize(player_status).length();
			int right_size = PlainTextComponentSerializer.plainText().serialize(player_stats).length();
			int center_padding = 51 - left_size - right_size;
			player_status = player_status.append(text(" ".repeat(center_padding))).append(player_stats);
			// Bukkit.getLogger().severe("total_len: " + total_len);

			if (player == null) {
				disc_list.add(player_status);
			} else if (gc.teams.get_team(player) == p_team) {
				allay.add(player_status);
			} else {
				enemy.add(player_status);
			}
		}

		for (Component c : allay) {
			footer = footer.append(c);
		}
		footer = footer.append(text("\n---------------------------------------------------").color(NamedTextColor.GRAY));
		for (Component c : enemy) {
			footer = footer.append(c);
		}
		if (disc_list.size() != 0) {
			footer = footer.append(text("\n---------------------------------------------------").color(NamedTextColor.GRAY));
		}
		for (Component c : disc_list) {
			footer = footer.append(c);
		}

		return footer;
	}

}

package gg.litestrike.game;

import gg.crystalized.lobby.Ranks;
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
									text("----------------------------------\uE101 / \uE103 / A / dmg ---\uE104----")
											.color(NamedTextColor.GRAY))
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
					.append(text(" / "))
					.append(text((int) Math.floor(pd.total_damage)))
					.append(text("    " + makeTwoDigits(pd.getMoney(), 4))).color(TextColor.color(0x0ab1c4));

			Component player_status;
			Component rank = text(" ").append(Ranks.getIcon(p)).append(text(" "));
			if (player == null) {
				player_status = text("\n ").append(text("[Disconnected] ")).append(rank).append(text(pd.player).color(NamedTextColor.GRAY));
			} else if (player.getGameMode() == GameMode.SPECTATOR) {
				player_status = text("\n ").append(text("[Dead] ")).append(rank).append(text(pd.player).color(NamedTextColor.GRAY));
			} else if (gc.teams.get_team(player) == Team.Placer) {
				player_status = text("\n ").append(text("[Alive] ")).append(rank).append(text(pd.player).color(Teams.PLACER_RED));
			} else {
				player_status = text("\n ").append(text("[Alive] ")).append(rank).append(text(pd.player).color(Teams.BREAKER_GREEN));
			}

			String left_size = PlainTextComponentSerializer.plainText().serialize(player_status);
			String right_size = PlainTextComponentSerializer.plainText().serialize(player_stats);
			int center_padding = 150 - (balance(left_size) + balance(right_size));
			String dots = ".".repeat(center_padding);
			player_status = player_status.append(text(dots).color(NamedTextColor.GRAY)).append(player_stats);
			// Bukkit.getLogger().severe(pd.player + " : " +
			// balance(PlainTextComponentSerializer.plainText().serialize(player_status)));

			Team p_team = gc.teams.get_team(p);
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

	public static int balance(String name) {
		char[] chars = name.toCharArray();
		int sum = 0;
		for (char c : chars) {
			sum += BitmapGlyphInfo.getBitmapGlyphInfo(c).width;
		}
		sum += name.length() - 1;
		return sum / 2;
	}

	 public static String makeTwoDigits(Integer num, int supposed){
	 	String s = num.toString();
	 	if(s.length() == supposed){
	 		return s;
	 	}

	 	if(supposed - s.length() > 0) {
	 		s = ".....".repeat(supposed - s.length()) + s;
	 	}

	 	return s;
	 }

}

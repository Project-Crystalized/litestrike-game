package gg.litestrike.game;

import gg.crystalized.lobby.Leaderboards;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TabListController {
	private static final Component TAB_HEADER = text("LITESTRIKE").color(NamedTextColor.GREEN)
			.decoration(TextDecoration.BOLD, true).append(text(" \uE100").color(NamedTextColor.WHITE));
	private static final Component FOOTER_PREFIX = text("")
			.append(
					text("----------------------------------\uE101 / \uE103 / A / dmg ---\uE104----")
							.color(NamedTextColor.GRAY));
	private static final Component SECTION_DIVIDER = text("\n---------------------------------------------------")
			.color(NamedTextColor.GRAY);
	private static final Component FOOTER_SUFFIX = text("\n---------------------------------------------------\n")
			.color(NamedTextColor.GRAY);

	public TabListController() {

		new BukkitRunnable() {
			@Override
			public void run() {
				GameController gc = Litestrike.getInstance().game_controller;
				if (gc == null) {
					cancel();
					return;
				}

				Map<Team, List<Component>> rowsByTeam = getPlayerStatRows(gc);
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendPlayerListFooter(getPlayerListFooter(gc.teams.get_team(p), rowsByTeam));
					p.sendPlayerListHeader(TAB_HEADER);

					for (Player entry : Bukkit.getOnlinePlayers()) {
						p.unlistPlayer(entry);
					}
				}

			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 20);
	}

	private static Map<Team, List<Component>> getPlayerStatRows(GameController gc) {
		Map<Team, List<Component>> rowsByTeam = new HashMap<>();

		for (PlayerData pd : gc.playerDataManager.getAll()) {
			Player player = Bukkit.getPlayer(pd.player);
			Component player_stats = text(pd.kills)
					.append(text(" / "))
					.append(text(pd.deaths))
					.append(text(" / "))
					.append(text(pd.assists))
					.append(text(" / "))
					.append(text((int) Math.floor(pd.total_damage)))
			.append(text("    " + makeTwoDigits(pd.getMoney(), 4))).color(TextColor.color(0x0ab1c4));

			Component rank = Component.empty();
			try {
				if (player != null) {
					Component icon = Ranks.getIcon(player);
					if (!PlainTextComponentSerializer.plainText().serialize(icon).isEmpty()) {
						rank = text(" ").append(icon).append(text(" "));
					}
				}
			} catch (NoClassDefFoundError e) {
			}
			Component player_status = build_player_status(rank, player, pd, gc);

			String left_size = PlainTextComponentSerializer.plainText().serialize(player_status);
			String right_size = PlainTextComponentSerializer.plainText().serialize(player_stats);
			int center_padding = Math.max(150 - (Leaderboards.balance(left_size) + Leaderboards.balance(right_size)), 0);
			String dots = ".".repeat(center_padding);
			player_status = player_status.append(text(dots).color(NamedTextColor.GRAY)).append(player_stats);

			rowsByTeam.computeIfAbsent(gc.teams.get_team(pd.player), k -> new ArrayList<>()).add(player_status);
		}

		return rowsByTeam;
	}

	private static Component getPlayerListFooter(Team viewerTeam, Map<Team, List<Component>> rowsByTeam) {
		Component footer = FOOTER_PREFIX;
		for (Component c : rowsByTeam.getOrDefault(viewerTeam, List.of())) {
			footer = footer.append(c);
		}
		footer = footer.append(SECTION_DIVIDER);
		for (Map.Entry<Team, List<Component>> entry : rowsByTeam.entrySet()) {
			if (entry.getKey() == viewerTeam) {
				continue;
			}
			for (Component c : entry.getValue()) {
				footer = footer.append(c);
			}
		}
		return footer.append(FOOTER_SUFFIX);
	}


	private static Component build_player_status(Component rank, Player player, PlayerData pd, GameController gc) {
		String tag;
		TextColor name_color;
		if (player == null) {
			tag = "[Disconnected] ";
			name_color = NamedTextColor.GRAY;
		} else if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.ADVENTURE) {
			tag = "[Dead] ";
			name_color = NamedTextColor.GRAY;
		} else if (gc.teams.get_team(player) == Team.Placer) {
			tag = "[Alive] ";
			name_color = Teams.PLACER_RED;
		} else {
			tag = "[Alive] ";
			name_color = Teams.BREAKER_GREEN;
		}

		Component status = text("\n ").append(text(tag)).append(rank);
		return status.append(text(pd.player).color(name_color));
	}

	public static String makeTwoDigits(Integer num, int supposed) {
		String s = num.toString();
		if (s.length() == supposed) {
			return s;
		}

		if (supposed - s.length() > 0) {
			s = ".....".repeat(supposed - s.length()) + s;
		}

		return s;
	}

}

package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.floodgate.api.FloodgateApi;

import static net.kyori.adventure.text.Component.text;

public class ScoreboardController {
	public static void setup_scoreboard(Teams t, int game_id) {
		for (Player p : t.get_placers()) {
			give_player_scoreboard(p, gg.litestrike.game.Team.Placer, t, game_id);
		}
		for (Player p : t.get_breakers()) {
			give_player_scoreboard(p, gg.litestrike.game.Team.Breaker, t, game_id);
		}
	}

	public static void give_player_scoreboard(Player p, gg.litestrike.game.Team t, Teams teams, int game_id) {
		Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
		FloodgateApi floodgateapi = FloodgateApi.getInstance();

		Team placers = sb.registerNewTeam("placers");
		placers.color(NamedTextColor.RED);
		placers.setAllowFriendlyFire(false);
		placers.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
		for (Player player : teams.get_placers()) {
			placers.addPlayer(player);
		}

		Team breakers = sb.registerNewTeam("breakers");
		breakers.color(NamedTextColor.GREEN);
		breakers.setAllowFriendlyFire(false);
		breakers.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
		for (Player player : teams.get_breakers()) {
			breakers.addPlayer(player);
		}

		// Bedrock scoreboard
		if (floodgateapi.isFloodgatePlayer(p.getUniqueId())) {
			new BedrockScoreboard(p, t, teams, game_id);
			return;
		}

		Component title = text("LITESTRIKE").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)
				.append(text(" \uE100").color(NamedTextColor.WHITE));
		Objective obj = sb.registerNewObjective("main", Criteria.DUMMY, title);
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);

		obj.getScore("11").setScore(11);
		obj.getScore("11").customName(text(""));

		if (t == gg.litestrike.game.Team.Breaker) {
			obj.getScore("10").customName(
					Component.translatable("crystalized.game.generic.team").append(text(": ")).append(Litestrike.BREAKER_TEXT));
		} else {
			obj.getScore("10").customName(
					Component.translatable("crystalized.game.generic.team").append(text(": ")).append(Litestrike.PLACER_TEXT));
		}
		obj.getScore("10").setScore(10);

		obj.getScore("9").setScore(9);
		obj.getScore("9").customName(Component.translatable("crystalized.game.generic.money").append(text(": ")));

		obj.getScore("8").setScore(8);
		obj.getScore("8").customName(text(""));

		obj.getScore("7").setScore(7);
		obj.getScore("7")
				.customName(Component.translatable("crystalized.game.litestrike.objective").color(TextColor.color(0xe64cce)));

		obj.getScore("6").setScore(6);
		obj.getScore("6").customName(text(""));

		obj.getScore("5").setScore(5);
		obj.getScore("5").customName(text(""));

		obj.getScore("4").setScore(4);
		obj.getScore("4").customName(text(""));

		if (t == gg.litestrike.game.Team.Breaker) {
			obj.getScore("3").customName(Component.text(""));
		} else {
			obj.getScore("3").customName(Component.text("Bomb Location:"));
		}
		obj.getScore("3").setScore(3);

		// this schows bomb_loc_string
		obj.getScore("2").setScore(2);
		obj.getScore("2").customName(text(""));

		obj.getScore("1").setScore(1);
		obj.getScore("1").customName(text(""));

		obj.getScore("0").setScore(0);
		obj.getScore("0").customName(text("ᴄʀʏꜱᴛᴀʟɪᴢᴇᴅ.ᴄᴄ ").color(TextColor.color(0xc4b50a))
				.append(text("" + game_id).color(NamedTextColor.GRAY)));

		Team bomb_loc = sb.registerNewTeam("bomb_loc");
		bomb_loc.addEntry("2");
		bomb_loc.prefix(text("Unknown"));
		obj.getScore("2").setScore(2);

		Team money_count = sb.registerNewTeam("money_count");
		money_count.addEntry("9");
		money_count.suffix(text("error"));
		obj.getScore("9").setScore(9);

		Team wins_placers = sb.registerNewTeam("wins_placers");
		wins_placers.addEntry("6");
		wins_placers.prefix(text("   "));
		wins_placers.suffix(text("\uE105\uE105\uE105\uE105\uE107 (0)"));
		obj.getScore("6").setScore(6);

		Team wins_breakers = sb.registerNewTeam("wins_breakers");
		wins_breakers.addEntry("5");
		wins_breakers.prefix(text("   "));
		wins_breakers.suffix(text("\uE105\uE105\uE105\uE105\uE107 (0)"));
		obj.getScore("5").setScore(5);

		p.setScoreboard(sb);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (Litestrike.getInstance().game_controller == null) {
					cancel();
					return;
				}
				render_bomb_display();
			}
		}.runTaskTimer(Litestrike.getInstance(), 5, 6);
	}

	public static void render_bomb_display() {
		Teams t = Litestrike.getInstance().game_controller.teams;
		FloodgateApi floodgateapi = FloodgateApi.getInstance();
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		for (Player p : t.get_breakers()) {
			Team bomb_loc = p.getScoreboard().getTeam("bomb_loc");
			if (b != null && b instanceof PlacedBomb) {
				bomb_loc.prefix(Component.text("Bomb: "));
				bomb_loc.suffix(Component.text(((PlacedBomb) b).get_bomb_loc_string(p)));
			} else {
				bomb_loc.prefix(Component.text(""));
				bomb_loc.suffix(Component.text(""));
			}
		}
		for (Player p : t.get_placers()) {
			String bomb_loc_string = "error";
			if (b == null) {
				bomb_loc_string = "Unknown";
			} else if (b instanceof InvItemBomb) {
				bomb_loc_string = ((InvItemBomb) b).get_bomb_loc_string(p);
			} else if (b instanceof DroppedBomb) {
				bomb_loc_string = ((DroppedBomb) b).get_bomb_loc_string(p);
			} else if (b instanceof PlacedBomb) {
				bomb_loc_string = ((PlacedBomb) b).get_bomb_loc_string(p);
			}

			Team bomb_loc = p.getScoreboard().getTeam("bomb_loc");
			if (floodgateapi.isFloodgatePlayer(p.getUniqueId())) {
				p.getScoreboard().getObjective("main").getScore("2").customName(text(bomb_loc_string));
			} else {
				bomb_loc.prefix(text(bomb_loc_string));
			}
		}
	}

	public static void set_win_display(List<gg.litestrike.game.Team> wins) {
		GameController gc = Litestrike.getInstance().game_controller;
		Component placer_text = text(render_win_display(gc.placer_wins_amt));
		Component breaker_text = text(render_win_display(gc.breaker_wins_amt));

		for (Player p : gc.teams.get_placers()) {
			Team breakers = p.getScoreboard().getTeam("wins_breakers");
			Team placers = p.getScoreboard().getTeam("wins_placers");
			if (breakers == null || placers == null) {
				continue;
			}
			placers.prefix(text("\uE109 ").decoration(TextDecoration.BOLD, true));
			breakers.suffix(breaker_text);
			placers.suffix(placer_text);
		}

		for (Player p : gc.teams.get_breakers()) {
			Team breakers = p.getScoreboard().getTeam("wins_breakers");
			Team placers = p.getScoreboard().getTeam("wins_placers");
			if (breakers == null || placers == null) {
				continue;
			}
			breakers.prefix(text("\uE109 ").decoration(TextDecoration.BOLD, true));
			breakers.suffix(breaker_text);
			placers.suffix(placer_text);
		}
	}

	private static String render_win_display(int amt) {
		String s = "";
		for (int i = 1; i <= GameController.SWITCH_ROUND; i++) {
			if (i <= amt) {
				s += "\uE106";
			} else {
				s += "\uE105";
			}
		}
		if (GameController.SWITCH_ROUND + 1 == amt) {
			s += "\uE108";
		} else {
			s += "\uE107";
		}

		s += "  (" + amt + ")";
		return s;
	}

	public static void set_player_money(String player, int money) {
		Player p = Bukkit.getPlayer(player);
		FloodgateApi floodgateapi = FloodgateApi.getInstance();
		if (p == null) {
			return;
		}
		Scoreboard sb = p.getScoreboard();
		Team money_count = sb.getTeam("money_count");
		if (money_count == null) {
			return;
		}

		money_count.suffix(text(money).color(TextColor.color(0x0ab1c4)).append(text("\uE104")));
	}

}

class BedrockScoreboard {
	public BedrockScoreboard(Player p, gg.litestrike.game.Team t, Teams teams, int game_id) {
		Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();

		Component title = text("LITESTRIKE").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)
				.append(text(" \uE100").color(NamedTextColor.WHITE));
		Objective obj = sb.registerNewObjective("main", Criteria.DUMMY, title);
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);

		obj.getScore("11").setScore(11);
		obj.getScore("11").customName(text("     "));

		if (t == gg.litestrike.game.Team.Breaker) {
			obj.getScore("10").customName(
					Component.translatable("crystalized.game.generic.team").append(text(": ")).append(Litestrike.BREAKER_TEXT));
		} else {
			obj.getScore("10").customName(
					Component.translatable("crystalized.game.generic.team").append(text(": ")).append(Litestrike.PLACER_TEXT));
		}
		obj.getScore("10").setScore(10);

		obj.getScore("9").setScore(9);
		obj.getScore("9").customName(Component.translatable("crystalized.game.generic.money").append(text(": ")));

		obj.getScore("8").setScore(8);
		obj.getScore("8").customName(text("    "));

		obj.getScore("7").setScore(7);
		obj.getScore("7")
				.customName(Component.translatable("crystalized.game.litestrike.objective").color(TextColor.color(0xe64cce)));

		obj.getScore("6").setScore(6);
		obj.getScore("6").customName(text("\uE105\uE105\uE105\uE105\uE107 (0) ")); // placers wins

		obj.getScore("5").setScore(5);
		obj.getScore("5").customName(text("\uE105\uE105\uE105\uE105\uE107 (0)")); // breakers wins

		obj.getScore("4").setScore(4);
		obj.getScore("4").customName(text(" "));

		obj.getScore("3").setScore(3);
		obj.getScore("3").customName(text("Bomb Location: "));

		obj.getScore("2").setScore(2);
		obj.getScore("2").customName(text("loading..."));

		obj.getScore("1").setScore(1);
		obj.getScore("1").customName(text(""));

		obj.getScore("0").setScore(0);
		obj.getScore("0").customName(text("ᴄʀʏꜱᴛᴀʟɪᴢᴇᴅ.ᴄᴄ ").color(TextColor.color(0xc4b50a))
				.append(text("" + game_id).color(NamedTextColor.GRAY)));

		p.setScoreboard(sb);

		// Couldn't get it to work in the individual methods for updating the
		// scoreboard, so BukkitRunnable instead
		new BukkitRunnable() {
			@Override
			public void run() {
				if (Litestrike.getInstance().game_controller == null) {
					cancel();
				}

				List<gg.litestrike.game.Team> wins = Litestrike.getInstance().game_controller.round_results;
				int placer_wins_amt = 0;
				int breaker_wins_amt = 0;
				for (gg.litestrike.game.Team w : wins) {
					if (w == gg.litestrike.game.Team.Placer) {
						placer_wins_amt += 1;
					} else {
						breaker_wins_amt += 1;
					}
				}

				PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);
				obj.getScore("9").customName(Component.translatable("crystalized.game.generic.money")
						.append(text(": "))
						.append(text(pd.getMoney()).color(TextColor.color(0x0ab1c4)))
						.append(text("\uE104")));

				if (t == gg.litestrike.game.Team.Breaker) {
					obj.getScore("6").customName(text("  ").append(text(render_win_display(placer_wins_amt))).append(text(" ")));
					obj.getScore("5").customName(text("\uE109 ").append(text(render_win_display(breaker_wins_amt))));
				} else {
					obj.getScore("6")
							.customName(text("\uE109 ").append(text(render_win_display(placer_wins_amt))).append(text(" ")));
					obj.getScore("5").customName(text("  ").append(text(render_win_display(breaker_wins_amt))));
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 0, 20);
	}

	private static String render_win_display(int amt) {
		String s = "";
		for (int i = 1; i <= GameController.SWITCH_ROUND; i++) {
			if (i <= amt) {
				s += "\uE106";
			} else {
				s += "\uE105";
			}
		}
		if (GameController.SWITCH_ROUND + 1 == amt) {
			s += "\uE108";
		} else {
			s += "\uE107";
		}

		s += "  (" + amt + ")";
		return s;
	}
}

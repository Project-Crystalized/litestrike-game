package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ScoreboardController {
	public static Scoreboard setup_scoreboard(Teams t) {
		Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
    Component title = Component.text(TextDecoration.BOLD + "" + NamedTextColor.WHITE + "LITESTRIKE");

		if (sb.getTeam("placers") == null) {
			Team placers = sb.registerNewTeam("placers");
			placers.color(NamedTextColor.RED);
			placers.setAllowFriendlyFire(false);
			placers.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
		}
		if (sb.getTeam("breakers") == null) {
			Team placers = sb.registerNewTeam("breakers");
			placers.color(NamedTextColor.GREEN);
			placers.setAllowFriendlyFire(false);
			placers.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
		}

    Objective obj = sb.registerNewObjective("main", Criteria.DUMMY, title);
    obj.setDisplaySlot(DisplaySlot.SIDEBAR);

    obj.getScore(NamedTextColor.WHITE + "").setScore(9);
    obj.getScore(NamedTextColor.GRAY + "").setScore(8);
    obj.getScore(NamedTextColor.DARK_GREEN + "").setScore(7);
    obj.getScore(NamedTextColor.DARK_AQUA + "").setScore(6);
    obj.getScore(NamedTextColor.DARK_GRAY + "").setScore(5);
    obj.getScore(NamedTextColor.AQUA + "Stay with your team!").setScore(4);
    obj.getScore(NamedTextColor.GREEN + "").setScore(3);
    obj.getScore(NamedTextColor.BLUE + "").setScore(2);
    obj.getScore(NamedTextColor.RED + "").setScore(1);
    obj.getScore(NamedTextColor.BLACK + "").setScore(0);

		Team ls_team = sb.registerNewTeam("ls_team");
		String ls_team_key = NamedTextColor.GRAY + "";
		ls_team.addEntry(ls_team_key);
		ls_team.prefix(Component.text("ᴛᴇᴀᴍ: "));
		ls_team.suffix(Component.text("error"));
		obj.getScore(ls_team_key).setScore(8);

		Team money_count = sb.registerNewTeam("money_count");
		String money_count_key = NamedTextColor.DARK_GREEN + "";
		money_count.addEntry(money_count_key);
		money_count.prefix(Component.text("ᴍᴏɴᴇʏ: "));
		money_count.suffix(Component.text("error"));
		obj.getScore(money_count_key).setScore(7);

		Team wins_placers = sb.registerNewTeam("wins_placers");
		String wins_placers_key = NamedTextColor.GREEN + "";
		wins_placers.addEntry(wins_placers_key);
		wins_placers.prefix(Component.text("error1"));
		wins_placers.suffix(Component.text("error2"));
		obj.getScore(wins_placers_key).setScore(3);

		Team wins_breakers = sb.registerNewTeam("wins_breakers");
		String wins_breakers_key = NamedTextColor.BLUE + "";
		wins_breakers.addEntry(wins_breakers_key);
		wins_breakers.prefix(Component.text("error1"));
		wins_breakers.suffix(Component.text("error2"));
		obj.getScore(wins_breakers_key).setScore(2);

		Team footline = sb.registerNewTeam("footline");
		String footline_key = NamedTextColor.BLACK + "";
		footline.addEntry(footline_key);
		footline.prefix(Component.text("ᴄʀʏꜱᴛᴀʟɪᴢᴇᴅ.ᴄᴄ "));
		footline.suffix(Component.text("error2"));
		obj.getScore(footline_key).setScore(0);

		Team placers = sb.getTeam("placers");
		Team breakers = sb.getTeam("breakers");
		for (Player p : t.get_placers()) {
			p.setScoreboard(sb);
			placers.addPlayer(p);
		}
		for (Player p : t.get_breakers()) {
			p.setScoreboard(sb);
			breakers.addPlayer(p);
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (Litestrike.getInstance().game_controller == null) {
					cancel();
					return;
				}
				for (Player p : Litestrike.getInstance().game_controller.teams.get_breakers()) {
					ls_team.suffix(Litestrike.BREAKER_TEXT);
				}
				for (Player p : Litestrike.getInstance().game_controller.teams.get_placers()) {
					ls_team.suffix(Litestrike.PLACER_TEXT);
				}
				for (Player p : Bukkit.getOnlinePlayers()) {
					
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 5, (20 * 3));

		return sb;
	}

	private static void update_scoreboard(Player p) {
		Scoreboard s = p.getScoreboard();


	}
}

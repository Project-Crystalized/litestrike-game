package gg.litestrike.game;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextColor;

import static net.kyori.adventure.text.Component.text;

public class QueueScoreboard {
	private Scoreboard sb;

	public QueueScoreboard() {
		sb = Bukkit.getScoreboardManager().getNewScoreboard();

		Component title = text("LITESTRIKE").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
		Objective obj = sb.registerNewObjective("main", Criteria.DUMMY, title);

		obj.setDisplaySlot(DisplaySlot.SIDEBAR);

		obj.getScore("7").setScore(7);
		obj.getScore("7").customName(text("   "));

		obj.getScore("6").setScore(6);
		obj.getScore("6").customName(text("Waiting for players"));

		obj.getScore("5").setScore(5);
		obj.getScore("5").customName(text("  "));

		obj.getScore("4").setScore(4);
		obj.getScore("4").customName(text(" "));

		obj.getScore("3").setScore(3);
		obj.getScore("3").customName(text("You are Playing on the Map:"));

		obj.getScore("2").setScore(2);
		obj.getScore("2")
				.customName(text("" + Litestrike.getInstance().mapdata.map_name).color(NamedTextColor.DARK_PURPLE));

		obj.getScore("1").setScore(1);
		obj.getScore("1").customName(text(""));

		obj.getScore("0").setScore(0);
		obj.getScore("0").customName(text("ᴄʀʏꜱᴛᴀʟɪᴢᴇᴅ.ᴄᴄ ").color(TextColor.color(0xc4b50a)));

		obj.getScore("-1").setScore(-1);
		obj.getScore("-1").customName(MiniMessage.miniMessage().deserialize("<gradient:dark_red:red>Candy's Practise Server</gradient>"));

		Team player_count = sb.registerNewTeam("player_count");
		player_count.addEntry("5");
		player_count.prefix(text("0"));
		player_count.suffix(text("/" + Litestrike.PLAYERS_TO_START));
		obj.getScore("5").setScore(5);

	}

	public void show_queue_scoreboard(Player p) {
		p.setScoreboard(sb);
		update_player_count();
	}

	public void update_player_count() {
		sb.getTeam("player_count").prefix(text("" + QueueSystem.people_in_que()));
	}
}

package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import gg.litestrike.game.GameController.RoundState;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

public class BossBarDisplay {
	BossBar bb;
	BossBar background;
	BossBar background_br; // exists because bedrock is an inconsistent piece of shit and the bossbar isn't
													// aligned properly, so I make another unicode thats offsetted to work - Callum

	public BossBarDisplay() {
		bb = BossBar.bossBar(Component.text(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
		background = BossBar.bossBar(Component.text(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
		background_br = BossBar.bossBar(Component.text(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (Litestrike.getInstance().game_controller == null) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.hideBossBar(background_br);
						p.hideBossBar(background);
					}
					Bukkit.getServer().hideBossBar(bb);
					return;
				}

				background_br.name(Component.text("\uE401"));
				background.name(Component.text("\uE400"));
				bb.name(Component.text(renderBossBar()));
			}
		}.runTaskTimer(Litestrike.getInstance(), 5, 1);
	}

	public void showBossBar() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId())) {
				p.showBossBar(background_br);
				p.hideBossBar(background);
			} else {
				p.showBossBar(background);
				p.hideBossBar(background_br);
			}
		}
		Bukkit.getServer().showBossBar(bb);
	}

	private String renderBossBar() {
		GameController gc = Litestrike.getInstance().game_controller;
		int player_tiles = Math.max(gc.teams.get_placers().size(), gc.teams.get_placers().size());

		// String bar = "\uE200"; // green start tile
		String bar = "";
		for (String name : gc.teams.get_initial_breakers()) {
			Player p = Bukkit.getPlayer(name);
			if (p == null) {
				bar += "\uE14C";
			} else if (p.getGameMode() == GameMode.SURVIVAL) {
				bar += "\uE14A";
			} else {
				bar += "\uE14B";
			}
		}
		int filler_tiles = player_tiles - gc.teams.get_initial_breakers().size();
		for (int i = 0; i < filler_tiles; i++) {
			bar += " ".repeat(2);
		}
		// bar += "\uE201"; // green end tile

		List<Team> wins = Litestrike.getInstance().game_controller.round_results;
		int placer_wins_amt = 0;
		int breaker_wins_amt = 0;
		for (gg.litestrike.game.Team w : wins) {
			if (w == gg.litestrike.game.Team.Placer) {
				placer_wins_amt += 1;
			} else {
				breaker_wins_amt += 1;
			}
		}

		bar += " ".repeat(4) + getGreenScore(breaker_wins_amt);
		bar += " ".repeat(3) + "." + get_current_timer_formated() + "." + " ".repeat(3);
		bar += getRedScore(placer_wins_amt) + " ".repeat(4);

		// bar += "\uE206"; // red start tile
		for (String name : gc.teams.get_initial_placers()) {
			Player p = Bukkit.getPlayer(name);
			if (p == null) {
				bar += "\uE14F";
			} else if (p.getGameMode() == GameMode.SURVIVAL) {
				bar += "\uE14D";
			} else {
				bar += "\uE14E";
			}
		}
		filler_tiles = player_tiles - gc.teams.get_initial_placers().size();
		for (int i = 0; i < filler_tiles; i++) {
			bar += " ".repeat(2);
		}
		// bar += "\uE207"; // red end tile

		return bar;
	}

	private String getGreenScore(int score) {
		switch (score) {
			case 5 -> {
				return "\uE150";
			}
			case 4 -> {
				return "\uE151";
			}
			case 3 -> {
				return "\uE152";
			}
			case 2 -> {
				return "\uE153";
			}
			case 1 -> {
				return "\uE154";
			}
			case 0 -> {
				return "\uE155";
			}
		}
		return "?";
	}

	private String getRedScore(int score) {
		switch (score) {
			case 5 -> {
				return "\uE15B";
			}
			case 4 -> {
				return "\uE15A";
			}
			case 3 -> {
				return "\uE159";
			}
			case 2 -> {
				return "\uE158";
			}
			case 1 -> {
				return "\uE157";
			}
			case 0 -> {
				return "\uE156";
			}
		}
		return "?";
	}

	private String get_current_timer_formated() {
		// int timer = get_current_timer() / 20;
		// String output = "";
		// char[] timerArray = ("" + timer).toCharArray();
		// // Keeping the length exactly 3 characters if possible
		// switch (timerArray.length) {
		// case 2 -> {
		// output = "0";
		// }
		// case 1 -> {
		// output = "00";
		// }
		// }
		// output += "" + timer;
		// return output;

		// Can mess up the positioning of the newer bossbar, commented out - Callum
		int timer = get_current_timer();
		if (timer >= (20 * 10)) {
			return "" + ((timer / 20));
		} else {
			return String.format("%.2f", (timer / 20.0));
		}
	}

	private int get_current_timer() {
		GameController gc = Litestrike.getInstance().game_controller;

		if (gc.bomb instanceof PlacedBomb) {
			PlacedBomb pb = (PlacedBomb) gc.bomb;
			return Bomb.DETONATION_TIME - pb.timer;
		}

		switch (gc.round_state) {
			case RoundState.PreRound:
				return GameController.PRE_ROUND_TIME - gc.phase_timer;
			case RoundState.Running:
				return GameController.RUNNING_TIME - gc.phase_timer;
			case RoundState.PostRound:
				return GameController.POST_ROUND_TIME - gc.phase_timer;
			case RoundState.GameFinished:
				return GameController.FINISH_TIME - gc.phase_timer;
		}
		throw new RuntimeException("a error occurred rendering tablist");
	}
}

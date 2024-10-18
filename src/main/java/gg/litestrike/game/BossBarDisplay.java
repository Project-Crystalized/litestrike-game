package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public class BossBarDisplay {

	public BossBarDisplay() {
		BossBar bb = BossBar.bossBar(Component.text(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
		Bukkit.getServer().showBossBar(bb);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (Litestrike.getInstance().game_controller == null) {
					Bukkit.getServer().hideBossBar(bb);
					cancel();
				}

				bb.name(Component.text(renderBossBar()));
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 4);
	}

	public String renderBossBar() {
		GameController gc = Litestrike.getInstance().game_controller;
		int player_tiles = Math.max(gc.teams.get_placers().size(), gc.teams.get_placers().size());

		String bar = "\uE200"; // green start tile
		for (String name : gc.teams.get_initial_breakers()) {
			Player p = Bukkit.getPlayer(name);
			if (p == null) {
				bar += "\uE205";
			} else if (p.getGameMode() == GameMode.SURVIVAL) {
				bar += "\uE203";
			} else {
				bar += "\uE204";
			}
		}
		int filler_tiles = player_tiles - gc.teams.get_initial_breakers().size();
		for (int i = 0; i < filler_tiles; i++) {
			bar += "\uE202"; // blank green tile
		}
		bar += "\uE201"; // green end tile

		bar += "   " + get_current_timer() / 20 + "   ";

		bar += "\uE206"; // red start tile
		for (String name : gc.teams.get_initial_placers()) {
			Player p = Bukkit.getPlayer(name);
			if (p == null) {
				bar += "\uE20B";
			} else if (p.getGameMode() == GameMode.SURVIVAL) {
				bar += "\uE209";
			} else {
				bar += "\uE20A";
			}
		}
		filler_tiles = player_tiles - gc.teams.get_initial_placers().size();
		for (int i = 0; i < filler_tiles; i++) {
			bar += "\uE208"; // blank red tile
		}
		bar += "\uE207"; // red end tile

		return bar;
	}

	private int get_current_timer() {
		GameController gc = Litestrike.getInstance().game_controller;

		if (gc.bomb.bomb_loc instanceof PlacedBomb) {
			return Bomb.DETONATION_TIME - gc.bomb.timer;
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
		throw new RuntimeException();
	}
}

package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

// TODO add a sanity checker class

enum Team {
	Placer,
	Breaker,
}

public final class Litestrike extends JavaPlugin {

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();

	public GameController game_controller;

	// this is set by the /force_start command
	public boolean is_force_starting = false;

	// constants for Placer and breaker text
	public static final Component PLACER_TEXT = Component.text("Placer").color(TextColor.color(0xe31724)).decoration(TextDecoration.BOLD, true);
	public static final Component BREAKER_TEXT = Component.text("Breaker").color(TextColor.color(0x0f9415)).decoration(TextDecoration.BOLD, true);


	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new MapData(), this);
		this.getCommand("mapdata").setExecutor(new DebugCommands());
		this.getCommand("force_start").setExecutor(new DebugCommands());
		this.getCommand("player_info").setExecutor(new DebugCommands());

		new BukkitRunnable() {
			int countdown = 11;
			@Override
			public void run() {

				// if there is already a game going on, do nothing
				if (game_controller != null) {
					return;
				}

				// if more then 6 players online, count down, else reset countdown
				if (Bukkit.getOnlinePlayers().size() >= 6 || is_force_starting) {
					countdown -= 1;
					count_down_animation(countdown);
				} else {
					countdown = 11;
					return;
				}

				// if countdown reaches zero, we start the game
				if (countdown == 0) {
					countdown = 11;
					game_controller = new GameController();
					is_force_starting = false;
					return;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 20);
	}

	@Override
	public void onDisable() {
	}

	public static Litestrike getInstance() {
		return getPlugin(Litestrike.class);
	}

	// plays every second while the game is counting down to start
	private void count_down_animation(int i) {
		switch (i) {
			case 10:
			case 5: {
				Bukkit.getServer().sendMessage(Component.text("The game will start in " + i + " seconds!"));
				break;
			}
			case 3:
			case 2:
			case 1: {
				Bukkit.getServer().sendMessage(Component.text("The game will start in " + i + " seconds!"));
				Bukkit.getServer().playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 1));
				Bukkit.getServer().showTitle(Title.title(Component.text(i), Component.text("")));
			}
		}
	};
}

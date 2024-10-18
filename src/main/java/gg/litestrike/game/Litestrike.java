package gg.litestrike.game;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

enum Team {
	Placer,
	Breaker,
}

public final class Litestrike extends JavaPlugin implements Listener {

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();
	public GameController game_controller;

	// this is set by the /force_start command
	public boolean is_force_starting = false;

	// constants for Placer and breaker text
	public static final Component PLACER_TEXT = Component.text("Placer").color(TextColor.color(0xe31724))
			.decoration(TextDecoration.BOLD, true);
	public static final Component BREAKER_TEXT = Component.text("Breaker").color(TextColor.color(0x0f9415))
			.decoration(TextDecoration.BOLD, true);

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(this.mapdata, this);
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getServer().getPluginManager().registerEvents(new Shop(null), this);
		DebugCommands dc = new DebugCommands();

		Bukkit.getServer().getPluginManager().registerEvents(new BombListener(), this);

		this.getCommand("mapdata").setExecutor(dc);
		this.getCommand("force_start").setExecutor(dc);
		this.getCommand("player_info").setExecutor(dc);
		this.getCommand("bomb_info").setExecutor(dc);

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
				if (countdown == 0 || is_force_starting) {
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

	// setup stuff like gamerules
	@EventHandler
	public void onWorldInit(WorldInitEvent e) {
		World w = e.getWorld();

		// disable natural regen
		w.setGameRule(GameRule.NATURAL_REGENERATION, false);

		// /gamerule spawnChunkRadius needs to be set to 0 before world load,
		// otherwise the border detection can fail.
		// so we set it to 0 and disable the plugin if it wasnt at 0 already
		/*
		if (w.getGameRuleValue(GameRule.SPAWN_CHUNK_RADIUS) != 0) {
			Bukkit.getLogger().log(Level.SEVERE,
					"LITESTRIKE: The Gamerule SPAWN_CHUNK_RADIUS needs to be set to zero in order for Litestrike to work!");
			Bukkit.getLogger().log(Level.SEVERE,
					"LITESTRIKE: The GameRule SPAWN_CHUNK_RADIUS was set to 0! Please restart the server now to prevent bugs.");
			w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
			Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());
		}
		 */

	}

}

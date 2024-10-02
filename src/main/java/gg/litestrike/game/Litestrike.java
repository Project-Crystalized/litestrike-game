package gg.litestrike.game;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

// TODO add a sanity checker class

enum Team {
	Placer,
	Breaker,
}

public final class Litestrike extends JavaPlugin implements Listener {

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();

	public GameController game_controller;

	// constants for Placer and breaker text
	public static final Component PLACER_TEXT = Component.text("Placer").color(TextColor.color(0xe31724)).decoration(TextDecoration.BOLD, true);
	public static final Component BREAKER_TEXT = Component.text("Breaker").color(TextColor.color(0x0f9415)).decoration(TextDecoration.BOLD, true);


	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new MapData(), this);
		this.getCommand("mapdata").setExecutor(mapdata);

		new BukkitRunnable() {
			int countdown = 11;
			@Override
			public void run() {

				// if there is already a game going on, do nothing
				if (game_controller != null) {
					return;
				}

				// if more then 6 players online, count down, else reset countdown
				if (Bukkit.getOnlinePlayers().size() >= 6) {
					countdown -= 1;
				} else {
					countdown = 11;
					return;
				}

				// if countdown reaches zero, we start the game
				if (countdown == 0) {
					countdown = 11;
					game_controller = new GameController();
					return;
				}

				// while counting down, play sound/chat message/title
				if (countdown < 11) {
					count_down_animation(countdown);
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

	@EventHandler
	public void onWorldLoad(WorldLoadEvent e) {
		Bukkit.getLogger().info("DEBUGG loading a world");

		World w = e.getWorld();

		if (w.getGameRuleValue(GameRule.SPAWN_CHUNK_RADIUS) != 0) {
			Bukkit.getLogger().log(Level.SEVERE, "The Gamerule SPAWN_CHUNK_RADIUS needs to be set to zero in order for Litestrike to work!");
			Bukkit.getLogger().log(Level.SEVERE, "The GameRule SPAWN_CHUNK_RADIUS was set to 0! Please restart the server now to prevent bugs.");
			w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
		}
	}

	// plays every second while the game is counting down to start
	private void count_down_animation(int i) {
		// TODO chat message, sound, title
	};
}

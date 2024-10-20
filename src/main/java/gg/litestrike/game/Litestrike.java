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
	public static final Component PLACER_TEXT = Component.text("Placer").color(Teams.PLACER_RED)
			.decoration(TextDecoration.BOLD, true);
	public static final Component BREAKER_TEXT = Component.text("Breaker").color(Teams.BREAKER_GREEN)
			.decoration(TextDecoration.BOLD, true);

	public static TextColor YELLOW = TextColor.color(0xfbea85);

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(this.mapdata, this);
		this.getServer().getPluginManager().registerEvents(this, this);
		DebugCommands dc = new DebugCommands();

		Bukkit.getServer().getPluginManager().registerEvents(new BombListener(), this);

		this.getCommand("mapdata").setExecutor(dc);
		this.getCommand("force_start").setExecutor(dc);
		this.getCommand("player_info").setExecutor(dc);
		this.getCommand("soundd").setExecutor(dc);

		new BukkitRunnable() {
			int countdown = 11;

			@Override
			public void run() {

				// if there is already a game going on, do nothing
				if (game_controller != null) {
					return;
				}

				// if more then 6 players online, count down, else reset countdown
				if (Bukkit.getOnlinePlayers().size() >= 100 || is_force_starting) {
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
			case 3:
			case 2:
			case 1:
				SoundEffects.countdown_beep();
				Bukkit.getServer().showTitle(Title.title(Component.text(i), Component.text("")));
			case 10:
			case 5:
				Bukkit.getServer().sendMessage(Component.text("ᴛʜᴇ ɢᴀᴍᴇ ᴡɪʟʟ ꜱᴛᴀʀᴛ ɪɴ  " + i + " ꜱᴇᴄᴏɴᴅꜱ!")
						.color(Litestrike.YELLOW));
		}
	};

	// setup stuff like gamerules
	@EventHandler
	public void onWorldInit(WorldInitEvent e) {
		World w = e.getWorld();

		w.setGameRule(GameRule.NATURAL_REGENERATION, false);
		w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
		w.setGameRule(GameRule.DO_INSOMNIA, false);
		w.setGameRule(GameRule.MOB_GRIEFING, false);
		w.setGameRule(GameRule.DO_FIRE_TICK, false);
		w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

		// /gamerule spawnChunkRadius needs to be set to 0 before world load,
		// otherwise the border detection can fail.
		// so we set it to 0 and disable the plugin if it wasnt at 0 already
		if (w.getGameRuleValue(GameRule.SPAWN_CHUNK_RADIUS) != 0) {
			Bukkit.getLogger().log(Level.SEVERE,
					"LITESTRIKE: The Gamerule SPAWN_CHUNK_RADIUS needs to be set to zero in order for Litestrike to work!");
			Bukkit.getLogger().log(Level.SEVERE,
					"LITESTRIKE: The GameRule SPAWN_CHUNK_RADIUS was set to 0! Please restart the server now to prevent bugs.");
			w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
			Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());
		}

	}
}

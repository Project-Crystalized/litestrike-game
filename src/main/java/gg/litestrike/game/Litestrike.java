package gg.litestrike.game;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

// TODO add a sanity checker class

public final class Litestrike extends JavaPlugin implements Listener {

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();

	public GameController game_controller;

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new MapData(), this);
		this.getCommand("mapdata").setExecutor(mapdata);

		// TODO bukkit runnable taht creates game controller
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
}

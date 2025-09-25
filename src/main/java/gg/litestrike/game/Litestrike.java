package gg.litestrike.game;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.World;

import org.bukkit.plugin.java.JavaPlugin;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.logging.Level;

enum Team {
	Placer,
	Breaker,
}

public final class Litestrike extends JavaPlugin {
	public static final Path socketPath = Path
			.of(System.getProperty("user.home") + "/sockets")
			.resolve("crystalized_lobby.socket");

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();
	public GameController game_controller;

	public BossBarDisplay bbd;

	public ProtocolManager protocolManager;

	public PartyManager party_manager = new PartyManager();

	public ManualTeams manual_teams = new ManualTeams();

	// player amount required to autostart
	public static final int PLAYERS_TO_START = 99;
	public static final int PLAYER_CAP = 100;

	// constants for Placer and breaker text
	public static final Component PLACER_TEXT = Component.translatable("crystalized.game.litestrike.placers")
			.color(Teams.PLACER_RED)
			.decoration(TextDecoration.BOLD, true);
	public static final Component BREAKER_TEXT = Component.translatable("crystalized.game.litestrike.breakers")
			.color(Teams.BREAKER_GREEN)
			.decoration(TextDecoration.BOLD, true);

	public static final TextColor YELLOW = TextColor.color(0xfbea85);

	@Override
	public void onEnable() {
		protocolManager = ProtocolLibrary.getProtocolManager();

		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new DeathHandler(), this);
		this.getServer().getPluginManager().registerEvents(this.mapdata, this);
		this.getServer().getPluginManager().registerEvents(new ShopListener(), this);
		this.getServer().getPluginManager().registerEvents(new BombListener(), this);

		// registers all listeners for map specific features
		if (mapdata.map_features != null) {
			mapdata.map_features.register_listeners(this);
		}

		DebugCommands dc = new DebugCommands();
		this.getCommand("mapdata").setExecutor(dc);
		this.getCommand("force_start").setExecutor(dc);
		this.getCommand("player_info").setExecutor(dc);
		this.getCommand("soundd").setExecutor(dc);
		this.getCommand("debug_log").setExecutor(dc);

		saveResource("config.yml", false);
		int configVersion;
		if (getConfig().getInt("version") != 1) {
			configVersion = getConfig().getInt("version");
			getLogger().log(Level.SEVERE,
					"Invalid Version, Please update your litestrike/config.yml file. Expecting 1 but found " + configVersion
							+ ". You may experience fatal issues.");
		}

		// register the manual_teams command
		// TODO deprecated in favor of a config file
		// this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
		// event -> event.registrar().register("manual_teams", manual_teams));

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:litestrike");
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", party_manager);
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", new QueueSystem());
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:essentials");

		bbd = new BossBarDisplay();

		LsDatabase.setup_databases();

		World w = Bukkit.getWorld("world");

		w.setGameRule(GameRule.NATURAL_REGENERATION, false);
		w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
		w.setGameRule(GameRule.DO_INSOMNIA, false);
		w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		w.setGameRule(GameRule.MOB_GRIEFING, false);
		w.setGameRule(GameRule.DO_FIRE_TICK, false);
		w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
		w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
		w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
		w.setGameRule(GameRule.LOCATOR_BAR, false);

		for (Chunk c : w.getLoadedChunks()) {
			mapdata.check_chunk(c);
		}

		protocolManager.addPacketListener(ProtocolLibLib.change_bomb_carrier_armor_color());
		protocolManager.addPacketListener(ProtocolLibLib.make_allys_glow());
	}

	@Override
	public void onDisable() {
	}

	public static Litestrike getInstance() {
		return getPlugin(Litestrike.class);
	}

	public void sendPluginMessage(@NonNull String channel, @NonNull String message) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF(message);
		Bukkit.getServer().sendPluginMessage(this, channel, out.toByteArray());
	}

}

package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

enum Team {
	Placer,
	Breaker,
}

public final class Litestrike extends JavaPlugin {

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();
	public GameController game_controller;

	public BossBarDisplay bbd;

	// this is set by the /force_start command
	public boolean is_force_starting = false;

	public QueScoreboard qsb;

	public ProtocolManager protocolManager;

	// player amount required to autostart
	public static final int PLAYERS_TO_START = 20;

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

		qsb = new QueScoreboard();

		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(this.mapdata, this);
		this.getServer().getPluginManager().registerEvents(new ShopListener(), this);
		this.getServer().getPluginManager().registerEvents(new BombListener(), this);

		if (mapdata.levitation_pads) {
			this.getServer().getPluginManager().registerEvents(new LeviPadListener(), this);
		}

		DebugCommands dc = new DebugCommands();
		this.getCommand("mapdata").setExecutor(dc);
		this.getCommand("force_start").setExecutor(dc);
		this.getCommand("player_info").setExecutor(dc);
		this.getCommand("soundd").setExecutor(dc);

		bbd = new BossBarDisplay();

		new BukkitRunnable() {
			int countdown = 11;

			@Override
			public void run() {

				// if there is already a game going on, do nothing
				if (game_controller != null) {
					return;
				}

				qsb.update_player_count();

				// if more then 6 players online, count down, else reset countdown
				if (Bukkit.getOnlinePlayers().size() >= PLAYERS_TO_START || is_force_starting) {
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
					Bukkit.getLogger().info("A GAME is starting!");
					return;
				}
			}
		}.runTaskTimer(this, 1, 20);

		LsDatabase.setup_databases();

		World w = Bukkit.getWorld("world");

		w.setGameRule(GameRule.NATURAL_REGENERATION, false);
		w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
		w.setGameRule(GameRule.DO_INSOMNIA, false);
		w.setGameRule(GameRule.MOB_GRIEFING, false);
		w.setGameRule(GameRule.DO_FIRE_TICK, false);
		w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
		w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
		w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);

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
				Audience.audience(Bukkit.getOnlinePlayers())
						.sendMessage(
								(Component.translatable("crystalized.game.litestrike.start1")
										.append(Component.text("" + i))
										.append(Component.translatable("crystalized.game.litestrike.start2")))
										.color(Litestrike.YELLOW));
		}
	};
}

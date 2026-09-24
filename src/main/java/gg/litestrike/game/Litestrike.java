package gg.litestrike.game;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

enum Team {
	Placer,
	Breaker,
}

public final class Litestrike extends JavaPlugin implements PluginMessageListener {

	// holds all the config about a map, like the spawn/border coordinates
	public final MapData mapdata = new MapData();
	public GameController game_controller;

	public BossBarDisplay bbd;

	public PartyManager party_manager = new PartyManager();

	public ManualTeams manual_teams;

	public GameConfig gameConfig;

	// constants for Placer and breaker text
	public static final Component PLACER_TEXT = Component.translatable("crystalized.game.litestrike.placers")
			.color(Teams.PLACER_RED)
			.decoration(TextDecoration.BOLD, true);
	public static final Component BREAKER_TEXT = Component.translatable("crystalized.game.litestrike.breakers")
			.color(Teams.BREAKER_GREEN)
			.decoration(TextDecoration.BOLD, true);

	public static final TextColor YELLOW = TextColor.color(0xfbea85);

	@Override
	public void onLoad(){
		PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
		PacketEvents.getAPI().getSettings().reEncodeByDefault(false).checkForUpdates(true).bStats(false);
		PacketEvents.getAPI().load();
		EventManager events = PacketEvents.getAPI().getEventManager();
		events.registerListener(new ProtocolLibLib(), PacketListenerPriority.NORMAL);
	}

	@Override
	public void onEnable() {
		PacketEvents.getAPI().init();
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new DeathHandler(), this);
		this.getServer().getPluginManager().registerEvents(this.mapdata, this);
		this.getServer().getPluginManager().registerEvents(new ShopListener(), this);
		this.getServer().getPluginManager().registerEvents(new BombListener(), this);
		this.getServer().getPluginManager().registerEvents(new Communicator(), this);
		this.getServer().getPluginManager().registerEvents(new GameCompass(), this);

		// registers all listeners for map specific features
		if (mapdata.map_features != null) {
			mapdata.map_features.register_listeners(this);
		}

		saveResource("config.yml", false);
		saveResource("items.json", false);
		LSItem.shopItems.size();
		int configVersion;
		if (getConfig().getInt("version") != 1) {
			configVersion = getConfig().getInt("version");
			getLogger().log(Level.SEVERE,
					"Invalid Version, Please update your litestrike/config.yml file. Expecting 1 but found " + configVersion
							+ ". You may experience fatal issues.");
		}

		gameConfig = new GameConfig(getConfig());
		manual_teams = new ManualTeams(gameConfig);

		GameConfigCommand gcc = new GameConfigCommand(gameConfig);
		DebugCommands dc = new DebugCommands();
		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			event.registrar().register(manual_teams.build().build(), "to set up teams manually", List.of());
			event.registrar().register(gcc.build().build(), "View and modify game settings", List.of());
			event.registrar().register(dc.build(manual_teams.build(), gcc.build()).build(), "Litestrike debug commands",
					List.of());
		});

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:litestrike");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:litestrike", this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", party_manager);
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", new QueueSystem());
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:essentials");

		bbd = new BossBarDisplay();

		LsDatabase.setup_databases();

		World w = Bukkit.getWorld("world");

		w.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
		w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
		w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
		w.setGameRule(GameRules.SPAWN_MOBS, false);
		w.setGameRule(GameRules.MOB_GRIEFING, false);
		w.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
		w.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
		w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
		w.setGameRule(GameRules.LOCATOR_BAR, false);

		for (Chunk c : w.getLoadedChunks()) {
			mapdata.check_chunk(c);
		}

		teleportBackUp();
	}

	@Override
	public void onDisable() {
		PacketEvents.getAPI().terminate();
	}

	public static Litestrike getInstance() {
		return getPlugin(Litestrike.class);
	}

	public void teleportBackUp() {
		new BukkitRunnable() {
			@Override
			public void run() {
				World w = Bukkit.getWorld("world");
				try {
					if (game_controller != null) {
						return;
					}
					for (Player p : Bukkit.getOnlinePlayers()) {
						if (p.getY() < -64) {
							p.teleport(mapdata.get_queue_spawn(w));
						}
					}
				} catch (Exception e) {
					Bukkit.getLogger().severe("stopped teleportBackUp method");
					cancel();
				}
			}
		}.runTaskTimer(this, 1, 20);
	}

	public void sendPluginMessage(@NonNull String channel, @NonNull String message) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF(message);
		Bukkit.getServer().sendPluginMessage(this, channel, out.toByteArray());
	}

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
		if (!channel.equals("crystalized:main")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String message1 = in.readUTF();
		if (message1.contains("ranked_on")) {
			this.gameConfig.ranked = true;
			Bukkit.getLogger().info("set ranked on");
		} else if (message1.contains("ranked_off")) {
			this.gameConfig.ranked = false;
			Bukkit.getLogger().info("set ranked off");
		}
	}
}

class GameCompass implements Listener {
	@EventHandler
	public void onCompassClick(PlayerInteractEvent e){
		if(e.getItem() == null || e.getItem().getType() != Material.COMPASS){
			return;
		}

		if(e.getPlayer().getGameMode() != GameMode.ADVENTURE){
			return;
		}

		int teamSize = 0;
		ArrayList<List<Player>> allPlayerSortedInTeams = new ArrayList<>();
		GameController gc = Litestrike.getInstance().game_controller;
		allPlayerSortedInTeams.add(gc.teams.get_breakers());
		allPlayerSortedInTeams.add(gc.teams.get_placers());

		teamSize = Math.max(allPlayerSortedInTeams.getFirst().size(), allPlayerSortedInTeams.getLast().size());

		int inventorySize = allPlayerSortedInTeams.size() * teamSize * 2;
		int[] possibleSizes = new int[]{9, 18, 27, 36, 45, 54};
		for(int i : possibleSizes){
			if(inventorySize % 9 == 0) break;
			if(inventorySize <= i){
				inventorySize = i;
				break;
			}
		}
		Inventory inv = Bukkit.createInventory(null, inventorySize, Component.text(""));
		int slot = 0;
		for(List<Player> team : allPlayerSortedInTeams){
			for(Player p : team){
				inv.setItem(slot, buildItem(p.getName()));
				slot++;
			}
			if(slot % 9 != 0) slot++;
		}
		e.getPlayer().openInventory(inv);
	}

	@EventHandler
	public void onHeadClick(InventoryClickEvent e){
		if(e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PLAYER_HEAD){
			return;
		}
		if(e.getWhoClicked().getGameMode() != GameMode.ADVENTURE){
			return;
		}
		e.setCancelled(true);
		ItemStack item = e.getCurrentItem();
		SkullMeta skull = (SkullMeta) item.getItemMeta();
		PlayerProfile profile = skull.getPlayerProfile();
		if(profile == null || profile.getId() == null) return;
		OfflinePlayer player = Bukkit.getOfflinePlayer(profile.getId());
		if(player.getPlayer() == null) return;
		e.getWhoClicked().teleport(player.getPlayer());
	}

	public static ItemStack buildItem(String name){
		OfflinePlayer player = Bukkit.getOfflinePlayer(name);
		PlayerProfile profile = player.getPlayerProfile();
		ItemStack play = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta skull = (SkullMeta) play.getItemMeta();
		skull.setPlayerProfile(profile);
		play.setItemMeta(skull);

		ItemMeta meta = play.getItemMeta();
		Component displayName = Component.text("\uE103").color(WHITE).decoration(ITALIC, false).append(Component.text(name).color(GRAY).decoration(ITALIC, true));
		if(player.getPlayer() != null && player.getPlayer().getGameMode() != GameMode.ADVENTURE) displayName = player.getPlayer().displayName().decoration(ITALIC, false);
		meta.displayName(displayName);
		play.setItemMeta(meta);

		return play;
	}
}
package gg.litestrike.game.mapfeatures;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gg.litestrike.game.Litestrike;

public class MapFeatures implements Listener {
	private static Set<Player> fall_protected_players = new HashSet<>();

	protected static Material launch_pad_block;
	protected static Material levi_pad_block;
	protected static Material jump_pad_block;
	protected static Material auto_jump_pad_block;

	public boolean can_plant_below = true;
	public boolean can_plant_side = true;

	public BigDoor bigDoor;
	public CargoDoor cargoDoor;

	public MapFeatures(JsonObject json_main) {
		JsonObject json = json_main.getAsJsonObject("map_features");
		JsonElement lp_block = json.get("launch_pad_block");
		if (lp_block != null) {
			launch_pad_block = Material.matchMaterial(lp_block.getAsString());
		}

		JsonElement levi_block = json.get("levi_pad_block");
		if (levi_block == null) {
			levi_block = json.get("levitation_pad_block");
		}
		if (levi_block != null) {
			levi_pad_block = Material.matchMaterial(levi_block.getAsString());
		}

		JsonElement jump_block = json.get("jump_pad_block");
		if (jump_block != null) {
			jump_pad_block = Material.matchMaterial(jump_block.getAsString());
		}

		JsonElement auto_jump_block = json.get("auto_jump_pad_block");
		if (auto_jump_block != null) {
			auto_jump_pad_block = Material.matchMaterial(auto_jump_block.getAsString());
		}

		JsonObject jo_big_door = json.getAsJsonObject("big_door");
		if (jo_big_door != null) {
			bigDoor = new BigDoor(jo_big_door);
		}

		if (json_main.get("map_name").getAsString().contains("Cargo")) {
			Bukkit.getLogger().info("");
			Bukkit.getLogger().info("loaded a map with the Cargo door");
			Bukkit.getLogger().info("");
			cargoDoor = new CargoDoor();
		}

		JsonElement plant_below = json.get("can_plant_below");
		if (plant_below != null) {
			can_plant_below = plant_below.getAsBoolean();
		}

		JsonElement plant_side = json.get("can_plant_side");
		if (plant_side != null) {
			can_plant_side = plant_side.getAsBoolean();
		}

	}

	public String toString() {
		return "\nlaunch_pad_block: " + launch_pad_block +
				"\nlevi_pad_block: " + levi_pad_block +
				"\njump_pad_block: " + jump_pad_block +
				"\nauto_jump_pad_block: " + auto_jump_pad_block;
	}

	public void register_listeners(Litestrike plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		if (launch_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new LaunchPadListener(), plugin);
		}
		if (levi_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new LeviPadListener(), plugin);
		}
		if (jump_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new JumpPadListener(), plugin);
		}
		if (auto_jump_pad_block != null) {
			plugin.getServer().getPluginManager().registerEvents(new AutoJumpPadListener(), plugin);
		}
		if (bigDoor != null) {
			plugin.getServer().getPluginManager().registerEvents(bigDoor, plugin);
		}
		if (cargoDoor != null) {
			plugin.getServer().getPluginManager().registerEvents(cargoDoor, plugin);
		}
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FALL && fall_protected_players.contains(e.getEntity())) {
			e.setCancelled(true);
			fall_protected_players.remove(e.getEntity());
			fall_protected_players.removeIf(pl -> !pl.isConnected());
		}
	}

	// this is called from GameController next_round AND start_round
	public void reset_structures() {
		if (bigDoor != null) {
			bigDoor.regenerate_door();
		}
		if (cargoDoor != null) {
			cargoDoor.close_door();
		}
	}

	public static void fall_protect_player(Player p, int time) {
		fall_protected_players.add(p);
		new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				i++;
				if (i > time || !fall_protected_players.contains(p)) {
					fall_protected_players.remove(p);
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 5, 20);
	}
}

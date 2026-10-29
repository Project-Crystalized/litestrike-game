package gg.litestrike.game.mapfeatures;

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
	private static final FallProtectionTracker fall_protection = new FallProtectionTracker();
	private boolean fall_protection_cleanup_started = false;

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
		start_fall_protection_cleanup(plugin);
	}

	private void start_fall_protection_cleanup(Litestrike plugin) {
		if (fall_protection_cleanup_started) {
			return;
		}
		fall_protection_cleanup_started = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				fall_protection.removeExpired(Bukkit.getCurrentTick());
			}
		}.runTaskTimer(plugin, 20, 20);
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent e) {
		if (e.getCause() != DamageCause.FALL || !(e.getEntity() instanceof Player p)) {
			return;
		}
		if (fall_protection.consume(p.getUniqueId(), Bukkit.getCurrentTick())) {
			e.setCancelled(true);
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
		fall_protection.protect(p.getUniqueId(), Bukkit.getCurrentTick(), time);
	}
}

package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.Block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gg.litestrike.game.mapfeatures.MapFeatures;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

// this will read a file, map_config.json, in the current world directory,
// from this file it will read, for example: the spawn points and map_name.
// this is a singleton
public class MapData implements Listener {
	public double[] placer_spawn;
	public double[] breaker_spawn;
	public double[] queue_spawn;

	public String map_name;

	// toggelable map-specific features
	public MapFeatures map_features;

	// border gets placed 1 block above this block type
	public Material border_marker;
	public Material border_block_type;
	public int border_height = 7;

	public Material bomb_plant_block = Material.TERRACOTTA;

	// can be null if no podium is being used
	public PodiumData podium;

	public boolean ranked = false;

	public Set<int[]> border_blocks = Collections.synchronizedSet(new HashSet<int[]>());

	public void raiseBorder(World w) {
		setBorderBlock(border_block_type, w);
	}

	public void lowerBorder(World w) {
		setBorderBlock(Material.AIR, w);
	}

	private void setBorderBlock(Material m, World w) {
		if (m.isBlock()) {
			for (int[] b : border_blocks) {
				for (int i = 0; i < border_height; i++) { // go until border_height
					Block block = w.getBlockAt(b[0], b[1] + 2 + i, b[2]);
					if (block.isEmpty() || block.getType() == border_block_type || block.getType() == Material.LIGHT) {
						block.setType(m);
					}
				}
			}
		} else {
			Bukkit.getLogger().log(Level.SEVERE, "a Material that isnt a block was used for the border!!");
		}

	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		Chunk c = e.getChunk();
		check_chunk(c);
	}

	public void check_chunk(Chunk c) {
		if (is_search_chunk(c.getX(), c.getZ(), c.getWorld())) {
			ChunkSnapshot cs = c.getChunkSnapshot(true, false, false, false);
			int min = c.getWorld().getMinHeight();

			new BukkitRunnable() {
				@Override
				public void run() {
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							for (int y = min; y < cs.getHighestBlockYAt(x, z); y++) {
								if (cs.getBlockType(x, y, z) == border_marker) {
									Litestrike.getInstance().mapdata.border_blocks
											.add(new int[] { cs.getX() * 16 + x, y, cs.getZ() * 16 + z });
								}
							}
						}
					}
				}
			}.runTaskAsynchronously(Litestrike.getInstance());
		}
	}

	private void load_spawn_coords(JsonObject json) {
		JsonArray p_spawn = json.get("placer_spawn").getAsJsonArray();
		this.placer_spawn = new double[] { p_spawn.get(0).getAsDouble(), p_spawn.get(1).getAsDouble(),
				p_spawn.get(2).getAsDouble() };

		JsonArray b_spawn = json.get("breaker_spawn").getAsJsonArray();
		this.breaker_spawn = new double[] { b_spawn.get(0).getAsDouble(), b_spawn.get(1).getAsDouble(),
				b_spawn.get(2).getAsDouble() };

		JsonArray q_spawn = json.get("queue_spawn").getAsJsonArray();
		this.queue_spawn = new double[] { q_spawn.get(0).getAsDouble(), q_spawn.get(1).getAsDouble(),
				q_spawn.get(2).getAsDouble() };
	}

	private void load_border_values(JsonObject json) {
		String b_mark = json.get("border_marker").getAsString();
		this.border_marker = Material.matchMaterial(b_mark);
		if (!border_marker.isBlock()) {
			throw new RuntimeException("border_marker needs to be a placable block");
		}

		String b_type = json.get("border_block_type").getAsString();
		this.border_block_type = Material.matchMaterial(b_type);
		if (!border_block_type.isBlock()) {
			throw new RuntimeException("border_block_type needs to be a placable block");
		}

		JsonElement border_height = json.get("border_height");
		if (border_height != null) {
			this.border_height = border_height.getAsInt();
		}
	}

	private void parse_config_v2(JsonObject json) {
		load_spawn_coords(json);
		this.map_name = json.get("map_name").getAsString();

		load_border_values(json);

		JsonElement jp = json.get("enable_jump_pads");
		JsonElement launch_pad = json.get("enable_launch_pads");
		boolean launch_pads = (jp != null && jp.getAsBoolean()) || (launch_pad != null && launch_pad.getAsBoolean());

		JsonElement lp = json.get("enable_levitation_pads");
		boolean levi_pads = (lp != null && lp.getAsBoolean());

		map_features = new MapFeatures(launch_pads, levi_pads);

		JsonObject jo_podium = json.getAsJsonObject("podium");
		if (jo_podium != null) {
			this.podium = new PodiumData(jo_podium);
		}
	}

	public void parse_config_v3(JsonObject json) {
		load_spawn_coords(json);

		this.map_name = json.get("map_name").getAsString();

		load_border_values(json);

		JsonElement plant_block = json.get("bomb_plant_block");
		if (plant_block != null) {
			bomb_plant_block = Material.matchMaterial(plant_block.getAsString());
		}

		JsonObject jo_map_features = json.getAsJsonObject("map_features");
		if (jo_map_features != null) {
			map_features = new MapFeatures(jo_map_features);
		}

		JsonObject jo_podium = json.getAsJsonObject("podium");
		if (jo_podium != null) {
			this.podium = new PodiumData(jo_podium);
		}

		JsonElement ranked = json.get("is_ranked");
		if (ranked != null && ranked.getAsBoolean() == true) {
			Bukkit.getLogger().info("Registered this server to be a ranked Litestrike server!");
			this.ranked = true;
		}

	}

	public MapData() {
		try {
			String file_content = Files.readString(Paths.get("./world/map_config.json"));
			JsonObject json = JsonParser.parseString(file_content).getAsJsonObject();

			JsonElement v = json.get("version");
			if (v == null) {
				throw new Exception("Your map_config.json is missing a version field, please update your map_config.json");
			}
			switch (v.getAsInt()) {
				case 2:
					Bukkit.getLogger().warning("[Litestrike] loading a version 2 map config file, consider updating it");
					parse_config_v2(json);
					break;
				case 3:
					Bukkit.getLogger().info("[Litestrike] loading a version 3 map config file");
					parse_config_v3(json);
					break;
				default:
					throw new Exception("incorrect map_config.json file version, please update your map_config.json");
			}

		} catch (Exception e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not load the maps configuration file!\n Error: " + e);
			e.printStackTrace();
			Bukkit.getLogger().log(Level.SEVERE, "The Plugin will be disabled!");
			Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());
			throw new RuntimeException(new Exception());
		}
	}

	public Location get_placer_spawn(World w) {
		return new Location(w, placer_spawn[0], placer_spawn[1], placer_spawn[2]);
	}

	public Location get_breaker_spawn(World w) {
		return new Location(w, breaker_spawn[0], breaker_spawn[1], breaker_spawn[2]);
	}

	public Location get_queue_spawn(World w) {
		return new Location(w, queue_spawn[0], queue_spawn[1], queue_spawn[2]);
	}

	public String toString() {
		String s = "placer_spawn: " + Arrays.toString(this.placer_spawn) +
				"\nbreaker_spawn: " + Arrays.toString(this.breaker_spawn) +
				"\nqueue_spawn: " + Arrays.toString(this.queue_spawn) +
				"\nmap_name: " + this.map_name +
				"\nborder_marker: " + this.border_marker +
				"\nborder_block_type: " + this.border_block_type +
				"\namount of known border blocks: " + this.border_blocks.size();
		if (map_features != null) {
			s = s + "\n\nmap_features: " + map_features.toString();
		}
		if (this.podium != null) {
			s = s + "\n\npodium:\nspawn:" + Arrays.toString(this.podium.spawn) +
					"\nfirst:" + Arrays.toString(this.podium.first) +
					"\nsecond:" + Arrays.toString(this.podium.second) +
					"\nthird:" + Arrays.toString(this.podium.third);
		}
		return s;
	}

	// if this returns true for a chunk, the chunk is searched for border blocks.
	// this returns true if the chunk is within 5 chunks of the spawn points
	private boolean is_search_chunk(int chunk_x, int chunk_z, World w) {

		// check in range of placer spawn
		Chunk placer_spawn_chunk = get_placer_spawn(w).getChunk();
		int lower_x_bound = placer_spawn_chunk.getX() - 5;
		int upper_x_bound = placer_spawn_chunk.getX() + 5;
		int lower_z_bound = placer_spawn_chunk.getZ() - 5;
		int upper_z_bound = placer_spawn_chunk.getZ() + 5;
		if ((chunk_x >= lower_x_bound && chunk_x <= upper_x_bound)
				&& (chunk_z >= lower_z_bound && chunk_z <= upper_z_bound)) {
			return true;
		}

		// check in range of braeker spawn
		Chunk breaker_spawn_chunk = get_breaker_spawn(w).getChunk();
		lower_x_bound = breaker_spawn_chunk.getX() - 5;
		upper_x_bound = breaker_spawn_chunk.getX() + 5;
		lower_z_bound = breaker_spawn_chunk.getZ() - 5;
		upper_z_bound = breaker_spawn_chunk.getZ() + 5;
		if ((chunk_x >= lower_x_bound && chunk_x <= upper_x_bound)
				&& (chunk_z >= lower_z_bound && chunk_z <= upper_z_bound)) {
			return true;
		}

		// if not in range of either, return false
		return false;
	};
}

class PodiumData {
	public final double[] spawn;
	public final double[] first;
	public final double[] second;
	public final double[] third;

	public PodiumData(JsonObject jo) {
		JsonArray j_spawn = jo.get("spawn").getAsJsonArray();
		this.spawn = new double[] { j_spawn.get(0).getAsDouble(), j_spawn.get(1).getAsDouble(),
				j_spawn.get(2).getAsDouble() };

		JsonArray j_first = jo.get("first").getAsJsonArray();
		this.first = new double[] { j_first.get(0).getAsDouble(), j_first.get(1).getAsDouble(),
				j_first.get(2).getAsDouble() };

		JsonArray j_second = jo.get("second").getAsJsonArray();
		this.second = new double[] { j_second.get(0).getAsDouble(), j_second.get(1).getAsDouble(),
				j_second.get(2).getAsDouble() };

		JsonArray j_third = jo.get("third").getAsJsonArray();
		this.third = new double[] { j_third.get(0).getAsDouble(), j_third.get(1).getAsDouble(),
				j_third.get(2).getAsDouble() };
	}

	public Location get_spawn(World w) {
		return new Location(w, spawn[0], spawn[1], spawn[2]);
	}

	public Location get_first(World w) {
		return new Location(w, first[0], first[1], first[2]);
	}

	public Location get_second(World w) {
		return new Location(w, second[0], second[1], second[2]);
	}

	public Location get_third(World w) {
		return new Location(w, third[0], third[1], third[2]);
	}
}

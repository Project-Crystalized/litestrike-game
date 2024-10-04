package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.BlockFace;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
	public final double[] placer_spawn;
	public final double[] breaker_spawn;
	public final double[] que_spawn;

	public final String map_name;

	// toggelable map-specific features
	public final Boolean jump_pads;
	public final Boolean openable_doors;

	// border gets placed 1 block above this block type
	public final Material border_specifier;

	public Set<int[]> border_blocks = Collections.synchronizedSet(new HashSet<int[]>());

	public void raiseBorder(World w) {
		for (int[] b : border_blocks) {
			w.getBlockAt(b[0], b[1], b[2]).getRelative(BlockFace.UP, 2).setType(Material.RED_CONCRETE);
			w.getBlockAt(b[0], b[1], b[2]).getRelative(BlockFace.UP, 3).setType(Material.RED_CONCRETE);
		}
	}

	public void lowerBorder(World w) {
		for (int[] b : border_blocks) {
			w.getBlockAt(b[0], b[1], b[2]).getRelative(BlockFace.UP, 2).setType(Material.AIR);
			w.getBlockAt(b[0], b[1], b[2]).getRelative(BlockFace.UP, 3).setType(Material.AIR);
		}
	}

	// IMPORTANT this only works of /gamerule spawnChunkRadius is set to 0
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		Chunk c = e.getChunk();

		if (is_search_chunk(c.getX(), c.getZ(), c.getWorld())) {
			ChunkSnapshot cs = c.getChunkSnapshot(true, false, false, false);

			new BukkitRunnable() {
				@Override
				public void run() {
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							for (int y = -64; y < cs.getHighestBlockYAt(x, z); y++) {
								if (cs.getBlockType(x, y, z) == border_specifier) {
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

	@EventHandler
	public void onWorldInit(WorldInitEvent e) {
		World w = e.getWorld();

		if (w.getGameRuleValue(GameRule.SPAWN_CHUNK_RADIUS) != 0) {
			Bukkit.getLogger().log(Level.SEVERE,
					"LITESTRIKE: The Gamerule SPAWN_CHUNK_RADIUS needs to be set to zero in order for Litestrike to work!");
			Bukkit.getLogger().log(Level.SEVERE,
					"LITESTRIKE: The GameRule SPAWN_CHUNK_RADIUS was set to 0! Please restart the server now to prevent bugs.");
			w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
			Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());
		}
	}

	public MapData() {
		try {
			String file_content = Files.readString(Paths.get("./world/map_config.json"));
			JsonObject json = JsonParser.parseString(file_content).getAsJsonObject();

			// pitch and yaw are not needed, as we just make players look at enemy spawn
			JsonArray p_spawn = json.get("placer_spawn").getAsJsonArray();
			this.placer_spawn = new double[] { p_spawn.get(0).getAsDouble(), p_spawn.get(1).getAsDouble(),
					p_spawn.get(2).getAsDouble() };

			JsonArray b_spawn = json.get("breaker_spawn").getAsJsonArray();
			this.breaker_spawn = new double[] { b_spawn.get(0).getAsDouble(), b_spawn.get(1).getAsDouble(),
					b_spawn.get(2).getAsDouble() };

			JsonArray q_spawn = json.get("que_spawn").getAsJsonArray();
			this.que_spawn = new double[] { q_spawn.get(0).getAsDouble(), q_spawn.get(1).getAsDouble(),
					q_spawn.get(2).getAsDouble() };

			this.map_name = json.get("map_name").getAsString();

			String b_spec = json.get("border_specifier").getAsString();
			this.border_specifier = Material.matchMaterial(b_spec);

			this.jump_pads = json.get("enable_jump_pads") != null;
			this.openable_doors = json.get("enable_openable_doors") != null;

		} catch (Exception e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not load the maps configuration file!\n" + e);
			Bukkit.getLogger().log(Level.SEVERE, "The Plugin will be disabled!");
			// disable plugin when failure
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

	public Location get_que_spawn(World w) {
		return new Location(w, que_spawn[0], que_spawn[1], que_spawn[2]);
	}

	public String toString() {
		return "placer_spawn: " + Arrays.toString(this.placer_spawn) +
				"\nbreaker_spawn: " + Arrays.toString(this.breaker_spawn) +
				"\nque_spawn: " + Arrays.toString(this.que_spawn) +
				"\nmap_name: " + this.map_name +
				"\nborder_specifier: " + this.border_specifier +
				"\nenable_jump_pads: " + this.jump_pads +
				"\nenable_openable_doors: " + this.openable_doors +
				"\namount of known border blocks: " + this.border_blocks.size();
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

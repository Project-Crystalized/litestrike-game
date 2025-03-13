package gg.litestrike.game.mapfeatures;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gg.litestrike.game.Litestrike;

public class BigDoor implements Listener {

	private double[] center_coords;
	private int radius;
	private Material material;

	public BigDoor(JsonObject json) {
		try {
			JsonArray c = json.get("center").getAsJsonArray();
			this.center_coords = new double[] { c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble() };

			JsonElement radius_json = json.get("radius");
			if (radius_json != null) {
				radius = radius_json.getAsInt();
			}

			JsonElement material_json = json.get("material");
			if (material_json != null) {
				material = Material.matchMaterial(material_json.getAsString());
			}

		} catch (Exception e) {
			Bukkit.getLogger().severe("Couldnt load big_door data due to config file error.");
			Bukkit.getLogger().severe("Error:" + e);
		}
	}

	public Location get_center(World w) {
		return new Location(w, center_coords[0], center_coords[1], center_coords[2]);
	}

	public void regenerate_door() {
		Bukkit.getLogger().severe("regenerating door");
		List<Block> blocks = getSphere(get_center(Bukkit.getWorld("world")), radius, false);
		for (Block b : blocks) {
			if (b.getType().isAir() || b.getType() == Material.LIGHT) {
				b.setType(material);
			}
		}
	}

	public void open_door() {
		Bukkit.getLogger().severe("opening door");
		new BukkitRunnable() {
			private int i = 0;

			public void run() {
				i++;
				if (i > radius) {
					cancel();
					return;
				}

				List<Block> blocks = getSphere(get_center(Bukkit.getWorld("world")), i, false);
				for (Block b : blocks) {
					if (b.getType() == material) {
						b.setType(Material.AIR);
					}
				}
			};

		}.runTaskTimer(Litestrike.getInstance(), 0, 10);
	}

	public List<Block> getSphere(Location location, int radius, boolean empty) {
		List<Block> blocks = new ArrayList<>();

		int bx = location.getBlockX();
		int bz = location.getBlockZ();

		for (int x = bx - radius; x <= bx + radius; x++) {
			for (int z = bz - radius; z <= bz + radius; z++) {
				double distance = ((bx - x) * (bx - x) + (bz - z) * (bz - z));
				if (distance < radius * radius && (!empty && distance < (radius - 1) * (radius - 1))) {
					blocks.add(new Location(location.getWorld(), x, location.getBlockY(), z).getBlock());
				}
			}
		}

		return blocks;
	}
}

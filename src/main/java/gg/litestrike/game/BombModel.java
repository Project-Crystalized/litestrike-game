package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class BombModel {
	private ArmorStand model;

	public void spawn_planting_model(Location loc) {
	}

	public void bomb_plant() {
	}

	public void bomb_exploded() {
	}

	// takes in the planting timer
	public void raise_bomb(int planting_timer) {
	}

	public void remove() {
		if (model == null) {
			Bukkit.getLogger().severe("tried to remove non existant bomb_model");
		}
		model.remove();
	}
}

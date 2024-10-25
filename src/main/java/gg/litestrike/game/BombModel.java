package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BombModel {
	private ArmorStand model;
	private Location after_plant_loc;

	// creates the model one block below the ground so it can rise
	// takes in, the block location where the bomb will be after planting
	public void spawn_model(Location loc) {
		if (model != null) {
			this.remove();
		}
		after_plant_loc = loc.clone();
		model = (ArmorStand) loc.getWorld().spawn(loc.clone().add(0, -100, 0), ArmorStand.class);
		model.setSmall(true);
		model.setGravity(false);
		model.setCanPickupItems(false);
		model.setVisible(false);
		model.setMarker(true);
		model.teleport(loc);

		ItemStack item = Bomb.bomb_item();
		ItemMeta im = item.getItemMeta();
		im.setCustomModelData(30);
		item.setItemMeta(im);
		model.getEquipment().setHelmet(item);
	}

	public void bomb_mining() {
		if (model == null) {
			Bukkit.getLogger().severe("tried to put bombmodel into mining state when it didnt exist");
		}
		ItemMeta im = model.getEquipment().getHelmet().getItemMeta();
		im.setCustomModelData(29);
		model.getEquipment().getHelmet().setItemMeta(im);
	}

	public void stop_bomb_mining() {
		if (model == null) {
			Bukkit.getLogger().severe("tried to stop bombmodel mining state when model didnt exist");
		}
		ItemMeta im = model.getEquipment().getHelmet().getItemMeta();
		im.setCustomModelData(30);
		model.getEquipment().getHelmet().setItemMeta(im);
	}

	public void bomb_plant(Location loc) {
		this.remove();
		this.spawn_model(loc.clone().add(0.5, 0, 0.5));
	}

	public void bomb_exploded() {
	}

	// takes in the planting timer
	// to get a planting percentage, divide it by the BombListener.PLANT_TIME
	public void raise_bomb(int planting_timer) {
		if (model == null) {
			Bukkit.getLogger().severe("tried to raise bombmodel when it didnt exist");
		}
		double plant_percentage = (double) planting_timer / (double) BombListener.PLANT_TIME;
		model.teleport(after_plant_loc.clone().add(0.5, plant_percentage, 0.5));
	}

	public void remove() {
		if (model != null) {
			model.remove();
		}
		after_plant_loc = null;
	}
}

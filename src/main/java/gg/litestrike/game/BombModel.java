package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;

public class BombModel {
	private ArmorStand model;
	private Location after_plant_loc;

	public static final int MODEL_ACTIVE = 0;
	public static final int MODEL_BREAKING = 1;

	// creates the model one block below the ground so it can rise
	// takes in, the block location where the bomb will be after planting
	public void spawn_model(Location loc) {
		this.remove();
		after_plant_loc = loc.clone();
		model = (ArmorStand) loc.getWorld().spawn(loc.clone().add(0, -100, 0), ArmorStand.class, model -> {

			model.getAttribute(Attribute.SCALE).setBaseValue(0.5);
			model.setGravity(false);
			model.setCanPickupItems(false);
			model.setVisible(false);
			model.setMarker(true);

			ItemStack item = Bomb.bomb_item();
			ItemMeta im = item.getItemMeta();
			im.setCustomModelData(MODEL_ACTIVE);
			item.setItemMeta(im);
			model.getEquipment().setHelmet(item);
		});

		model.teleport(loc);
	}

	private void change_custom_model_to(int custom_model) {
		if (model == null) {
			Bukkit.getLogger().severe("tried to put bombmodel into mining state when it didnt exist");
		}
		ItemStack helmet = model.getEquipment().getHelmet();
		ItemMeta im = helmet.getItemMeta();
		im.setCustomModelData(custom_model);
		im.setItemModel(new NamespacedKey("crystalized", "models/bomb/shard"));
		helmet.setItemMeta(im);
		model.getEquipment().setHelmet(helmet);

	}

	public void bomb_mining() {
		change_custom_model_to(MODEL_BREAKING);
	}

	public void stop_bomb_mining() {
		change_custom_model_to(MODEL_ACTIVE);
	}

	public void bomb_plant(Location loc, BlockFace bf) {
		this.remove();
		this.spawn_model(loc);

		if (bf == BlockFace.WEST) {
			model.setHeadPose(EulerAngle.ZERO.add(0, 0, Math.toRadians(-90)));
			model.teleport(model.getLocation().add(0.3, -0.2, 0.5));
		} else if (bf == BlockFace.EAST) {
			model.setHeadPose(EulerAngle.ZERO.add(0, 0, Math.toRadians(90)));
			model.teleport(model.getLocation().add(0.7, -0.2, 0.5));
		} else if (bf == BlockFace.SOUTH) {
			model.setHeadPose(EulerAngle.ZERO.add(Math.toRadians(90), 0, 0));
			model.teleport(model.getLocation().add(0.5, -0.2, 0.7));
		} else if (bf == BlockFace.NORTH) {
			model.setHeadPose(EulerAngle.ZERO.add(Math.toRadians(-90), 0, 0));
			model.teleport(model.getLocation().add(0.5, -0.2, 0.3));
		} else if (bf == BlockFace.DOWN) {
			model.setHeadPose(EulerAngle.ZERO.add(Math.toRadians(180), 0, 0));
			model.teleport(model.getLocation().add(0.5, -0.5, 0.5));
		} else if (bf == BlockFace.UP) {
			model.teleport(model.getLocation().add(0.5, 0, 0.5));
		}
	}

	// takes in the planting timer
	// to get a planting percentage, divide it by the BombListener.PLANT_TIME
	public void raise_bomb(int planting_timer, BlockFace bf) {
		if (model == null) {
			Bukkit.getLogger().severe("tried to raise bombmodel when it didnt exist");
		}
		if (!bf.isCartesian()) {
			Bukkit.getLogger().severe("non cartesian block face???");
		}
		double plant_percentage = ((double) planting_timer / (double) BombListener.PLANT_TIME) / 2.0;
		model.teleport(
				after_plant_loc.clone().add(0.5, 0, 0.5)
						.add(bf.getDirection().multiply(plant_percentage)));

		if (bf == BlockFace.WEST) {
			model.setHeadPose(EulerAngle.ZERO.add(0, 0, Math.toRadians(-90)));
			model.teleport(model.getLocation().add(-0.5, -0.2, 0));
		} else if (bf == BlockFace.EAST) {
			model.setHeadPose(EulerAngle.ZERO.add(0, 0, Math.toRadians(90)));
			model.teleport(model.getLocation().add(0.5, -0.2, 0));
		} else if (bf == BlockFace.SOUTH) {
			model.setHeadPose(EulerAngle.ZERO.add(Math.toRadians(90), 0, 0));
			model.teleport(model.getLocation().add(0, -0.2, 0.5));
		} else if (bf == BlockFace.NORTH) {
			model.setHeadPose(EulerAngle.ZERO.add(Math.toRadians(-90), 0, 0));
			model.teleport(model.getLocation().add(0, -0.2, -0.5));
		} else if (bf == BlockFace.DOWN) {
			model.setHeadPose(EulerAngle.ZERO.add(Math.toRadians(180), 0, 0));
			model.teleport(model.getLocation().add(0, -0.7, 0));
		} else if (bf == BlockFace.UP) {
			model.teleport(model.getLocation().add(0, 0.5, 0));
		}
	}

	public void remove() {
		if (model != null) {
			model.remove();
		}
		after_plant_loc = null;
	}
}

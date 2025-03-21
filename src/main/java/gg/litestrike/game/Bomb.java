package gg.litestrike.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import static org.bukkit.Particle.*;
import static net.kyori.adventure.text.Component.text;

public interface Bomb {
	static final int DETONATION_TIME = (20 * 40);

	public static ItemStack bomb_item() {
		ItemStack item = new ItemStack(Material.CHARCOAL);
		ItemMeta im = item.getItemMeta();

		// set item lore
		List<Component> lore = new ArrayList<Component>();
		lore.add(Component.translatable("crystalized.item.bomb.desc").color(TextColor.color(0xf4d167))
				.decoration(TextDecoration.ITALIC, false));
		lore.add(Component.translatable("crystalized.item.bomb.desc2").color(TextColor.color(0xf4d167))
				.decoration(TextDecoration.ITALIC, false));
		lore.add(Component.translatable("crystalized.item.bomb.desc3").color(NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
		im.lore(lore);

		im.setCustomModelData(BombModel.MODEL_ACTIVE);
		im.setItemModel(new NamespacedKey("crystalized", "models/bomb/shard"));

		im.displayName(Component.translatable("crystalized.item.bomb.name").color(TextColor.color(0xe64cce))
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));

		item.setItemMeta(im);
		return item;
	}

	public static void give_bomb(Player inv) {
		// some sanity checks
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		if (b instanceof InvItemBomb || b instanceof PlacedBomb) {
			// bomb was in a invalid state
			Bukkit.getLogger().severe("ERROR: bomb got given while already placed or in inv. Check the Bomb Logic!");
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				Bukkit.getLogger().severe(ste.toString());
			}
		}

		if (b != null) {
			b.remove();
		}
		Litestrike.getInstance().game_controller.bomb = new InvItemBomb(inv);
	}

	public static String get_arrow(Player p, Location loc2) {
		Location p_loc = p.getLocation().clone();
		p_loc.setY(0);
		p_loc.setPitch(0);

		Location loc = loc2.clone();
		loc.setY(0);

		Vector blockDirection = loc.subtract(p_loc).toVector().normalize();

		double x1 = blockDirection.getX();
		double z1 = blockDirection.getZ();
		double x2 = p_loc.getDirection().getX();
		double z2 = p_loc.getDirection().getZ();

		double angle = Math.toDegrees(Math.atan2(x1 * z2 - z1 * x2, x1 * x2 + z1 * z2));

		if (angle >= -22.5 && angle <= 22.5) {
			return "\uE110";
		} else if (angle <= 67.5 && angle > 0) {
			return "\uE117";
		} else if (angle <= 112.5 && angle > 0) {
			return "\uE116";
		} else if (angle <= 157.5 && angle > 0) {
			return "\uE115";
		} else if (angle >= -67.5 && angle < 0) {
			return "\uE111";
		} else if (angle >= -112.5 && angle < 0) {
			return "\uE112";
		} else if (angle >= -157.5 && angle < 0) {
			return "\uE113";
		} else if (angle <= 190 && angle >= -190) {
			return "\uE114";
		} else {
			return "error invalid angle";
		}
	}

	public void remove();
}

class DroppedBomb implements Bomb {
	Item item;

	public DroppedBomb(Item item) {
		this.item = item;

		item.setGlowing(true);
		item.setInvulnerable(true);

		ProtocolLibLib.update_armor();
	}

	public String get_bomb_loc_string(Player p) {
		if (item == null || item.isDead()) {
			return "Dropped, but the item is dead.";
		}
		String arrow = Bomb.get_arrow(p, item.getLocation());
		return "Dropped Somewhere " + arrow;
	}

	@Override
	public void remove() {
		item.remove();
	}
}

class PlacedBomb implements Bomb {
	public Block block;
	private BombModel bomb_model;

	public boolean is_detonated = false;
	public boolean is_broken = false;
	public int timer = 0;

	public PlacedBomb(Block block, BombModel bm, BlockFace bf) {
		this.block = block;
		this.bomb_model = bm;

		SoundEffects.bomb_plant_finish(block.getLocation());
		block.setType(Material.BARRIER);
		Bukkit.getServer().showTitle(Title
				.title(Component.translatable("crystalized.game.litestrike.bombplanted").color(Litestrike.YELLOW), text("")));
		start_explosion_timer();
		bomb_model.bomb_plant(block.getLocation(), bf);

		ProtocolLibLib.update_armor();
	}

	public String get_bomb_loc_string(Player p) {
		String arrow = Bomb.get_arrow(p, block.getLocation());
		return "Placed at a site " + arrow;
	}

	@Override
	public void remove() {
		block.setType(Material.AIR);
		bomb_model.remove();
	}

	private void start_explosion_timer() {
		new BukkitRunnable() {
			int last_beep = 0;

			@Override
			public void run() {
				if (!(Litestrike.getInstance().game_controller.bomb instanceof PlacedBomb) || is_broken || is_detonated) {
					cancel();
					return;
				}

				timer += 1;

				int freq = 20 + (int) (-0.025 * timer);
				if (timer - last_beep > freq) {
					last_beep = timer;
					Sound sound = Sound.sound(Key.key("block.note_block.bit"), Sound.Source.AMBIENT, 1.9f, 1.8f);
					Bukkit.getServer().playSound(sound, block.getX(), block.getY(), block.getZ());
					block.getWorld().spawnParticle(RAID_OMEN, block.getLocation().add(0.5, 0.5, 0.5), 3);
				}

				if (timer == DETONATION_TIME) {
					remove();
					explode();
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	private void explode() {
		is_detonated = true;
		block.getWorld().spawnParticle(TRIAL_SPAWNER_DETECTION, block.getLocation().add(0.5, 0.5, 0.5), 5000, 1, 1, 1);
		block.getWorld().spawnParticle(CAMPFIRE_COSY_SMOKE, block.getLocation().add(0.5, 0.5, 0.5), 500, 1, 1, 1);
		block.getWorld().playSound(Sound.sound(Key.key("entity.dragon_fireball.explode"), Sound.Source.AMBIENT, 20, 1),
				block.getX(), block.getY(), block.getZ());
		for (Player p : Bukkit.getOnlinePlayers()) {
			double distance = p.getLocation().distance(block.getLocation());
			if (distance < 15) {
				p.setHealth(0);
			}
			if (distance < 30) {
				p.damage(30 - distance);
			}
		}

		// the explosion animation
		new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				if (i % 10 == 0) {
					block.getWorld().playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 1),
							block.getX(), block.getY(), block.getZ());
				}
				i += 1;
				if (i == (20 * 4)) {
					remove();
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

}

class InvItemBomb implements Bomb {
	Player player;

	public InvItemBomb(Player p) {
		this.player = p;
		player.getInventory().addItem(Bomb.bomb_item());

		ProtocolLibLib.update_armor();
	}

	public String get_bomb_loc_string(Player p) {
		if (p == player) {
			return "In your Inventory";
		}
		String arrow = Bomb.get_arrow(p, player.getLocation());
		return "In someone's Inventory " + arrow;
	}

	@Override
	public void remove() {
		player.getInventory().removeItemAnySlot(Bomb.bomb_item());
	}

	public void place_bomb(Block bomb_block, BombModel bm, BlockFace bf) {
		remove();
		Litestrike.getInstance().game_controller.bomb = new PlacedBomb(bomb_block, bm, bf);
	}

	public void drop_bomb(Item item) {
		remove();
		Litestrike.getInstance().game_controller.bomb = new DroppedBomb(item);
	}
}

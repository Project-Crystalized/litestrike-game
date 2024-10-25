package gg.litestrike.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import static net.kyori.adventure.text.Component.text;

public interface Bomb {
	static final int DETONATION_TIME = (20 * 40);

	public static ItemStack bomb_item() {
		ItemStack item = new ItemStack(Material.CHARCOAL);
		ItemMeta im = item.getItemMeta();

		// set item lore
		List<Component> lore = new ArrayList<Component>();
		lore.add(text("This is the bomb. place it at the bomb sites").color(NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
		lore.add(text("** maybe some lore stuff here, ask mira or someone idk**").color(NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
		im.lore(lore);

		im.setCustomModelData(29);

		im.displayName(text("The Bomb!!"));

		// these apis are deprecated:
		// Set<Material> can_place_set = new HashSet<Material>();
		// can_place_set.add(Material.TERRACOTTA);
		// im.setCanPlaceOn(can_place_set);

		item.setItemMeta(im);
		return item;
	}

	public static void give_bomb(PlayerInventory inv) {
		// some sanity checks
		Bomb b = Litestrike.getInstance().game_controller.bomb;
		if (b instanceof InvItemBomb || b instanceof PlacedBomb) {
			// bomb was in a invalid state
			Bukkit.getLogger().severe("ERROR: bomb got given while already placed or in inv. Check the Bomb Logic!");
		}

		if (b != null) {
			b.remove();
		}
		Litestrike.getInstance().game_controller.bomb = new InvItemBomb(inv);
	}

	public void remove();
}

class DroppedBomb implements Bomb {
	Item item;

	public DroppedBomb(Item item) {
		this.item = item;

		item.setGlowing(true);
		item.setInvulnerable(true);
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

	public PlacedBomb(Block block, BombModel bm) {
		this.block = block;
		this.bomb_model = bm;

		SoundEffects.bomb_plant_finish();
		block.setType(Material.BARRIER);
		Bukkit.getServer().showTitle(Title.title(text("ᴛʜᴇ ʙᴏᴍʙ ʜᴀꜱ ʙᴇᴇɴ ᴘʟᴀɴᴛᴇᴅ!").color(Litestrike.YELLOW), text("")));
		start_explosion_timer();
		bomb_model.bomb_plant(block.getLocation());
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
				}

				if (timer == DETONATION_TIME) {
					bomb_model.bomb_exploded();
					explode();
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	private void explode() {
		is_detonated = true;

		// the explosion animation
		new BukkitRunnable() {
			int i = 0;

			@Override
			public void run() {
				if (i % 10 == 0) {
					block.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, block.getLocation(), 5);
					block.getWorld().playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1, 1),
							block.getX(), block.getY(), block.getZ());
				}
				for (Player p : Bukkit.getOnlinePlayers()) {
					double distance = p.getLocation().distance(block.getLocation());
					if (distance < 15) {
						p.setHealth(0);
					}
					if (distance < 30) {
						p.damage(30 - distance);
					}
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
	PlayerInventory p_inv;

	public InvItemBomb(PlayerInventory p_inv) {
		this.p_inv = p_inv;
		p_inv.addItem(Bomb.bomb_item());
	}

	@Override
	public void remove() {
		p_inv.remove(Bomb.bomb_item());
	}

	public void place_bomb(Block bomb_block, BombModel bm) {
		remove();
		Litestrike.getInstance().game_controller.bomb = new PlacedBomb(bomb_block, bm);
	}

	public void drop_bomb(Item item) {
		remove();
		Litestrike.getInstance().game_controller.bomb = new DroppedBomb(item);
	}
}

package gg.litestrike.game.mapfeatures;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import gg.litestrike.game.Litestrike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class CargoDoor implements Listener {
	private boolean is_door_open = false;

	@EventHandler
	public void onInteractLever(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.LEVER) {
			open_door();
		}
	}

	public void open_door() {
		if (is_door_open) {
			return;
		}
		is_door_open = true;
		new BukkitRunnable() {
			private int i = 0;
			@Override
			public void run() {
				i++;
				switch (i) {
					case 1:
						door_animation(-2);
						break;
					case 15:
						door_animation(2);
						break;
					case 30:
						door_animation(6);
						break;
					case 45:
						door_animation(10);
						cancel();
						break;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	public void close_door() {
		is_door_open = false;
		door_animation(-2);
	}

	private void door_animation(int y) {
		Random ran = new Random();
		World w = Bukkit.getWorld("world");
		Sound sound = Sound.sound(Key.key("minecraft:block.iron_door.close"), Sound.Source.AMBIENT, 3f, 0.5f);
		Bukkit.getServer().playSound(sound, -39, 56, 50);
		for (int x = -50; x < -32; x++) {
			for (int z = 45; z < 55; z++) {
				// copy block
				Block b = w.getBlockAt(x, 56, z);
				b.setType(w.getBlockAt(x, y, z).getType());

				// set particle
				if (ran.nextInt(3) == 2) { // 33% chance
					w.spawnParticle(Particle.LARGE_SMOKE, x, 56, z, 20);
				}
			}
		}
	}
}

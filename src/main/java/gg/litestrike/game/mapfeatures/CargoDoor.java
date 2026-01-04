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

import gg.litestrike.game.GameController;
import gg.litestrike.game.Litestrike;
import gg.litestrike.game.GameController.RoundState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;


public class CargoDoor implements Listener {
	private boolean is_door_open = false;
	final int DOOR_Y = 23;

	@EventHandler
	public void onInteractLever(PlayerInteractEvent e) {
		if (Litestrike.getInstance().game_controller == null) {
			return;
		}
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

				GameController gc = Litestrike.getInstance().game_controller;
				if (gc == null || gc.round_state != RoundState.Running) {
					cancel();
					return;
				}

				i++;
				switch (i) {
					case 1:
						door_animation(-54);
						break;
					case 15:
						door_animation(-50);
						break;
					case 30:
						door_animation(-46);
						break;
					case 45:
						door_animation(-42);
						cancel();
						break;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	public void close_door() {
		is_door_open = false;
		door_animation(-54);
	}

	private void door_animation(int y) {
		Random ran = new Random();
		World w = Bukkit.getWorld("world");
		Sound sound = Sound.sound(Key.key("minecraft:block.iron_door.close"), Sound.Source.AMBIENT, 3f, 0.5f);
		Bukkit.getServer().playSound(sound, 182, DOOR_Y, 157);
		for (int x = 176; x < 187; x++) {
			for (int z = 152; z < 170; z++) {
				// copy block
				Block b = w.getBlockAt(x, DOOR_Y, z);
				b.setType(w.getBlockAt(x, y, z).getType());

				// set particle
				if (ran.nextInt(3) == 2) { // 33% chance
					w.spawnParticle(Particle.LARGE_SMOKE, x, DOOR_Y, z, 20);
				}
			}
		}
	}
}

package gg.litestrike.game;

import static net.kyori.adventure.text.Component.text;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class SoundEffects {
	public static void round_start() {
		new BukkitRunnable() {
			int timer = 0;

			@Override
			public void run() {
				switch (timer) {
					case 0:
					case 8:
					case 16:
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.bit"), Sound.Source.AMBIENT, 1f, 1.681793f));
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.bass"), Sound.Source.AMBIENT, 1f, 0.594604f));
						break;

					case 24:
					case 26:
					case 28:
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.cow_bell"), Sound.Source.AMBIENT, 1f, 0.629961f));
						Bukkit.getServer().playSound(Sound.sound(Key.key("block.note_block.harp"), Sound.Source.AMBIENT, 1f, 0.5f));
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.snare"), Sound.Source.AMBIENT, 1f, 0.5f));
				}
				timer += 1;
				if (timer > 30) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 276, 2);
	}

	public static void round_lost(Audience a) {
		new BukkitRunnable() {
			int timer = 0;

			@Override
			public void run() {
				switch (timer) {
					case 0:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), Sound.Source.AMBIENT, 1f, 1.0f));
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), Sound.Source.AMBIENT, 1f, 1.122462f));
						break;
					case 2:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), Sound.Source.AMBIENT, 1f, 0.840896f));
						break;
					case 4:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), Sound.Source.AMBIENT, 1f, 0.749154f));
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), Sound.Source.AMBIENT, 1f, 1.0f));
						break;
					case 6:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), Sound.Source.AMBIENT, 1f, 0.629961f));
						break;
					case 8:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), Sound.Source.AMBIENT, 1f, 0.561231f));
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), Sound.Source.AMBIENT, 1f, 0.840896f));
						break;
					case 11:
					case 12:
					case 14:
					case 15:
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), Sound.Source.AMBIENT, 1f, 0.749154f));
				}
				timer += 1;
				if (timer > 20) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 2);
	}

	public static void round_won(Audience a) {
		new BukkitRunnable() {
			int timer = 0;

			@Override
			public void run() {
				switch (timer) {
					case 0:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.AMBIENT, 1f, 0.5f));
						break;
					case 1:
						a.playSound(Sound.sound(Key.key("block.note_block.flute"), Sound.Source.AMBIENT, 1f, 1.122462f));
						break;
					case 2:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.AMBIENT, 1f, 0.561231f));
						break;
					case 4:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.AMBIENT, 1f, 0.629961f));
						break;
					case 5:
						a.playSound(Sound.sound(Key.key("block.note_block.flute"), Sound.Source.AMBIENT, 1f, 1.498307f));
						break;
					case 6:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.AMBIENT, 1f, 0.749154f));
						break;
					case 8:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.AMBIENT, 1f, 0.840896f));
						break;
					case 9:
						a.playSound(Sound.sound(Key.key("block.note_block.flute"), Sound.Source.AMBIENT, 1f, 2.0f));
						break;
					case 12:
					case 13:
					case 14:
					case 15:
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), Sound.Source.AMBIENT, 1f, 2.0f));
						break;
					case 21:
						a.playSound(Sound.sound(Key.key("block.note_block.bass"), Sound.Source.AMBIENT, 1f, 1.0f));
						break;
				}
				timer += 1;
				if (timer > 30) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 2);
	}

	public static void bomb_plant_start() {
		new BukkitRunnable() {
			int timer = 0;

			@Override
			public void run() {
				switch (timer) {
					case 0:
					case 1:
					case 2:
					case 6:
					case 7:
					case 8:
					case 12:
					case 13:
					case 14:
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.AMBIENT, 1f, 0.5f));
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.chime"), Sound.Source.AMBIENT, 1f, 0.5f));
				}
				timer += 1;
				if (timer > 20) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	public static void bomb_beep(int x, int y, int z) {
		Sound sound1 = Sound.sound(Key.key("block.note_block.bit"), Sound.Source.AMBIENT, 1.9f, 1.8f);
		Sound sound2 = Sound.sound(Key.key("block.note_block.xylophone"), Sound.Source.AMBIENT, 1.9f, 0.8f);
		Bukkit.getServer().playSound(sound1, x, y, z);
		Bukkit.getServer().playSound(sound2, x, y, z);

		Bukkit.getServer().sendMessage(text(Bukkit.getWorld("world").getFullTime()));

		// new BukkitRunnable() {
		// @Override
		// public void run() {
		// Bukkit.getServer().sendMessage(text(Bukkit.getWorld("world").getFullTime()));
		// }
		// }.runTaskLater(Litestrike.getInstance(), 2);

	}

}

package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import static net.kyori.adventure.sound.Sound.Source.AMBIENT;

public class SoundEffects {
	public static void round_start() {
		new BukkitRunnable() {

			@Override
			public void run() {
				GameController gc = Litestrike.getInstance().game_controller;
				if (gc.round_state != RoundState.PreRound && gc.phase_timer > 6) {
					cancel();
				}
				if (gc.phase_timer > GameController.PRE_ROUND_TIME - 80 && gc.phase_timer % 20 == 0) {
					countdown_beep();
				}
				if (gc.round_state == RoundState.Running && gc.phase_timer % 3 == 0) {
					Bukkit.getServer()
							.playSound(Sound.sound(Key.key("block.note_block.cow_bell"), AMBIENT, 1f, 0.629961f));
					Bukkit.getServer().playSound(Sound.sound(Key.key("block.note_block.harp"), AMBIENT, 1f, 0.5f));
					Bukkit.getServer()
							.playSound(Sound.sound(Key.key("block.note_block.snare"), AMBIENT, 1f, 0.5f));
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 150, 1);
	}

	public static void round_lost(Audience a) {
		new BukkitRunnable() {
			int timer = 0;

			@Override
			public void run() {
				switch (timer) {
					case 0:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), AMBIENT, 1f, 1.0f));
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), AMBIENT, 1f, 1.122462f));
						break;
					case 2:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), AMBIENT, 1f, 0.840896f));
						break;
					case 4:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), AMBIENT, 1f, 0.749154f));
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), AMBIENT, 1f, 1.0f));
						break;
					case 6:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), AMBIENT, 1f, 0.629961f));
						break;
					case 8:
						a.playSound(Sound.sound(Key.key("block.note_block.xylophone"), AMBIENT, 1f, 0.561231f));
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), AMBIENT, 1f, 0.840896f));
						break;
					case 11:
					case 12:
					case 14:
					case 15:
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), AMBIENT, 1f, 0.749154f));
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
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), AMBIENT, 1f, 0.5f));
						break;
					case 1:
						a.playSound(Sound.sound(Key.key("block.note_block.flute"), AMBIENT, 1f, 1.122462f));
						break;
					case 2:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), AMBIENT, 1f, 0.561231f));
						break;
					case 4:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), AMBIENT, 1f, 0.629961f));
						break;
					case 5:
						a.playSound(Sound.sound(Key.key("block.note_block.flute"), AMBIENT, 1f, 1.498307f));
						break;
					case 6:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), AMBIENT, 1f, 0.749154f));
						break;
					case 8:
						a.playSound(Sound.sound(Key.key("block.note_block.bell"), AMBIENT, 1f, 0.840896f));
						break;
					case 9:
						a.playSound(Sound.sound(Key.key("block.note_block.flute"), AMBIENT, 1f, 2.0f));
						break;
					case 12:
					case 13:
					case 14:
					case 15:
						a.playSound(Sound.sound(Key.key("block.note_block.didgeridoo"), AMBIENT, 1f, 2.0f));
						break;
					case 21:
						a.playSound(Sound.sound(Key.key("block.note_block.bass"), AMBIENT, 1f, 1.0f));
						break;
				}
				timer += 1;
				if (timer > 30) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 2);
	}

	public static void bomb_plant_finish(Location loc) {
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
								.playSound(Sound.sound(Key.key("block.note_block.pling"), AMBIENT, 1f, 0.5f));
						Bukkit.getServer()
								.playSound(Sound.sound(Key.key("block.note_block.chime"), AMBIENT, 1f, 0.5f));
						bomb_particles(loc);
				}
				timer += 1;
				if (timer > 20) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	public static void bomb_particles(Location loc) {
		loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 2, 2, 2, 2);
		loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 2, 2, 2, 2);
		loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 2, 2, 2, 2);
		loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 2, 2, 2, 2);
		loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 2, 2, 2, 2);
	}

	public static void ally_death(Audience a) {
		Sound sound1 = Sound.sound(Key.key("block.note_block.didgeridoo"), AMBIENT, 2f, 0.5f);
		Sound sound2 = Sound.sound(Key.key("block.note_block.snare"), AMBIENT, 2f, 0.5f);
		a.playSound(sound1);
		a.playSound(sound2);

		new BukkitRunnable() {
			@Override
			public void run() {
				a.playSound(sound1);
				a.playSound(sound2);
			}
		}.runTaskLater(Litestrike.getInstance(), 3);
	}

	public static void enemy_death(Audience a) {
		a.playSound(Sound.sound(Key.key("block.note_block.bit"), AMBIENT, 2f, 1f));
		a.playSound(Sound.sound(Key.key("block.note_block.basedrum"), AMBIENT, 2f, 1f));

		new BukkitRunnable() {
			@Override
			public void run() {
				a.playSound(Sound.sound(Key.key("block.note_block.bit"), AMBIENT, 2f, 1.498307f));
				a.playSound(Sound.sound(Key.key("block.note_block.basedrum"), AMBIENT, 2f, 1.498307f));
			}
		}.runTaskLater(Litestrike.getInstance(), 2);
	}

	public static void countdown_beep() {
		Bukkit.getServer()
				.playSound(Sound.sound(Key.key("block.note_block.bit"), AMBIENT, 1f, 1.681793f));
		Bukkit.getServer()
				.playSound(Sound.sound(Key.key("block.note_block.bass"), AMBIENT, 1f, 0.594604f));
	}

	public static Sound start_game_sound() {
		return Sound.sound(Key.key("crystalized:effect.countdown_end"), AMBIENT, 50, 1);
	}

	public static void start_breaking(int x, int y, int z) {
		Sound sound = Sound.sound(Key.key("block.note_block.xylophone"), AMBIENT, 1.9f, 1f);
		Bukkit.getServer().playSound(sound, x, y, z);

		new BukkitRunnable() {
			public void run() {
				Bukkit.getServer().playSound(sound, x, y, z);
			};
		}.runTaskLater(Litestrike.getInstance(), 2);
		new BukkitRunnable() {
			public void run() {
				Bukkit.getServer().playSound(sound, x, y, z);
			};
		}.runTaskLater(Litestrike.getInstance(), 3);
	}

	public static void start_planting(int x, int y, int z) {
		new BukkitRunnable() {
			int timer = 1;

			@Override
			public void run() {
				Sound sound = Sound.sound(Key.key("block.note_block.bit"), AMBIENT, 1.9f, 0.1f * timer);
				Bukkit.getServer().playSound(sound, x, y, z);

				if (timer % 3 == 0) {
					Sound sound_bass = Sound.sound(Key.key("block.note_block.basedrum"), AMBIENT, 1.9f, 1f);
					Bukkit.getServer().playSound(sound_bass, x, y, z);
				}

				timer++;
				if (timer > 11) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	public static void stop_planting(int x, int y, int z) {
		new BukkitRunnable() {
			int timer = 11;

			@Override
			public void run() {
				Sound sound = Sound.sound(Key.key("block.note_block.bit"), AMBIENT, 1.9f, 0.1f * timer);
				Bukkit.getServer().playSound(sound, x, y, z);

				if (timer % 3 == 0) {
					Sound sound_bass = Sound.sound(Key.key("block.note_block.basedrum"), AMBIENT, 1.9f, 1f);
					Bukkit.getServer().playSound(sound_bass, x, y, z);
				}

				timer--;
				if (timer < 1) {
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 1);
	}

	public static void round_end_sound() {
		// TODO maybe a trumped sound similar to tubnet
	}

}

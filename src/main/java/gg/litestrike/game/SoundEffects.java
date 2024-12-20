package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import static net.kyori.adventure.sound.Sound.Source.AMBIENT;

public class SoundEffects {
	public static void game_start() {
		Audience.audience(Bukkit.getOnlinePlayers())
			.playSound(Sound.sound(Key.key("crystalized:effect.ls_game_start"), AMBIENT, 1f, 1f));
	}
	public static void round_start() {
		new BukkitRunnable() {
			@Override
			public void run() {
				GameController gc = Litestrike.getInstance().game_controller;
				if (gc.round_state != RoundState.PreRound) {
					cancel();
				}
				if (gc.phase_timer == GameController.PRE_ROUND_TIME - 80) {
					Bukkit.getServer().playSound(Sound.sound(Key.key("crystalized:effect.ls_round_start"), AMBIENT, 1f, 1f));
					cancel();
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 150, 1);
	}

	public static void round_lost(Audience a) {
		a.playSound(Sound.sound(Key.key("crystalized:effect.ls_round_lost"), AMBIENT, 1f, 1.0f));
	}

	public static void round_won(Audience a) {
		a.playSound(Sound.sound(Key.key("crystalized:effect.ls_round_won"), AMBIENT, 1f, 1.0f));
	}

	public static void bomb_plant_finish(Location loc) {
		Audience.audience(Bukkit.getOnlinePlayers()).playSound(Sound.sound(Key.key("crystalized:effect.star_plant"), AMBIENT, 1f, 1.0f));
		new BukkitRunnable() {
			int timer = 0;
			@Override
			public void run() {
				bomb_particles(loc);
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

	public static void round_end_sound(Team winner) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			Team player_team = Litestrike.getInstance().game_controller.teams.get_team(p);
			if (player_team == winner) {
				p.playSound(Sound.sound(Key.key("crystalized:effect.ls_game_won"), AMBIENT, 50, 1));
			} else {
				p.playSound(Sound.sound(Key.key("crystalized:effect.ls_game_lost"), AMBIENT, 50, 1));
			}
		}
	}

}

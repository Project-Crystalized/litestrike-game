package gg.litestrike.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.kyori.adventure.audience.Audience;

import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.format.NamedTextColor;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

public class QueueSystem implements PluginMessageListener {
	// measure in half seconds
	private static int time_since_velocity_update = 100;

	private static int last_velocity_que_update;

	// this is set by the /force_start command
	public static boolean is_force_starting = false;

	public static QueueScoreboard qsb;

	public QueueSystem() {
		qsb = new QueueScoreboard();

		new BukkitRunnable() {
			@Override
			public void run() {
				time_since_velocity_update += 1;
			}
		}.runTaskTimer(Litestrike.getInstance(), 7, 10);

		new BukkitRunnable() {
			int countdown = 11;

			@Override
			public void run() {

				// if there is already a game going on, do nothing
				if (Litestrike.getInstance().game_controller != null) {
					return;
				}

				qsb.update_player_count();

				int in_que = people_in_que();
				// if more then 6 players online, count down, else reset countdown
				if ((in_que >= Litestrike.PLAYERS_TO_START && in_que % 2 == 0) || is_force_starting) {
					if (in_que >= Litestrike.PLAYER_CAP) {
						is_force_starting = true;
					}
					countdown -= 1;
					count_down_animation(countdown);
				} else {
					if (countdown != 11) {
						Audience.audience(Bukkit.getOnlinePlayers())
								.sendMessage(text("Stopped Countdown, wrong number of players.").color(Litestrike.YELLOW));
					}
					countdown = 11;
					return;
				}

				// if countdown reaches zero, we start the game
				if (countdown == 0 || is_force_starting) {
					countdown = 11;
					Litestrike.getInstance().game_controller = new GameController();
					is_force_starting = false;
					Bukkit.getLogger().info("A GAME is starting!");
					SoundEffects.game_start();

					// signals that the game has started to the proxy
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("start_game");
					for (Player p : Bukkit.getOnlinePlayers()) {
						out.writeUTF(p.getName());
						p.getInventory().clear();
					}
					Player p = (Player) Bukkit.getOnlinePlayers().toArray()[0];
					p.sendPluginMessage(Litestrike.getInstance(), "crystalized:litestrike", out.toByteArray());
					Litestrike.getInstance().party_manager.clear_partys();

					return;
				}
			}
		}.runTaskTimer(Litestrike.getInstance(), 1, 20);

	}

	public static int people_in_que() {
		if (time_since_velocity_update > 20) {
			return Bukkit.getOnlinePlayers().size();
		} else {
			return last_velocity_que_update;
		}
	}

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
		if (!channel.equals("crystalized:main")) {
			return;
		}

		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String message1 = in.readUTF();
		if (!(message1.contains("queue_size"))) {
			return;
		}

		last_velocity_que_update = in.readInt();
		time_since_velocity_update = 0;
	}

	// plays every second while the game is counting down to start
	private void count_down_animation(int i) {
		Audience players = Audience.audience(Bukkit.getOnlinePlayers());
		switch (i) {
			case 0:
				players.playSound(SoundEffects.start_game_sound());
				break;
			case 3:
				players.showTitle(Title.title(text("Starting in:").color(NamedTextColor.GREEN),
						text("3").color(NamedTextColor.RED)
								.append(text(" 2 1").color(NamedTextColor.GRAY))));
				SoundEffects.countdown_beep();
				break;
			case 2:
				players.showTitle(Title.title(text("Starting in:").color(NamedTextColor.GREEN),
						text("3").color(NamedTextColor.GRAY)
								.append(text(" 2").color(NamedTextColor.RED))
								.append(text(" 1").color(NamedTextColor.GRAY))));
				SoundEffects.countdown_beep();
				break;
			case 1:
				players.showTitle(
						Title.title(text("Starting in:").color(NamedTextColor.GREEN), text("3 2 ").color(NamedTextColor.GRAY)
								.append(text("1").color(NamedTextColor.RED))));
				SoundEffects.countdown_beep();
				break;
			case 10:
			case 5:
				players.sendMessage(
						(translatable("crystalized.game.litestrike.start1")
								.append(text("" + i))
								.append(translatable("crystalized.game.litestrike.start2")))
								.color(Litestrike.YELLOW));
		}
	};
}

package gg.litestrike.game;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.command.CommandExecutor;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class DebugCommands implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {

		switch (label) {
			case "mapdata":
				return run_mapdata(args, commandSender);
			case "player_info":
				return run_player_info(args, commandSender);
			case "force_start":
				return run_force_start(args, commandSender);
			case "soundd":
				return run_sound_info(args, commandSender);
			case "big_door":
				return run_big_door(args, commandSender);
			case "debug_log":
				return debug_log(args, commandSender);
			default:
				return false;
		}
	}

	private boolean run_big_door(String[] args, CommandSender commandSender) {
		return true;
	}

	private boolean run_sound_info(String[] args, CommandSender commandSender) {
		if (args.length == 0) {
			return false;
		}

		switch (args[0]) {
			case "round_lost":
				SoundEffects.round_lost(Bukkit.getServer());
				break;
			case "round_won":
				SoundEffects.round_won(Bukkit.getServer());
				break;
			case "ally_death":
				SoundEffects.ally_death(Bukkit.getServer());
				break;
			case "enemy_death":
				SoundEffects.enemy_death(Bukkit.getServer());
				break;

		}
		return true;
	}

	private boolean run_force_start(String[] args, CommandSender commandSender) {
		QueueSystem.is_force_starting = true;
		Bukkit.getServer().sendMessage(Component.text("Force starting the GAME!!!"));
		return true;
	}

	private boolean run_mapdata(String[] args, CommandSender commandSender) {
		MapData mapdata = Litestrike.getInstance().mapdata;
		commandSender.sendMessage(mapdata.toString());

		if (args.length == 0) {
			return true;
		}

		if (args[0].equals("open")) {
			Litestrike.getInstance().mapdata.map_features.bigDoor.open_door();
		} else {
			Litestrike.getInstance().mapdata.map_features.bigDoor.regenerate_door();
		}

		return true;
	}

	private boolean run_player_info(String[] args, CommandSender commandSender) {
		if (args.length == 0) {
			return false;
		}

		String party_info = Litestrike.getInstance().party_manager.print_partys();
		Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(Component.text(party_info));
		if (Litestrike.getInstance().game_controller == null) {
			commandSender.sendMessage("Error, can only get player_data if a game is currently running.");
			return true;
		}

		try {
			Player p = Bukkit.getPlayer(args[0]);
			PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);

			commandSender.sendMessage(pd.toString());
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error running the /player_info command\n" + e);
			return false;
		}
		return true;

	}

	private boolean debug_log(String[] args, CommandSender commandSender){
		if(!(commandSender instanceof Player)){
			return false;
		}
		return true;
	}
}

package gg.litestrike.game;

import org.bukkit.command.CommandExecutor;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

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
			default:
				return false;
		}
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
		Litestrike.getInstance().is_force_starting = true;
		Bukkit.getServer().sendMessage(Component.text("Force starting the GAME!!!"));
		return true;
	}

	private boolean run_mapdata(String[] args, CommandSender commandSender) {
		Player cmd_sender = (Player) commandSender;
		World w = cmd_sender.getWorld();
		MapData mapdata = Litestrike.getInstance().mapdata;

		commandSender.sendMessage(mapdata.toString());

		if (args.length == 0) {
			return true;
		}
		switch (args[0]) {
			case "raise_border": {
				mapdata.raiseBorder(w);
				break;
			}
			case "lower_border": {
				mapdata.lowerBorder(w);
				break;
			}
		}
		return true;
	}

	private boolean run_player_info(String[] args, CommandSender commandSender) {
		if (args.length == 0) {
			return false;
		}
		if (Litestrike.getInstance().game_controller == null) {
			commandSender.sendMessage("Error, can only get player_data if a game is currently running.");
			return true;
		}

		// Bukkit.getLogger().warning("player data amt: " +
		// Litestrike.getInstance().game_controller.playerDatas.size());

		try {
			Player p = Bukkit.getPlayer(args[0]);
			PlayerData pd = Litestrike.getInstance().game_controller.getPlayerData(p);

			commandSender.sendMessage(pd.toString());
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error running the /player_info command\n" + e);
			return false;
		}

		// for (Player p :
		// Litestrike.getInstance().game_controller.teams.get_breakers()) {
		// commandSender.sendMessage("breaker: " + p.getName());
		// }
		//
		// for (Player p : Litestrike.getInstance().game_controller.teams.get_placers())
		// {
		// commandSender.sendMessage("placer: " + p.getName());
		// }

		return true;

	}
}

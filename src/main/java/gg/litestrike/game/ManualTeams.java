package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public class ManualTeams implements BasicCommand {

	public boolean is_enabled = false;
	public List<String> breakers = new ArrayList<>();
	public List<String> placers = new ArrayList<>();

	@Override
	public void execute(CommandSourceStack commandSource, String[] args) {
		if (args.length == 0) {
			commandSource.getSender().sendMessage("the command was incomplete");
			return;
		}
		switch (args[0]) {
			case "clear": {
				is_enabled = false;
				breakers.clear();
				placers.clear();
				return;
			}
			case "remove": {
				if (args.length < 2) {
					commandSource.getSender().sendMessage("Please enter ther playername to remove");
					return;
				}
				breakers.remove(args[1]);
				placers.remove(args[1]);
				if (placers.size() == 0 && breakers.size() == 0) {
					is_enabled = false;
				}
				commandSource.getSender().sendMessage("removed "+args[1]+" from the teams");
				return;
			}
			case "show": {
				if (!is_enabled) {
				commandSource.getSender().sendMessage("Manual Teams are currently not enabled. Run \"/manual_teams add <player_name> <team>\" to enable.");
				} else {
					commandSource.getSender().sendMessage("Placers:");
					for (String s : placers) {
						commandSource.getSender().sendMessage(s);
					}
					commandSource.getSender().sendMessage("\nBreakers:");
					for (String s : breakers) {
						commandSource.getSender().sendMessage(s);
					}
				}
				return;
			}
			case "add": {
				if (args.length < 3) {
					commandSource.getSender().sendMessage("Please enter ther playername and team to add");
					return;
				}
				if (Bukkit.getPlayer(args[1]) == null) {
					commandSource.getSender().sendMessage("Warn: The player "+args[1]+" doesnt seem to be online, adding him anyways");
				}
				if (args[1].startsWith("b")) {
					breakers.add(args[2]);
					placers.remove(args[2]);
					is_enabled = true;
					commandSource.getSender().sendMessage("added "+args[2]+" to the breaker team.");
				} else if (args[1].startsWith("p")) {
					is_enabled = true;
					placers.add(args[2]);
					breakers.remove(args[2]);
					commandSource.getSender().sendMessage("added "+args[2]+" to the placer team.");
				} else {
					commandSource.getSender().sendMessage("Error: The team "+args[1]+" doesnt seem to exist");
				}
				return;
			}
		}
	}

	@Override
	public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
		if (args.length <= 1) {
			return Arrays.asList("clear", "add", "show", "remove");
		}
		if (args[0].equals("clear") || args[0].equals("show")) {
			return List.of();
		}
		if (args.length <= 2 && args[0].equals("add")) {
			return Arrays.asList("breakers", "placers");
		}
		
    return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
	}
}

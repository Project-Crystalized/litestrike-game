package gg.litestrike.game;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class ManualTeams {
	private final GameConfig gameConfig;

	public ManualTeams(GameConfig gameConfig) {
		this.gameConfig = gameConfig;
	}

	public LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("manual_teams")
				.requires(source -> source.getSender().hasPermission("litestrike.command.game"))
				.then(Commands.literal("clear").executes(this::runClear))
				.then(Commands.literal("show").executes(this::runShow))
				.then(Commands.literal("remove")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests((context, builder) -> DebugCommands.suggestMatching(builder, onlinePlayerNames()))
								.executes(this::runRemove)))
				.then(Commands.literal("add")
						.then(Commands.argument("team", StringArgumentType.word())
								.suggests((context, builder) -> DebugCommands.suggestMatching(builder, Arrays.asList("breakers", "placers")))
								.then(Commands.argument("player", StringArgumentType.word())
										.suggests((context, builder) -> DebugCommands.suggestMatching(builder, onlinePlayerNames()))
										.executes(this::runAdd))));
	}

	private int runClear(CommandContext<CommandSourceStack> context) {
		gameConfig.manualTeamsEnabled = false;
		gameConfig.breakers.clear();
		gameConfig.placers.clear();
		return Command.SINGLE_SUCCESS;
	}

	private int runShow(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		if (!gameConfig.manualTeamsEnabled) {
			sender.sendMessage("Manual Teams are currently not enabled. Run \"/manual_teams add <player_name> <team>\" to enable.");
		} else {
			sender.sendMessage("Placers:");
			for (String s : gameConfig.placers) {
				sender.sendMessage(s);
			}
			sender.sendMessage("\nBreakers:");
			for (String s : gameConfig.breakers) {
				sender.sendMessage(s);
			}
		}
		return Command.SINGLE_SUCCESS;
	}

	private int runRemove(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		String player = context.getArgument("player", String.class);
		gameConfig.breakers.remove(player);
		gameConfig.placers.remove(player);
		if (gameConfig.placers.isEmpty() && gameConfig.breakers.isEmpty()) {
			gameConfig.manualTeamsEnabled = false;
		}
		sender.sendMessage("removed " + player + " from the teams");
		return Command.SINGLE_SUCCESS;
	}

	private int runAdd(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		String team = context.getArgument("team", String.class);
		String player = context.getArgument("player", String.class);
		if (Bukkit.getPlayer(player) == null) {
			sender.sendMessage("Warn: The player " + player + " doesnt seem to be online, adding him anyways");
		}
		if (team.startsWith("b")) {
			gameConfig.breakers.add(player);
			gameConfig.manualTeamsEnabled = true;
			sender.sendMessage("added " + player + " to the breaker team.");
		} else if (team.startsWith("p")) {
			gameConfig.manualTeamsEnabled = true;
			gameConfig.placers.add(player);
			sender.sendMessage("added " + player + " to the placer team.");
		} else {
			sender.sendMessage("Error: The team " + team + " doesnt seem to exist");
		}
		return Command.SINGLE_SUCCESS;
	}

	private static List<String> onlinePlayerNames() {
		return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
	}
}

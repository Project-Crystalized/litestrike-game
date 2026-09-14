package gg.litestrike.game;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class DebugCommands {

	public static CompletableFuture<Suggestions> suggestMatching(SuggestionsBuilder builder, Collection<String> options) {
		String remaining = builder.getRemaining().toLowerCase();
		for (String option : options) {
			if (option.toLowerCase().startsWith(remaining)) {
				builder.suggest(option);
			}
		}
		return builder.buildFuture();
	}

	public LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("litestrike")
				.requires(source -> source.getSender().hasPermission("litestrike.command.game"))
				.then(buildMapdata())
				.then(buildForceStart())
				.then(buildPlayerInfo());
	}

	public LiteralArgumentBuilder<CommandSourceStack> buildMapdata() {
		return Commands.literal("mapdata")
				.executes(this::run_mapdata)
				.then(Commands.argument("action", StringArgumentType.word())
						.suggests((context, builder) -> suggestMatching(builder, List.of("open")))
						.executes(this::run_mapdata_action));
	}

	public LiteralArgumentBuilder<CommandSourceStack> buildForceStart() {
		return Commands.literal("force_start")
				.executes(this::run_force_start);
	}

	public LiteralArgumentBuilder<CommandSourceStack> buildPlayerInfo() {
		return Commands.literal("player_info")
				.then(Commands.argument("player", StringArgumentType.word())
						.suggests((context, builder) -> suggestMatching(builder,
								Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()))
						.executes(this::run_player_info));
	}

	private int run_force_start(CommandContext<CommandSourceStack> context) {
		QueueSystem.is_force_starting = true;
		Bukkit.getServer().sendMessage(Component.text("Force starting the GAME!!!"));
		return Command.SINGLE_SUCCESS;
	}

	private int run_mapdata(CommandContext<CommandSourceStack> context) {
		context.getSource().getSender().sendMessage(Litestrike.getInstance().mapdata.toString());
		return Command.SINGLE_SUCCESS;
	}

	private int run_mapdata_action(CommandContext<CommandSourceStack> context) {
		context.getSource().getSender().sendMessage(Litestrike.getInstance().mapdata.toString());

		if (context.getArgument("action", String.class).equals("open")) {
			Litestrike.getInstance().mapdata.map_features.bigDoor.open_door();
		} else {
			Litestrike.getInstance().mapdata.map_features.bigDoor.regenerate_door();
		}

		return Command.SINGLE_SUCCESS;
	}

	private int run_player_info(CommandContext<CommandSourceStack> context) {
		CommandSender commandSender = context.getSource().getSender();

		String party_info = Litestrike.getInstance().party_manager.print_partys();
		Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(Component.text(party_info));
		if (Litestrike.getInstance().game_controller == null) {
			commandSender.sendMessage("Error, can only get player_data if a game is currently running.");
			return Command.SINGLE_SUCCESS;
		}

		try {
			Player p = Bukkit.getPlayer(context.getArgument("player", String.class));
			PlayerData pd = Litestrike.getInstance().game_controller.playerDataManager.get(p);

			commandSender.sendMessage(pd.toString());
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error running the /player_info command\n" + e);
			commandSender.sendMessage("Error running /player_info, see console. Usage: /player_info <player_name>");
		}
		return Command.SINGLE_SUCCESS;

	}
}

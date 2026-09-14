package gg.litestrike.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GameConfigCommand {
	private final GameConfig gameConfig;

	public GameConfigCommand(GameConfig gameConfig) {
		this.gameConfig = gameConfig;
	}

	public LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("game_config")
				.requires(source -> source.getSender().hasPermission("litestrike.command.game"))
				.then(Commands.literal("show").executes(this::runShow))
				.then(Commands.literal("get")
						.then(Commands.argument("key", StringArgumentType.word())
								.suggests((context, builder) -> DebugCommands.suggestMatching(builder, settingKeys()))
								.executes(this::runGet)))
				.then(Commands.literal("set")
						.then(Commands.argument("key", StringArgumentType.word())
								.suggests((context, builder) -> DebugCommands.suggestMatching(builder, settingKeys()))
								.then(Commands.argument("value", StringArgumentType.greedyString())
										.suggests((context, builder) -> {
											if (isBooleanKey(context.getArgument("key", String.class))) {
												return DebugCommands.suggestMatching(builder, List.of("true", "false"));
											}
											return builder.buildFuture();
										})
										.executes(this::runSet))))
				.then(Commands.literal("defaults").executes(this::runDefaults));
	}

	private int runShow(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		sender.sendMessage(Component.text("Game Settings:").color(NamedTextColor.GOLD));
		for (Map.Entry<String, String> entry : gameConfig.getAll().entrySet()) {
			sender.sendMessage(Component.text("  " + entry.getKey() + ": " + entry.getValue())
					.color(NamedTextColor.GRAY));
		}
		return Command.SINGLE_SUCCESS;
	}

	private int runGet(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		String key = context.getArgument("key", String.class);
		String value = gameConfig.get(key);
		if (value == null) {
			sender.sendMessage(Component.text("Unknown key: " + key).color(NamedTextColor.RED));
			return Command.SINGLE_SUCCESS;
		}
		sender.sendMessage(Component.text(key + ": " + value).color(NamedTextColor.GRAY));
		return Command.SINGLE_SUCCESS;
	}

	private int runDefaults(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		gameConfig.defaults();
		sender.sendMessage(Component.text("All settings reset to defaults.").color(NamedTextColor.GREEN));
		return Command.SINGLE_SUCCESS;
	}

	private int runSet(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.getSource().getSender();
		String key = context.getArgument("key", String.class);
		String value = context.getArgument("value", String.class).trim();
		try {
			if (!gameConfig.set(key, value)) {
				sender.sendMessage(Component.text("Unknown key: " + key).color(NamedTextColor.RED));
				return Command.SINGLE_SUCCESS;
			}
		} catch (NumberFormatException e) {
			sender.sendMessage(Component.text("Invalid number for " + key + ": " + value)
					.color(NamedTextColor.RED));
			return Command.SINGLE_SUCCESS;
		}
		sender.sendMessage(Component.text("Set " + key + " = " + gameConfig.get(key))
				.color(NamedTextColor.GREEN));
		return Command.SINGLE_SUCCESS;
	}

	private static List<String> settingKeys() {
		List<String> keys = new ArrayList<>();
		for (GameConfig.Setting setting : GameConfig.Setting.values()) {
			keys.add(setting.toString());
		}
		return keys;
	}

	private static boolean isBooleanKey(String key) {
		for (GameConfig.Setting setting : GameConfig.Setting.values()) {
			if (setting.toString().equals(key)) {
				return setting.isBoolean();
			}
		}
		return false;
	}
}

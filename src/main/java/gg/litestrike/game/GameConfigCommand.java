package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GameConfigCommand implements CommandExecutor, TabCompleter {
	private final GameConfig gameConfig;

	public GameConfigCommand(GameConfig gameConfig) {
		this.gameConfig = gameConfig;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			return false;
		}

		switch (args[0]) {
			case "show":
				return runShow(sender);
			case "get":
				return runGet(sender, args);
			case "set":
				return runSet(sender, args);
			default:
				return false;
		}
	}

	private boolean runShow(CommandSender sender) {
		sender.sendMessage(Component.text("Game Settings:").color(NamedTextColor.GOLD));
		for (Map.Entry<String, String> entry : gameConfig.getAll().entrySet()) {
			sender.sendMessage(Component.text("  " + entry.getKey() + ": " + entry.getValue())
					.color(NamedTextColor.GRAY));
		}
		return true;
	}

	private boolean runGet(CommandSender sender, String[] args) {
		if (args.length < 2) {
			return false;
		}
		String value = gameConfig.get(args[1]);
		if (value == null) {
			sender.sendMessage(Component.text("Unknown key: " + args[1]).color(NamedTextColor.RED));
			return true;
		}
		sender.sendMessage(Component.text(args[1] + ": " + value).color(NamedTextColor.GRAY));
		return true;
	}

	private boolean runSet(CommandSender sender, String[] args) {
		if (args.length < 3) {
			return false;
		}
		String key = args[1];
		String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
		try {
			if (!gameConfig.set(key, value)) {
				sender.sendMessage(Component.text("Unknown key: " + key).color(NamedTextColor.RED));
				return true;
			}
		} catch (NumberFormatException e) {
			sender.sendMessage(Component.text("Invalid number for " + key + ": " + value)
					.color(NamedTextColor.RED));
			return true;
		}
		sender.sendMessage(Component.text("Set " + key + " = " + gameConfig.get(key))
				.color(NamedTextColor.GREEN));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) {
			return filterPartial(args[0], List.of("show", "get", "set"));
		}
		if (args.length == 2 && (args[0].equals("set") || args[0].equals("get"))) {
			return filterPartial(args[1], settingKeys());
		}
		if (args.length == 3 && args[0].equals("set")) {
			String key = args[1];
			if (isBooleanKey(key)) {
				return filterPartial(args[2], List.of("true", "false"));
			}
		}
		return List.of();
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

	private List<String> filterPartial(String partial, List<String> options) {
		List<String> result = new ArrayList<>();
		for (String option : options) {
			if (option.toLowerCase().startsWith(partial.toLowerCase())) {
				result.add(option);
			}
		}
		return result;
	}
}

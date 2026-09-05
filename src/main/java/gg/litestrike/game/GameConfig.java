// all game config variables should have a sensible default
// some of them can be overwritten with variables from the plugin config.yml
// all of them can be set via the command
package gg.litestrike.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

public class GameConfig {
	public enum Setting {
		playersToStart,
		playerCap,
		switchRound,
		preRoundTime,
		runningTime,
		postRoundTime,
		finishTime,
		plantTime,
		breakTime,
		killMoney,
		assistMoney,
		winRoundMoney,
		loseRoundMoney,
		nextRoundMoney,
		lastRoundMoney,
		plantMoney,
		breakMoney,
		launchPadProtection,
		launchPadHighPingProtection,
		leviPadProtection,
		autoJumpPadProtection,
		jumpPadProtection,
		freeShop,
		fastGame,
		manualTeamsEnabled,
		ranked,
		placers,
		breakers;

		public boolean isBoolean() {
			return this == freeShop || this == fastGame || this == manualTeamsEnabled || this == ranked;
		}
	}

	public int playersToStart = 6;
	public int playerCap = 8;
	public int switchRound = 4;
	public int preRoundTime = 20 * 23;
	public int runningTime = 180 * 20;
	public int postRoundTime = 5 * 20;
	public int finishTime = 20 * 12;
	public int plantTime = 20 * 5;
	public int breakTime = 20 * 7;
	public int killMoney = 400;
	public int assistMoney = 50;
	public int winRoundMoney = 700;
	public int loseRoundMoney = 400;
	public int nextRoundMoney = 1000;
	public int lastRoundMoney = 5000;
	public int plantMoney = 0;
	public int breakMoney = 0;
	public int launchPadProtection = 20 * 2;
	public int launchPadHighPingProtection = 20 * 4;
	public int leviPadProtection = 20 * 6;
	public int autoJumpPadProtection = 20 * 7;
	public int jumpPadProtection = 20 * 7;
	public boolean freeShop = false;
	public boolean fastGame = false;
	public boolean manualTeamsEnabled = false;
	public boolean ranked = false;
	public List<String> placers = new ArrayList<>();
	public List<String> breakers = new ArrayList<>();

	public void defaults() {
		playersToStart = 6;
		playerCap = 8;
		switchRound = 4;
		preRoundTime = 20 * 23;
		runningTime = 180 * 20;
		postRoundTime = 5 * 20;
		finishTime = 20 * 12;
		plantTime = 20 * 5;
		breakTime = 20 * 7;
		killMoney = 400;
		assistMoney = 50;
		winRoundMoney = 700;
		loseRoundMoney = 400;
		nextRoundMoney = 1000;
		lastRoundMoney = 5000;
		plantMoney = 0;
		breakMoney = 0;
		launchPadProtection = 20 * 2;
		launchPadHighPingProtection = 20 * 4;
		leviPadProtection = 20 * 6;
		autoJumpPadProtection = 20 * 7;
		jumpPadProtection = 20 * 7;
		freeShop = false;
		fastGame = false;
		manualTeamsEnabled = false;
		ranked = false;
		placers = new ArrayList<>();
		breakers = new ArrayList<>();
	}

	public GameConfig(FileConfiguration config) {
		fastGame = config.getBoolean("fast-game");
		manualTeamsEnabled = config.getBoolean("teams.enable");
		ranked = config.getBoolean("ranked");
		placers = new ArrayList<>(config.getStringList("teams.placers"));
		breakers = new ArrayList<>(config.getStringList("teams.breakers"));

		if (fastGame) {
			applyFastGame();
		}
	}

	private void applyFastGame() {
		preRoundTime = 20 * 5;
		postRoundTime = 1 * 20;
	}

	public boolean set(String key, String value) {
		for (Setting setting : Setting.values()) {
			if (setting.toString().equals(key)) {
				applySetting(setting, value);
				return true;
			}
		}
		return false;
	}

	private void applySetting(Setting setting, String value) {
		switch (setting) {
			case playersToStart -> playersToStart = Integer.parseInt(value);
			case playerCap -> playerCap = Integer.parseInt(value);
			case switchRound -> switchRound = Integer.parseInt(value);
			case preRoundTime -> preRoundTime = Integer.parseInt(value);
			case runningTime -> runningTime = Integer.parseInt(value);
			case postRoundTime -> postRoundTime = Integer.parseInt(value);
			case finishTime -> finishTime = Integer.parseInt(value);
			case plantTime -> plantTime = Integer.parseInt(value);
			case breakTime -> breakTime = Integer.parseInt(value);
			case killMoney -> killMoney = Integer.parseInt(value);
			case assistMoney -> assistMoney = Integer.parseInt(value);
			case winRoundMoney -> winRoundMoney = Integer.parseInt(value);
			case loseRoundMoney -> loseRoundMoney = Integer.parseInt(value);
			case nextRoundMoney -> nextRoundMoney = Integer.parseInt(value);
			case lastRoundMoney -> lastRoundMoney = Integer.parseInt(value);
			case plantMoney -> plantMoney = Integer.parseInt(value);
			case breakMoney -> breakMoney = Integer.parseInt(value);
			case launchPadProtection -> launchPadProtection = Integer.parseInt(value);
			case launchPadHighPingProtection -> launchPadHighPingProtection = Integer.parseInt(value);
			case leviPadProtection -> leviPadProtection = Integer.parseInt(value);
			case autoJumpPadProtection -> autoJumpPadProtection = Integer.parseInt(value);
			case jumpPadProtection -> jumpPadProtection = Integer.parseInt(value);
			case freeShop -> freeShop = Boolean.parseBoolean(value);
			case fastGame -> {
				fastGame = Boolean.parseBoolean(value);
				applyFastGame();
			}
			case manualTeamsEnabled -> manualTeamsEnabled = Boolean.parseBoolean(value);
			case ranked -> ranked = Boolean.parseBoolean(value);
			case placers -> placers = parsePlayerList(value);
			case breakers -> breakers = parsePlayerList(value);
		}
	}

	public String get(String key) {
		for (Setting setting : Setting.values()) {
			if (setting.toString().equals(key)) {
				return getValue(setting);
			}
		}
		return null;
	}

	public Map<String, String> getAll() {
		Map<String, String> map = new LinkedHashMap<>();
		for (Setting setting : Setting.values()) {
			map.put(setting.toString(), getValue(setting));
		}
		return map;
	}

	private String getValue(Setting setting) {
		return switch (setting) {
			case playersToStart -> String.valueOf(playersToStart);
			case playerCap -> String.valueOf(playerCap);
			case switchRound -> String.valueOf(switchRound);
			case preRoundTime -> String.valueOf(preRoundTime);
			case runningTime -> String.valueOf(runningTime);
			case postRoundTime -> String.valueOf(postRoundTime);
			case finishTime -> String.valueOf(finishTime);
			case plantTime -> String.valueOf(plantTime);
			case breakTime -> String.valueOf(breakTime);
			case killMoney -> String.valueOf(killMoney);
			case assistMoney -> String.valueOf(assistMoney);
			case winRoundMoney -> String.valueOf(winRoundMoney);
			case loseRoundMoney -> String.valueOf(loseRoundMoney);
			case nextRoundMoney -> String.valueOf(nextRoundMoney);
			case lastRoundMoney -> String.valueOf(lastRoundMoney);
			case plantMoney -> String.valueOf(plantMoney);
			case breakMoney -> String.valueOf(breakMoney);
			case launchPadProtection -> String.valueOf(launchPadProtection);
			case launchPadHighPingProtection -> String.valueOf(launchPadHighPingProtection);
			case leviPadProtection -> String.valueOf(leviPadProtection);
			case autoJumpPadProtection -> String.valueOf(autoJumpPadProtection);
			case jumpPadProtection -> String.valueOf(jumpPadProtection);
			case freeShop -> String.valueOf(freeShop);
			case fastGame -> String.valueOf(fastGame);
			case manualTeamsEnabled -> String.valueOf(manualTeamsEnabled);
			case ranked -> String.valueOf(ranked);
			case placers -> String.join(",", placers);
			case breakers -> String.join(",", breakers);
		};
	}

	public static List<String> parsePlayerList(String value) {
		List<String> result = new ArrayList<>();
		for (String s : value.split(",")) {
			String trimmed = s.trim();
			if (!trimmed.isEmpty()) {
				result.add(trimmed);
			}
		}
		return result;
	}
}

package gg.litestrike.game;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

public class PlayerDataManager {
	private final Map<String, PlayerData> dataMap = new LinkedHashMap<>();

	public void addPlayer(Player p) {
		dataMap.put(p.getName(), new PlayerData(p));
	}

	public PlayerData get(Player p) {
		return dataMap.get(p.getName());
	}

	public PlayerData get(String name) {
		return dataMap.get(name);
	}

	public Collection<PlayerData> getAll() {
		return dataMap.values();
	}

	// returns player data sorted by score, highest score first
	public List<PlayerData> getSortedByScore() {
		return dataMap.values().stream()
				.sorted(Collections.reverseOrder(new PlayerDataComparator()))
				.collect(Collectors.toList());
	}

	public void clear() {
		dataMap.clear();
	}
}

class PlayerDataComparator implements Comparator<PlayerData> {
	@Override
	public int compare(PlayerData arg0, PlayerData arg1) {
		return Double.compare(arg0.calc_player_score(), arg1.calc_player_score());
	}
}

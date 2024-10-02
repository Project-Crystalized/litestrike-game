package gg.litestrike.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.lang.Exception;

public class Teams {
	public List<Player> placers;
	public List<Player> breakers;

	// constants containing text for the teams names

	public Teams() {
		List<Player> list = new ArrayList<Player>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			list.add(p);
		}
		Collections.shuffle(list);
		int middle = list.size() / 2;

		// if odd, breakers get more
		placers = list.subList(0, middle);
		breakers = list.subList(middle, list.size());
	}

	public Team get_team(Player p) {
		if (placers.contains(p)) {
			return Team.Placer;
		}

		if (breakers.contains(p)) {
			return Team.Breaker;
		}

		Bukkit.getLogger().log(Level.SEVERE, "A player that wasnt in any Team was found");
		Bukkit.getLogger().log(Level.SEVERE, "The Plugin will be disabled!");
		// disable plugin when failure
		Bukkit.getPluginManager().disablePlugin(Litestrike.getInstance());

		throw new RuntimeException(new Exception("player is in no team"));
	}

}
